package com.xiaojidiu.philosophy.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import java.io.IOException
import com.xiaojidiu.philosophy.domain.model.BgmTrack
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import com.xiaojidiu.philosophy.BuildConfig

private val Context.preferences by preferencesDataStore("little_thoughts")
data class SettingsState(val onboarding: Boolean = false, val theme: String = "system", val reminder: Boolean = false, val hour: Int = 20, val minute: Int = 0, val aiBaseUrl: String = BuildConfig.AI_API_BASE_URL, val aiMode: String = if(BuildConfig.AI_API_BASE_URL.isBlank()) "demo" else "backend", val modelBaseUrl: String = "https://dashscope.aliyuncs.com/compatible-mode/v1/", val modelName: String = "qwen-plus", val modelProvider: String = "qwen", val nickname: String = "我", val avatar: String = "sprout", val bgmFile: String? = null, val bgmName: String = "", val bgmVolume: Float = .35f, val bgmLoop: Boolean = true, val bgmTracks: List<BgmTrack> = emptyList(), val bgmMode: String = "list")
class PreferencesRepository(private val context: Context, private val apiKeys: ApiKeyStore = ApiKeyStore(context)) {
    private val bgmPlaylist=stringPreferencesKey("bgmPlaylist")
    private val bgmMode=stringPreferencesKey("bgmMode")
    private fun tracks(p: Preferences): List<BgmTrack> = p[bgmPlaylist]?.let { Json.decodeFromString<List<BgmTrack>>(it) } ?: p[bgmFile]?.let { listOf(BgmTrack(it,p[bgmName] ?: "我的阅读 BGM")) } ?: emptyList()
    private val bgmPending=stringPreferencesKey("bgmPending")
    private val bgmFile=stringPreferencesKey("bgmFile")
    private val bgmName=stringPreferencesKey("bgmName")
    private val bgmVolume=floatPreferencesKey("bgmVolume")
    private val bgmLoop=booleanPreferencesKey("bgmLoop")
    private val nickname = stringPreferencesKey("nickname")
    private val avatar = stringPreferencesKey("avatar")
    private val onboarding = booleanPreferencesKey("hasCompletedOnboarding")
    private val theme = stringPreferencesKey("theme")
    private val reminder = booleanPreferencesKey("reminder")
    private val hour = intPreferencesKey("hour")
    private val minute = intPreferencesKey("minute")
    private val seed = intPreferencesKey("seedVersion")
    private val aiBaseUrl = stringPreferencesKey("aiBaseUrl")
    private val aiMode = stringPreferencesKey("aiMode")
    private val modelBaseUrl = stringPreferencesKey("modelBaseUrl")
    private val modelName = stringPreferencesKey("modelName")
    private val modelProvider = stringPreferencesKey("modelProvider")
    val settings: Flow<SettingsState> = context.preferences.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map {
        val backend=it[aiBaseUrl] ?: BuildConfig.AI_API_BASE_URL
        SettingsState(it[onboarding] ?: false, it[theme] ?: "system", it[reminder] ?: false, it[hour] ?: 20, it[minute] ?: 0, backend, it[aiMode] ?: if(backend.isBlank()) "demo" else "backend", it[modelBaseUrl] ?: "https://dashscope.aliyuncs.com/compatible-mode/v1/", it[modelName] ?: "qwen-plus", it[modelProvider] ?: "qwen", it[nickname] ?: "我", it[avatar] ?: "sprout",it[bgmFile],it[bgmName] ?: "",(it[bgmVolume] ?: .35f).coerceIn(0f,1f),it[bgmLoop] ?: true,tracks(it),it[bgmMode] ?: if(it[bgmLoop]==false) "sequential" else "list")
    }
    suspend fun setBgm(file: String,name: String) {context.preferences.edit {it[bgmFile]=file;it[bgmName]=name;it.remove(bgmPlaylist);it.remove(bgmPending)}}
    suspend fun pendingBgm(): String? = context.preferences.data.first()[bgmPending]
    suspend fun setPendingBgm(name: String?) {context.preferences.edit {if(name==null) it.remove(bgmPending) else it[bgmPending]=name}}
    suspend fun setBgmVolume(value: Float) {context.preferences.edit {it[bgmVolume]=value.coerceIn(0f,1f)}}
    suspend fun setBgmLoop(value: Boolean) {context.preferences.edit {it[bgmLoop]=value}}
    suspend fun addBgmTrack(track: BgmTrack) {context.preferences.edit {
        val list=tracks(it);require(list.size<50) {"播放列表最多收下 50 首音乐。"}
        it[bgmPlaylist]=Json.encodeToString(list+track)
        if(it[bgmFile]==null) {it[bgmFile]=track.file;it[bgmName]=track.title}
        it.remove(bgmPending)
    }}
    suspend fun selectBgmTrack(file: String) {context.preferences.edit {p->tracks(p).find {it.file==file}?.let {p[bgmFile]=it.file;p[bgmName]=it.title}}}
    suspend fun removeBgmTrack(file: String) {context.preferences.edit {p->
        val old=tracks(p);val list=old.filterNot {it.file==file};p[bgmPlaylist]=Json.encodeToString(list)
        if(p[bgmFile]==file) {
            val next=list.getOrNull(old.indexOfFirst {it.file==file}.coerceAtLeast(0)) ?: list.firstOrNull()
            if(next==null) {p.remove(bgmFile);p.remove(bgmName)} else {p[bgmFile]=next.file;p[bgmName]=next.title}
        }
    }}
    suspend fun setBgmMode(value: String) {require(value in listOf("list","single","sequential"));context.preferences.edit {it[bgmMode]=value}}
    suspend fun clearBgm() {context.preferences.edit {it.remove(bgmFile);it.remove(bgmName);it[bgmPlaylist]="[]"}}
    suspend fun setIdentity(name: String, image: String) { require(name.isNotBlank() && name.length<=20); require(image in listOf("sprout","moon","cat","cloud","sun","planet")); context.preferences.edit {it[nickname]=name.trim();it[avatar]=image} }
    suspend fun completeOnboarding() { context.preferences.edit { it[onboarding] = true } }
    suspend fun setTheme(value: String) { context.preferences.edit { it[theme] = value } }
    suspend fun setAiBaseUrl(value: String) { context.preferences.edit { it[aiBaseUrl] = value; it[aiMode]=if(value.isBlank()) "demo" else "backend" } }
    suspend fun setDirectModel(url: String, model: String, provider: String) {context.preferences.edit {it[modelBaseUrl]=url;it[modelName]=model;it[modelProvider]=provider;it[aiMode]="direct"}}
    suspend fun setDemoMode() {context.preferences.edit {it[aiMode]="demo"}}
    suspend fun setReminder(enabled: Boolean, h: Int, m: Int) { context.preferences.edit { it[reminder] = enabled; it[hour] = h; it[minute] = m } }
    suspend fun seedVersion() = context.preferences.data.first()[seed] ?: 0
    suspend fun setSeedVersion(value: Int) { context.preferences.edit { it[seed] = value } }
    suspend fun reset() { apiKeys.clear();context.preferences.edit { it.clear() } }
}

