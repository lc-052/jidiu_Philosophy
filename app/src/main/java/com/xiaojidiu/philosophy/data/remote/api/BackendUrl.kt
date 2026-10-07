package com.xiaojidiu.philosophy.data.remote.api

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object BackendUrl {
    fun normalize(value: String, allowLocalHttp: Boolean = false): String {
        if(value.isBlank()) return ""
        val url=value.trim().toHttpUrlOrNull() ?: throw IllegalArgumentException("请输入完整的服务地址。")
        require(url.username.isEmpty() && url.password.isEmpty() && url.query==null && url.fragment==null) {"服务地址不能包含账号、密码、查询参数或片段。"}
        require(url.isHttps || allowLocalHttp && url.host in setOf("10.0.2.2","127.0.0.1","localhost")) {"服务地址必须使用 HTTPS；Debug 版仅允许本机调试地址使用 HTTP。"}
        return url.toString().let {if(it.endsWith('/')) it else "$it/"}
    }
}
