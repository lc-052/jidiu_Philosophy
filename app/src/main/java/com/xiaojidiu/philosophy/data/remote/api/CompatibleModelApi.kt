package com.xiaojidiu.philosophy.data.remote.api

import com.xiaojidiu.philosophy.data.remote.dto.*
import retrofit2.http.*

interface CompatibleModelApi {
    @POST("chat/completions") suspend fun complete(@Header("Authorization") authorization: String, @Body request: ChatCompletionRequest): ChatCompletionResponse
}
