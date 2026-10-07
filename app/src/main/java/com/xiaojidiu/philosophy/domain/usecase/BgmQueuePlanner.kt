package com.xiaojidiu.philosophy.domain.usecase

object BgmQueuePlanner {
    fun next(size: Int, current: Int, mode: String, automatic: Boolean): Int? {
        if(size<=0 || current !in 0 until size) return null
        if(automatic && mode=="single") return current
        if(automatic && mode=="sequential" && current==size-1) return null
        return (current+1)%size
    }
}
