package com.xiaojidiu.philosophy.data.remote.api

import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

class ModelConfigurationException(message: String): IllegalArgumentException(message)

object AiErrorMessages {
    fun describe(error: Exception): String = when(error) {
        is ModelConfigurationException -> error.message ?: "请重新检查模型配置。"
        is HttpException -> when(error.code()) {
            401 -> "API Key 无效或已过期，请重新填写并保存。"
            403 -> "没有模型调用权限，请核对 API Key、地域和业务空间。"
            404 -> "找不到接口或模型，请检查接口地址与模型名称。"
            429 -> "调用额度或频率受限，请检查供应商账户额度，稍后重试。"
            400,422 -> "模型不支持当前请求，请核对模型名称和 JSON 输出能力。"
            in 300..399 -> "接口发生跳转，请填写供应商给出的最终兼容接口地址。"
            else -> "模型服务暂时不可用（${error.code()}），请稍后重试。"
        }
        is SerializationException -> "模型返回格式不符合圆桌协议，请更换支持 JSON 输出的模型或重试。"
        is IOException -> "网络连接失败，请检查网络及接口地址后重试。"
        else -> "会议未能完成，请检查模型配置后重试。"
    }
}
