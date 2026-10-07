package com.xiaojidiu.philosophy

import com.xiaojidiu.philosophy.data.remote.api.ModelUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelUrlTest {
    @Test fun fullCompletionEndpointBecomesBaseUrl() {assertEquals("https://example.com/v1/",ModelUrl.normalize("https://example.com/v1/chat/completions"))}
    @Test fun existingBaseUrlAndHostnameArePreserved() {assertEquals("https://chat/completions/",ModelUrl.normalize("https://chat/completions"));assertEquals("https://example.com/v1/",ModelUrl.normalize("https://example.com/v1"))}
    @Test(expected=IllegalArgumentException::class) fun blankIsNotADirectModel() {ModelUrl.normalize("")}
}
