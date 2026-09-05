package com.shadowcalc.app

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shadowcalc.app.databinding.ActivityVideoPlayerBinding
import java.io.File

class VideoPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoPlayerBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var vaultManager: VaultManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        vaultManager = VaultManager(this, securityManager)
        val path = intent.getStringExtra("path") ?: return finish()
        val file = File(path)
        val decrypted = vaultManager.decryptFile(file) ?: return Toast.makeText(this, "Cannot decrypt", Toast.LENGTH_SHORT).show().also { finish() }
        val temp = File(cacheDir, "temp_video_" + System.currentTimeMillis() + ".mp4")
        temp.writeBytes(decrypted)
        binding.videoView.setVideoURI(Uri.fromFile(temp))
        val mc = MediaController(this)
        binding.videoView.setMediaController(mc)
        mc.setAnchorView(binding.videoView)
        binding.videoView.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.videoView.stopPlayback()
    }
}
