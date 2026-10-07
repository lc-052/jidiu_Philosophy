package com.xiaojidiu.philosophy.data.remote.api
import retrofit2.http.*
import com.xiaojidiu.philosophy.data.remote.dto.*
interface AiApiService {
    @POST("round-table") suspend fun discuss(@Body request: RoundTableRequest): RoundTableResponse
    @GET("health") suspend fun health(): BackendHealth
}
