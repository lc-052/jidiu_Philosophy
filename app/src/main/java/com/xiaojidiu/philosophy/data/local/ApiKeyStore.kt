package com.xiaojidiu.philosophy.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Ciphertext only in private preferences; the encryption key stays in Android Keystore. */
class ApiKeyStore(context: Context) {
    private val prefs=context.getSharedPreferences("model_credentials",Context.MODE_PRIVATE)
    private val alias="little_thoughts_user_api_key_v1"
    private fun encryptionKey(): SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply {load(null)}
        return store.getKey(alias,null) as? SecretKey ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }
    suspend fun save(baseUrl: String, apiKey: String)=withContext(Dispatchers.IO) {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply {init(Cipher.ENCRYPT_MODE,encryptionKey())}
        val payload=Json.encodeToString(listOf(baseUrl,apiKey)).toByteArray(Charsets.UTF_8)
        val encrypted=cipher.doFinal(payload)
        val envelope=Base64.encodeToString(cipher.iv,Base64.NO_WRAP)+":"+Base64.encodeToString(encrypted,Base64.NO_WRAP)
        check(prefs.edit().putString("encrypted",envelope).commit()) {"密钥未能保存。"}
    }
    suspend fun readFor(baseUrl: String): String?=withContext(Dispatchers.IO) {
        val envelope=prefs.getString("encrypted",null) ?: return@withContext null
        val parts=envelope.split(':')
        check(parts.size==2) {"密钥存储已损坏，请重新输入。"}
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply {init(Cipher.DECRYPT_MODE,encryptionKey(),GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)))}
        val decoded=Json.decodeFromString<List<String>>(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)).toString(Charsets.UTF_8))
        if(decoded.size==2 && decoded[0]==baseUrl) decoded[1] else null
    }
    suspend fun clear()=withContext(Dispatchers.IO) {check(prefs.edit().remove("encrypted").commit())}
}
