package com.shadowcalc.app

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class VaultManager(private val context: Context, private val securityManager: SecurityManager) {
    private val vaultDir = File(context.filesDir, "vault").apply { mkdirs() }
    val imagesDir = File(vaultDir, "images").apply { mkdirs() }
    val videosDir = File(vaultDir, "videos").apply { mkdirs() }
    val audioDir = File(vaultDir, "audio").apply { mkdirs() }
    val filesDir = File(vaultDir, "files").apply { mkdirs() }
    val trashDir = File(context.filesDir, "trash").apply { mkdirs() }
    val downloadsDir = File(vaultDir, "downloads").apply { mkdirs() }

    private fun getCipher(mode: Int): Cipher {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val key = securityManager.deriveVaultKey()
        if (mode == Cipher.ENCRYPT_MODE) {
            val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
            cipher.init(mode, key, IvParameterSpec(iv))
        } else {
            cipher.init(mode, key)
        }
        return cipher
    }

    fun encryptAndStore(uri: Uri, ctx: Context, type: String = "file"): Boolean {
        return try {
            val destDir = when (type) {
                "image" -> imagesDir
                "video" -> videosDir
                "audio" -> audioDir
                else -> filesDir
            }
            val fileName = "file_${System.currentTimeMillis()}.enc"
            val destFile = File(destDir, fileName)
            val cipher = getCipher(Cipher.ENCRYPT_MODE)
            val iv = cipher.iv
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { fos ->
                    fos.write(iv)
                    CipherOutputStream(fos, cipher).use { cos ->
                        input.copyTo(cos)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun decryptFile(file: File): File? {
        return try {
            val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val key = securityManager.deriveVaultKey()
            FileInputStream(file).use { fis ->
                val iv = ByteArray(16)
                fis.read(iv)
                cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
                FileOutputStream(tempFile).use { fos ->
                    CipherInputStream(fis, cipher).use { cis ->
                        cis.copyTo(fos)
                    }
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getImages(): List<File> = imagesDir.listFiles()?.toList() ?: emptyList()
    fun getVideos(): List<File> = videosDir.listFiles()?.toList() ?: emptyList()
    fun getAudio(): List<File> = audioDir.listFiles()?.toList() ?: emptyList()
    fun getFiles(): List<File> = filesDir.listFiles()?.toList() ?: emptyList()
    fun getDownloads(): List<File> = downloadsDir.listFiles()?.toList() ?: emptyList()

    fun moveToTrash(file: File, type: String) {
        val trashTypeDir = File(trashDir, type).apply { mkdirs() }
        file.renameTo(File(trashTypeDir, file.name))
    }

    fun getAllTrash(): List<File> {
        return trashDir.listFiles()?.flatMap { it.listFiles()?.toList() ?: emptyList() } ?: emptyList()
    }

    fun emptyTrash() {
        trashDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    fun restoreFromTrash(file: File, type: String): Boolean {
        val destDir = when (type) {
            "image" -> imagesDir
            "video" -> videosDir
            "audio" -> audioDir
            else -> filesDir
        }
        return file.renameTo(File(destDir, file.name))
    }

    fun unhideToPublic(ctx: Context, file: File, type: String): Boolean {
        return try {
            val decrypted = decryptFile(file) ?: return false
            val mimeType = when (type) {
                "image" -> "image/*"
                "video" -> "video/*"
                "audio" -> "audio/*"
                else -> "*/*"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name.removePrefix("file_").removeSuffix(".enc"))
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    ctx.contentResolver.openOutputStream(it)?.use { os ->
                        FileInputStream(decrypted).use { input -> input.copyTo(os) }
                    }
                }
            } else {
                val dest = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.name.removePrefix("file_").removeSuffix(".enc"))
                decrypted.copyTo(dest, overwrite = true)
            }
            decrypted.delete()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveDownloadedVideo(bytes: ByteArray, fileName: String): Boolean {
        return try {
            val destFile = File(videosDir, "file_${System.currentTimeMillis()}.enc")
            val cipher = getCipher(Cipher.ENCRYPT_MODE)
            val iv = cipher.iv
            FileOutputStream(destFile).use { fos ->
                fos.write(iv)
                CipherOutputStream(fos, cipher).use { cos ->
                    cos.write(bytes)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getStorageBreakdown(): Map<String, Long> {
        return mapOf(
            "image" to (imagesDir.listFiles()?.sumOf { it.length() } ?: 0L),
            "video" to (videosDir.listFiles()?.sumOf { it.length() } ?: 0L),
            "audio" to (audioDir.listFiles()?.sumOf { it.length() } ?: 0L),
            "file" to (filesDir.listFiles()?.sumOf { it.length() } ?: 0L),
            "download" to (downloadsDir.listFiles()?.sumOf { it.length() } ?: 0L)
        )
    }

    fun getTotalStorageUsed(): Long {
        return getStorageBreakdown().values.sum()
    }
}
