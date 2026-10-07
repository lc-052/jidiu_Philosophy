package com.xiaojidiu.philosophy.domain.model

import kotlinx.serialization.Serializable

@Serializable data class BgmTrack(val file: String, val title: String, val duration: Int = 0)
data class BgmState(val title: String = "", val importing: Boolean = false, val loading: Boolean = false,
    val ready: Boolean = false, val playing: Boolean = false, val volume: Float = .35f,
    val loop: Boolean = true, val duration: Int = 0, val position: Int = 0, val error: String? = null,
    val tracks: List<BgmTrack> = emptyList(), val activeFile: String? = null,
    val mode: String = "list", val notice: String? = null)
