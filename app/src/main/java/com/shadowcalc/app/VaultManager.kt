package com.shadowcalc.app

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class VaultManager(context: Context) {
    private val vaultDir = File(context.filesDir, "vault")
    private val imagesDir = File(vaultDir, "images")
    private val videosDir = File(vaultDir, "videos")
    private val filesDir = File(vaultDir, "files")

    init {
        vaultDir.mkdirs()
        imagesDir.mkdirs()
        videosDir.mkdirs()
        filesDir.mkdirs()
    }

    private fun getKey(): ByteArray {
        val keyBytes = ByteArray(16)
        // In production, derive from user PIN + salt. Here fixed for prototype.
        "ShadowCalc2024!!".toByteArray().copyInto(keyBytes, 0, 0, 16)
        return keyBytes
    }

    fun encryptAndStore(sourceUri: Uri, context: Context, type: String): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return false
            val bytes = inputStream.readBytes()
            inputStream.close()

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(getKey(), "AES"), IvParameterSpec(iv))
            val encrypted = cipher.doFinal(bytes)

            val fileName = "file_" + System.currentTimeMillis() + ".enc"
            val destDir = when (type) {
                "image" -> imagesDir
                "video" -> videosDir
                else -> filesDir
            }
            val destFile = File(destDir, fileName)
            FileOutputStream(destFile).use { it.write(iv + encrypted) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun decryptFile(file: File): ByteArray? {
        return try {
            val allBytes = file.readBytes()
            val iv = allBytes.copyOfRange(0, 16)
            val encrypted = allBytes.copyOfRange(16, allBytes.size)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(getKey(), "AES"), IvParameterSpec(iv))
            cipher.doFinal(encrypted)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getImages(): List<File> = imagesDir.listFiles()?.toList() ?: emptyList()
    fun getVideos(): List<File> = videosDir.listFiles()?.toList() ?: emptyList()
    fun getFiles(): List<File> = filesDir.listFiles()?.toList() ?: emptyList()

    fun deleteFile(file: File): Boolean = file.delete()
}
