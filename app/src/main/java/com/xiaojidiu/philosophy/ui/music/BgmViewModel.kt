package com.xiaojidiu.philosophy.ui.music

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaojidiu.philosophy.data.repository.BgmRepository
import kotlinx.coroutines.launch

class BgmViewModel(private val repository: BgmRepository): ViewModel() {
    val state=repository.state
    fun importAudio(uris: List<Uri>)=viewModelScope.launch {repository.importAudio(uris)}
    fun toggle() {if(state.value.playing) repository.pause() else repository.play()}
    fun previewVolume(value: Float)=repository.previewVolume(value)
    fun saveVolume(value: Float)=viewModelScope.launch {repository.saveVolume(value)}
    fun mode(value: String)=viewModelScope.launch {repository.setMode(value)}
    fun select(file: String)=viewModelScope.launch {repository.select(file)}
    fun next()=viewModelScope.launch {repository.next()}
    fun previous()=viewModelScope.launch {repository.previous()}
    fun seek(value: Int)=repository.seek(value)
    fun remove(file: String)=viewModelScope.launch {repository.remove(file)}
}
