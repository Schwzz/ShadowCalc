package com.shadowcalc.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class PasswordEntry(
    val id: String,
    val title: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val date: Long
)

class PasswordManager(context: Context, private val securityManager: SecurityManager) {
    private val file = File(context.filesDir, "passwords_v3.enc")

    private fun getKey(): SecretKeySpec = securityManager.deriveVaultKey()

    fun saveEntries(entries: List<PasswordEntry>) {
        val arr = JSONArray()
        entries.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("title", it.title)
            obj.put("username", it.username)
            obj.put("password", it.password)
            obj.put("url", it.url)
            obj.put("notes", it.notes)
            obj.put("date", it.date)
            arr.put(obj)
        }
        val bytes = arr.toString().toByteArray()
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        cipher.init(Cipher.ENCRYPT_MODE, getKey(), IvParameterSpec(iv))
        val enc = cipher.doFinal(bytes)
        file.writeBytes(iv + enc)
    }

    fun loadEntries(): List<PasswordEntry> {
        if (!file.exists()) return emptyList()
        return try {
            val all = file.readBytes()
            val iv = all.copyOfRange(0, 16)
            val enc = all.copyOfRange(16, all.size)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, getKey(), IvParameterSpec(iv))
            val json = String(cipher.doFinal(enc))
            val arr = JSONArray(json)
            val list = mutableListOf<PasswordEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(PasswordEntry(
                    obj.getString("id"),
                    obj.getString("title"),
                    obj.getString("username"),
                    obj.getString("password"),
                    obj.getString("url"),
                    obj.getString("notes"),
                    obj.getLong("date")
                ))
            }
            list
        } catch (e: Exception) { emptyList() }
    }
}
