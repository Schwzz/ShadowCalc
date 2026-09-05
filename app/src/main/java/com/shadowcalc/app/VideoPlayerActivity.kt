package com.shadowcalc.app

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.shadowcalc.app.databinding.ActivityVideoPlayerBinding
import java.io.File

class VideoPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoPlayerBinding
    private lateinit var vaultManager: VaultManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vaultManager = VaultManager(this)

        val filePath = intent.getStringExtra("file_path")
        if (filePath != null) {
            val file = File(filePath)
            val decrypted = vaultManager.decryptFile(file)
            if (decrypted != null) {
                // Write decrypted bytes to temp file for MediaPlayer
                val tempFile = File(cacheDir, "temp_video_" + System.currentTimeMillis() + ".mp4")
                tempFile.writeBytes(decrypted)
                val uri = FileProvider.getUriForFile(this, "${packageName}.provider", tempFile)
                binding.videoView.setVideoURI(uri)
                val mediaController = MediaController(this)
                mediaController.setAnchorView(binding.videoView)
                binding.videoView.setMediaController(mediaController)
                binding.videoView.start()
            }
        }

        binding.btnClose.setOnClickListener {
            binding.videoView.stopPlayback()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.videoView.stopPlayback()
        // Clean up temp files
        cacheDir.listFiles()?.forEach { if (it.name.startsWith("temp_video_")) it.delete() }
    }
}
