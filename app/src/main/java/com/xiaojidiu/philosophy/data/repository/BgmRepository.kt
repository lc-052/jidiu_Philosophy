package com.xiaojidiu.philosophy.data.repository

import android.content.*
import android.media.*
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import com.xiaojidiu.philosophy.data.local.PreferencesRepository
import com.xiaojidiu.philosophy.domain.model.BgmState
import com.xiaojidiu.philosophy.domain.model.BgmTrack
import com.xiaojidiu.philosophy.domain.usecase.BgmQueuePlanner
import com.xiaojidiu.philosophy.util.BoundedAudioCopy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

/** One app-scoped, foreground-only player. Import owns a private copy, never uploads audio. */
class BgmRepository(private val context: Context,private val preferences: PreferencesRepository) {
    private data class Config(val file: String?,val title: String,val volume: Float,val mode: String,val tracks: List<BgmTrack>)
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val _state=MutableStateFlow(BgmState());val state=_state.asStateFlow()
    private val importLock=Mutex()
    private val recovered=CompletableDeferred<Unit>()
    private val folder=File(context.filesDir,"bgm")
    private var player: MediaPlayer?=null
    private var currentFile: String?=null
    private var config=Config(null,"",.35f,"list",emptyList())
    private var foreground=false
    private var wantsPlay=false
    private var resumeAfterChange=false
    private var hasFocus=false
    private var noisyRegistered=false
    private var ticker: Job?=null
    private val audio=context.getSystemService(AudioManager::class.java)
    private val attributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes)
        .setWillPauseWhenDucked(true).setOnAudioFocusChangeListener({change->
            when(change) {
                AudioManager.AUDIOFOCUS_GAIN -> {hasFocus=true;if(foreground && wantsPlay) startPrepared()}
                AudioManager.AUDIOFOCUS_LOSS -> {wantsPlay=false;pausePlayer();abandonFocus()}
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {hasFocus=false;pausePlayer()}
            }
        },Handler(Looper.getMainLooper())).build()
    private val noisy=object: BroadcastReceiver() {
        override fun onReceive(context: Context?,intent: Intent?) {if(intent?.action==AudioManager.ACTION_AUDIO_BECOMING_NOISY) pause()}
    }
    init {
        scope.launch {
            try {
                val pending=preferences.pendingBgm()
                if(pending!=null && preferences.settings.first().bgmTracks.none {it.file==pending}) withContext(Dispatchers.IO) {runCatching {ownedFile(pending).delete()}}
                preferences.setPendingBgm(null)
            } catch(e: CancellationException) {throw e} catch(e: Exception) {_state.update {it.copy(error="上次音频导入未完成，请重新选择文件。")}}
            finally {recovered.complete(Unit)}
            preferences.settings.map {Config(it.bgmFile,it.bgmName,it.bgmVolume,it.bgmMode,it.bgmTracks)}.distinctUntilChanged().collect {next->configure(next)}
        }
    }
    private fun ownedFile(name: String): File {
        require(Regex("[a-f0-9-]+\\.audio").matches(name)) {"音频记录无效，请重新导入。"}
        return File(folder,name)
    }
    private suspend fun configure(next: Config) {
        config=next
        if(currentFile==next.file) {
            _state.update {it.copy(volume=next.volume,mode=next.mode,tracks=next.tracks,loop=shouldLoop())}
            if(_state.value.ready) player?.apply {setVolume(next.volume,next.volume);isLooping=shouldLoop()}
            return
        }
        val resume=resumeAfterChange;resumeAfterChange=false
        releasePlayer();currentFile=next.file;wantsPlay=resume
        _state.value=BgmState(title=next.title,volume=next.volume,mode=next.mode,tracks=next.tracks,activeFile=next.file,
            loop=shouldLoop(),loading=next.file!=null,importing=_state.value.importing,notice=_state.value.notice)
        if(next.file==null) return
        val mp=MediaPlayer();player=mp
        try {
            mp.setAudioAttributes(attributes)
            mp.setOnPreparedListener {prepared->if(player===prepared) {
                prepared.isLooping=shouldLoop();prepared.setVolume(config.volume,config.volume)
                _state.update {it.copy(loading=false,ready=true,duration=prepared.duration,error=null)}
                if(wantsPlay && foreground) play()
                ticker?.cancel();ticker=scope.launch {
                    while(player===prepared) {
                        if(foreground && _state.value.playing) _state.update {it.copy(position=prepared.currentPosition)}
                        delay(500)
                    }
                }
            }}
            mp.setOnCompletionListener {if(player===it) {
                val index=BgmQueuePlanner.next(config.tracks.size,config.tracks.indexOfFirst {track->track.file==currentFile},config.mode,true)
                if(index!=null) scope.launch {select(config.tracks[index].file,true)}
                else {wantsPlay=false;_state.update {s->s.copy(playing=false,position=s.duration)};abandonFocus();unregisterNoisy()}
            }}
            mp.setOnErrorListener {failed,_,_->if(player===failed) failPlayback("这个音频无法播放，请重新导入设备支持的音频文件。");true}
            withContext(Dispatchers.IO) {val file=ownedFile(next.file);require(file.isFile) {"本地音频已不可用，请重新导入。"};mp.setDataSource(file.absolutePath)}
            mp.prepareAsync()
        } catch(e: CancellationException) {if(player===mp) releasePlayer();throw e}
        catch(e: Exception) {if(player===mp) failPlayback(e.message?.takeIf {e is IllegalArgumentException} ?: "本地音频无法读取，请重新导入。")}
    }
    private fun shouldLoop()=config.mode=="single" || (config.mode=="list" && config.tracks.size==1)
    suspend fun importAudio(uri: Uri)=importAudio(listOf(uri))
    suspend fun importAudio(uris: List<Uri>) {
        recovered.await();if(uris.isEmpty() || !importLock.tryLock()) return
        _state.update {it.copy(importing=true,error=null,notice=null)}
        var added=0;var failed=0;var failure: String?=null
        try {
            for(uri in uris.distinct()) {
                var imported: File?=null;var committed=false
                try {
                    require(preferences.settings.first().bgmTracks.size<50) {"播放列表最多收下 50 首音乐。"}
                    val track=withContext(Dispatchers.IO) {
                        require(uri.scheme=="content") {"请通过系统文件选择器选择本地音频。"}
                        val title=context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use {cursor->if(cursor.moveToFirst()) cursor.getString(0) else null}?.take(200)?.takeIf {it.isNotBlank()} ?: "我的阅读 BGM"
                        check(folder.isDirectory || folder.mkdirs())
                        val file=ownedFile("${UUID.randomUUID()}.audio");imported=file;preferences.setPendingBgm(file.name)
                        context.contentResolver.openInputStream(uri)?.use {input->file.outputStream().use {output->
                            val job=currentCoroutineContext();BoundedAudioCopy.copy(input,output) {job.ensureActive()}
                        }} ?: error("无法读取所选文件")
                        val metadata=MediaMetadataRetriever()
                        try {
                            metadata.setDataSource(file.absolutePath)
                            val duration=metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull() ?: 0
                            require(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)=="yes" && duration>0) {"这不是设备支持的音频文件，请选择 MP3、M4A、OGG 或 WAV 等音频。"}
                            BgmTrack(file.name,title,duration)
                        } finally {metadata.release()}
                    }
                    withContext(NonCancellable) {preferences.addBgmTrack(track);committed=true};added++
                } catch(e: CancellationException) {throw e}
                catch(e: Exception) {failed++;failure=if(e is IllegalArgumentException) e.message else "音频导入失败，请确认文件可以读取，并检查设备剩余空间。"}
                finally {if(!committed) withContext(NonCancellable+Dispatchers.IO) {imported?.delete();preferences.setPendingBgm(null)}}
            }
            _state.update {it.copy(error=if(failed>0) failure else null,notice="已加入 $added 首音乐"+if(failed>0) "，$failed 首未能导入。" else "。")}
        } finally {_state.update {it.copy(importing=false)};importLock.unlock()}
    }
    suspend fun select(file: String,autoplay: Boolean=true) {
        if(config.tracks.none {it.file==file}) return
        if(file==currentFile) {if(autoplay) play();return}
        resumeAfterChange=autoplay;preferences.selectBgmTrack(file)
    }
    suspend fun next() {BgmQueuePlanner.next(config.tracks.size,config.tracks.indexOfFirst {it.file==currentFile},config.mode,false)?.let {select(config.tracks[it].file,wantsPlay)}}
    suspend fun previous() {if(config.tracks.isNotEmpty()) select(config.tracks[(config.tracks.indexOfFirst {it.file==currentFile}-1+config.tracks.size)%config.tracks.size].file,wantsPlay)}
    suspend fun remove(file: String)=importLock.withLock {
        if(config.tracks.none {it.file==file}) return@withLock
        if(file==currentFile) {resumeAfterChange=wantsPlay;releasePlayer()}
        preferences.removeBgmTrack(file)
        withContext(Dispatchers.IO) {ownedFile(file).delete()}
    }
    fun setForeground(value: Boolean) {
        foreground=value
        if(!value) {pausePlayer();abandonFocus();unregisterNoisy()}
        else if(wantsPlay && _state.value.ready && !_state.value.playing) play()
    }
    fun play() {
        if(!foreground || !_state.value.ready) return
        if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            wantsPlay=false;_state.update {it.copy(error="暂时无法播放，其他音频可能正在占用声音。稍后再试一下。")};return
        }
        wantsPlay=true;hasFocus=true;startPrepared()
    }
    private fun startPrepared() {
        if(!foreground || !hasFocus || !_state.value.ready || !wantsPlay) return
        try {
            if(_state.value.position>=_state.value.duration) player?.seekTo(0)
            player?.start();_state.update {it.copy(playing=true,error=null)}
            if(!noisyRegistered) {ContextCompat.registerReceiver(context,noisy,IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),ContextCompat.RECEIVER_EXPORTED);noisyRegistered=true}
        } catch(e: Exception) {failPlayback("播放中断，请重新导入音频。")}
    }
    fun pause() {wantsPlay=false;pausePlayer();abandonFocus();unregisterNoisy()}
    private fun pausePlayer() {if(_state.value.ready && _state.value.playing) runCatching {player?.pause()};_state.update {it.copy(playing=false)}}
    private fun abandonFocus() {audio.abandonAudioFocusRequest(focus);hasFocus=false}
    private fun unregisterNoisy() {if(noisyRegistered) {context.unregisterReceiver(noisy);noisyRegistered=false}}
    fun seek(position: Int) {if(_state.value.ready) {player?.seekTo(position.toLong().coerceIn(0,_state.value.duration.toLong()),MediaPlayer.SEEK_CLOSEST);_state.update {it.copy(position=position.coerceIn(0,it.duration))}}}
    fun previewVolume(value: Float) {val volume=value.coerceIn(0f,1f);if(_state.value.ready) player?.setVolume(volume,volume);_state.update {it.copy(volume=volume)}}
    suspend fun saveVolume(value: Float) {preferences.setBgmVolume(value.coerceIn(0f,1f))}
    suspend fun setMode(value: String) {preferences.setBgmMode(value)}
    suspend fun clear()=importLock.withLock {
        pause();val files=preferences.settings.first().bgmTracks.map {it.file};releasePlayer();preferences.clearBgm()
        withContext(Dispatchers.IO) {for(file in files) runCatching {ownedFile(file).delete()}}
    }
    private fun failPlayback(message: String) {releasePlayer();_state.update {it.copy(loading=false,ready=false,playing=false,error=message)}}
    private fun releasePlayer() {
        wantsPlay=false;ticker?.cancel();ticker=null;abandonFocus();unregisterNoisy()
        player?.release();player=null;_state.update {it.copy(ready=false,playing=false)}
    }
}
