package com.xiaojidiu.philosophy.util

/** Display-only paragraph breaks. Stored and transmitted messages remain untouched. */
object DiscussionParagraphs {
    fun split(text: String): List<String> = text.replace("\r\n","\n").split(Regex("\n+"))
        .filter {it.isNotBlank()}.flatMap {block->
            if(block.length<=110) return@flatMap listOf(block)
            val sentences=Regex("[^。！？!?]*[。！？!?]+[”’\"）)]*|[^。！？!?]+$").findAll(block).map {it.value}.toList()
            val result=mutableListOf<String>();var current=""
            for(sentence in sentences) {
                if(current.isNotEmpty() && current.length+sentence.length>90) {result+=current;current=""}
                current+=sentence
            }
            if(current.isNotEmpty()) result+=current
            result.ifEmpty {listOf(block)}
        }
}
