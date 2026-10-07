package com.xiaojidiu.philosophy

import com.xiaojidiu.philosophy.data.remote.api.BackendUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class BackendUrlTest {
    @Test fun normalizeAndDemo() {
        assertEquals("",BackendUrl.normalize("  "))
        assertEquals("https://example.com/api/",BackendUrl.normalize(" https://example.com/api "))
        assertEquals("http://10.0.2.2:8080/",BackendUrl.normalize("http://10.0.2.2:8080",true))
    }
    @Test(expected=IllegalArgumentException::class) fun releaseRejectsCleartext() {BackendUrl.normalize("http://127.0.0.1:8080/")}
    @Test(expected=IllegalArgumentException::class) fun rejectProviderCredentialsInUrl() {BackendUrl.normalize("https://user:secret@example.com/")}
    @Test(expected=IllegalArgumentException::class) fun rejectQuery() {BackendUrl.normalize("https://example.com/?key=secret")}
    @Test(expected=IllegalArgumentException::class) fun debugRejectsRemoteHttp() {BackendUrl.normalize("http://example.com/",true)}
}
