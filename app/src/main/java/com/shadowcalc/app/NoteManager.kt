package com.shadowcalc.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class Note(val id: String, val title: String, val content: String, val date: Long, val folder: String = "")

class NoteManager(context: Context, private val securityManager: SecurityManager) {
    private val file = File(context.filesDir, "notes_v3.enc")

    private fun getKey(): SecretKeySpec = securityManager.deriveVaultKey()

    fun saveNotes(notes: List<Note>) {
        val arr = JSONArray()
        notes.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("title", it.title)
            obj.put("content", it.content)
            obj.put("date", it.date)
            obj.put("folder", it.folder)
            arr.put(obj)
        }
        val bytes = arr.toString().toByteArray()
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        cipher.init(Cipher.ENCRYPT_MODE, getKey(), IvParameterSpec(iv))
        val enc = cipher.doFinal(bytes)
        file.writeBytes(iv + enc)
    }

    fun loadNotes(): List<Note> {
        if (!file.exists()) return emptyList()
        return try {
            val all = file.readBytes()
            val iv = all.copyOfRange(0, 16)
            val enc = all.copyOfRange(16, all.size)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, getKey(), IvParameterSpec(iv))
            val json = String(cipher.doFinal(enc))
            val arr = JSONArray(json)
            val list = mutableListOf<Note>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(Note(
                    obj.getString("id"),
                    obj.getString("title"),
                    obj.getString("content"),
                    obj.getLong("date"),
                    obj.optString("folder", "")
                ))
            }
            list
        } catch (e: Exception) { emptyList() }
    }

    fun getFolders(notes: List<Note>): List<String> {
        return notes.map { it.folder }.filter { it.isNotEmpty() }.distinct().sorted()
    }
}
