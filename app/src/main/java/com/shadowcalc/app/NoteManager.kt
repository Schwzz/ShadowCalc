package com.shadowcalc.app

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import java.security.SecureRandom

data class Note(val id: String, val title: String, val content: String, val timestamp: Long)

class NoteManager(context: Context, private val securityManager: SecurityManager) {
    private val file = File(context.filesDir, "notes_v4.enc")
    private val gson = Gson()

    fun loadNotes(): List<Note> {
        if (!file.exists()) return emptyList()
        val decrypted = decrypt(file.readBytes()) ?: return emptyList()
        return try {
            gson.fromJson(decrypted, object : TypeToken<List<Note>>() {}.type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    fun saveNote(title: String, content: String) {
        val notes = loadNotes().toMutableList()
        notes.add(Note(System.currentTimeMillis().toString(), title, content, System.currentTimeMillis()))
        saveAll(notes)
    }

    fun updateNote(id: String, title: String, content: String) {
        val notes = loadNotes().map {
            if (it.id == id) it.copy(title = title, content = content, timestamp = System.currentTimeMillis()) else it
        }
        saveAll(notes)
    }

    fun deleteNote(id: String) {
        saveAll(loadNotes().filter { it.id != id })
    }

    private fun saveAll(notes: List<Note>) {
        val json = gson.toJson(notes)
        file.writeBytes(encrypt(json))
    }

    private fun encrypt(data: String): ByteArray {
        val key = securityManager.deriveVaultKey()
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        return iv + cipher.doFinal(data.toByteArray())
    }

    private fun decrypt(bytes: ByteArray): String? {
        return try {
            val key = securityManager.deriveVaultKey()
            val iv = bytes.copyOfRange(0, 16)
            val encrypted = bytes.copyOfRange(16, bytes.size)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
            String(cipher.doFinal(encrypted))
        } catch (e: Exception) { null }
    }
}