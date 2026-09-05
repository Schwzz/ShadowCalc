package com.shadowcalc.app

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class VaultManager(context: Context, private val securityManager: SecurityManager) {
    private val vaultDir = File(context.filesDir, "vault")
    private val imagesDir = File(vaultDir, "images")
    private val videosDir = File(vaultDir, "videos")
    private val audioDir = File(vaultDir, "audio")
    private val filesDir = File(vaultDir, "files")
    private val trashDir = File(context.filesDir, "trash")
    private val trashImages = File(trashDir, "images")
    private val trashVideos = File(trashDir, "videos")
    private val trashAudio = File(trashDir, "audio")
    private val trashFiles = File(trashDir, "files")
    private val intruderDir = File(context.filesDir, "intruders")

    init {
        listOf(vaultDir, imagesDir, videosDir, audioDir, filesDir,
            trashDir, trashImages, trashVideos, trashAudio, trashFiles, intruderDir).forEach { it.mkdirs() }
    }

    private fun getKey(): javax.crypto.spec.SecretKeySpec = securityManager.deriveVaultKey()

    fun encryptAndStore(sourceUri: Uri, context: Context, type: String): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return false
            val bytes = inputStream.readBytes()
            inputStream.close()
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), IvParameterSpec(iv))
            val encrypted = cipher.doFinal(bytes)
            val fileName = "file_" + System.currentTimeMillis() + ".enc"
            val destDir = when (type) {
                "image" -> imagesDir
                "video" -> videosDir
                "audio" -> audioDir
                else -> filesDir
            }
            FileOutputStream(File(destDir, fileName)).use { it.write(iv + encrypted) }
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    fun decryptFile(file: File): ByteArray? {
        return try {
            val allBytes = file.readBytes()
            if (allBytes.size < 16) return null
            val iv = allBytes.copyOfRange(0, 16)
            val encrypted = allBytes.copyOfRange(16, allBytes.size)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, getKey(), IvParameterSpec(iv))
            cipher.doFinal(encrypted)
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    fun getImages(): List<File> = imagesDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    fun getVideos(): List<File> = videosDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    fun getAudio(): List<File> = audioDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    fun getFiles(): List<File> = filesDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun getAllVaultFiles(): List<File> = getImages() + getVideos() + getAudio() + getFiles()

    fun moveToTrash(file: File, type: String): Boolean {
        val dest = when (type) {
            "image" -> trashImages
            "video" -> trashVideos
            "audio" -> trashAudio
            else -> trashFiles
        }
        return file.renameTo(File(dest, file.name))
    }
    fun restoreFromTrash(file: File, type: String): Boolean {
        val dest = when (type) {
            "image" -> imagesDir
            "video" -> videosDir
            "audio" -> audioDir
            else -> filesDir
        }
        return file.renameTo(File(dest, file.name))
    }
    fun getTrash(type: String): List<File> {
        val dir = when (type) {
            "image" -> trashImages
            "video" -> trashVideos
            "audio" -> trashAudio
            else -> trashFiles
        }
        return dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    fun getAllTrash(): List<File> = getTrash("image") + getTrash("video") + getTrash("audio") + getTrash("file")

    fun deleteFile(file: File): Boolean = file.delete()
    fun permanentDelete(file: File): Boolean = file.delete()
    fun emptyTrash() {
        listOf(trashImages, trashVideos, trashAudio, trashFiles).forEach { it.listFiles()?.forEach { f -> f.delete() } }
    }

    fun saveIntruderPhoto(bytes: ByteArray): File {
        val file = File(intruderDir, "intruder_${System.currentTimeMillis()}.jpg")
        file.writeBytes(bytes)
        return file
    }
    fun getIntruderPhotos(): List<File> = intruderDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    fun deleteIntruderPhoto(file: File) = file.delete()
}
