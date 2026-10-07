package com.xiaojidiu.philosophy.data.remote.api

import okhttp3.HttpUrl.Companion.toHttpUrl

object ModelUrl {
    fun normalize(value: String, allowLocalHttp: Boolean = false): String {
        require(value.isNotBlank()) {"请填写模型接口地址。"}
        val normalized=BackendUrl.normalize(value,allowLocalHttp).toHttpUrl()
        return if(normalized.encodedPath.endsWith("/chat/completions/")) normalized.newBuilder().encodedPath(normalized.encodedPath.removeSuffix("chat/completions/")).build().toString() else normalized.toString()
    }
}
