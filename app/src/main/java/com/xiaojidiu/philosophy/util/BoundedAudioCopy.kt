package com.xiaojidiu.philosophy.util

import java.io.InputStream
import java.io.OutputStream

object BoundedAudioCopy {
    const val MAX_BYTES=100L*1024*1024
    fun copy(input: InputStream, output: OutputStream, limit: Long=MAX_BYTES, checkActive: ()->Unit = {}): Long {
        val buffer=ByteArray(32*1024);var size=0L
        while(true) {
            checkActive()
            val count=input.read(buffer)
            if(count<0) break
            if(count==0) continue
            size+=count
            require(size<=limit) {"音频超过 100 MB，请选择小一些的文件。"}
            output.write(buffer,0,count)
        }
        require(size>0) {"这个音频文件是空的，请重新选择。"}
        return size
    }
}
