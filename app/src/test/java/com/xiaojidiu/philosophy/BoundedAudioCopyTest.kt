package com.xiaojidiu.philosophy

import com.xiaojidiu.philosophy.util.BoundedAudioCopy
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.*
import org.junit.Test

class BoundedAudioCopyTest {
    @Test fun copiesOriginalBytesWithoutModification() {
        val data=ByteArray(70000) {(it%251).toByte()};val output=ByteArrayOutputStream()
        assertEquals(data.size.toLong(),BoundedAudioCopy.copy(ByteArrayInputStream(data),output,100000))
        assertArrayEquals(data,output.toByteArray())
    }
    @Test fun exactLimitIsAllowed() {assertEquals(10L,BoundedAudioCopy.copy(ByteArrayInputStream(ByteArray(10)),ByteArrayOutputStream(),10))}
    @Test(expected=IllegalArgumentException::class) fun rejectsFileLargerThanLimit() {BoundedAudioCopy.copy(ByteArrayInputStream(ByteArray(11)),ByteArrayOutputStream(),10)}
    @Test(expected=IllegalArgumentException::class) fun rejectsEmptyAudio() {BoundedAudioCopy.copy(ByteArrayInputStream(byteArrayOf()),ByteArrayOutputStream())}
}
