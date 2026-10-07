package com.xiaojidiu.philosophy.data.remote.dto
object ResponseValidator {
    fun validate(response: RoundTableResponse,selected: List<String>,names: Map<String,String>,contextIds: Set<String>, summaryOnly: Boolean = false): RoundTableResponse {
        require(response.topic.isNotBlank() && response.topic.length<=1000 && (if(summaryOnly) response.messages.isEmpty() && response.summary!=null else response.messages.size in 1..24)) {"讨论返回了不完整的数据"}
        val ids=response.messages.map {it.id}
        require(ids.distinct().size==ids.size && ids.none {it in contextIds}) {"消息标识重复"}
        require(response.messages.all {it.id.isNotBlank() && it.id.length<=200 && it.philosopherId in selected && it.content.isNotBlank() && it.content.length<=6000 && it.type in listOf("argument","reply","summary")}) {"讨论返回了不支持的结构"}
        require(response.messages.all {it.responseTo==null || it.responseTo in contextIds || it.responseTo in ids}) {"找不到消息回应的对象"}
        response.summary?.let { summary->
            require(summary.disagreement.size in 1..4 && summary.commonGround.size<=4)
            (summary.disagreement+summary.commonGround).forEach {point->
                require(point.text.isNotBlank() && point.text.length<=500 && point.messageIds.size in 1..6 && point.messageIds.all {it in contextIds || it in ids}) {"小结缺少有效的发言依据"}
            }
        }
        return response.copy(messages=response.messages.map {it.copy(displayName=names[it.philosopherId] ?: error("未知参与者"))})
    }
}
