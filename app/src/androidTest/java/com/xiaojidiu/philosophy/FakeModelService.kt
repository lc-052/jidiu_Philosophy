package com.xiaojidiu.philosophy

import kotlinx.serialization.json.*
import java.net.InetAddress
import java.net.ServerSocket
import java.io.ByteArrayOutputStream
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

/** Local HTTP provider fixture. No real keys or paid calls. */
class FakeModelService(private val statusCode: Int=200): AutoCloseable {
    data class Received(val json: JsonObject, val authorization: String?)
    val requests=CopyOnWriteArrayList<Received>()
    private val server=ServerSocket(0,4,InetAddress.getByName("127.0.0.1"))
    val baseUrl="http://127.0.0.1:${server.localPort}/v1/"
    private val worker=thread(isDaemon=true) {
        while(!server.isClosed) {
            try {
                server.accept().use {socket->
                    socket.soTimeout=10000
                    val input=socket.getInputStream().buffered()
                    fun line(): String {
                        val bytes=ByteArrayOutputStream()
                        while(true) {val value=input.read();check(value>=0);if(value==10) break;if(value!=13) bytes.write(value)}
                        return bytes.toString("UTF-8")
                    }
                    line()
                    var length=0;var auth: String?=null
                    while(true) {val header=line();if(header.isEmpty()) break;if(header.startsWith("Content-Length:",true)) length=header.substringAfter(':').trim().toInt();if(header.startsWith("Authorization:",true)) auth=header.substringAfter(':').trim()}
                    val bytes=ByteArray(length);var offset=0
                    while(offset<length) {val count=input.read(bytes,offset,length-offset);check(count>0);offset+=count}
                    val payload=Json.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject
                    requests.add(Received(payload,auth))
                    val messages=payload["messages"]!!.jsonArray
                    val isProbe=messages.size==1
                    val reply=if(isProbe) "OK" else {
                        val material=Json.parseToJsonElement(messages[1].jsonObject["content"]!!.jsonPrimitive.content).jsonObject
                        val discussion=material["discussion"]!!.jsonObject
                        val context=discussion["messages"]?.jsonArray ?: JsonArray(emptyList())
                        buildJsonObject {
                            put("topic",discussion["topic"]!!)
                            putJsonArray("messages") {if(discussion["mode"]?.jsonPrimitive?.content!="summary") add(buildJsonObject {put("id","reply-${requests.size}");put("philosopherId","zhuangzi");put("displayName","不可信名称");put("content","思想推演：可以先检视努力所依据的尺度。价值未必仅由外部效率决定，也可以从自己的生活经验出发。");put("responseTo",context.lastOrNull()?.jsonObject?.get("id") ?: JsonNull);put("type",if(context.isEmpty()) "argument" else "reply")})}
                            if(discussion["mode"]?.jsonPrimitive?.content=="summary" || discussion["includeSummary"]?.jsonPrimitive?.booleanOrNull==true) putJsonObject("summary") {
                                putJsonArray("disagreement") {add(buildJsonObject {put("text","努力的价值是否应以外部尺度衡量？");putJsonArray("messageIds") {add(context.first().jsonObject["id"]!!) }})}
                                putJsonArray("commonGround") {}
                            }
                        }.toString()
                    }
                    val body=if(statusCode==200) buildJsonObject {putJsonArray("choices") {add(buildJsonObject {put("finish_reason","stop");putJsonObject("message") {put("role","assistant");put("content",reply)}})}}.toString() else """{"error":{"message":"test provider rejected key"}}"""
                    val encoded=body.toByteArray(Charsets.UTF_8)
                    socket.getOutputStream().apply {write("HTTP/1.1 $statusCode Response\r\nContent-Type: application/json\r\nContent-Length: ${encoded.size}\r\nConnection: close\r\n\r\n".toByteArray());write(encoded);flush()}
                }
            } catch(e: Exception) {if(!server.isClosed) throw e}
        }
    }
    override fun close() {server.close();worker.join(2000)}
}
