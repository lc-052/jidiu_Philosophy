package com.xiaojidiu.philosophy

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xiaojidiu.philosophy.domain.model.DebateMessage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class AiConnectionTest {
    @Test fun savedEndpointHealthRealRepliesAndContext()=runBlocking {
        val graph=ApplicationProvider.getApplicationContext<ThoughtsApplication>().graph
        val previous=graph.preferences.settings.first().aiBaseUrl
        val received=CopyOnWriteArrayList<JsonObject>()
        val server=ServerSocket(0,4,InetAddress.getByName("127.0.0.1"))
        server.soTimeout=15000
        val worker=thread {
            repeat(3) { index ->
                server.accept().use {socket->
                    socket.soTimeout=10000
                    val stream=socket.getInputStream().buffered()
                    fun readLine(): String {
                        val bytes=ByteArrayOutputStream()
                        while(true) {val next=stream.read();check(next>=0);if(next==10) break;if(next!=13) bytes.write(next)}
                        return bytes.toString("UTF-8")
                    }
                    val requestLine=readLine()
                    var length=0
                    while(true) {
                        val line=readLine()
                        if(line.isEmpty()) break
                        if(line.startsWith("Content-Length:",true)) length=line.substringAfter(':').trim().toInt()
                    }
                    if(length>0) {
                        val body=ByteArray(length)
                        var offset=0
                        while(offset<length) {val count=stream.read(body,offset,length-offset);check(count>0);offset+=count}
                        received.add(Json.parseToJsonElement(String(body,Charsets.UTF_8)).jsonObject)
                    }
                    val body=if(requestLine.contains("/health")) """{"status":"ok","configured":true,"model":"test-model"}""" else """{"topic":"会议话题","messages":[{"id":"reply-$index","philosopherId":"zhuangzi","displayName":"错误名称","content":"思想推演：重新检视努力所依据的尺度。","responseTo":null,"type":"argument"}]}"""
                    val bytes=body.toByteArray(Charsets.UTF_8)
                    socket.getOutputStream().apply {
                        write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        write(bytes);flush()
                    }
                }
            }
        }
        try {
            val endpoint="http://127.0.0.1:${server.localPort}/"
            graph.preferences.setAiBaseUrl(endpoint)
            assertEquals(endpoint,graph.preferences.settings.first().aiBaseUrl)
            assertTrue(graph.ai.checkConnection(endpoint).configured)
            val people=graph.ai.demos() // Seed data is present even before Room import.
            assertTrue(people.isNotEmpty())
            val catalog=Json.decodeFromString<List<com.xiaojidiu.philosophy.domain.model.Philosopher>>(ApplicationProvider.getApplicationContext<ThoughtsApplication>().assets.open("seed/philosophers.json").bufferedReader().use {it.readText()})
            val (response,demo)=graph.ai.request("Why try?",listOf("zhuangzi"),emptyList(),catalog,0)
            assertFalse(demo);assertEquals("庄子",response.messages.first().displayName)
            val context=listOf(DebateMessage("previous","zhuangzi","庄子","Prior answer",null,"argument"),DebateMessage("user-1","user","您","Why?",null,"user"))
            graph.ai.request("Why try?",listOf("zhuangzi"),context,catalog,0)
            assertEquals(2,received.size)
            assertEquals(2,received.last()["messages"]!!.jsonArray.size)
            graph.preferences.setAiBaseUrl("")
            try {
                graph.ai.request("Why try?",listOf("zhuangzi"),context,catalog,0)
                fail("Real conversations must not fall back to demo")
            } catch(expected: IllegalArgumentException) {assertTrue(expected.message!!.contains("连接讨论服务"))}
        } finally {
            graph.preferences.setAiBaseUrl(previous)
            server.close()
            worker.join(2000)
        }
    }
}
