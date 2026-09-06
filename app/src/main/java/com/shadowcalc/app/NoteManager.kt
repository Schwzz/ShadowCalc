package com.shadowcalc.app

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec

class NoteManager(private val context: Context, private val securityManager: SecurityManager) {
    private val notesFile = File(context.filesDir, "notes_v5.enc")
    private val gson = Gson()

    fun saveNotes(notes: List<Note>) {
        val json = gson.toJson(notes)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val key = securityManager.deriveVaultKey()
        val iv = ByteArray(16).apply { java.security.SecureRandom().nextBytes(this) }
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        java.io.FileOutputStream(notesFile).use { fos ->
            fos.write(iv)
            javax.crypto.CipherOutputStream(fos, cipher).use { cos ->
                cos.write(json.toByteArray())
            }
        }
    }

    fun loadNotes(): List<Note> {
        if (!notesFile.exists()) return emptyList()
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val key = securityManager.deriveVaultKey()
            java.io.FileInputStream(notesFile).use { fis ->
                val iv = ByteArray(16)
                fis.read(iv)
                cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
                javax.crypto.CipherInputStream(fis, cipher).use { cis ->
                    val json = cis.readBytes().toString(Charsets.UTF_8)
                    val type = object : TypeToken<List<Note>>() {}.type
                    gson.fromJson<List<Note>>(json, type) ?: emptyList()
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addNote(title: String, content: String) {
        val notes = loadNotes().toMutableList()
        notes.add(Note(System.currentTimeMillis(), title, content))
        saveNotes(notes)
    }

    fun deleteNote(id: Long) {
        val notes = loadNotes().filter { it.id != id }
        saveNotes(notes)
    }

    fun updateNote(id: Long, title: String, content: String) {
        val notes = loadNotes().map {
            if (it.id == id) it.copy(title = title, content = content) else it
        }
        saveNotes(notes)
    }
}

data class Note(val id: Long, val title: String, val content: String)
