package com.xiaojidiu.philosophy

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.ClipData
import android.media.*
import android.net.Uri
import android.graphics.Bitmap
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.geometry.Offset
import androidx.test.platform.app.InstrumentationRegistry
import com.xiaojidiu.philosophy.domain.model.BgmState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class BgmExperienceTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val app get()=ApplicationProvider.getApplicationContext<ThoughtsApplication>()
    private val graph get()=app.graph
    private fun waitState(predicate: (BgmState)->Boolean)=compose.waitUntil(15000) {predicate(graph.bgm.state.value)}
    private fun waitText(text: String)=compose.waitUntil(15000) {compose.onAllNodesWithText(text,substring=true).fetchSemanticsNodes().isNotEmpty()}
    private fun importThroughUi() {
        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK,Intent().setData(Uri.parse("content://com.xiaojidiu.philosophy.test.audio/tone")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)))
        waitText("今天脑子");compose.onNodeWithTag("tab_profile").performClick()
        compose.onNodeWithTag("profile_list").performScrollToNode(hasText("阅读 BGM"));compose.onNodeWithText("阅读 BGM").performClick()
        compose.onNodeWithTag("bgm_import").performClick()
        compose.waitUntil(15000) {graph.bgm.state.value.let {it.ready && !it.importing || it.error!=null}}
        assertNull("Import error",graph.bgm.state.value.error);waitState {it.ready && !it.importing}
        assertEquals("xjd-test-tone.wav",graph.bgm.state.value.title)
        assertTrue(Intents.getIntents().any {it.action==Intent.ACTION_OPEN_DOCUMENT && it.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)?.contains("audio/*")==true})
    }
    private fun screenshot(name: String) {
        val image=File(app.getExternalFilesDir(null),"screenshots/$name.png").apply {parentFile!!.mkdirs()}
        image.outputStream().use {compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.executeShellCommand("mkdir -p /sdcard/Download/little-thoughts-screenshots").use {FileInputStream(it.fileDescriptor).readBytes()}
        automation.executeShellCommand("cp ${image.absolutePath} /sdcard/Download/little-thoughts-screenshots/$name.png").use {FileInputStream(it.fileDescriptor).readBytes()}
    }
    private fun reset()=runBlocking {withContext(Dispatchers.Main) {graph.bgm.clear()};graph.preferences.reset();graph.preferences.completeOnboarding()}
    @Test fun importPlayLoopRotateVolumeBackgroundAndRemove() {
        reset();Intents.init()
        val scenario=ActivityScenario.launch(MainActivity::class.java)
        try {
            importThroughUi();screenshot("bgm")
            val name=runBlocking {graph.preferences.settings.first().bgmFile!!}
            val copy=File(app.filesDir,"bgm/$name");assertTrue(copy.isFile)
            compose.onNodeWithTag("bgm_list").performScrollToNode(hasTestTag("bgm_play"));compose.onNodeWithTag("bgm_play").performClick()
            waitState {it.playing && it.position>0}
            scenario.recreate();waitText("阅读 BGM");waitState {it.playing};assertEquals(name,runBlocking {graph.preferences.settings.first().bgmFile})
            compose.onNodeWithTag("bgm_list").performScrollToNode(hasTestTag("bgm_volume"));compose.onNodeWithTag("bgm_volume").performTouchInput {click(Offset(width*.25f,height/2f))}
            compose.waitUntil(5000) {runBlocking {graph.preferences.settings.first().bgmVolume<.3f}}
            scenario.moveToState(Lifecycle.State.CREATED);waitState {!it.playing}
            scenario.moveToState(Lifecycle.State.RESUMED);waitState {it.playing}
            compose.onNodeWithTag("bgm_list").performScrollToNode(hasTestTag("bgm_mode_sequential"));compose.onNodeWithTag("bgm_mode_sequential").performClick()
            compose.waitUntil(5000) {runBlocking {graph.preferences.settings.first().bgmMode=="sequential"}}
            waitState {!it.playing && it.position==it.duration}
            compose.onNodeWithContentDescription("返回").performClick();waitText("我的脑回路")
            compose.onNodeWithTag("bgm_mini_toggle").performClick();waitState {it.playing}
            compose.onNodeWithTag("bgm_mini_title").performClick();waitText("阅读 BGM")
            runBlocking {graph.preferences.setTheme("dark")};scenario.recreate();waitText("阅读 BGM");compose.waitForIdle();screenshot("bgm-dark")
            compose.onNodeWithTag("bgm_list").performScrollToNode(hasTestTag("bgm_remove"));compose.onNodeWithTag("bgm_remove").performClick()
            compose.onNodeWithText("确认移除").performClick();waitState {it.title.isEmpty() && !it.ready}
            compose.waitUntil(5000) {!copy.exists()}
            app.contentResolver.openInputStream(Uri.parse("content://com.xiaojidiu.philosophy.test.audio/tone"))!!.close() // Original provider file is retained.
        } finally {scenario.close();Intents.release();reset()}
    }
    @Test fun playlistImportsMultipleSwitchesAutomaticallyAndRemovesOnlySelectedTrack() {
        reset();Intents.init();val scenario=ActivityScenario.launch(MainActivity::class.java)
        try {
            val first=Uri.parse("content://com.xiaojidiu.philosophy.test.audio/tone")
            val second=Uri.parse("content://com.xiaojidiu.philosophy.test.audio/tone-b")
            val invalid=Uri.parse("content://com.xiaojidiu.philosophy.test.audio/invalid")
            val result=Intent().apply {clipData=ClipData.newUri(app.contentResolver,"music",first).apply {addItem(ClipData.Item(second));addItem(ClipData.Item(invalid))}}.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK,result))
            waitText("今天脑子");compose.onNodeWithTag("tab_profile").performClick()
            compose.onNodeWithTag("profile_list").performScrollToNode(hasText("阅读 BGM"));compose.onNodeWithText("阅读 BGM").performClick()
            compose.onNodeWithTag("bgm_import").performClick();waitState {it.tracks.size==2 && !it.importing && it.ready}
            assertNotNull(graph.bgm.state.value.error);assertTrue(graph.bgm.state.value.notice!!.contains("1 首未能导入"))
            val tracks=graph.bgm.state.value.tracks;val files=tracks.map {File(app.filesDir,"bgm/${it.file}")};assertTrue(files.all {it.isFile})
            assertTrue(Intents.getIntents().any {it.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,false)})
            compose.onNodeWithTag("bgm_list").performScrollToNode(hasTestTag("bgm_play"));compose.onNodeWithTag("bgm_play").performClick()
            waitState {it.playing && it.activeFile==tracks[1].file}
            assertTrue(files.all {it.isFile}) // Changing a song must not delete its predecessor.
            scenario.recreate();waitText("阅读 BGM");assertEquals(2,runBlocking {graph.preferences.settings.first().bgmTracks.size})
            compose.onNodeWithTag("bgm_list").performScrollToNode(hasTestTag("bgm_mode_single"));compose.onNodeWithTag("bgm_mode_single").performClick()
            compose.waitUntil(5000) {graph.bgm.state.value.mode=="single"}
            compose.onNodeWithTag("bgm_list").performScrollToNode(hasTestTag("bgm_previous"));compose.onNodeWithTag("bgm_previous").performClick()
            waitState {it.ready && it.playing && it.activeFile==tracks[0].file}
            val started=System.currentTimeMillis();compose.waitUntil(6000) {System.currentTimeMillis()-started>3500}
            assertEquals(tracks[0].file,graph.bgm.state.value.activeFile);assertTrue(graph.bgm.state.value.playing)
            compose.onNodeWithTag("bgm_next").performClick();waitState {it.ready && it.playing && it.activeFile==tracks[1].file}
            compose.onNodeWithTag("bgm_list").performScrollToNode(hasTestTag("bgm_track_${tracks[0].file}"));compose.onNodeWithTag("bgm_track_${tracks[0].file}").performClick()
            waitState {it.ready && it.playing && it.activeFile==tracks[0].file};screenshot("bgm-playlist")
            compose.onNodeWithTag("bgm_remove_${tracks[0].file}").performClick();compose.onNodeWithText("确认移除").performClick()
            waitState {it.tracks.size==1 && it.ready && it.activeFile==tracks[1].file}
            assertFalse(files[0].exists());assertTrue(files[1].exists())
        } finally {scenario.close();Intents.release();reset()}
    }
    @Test fun legacySingleTrackIsRetainedWhenAddingPlaylistTrack() {
        reset()
        val first="11111111-1111-1111-1111-111111111111.audio"
        runBlocking {graph.preferences.setBgm(first,"原来的音乐")}
        assertEquals(listOf(first),runBlocking {graph.preferences.settings.first().bgmTracks.map {it.file}})
        runBlocking {graph.preferences.addBgmTrack(com.xiaojidiu.philosophy.domain.model.BgmTrack("22222222-2222-2222-2222-222222222222.audio","新音乐"))}
        assertEquals(2,runBlocking {graph.preferences.settings.first().bgmTracks.size})
        assertEquals(first,runBlocking {graph.preferences.settings.first().bgmFile})
        reset()
    }
    @Test fun focusInterruptionResumesAndInvalidImportKeepsPreviousMusic() {
        reset();Intents.init()
        val scenario=ActivityScenario.launch(MainActivity::class.java)
        try {
            importThroughUi();compose.onNodeWithTag("bgm_list").performScrollToNode(hasTestTag("bgm_play"));compose.onNodeWithTag("bgm_play").performClick();waitState {it.playing}
            val audio=app.getSystemService(AudioManager::class.java)
            val temporary=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()).setOnAudioFocusChangeListener {}.build()
            try {
                assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED,audio.requestAudioFocus(temporary));waitState {!it.playing}
            } finally {audio.abandonAudioFocusRequest(temporary)}
            waitState {it.playing}
            val previous=runBlocking {graph.preferences.settings.first().bgmFile}
            runBlocking {withContext(Dispatchers.Main) {graph.bgm.importAudio(Uri.parse("content://com.xiaojidiu.philosophy.test.audio/invalid"))}}
            assertEquals(previous,runBlocking {graph.preferences.settings.first().bgmFile})
            assertTrue(graph.bgm.state.value.ready);assertNotNull(graph.bgm.state.value.error)
        } finally {scenario.close();Intents.release();reset()}
    }
}
