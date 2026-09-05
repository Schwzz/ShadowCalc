package com.shadowcalc.app

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
    private val downloadsDir = File(vaultDir, "downloads")
    private val trashDir = File(context.filesDir, "trash")
    private val trashImages = File(trashDir, "images")
    private val trashVideos = File(trashDir, "videos")
    private val trashAudio = File(trashDir, "audio")
    private val trashFiles = File(trashDir, "files")

    init {
        listOf(vaultDir, imagesDir, videosDir, audioDir, filesDir, downloadsDir,
            trashDir, trashImages, trashVideos, trashAudio, trashFiles).forEach { it.mkdirs() }
    }

    private fun getKey(): SecretKeySpec = securityManager.deriveVaultKey()

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
                "download" -> downloadsDir
                else -> filesDir
            }
            FileOutputStream(File(destDir, fileName)).use { it.write(iv + encrypted) }
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    fun saveDownloadedVideo(bytes: ByteArray, fileName: String): Boolean {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), IvParameterSpec(iv))
            val encrypted = cipher.doFinal(bytes)
            val outFile = File(downloadsDir, "${fileName}_${System.currentTimeMillis()}.enc")
            FileOutputStream(outFile).use { it.write(iv + encrypted) }
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
    fun getDownloads(): List<File> = downloadsDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    fun getAllVaultFiles(): List<File> = getImages() + getVideos() + getAudio() + getFiles() + getDownloads()

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
            "download" -> downloadsDir
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

    fun unhideToPublic(context: Context, file: File, type: String): Boolean {
        val decrypted = decryptFile(file) ?: return false
        val mimeType = when (type) {
            "image" -> "image/*"
            "video" -> "video/*"
            "audio" -> "audio/*"
            else -> "*/*"
        }
        val displayName = file.name.removeSuffix(".enc").removePrefix("file_")
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val collection = when (type) {
                    "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
                }
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    if (type == "file") put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(collection, values)
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { os -> os.write(decrypted) }
                }
            } else {
                val destDir = when (type) {
                    "image" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    "video" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                    "audio" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                    else -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                }
                val outFile = File(destDir, displayName)
                FileOutputStream(outFile).use { it.write(decrypted) }
            }
            file.delete()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isCriticalDirectory(file: File): Boolean {
        val critical = listOf(vaultDir, imagesDir, videosDir, audioDir, filesDir, downloadsDir, trashDir)
        return critical.any { it.absolutePath == file.absolutePath }
    }
}
