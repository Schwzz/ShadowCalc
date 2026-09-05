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
    private var tempFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vaultManager = VaultManager(this)
        val path = intent.getStringExtra("path")
        if (path != null) {
            val decrypted = vaultManager.decryptFile(File(path))
            if (decrypted != null) {
                tempFile = File(cacheDir, "temp_video_" + System.currentTimeMillis() + ".mp4")
                tempFile?.writeBytes(decrypted)
                tempFile?.let {
                    val uri = FileProvider.getUriForFile(this, "${packageName}.provider", it)
                    binding.videoView.setVideoURI(uri)
                    val mc = MediaController(this)
                    mc.setAnchorView(binding.videoView)
                    binding.videoView.setMediaController(mc)
                    binding.videoView.setOnPreparedListener { mp -> mp.start() }
                }
            }
        }
        binding.btnClose.setOnClickListener { finish() }
    }
    override fun onPause() { super.onPause(); binding.videoView.pause() }
    override fun onDestroy() { super.onDestroy(); binding.videoView.stopPlayback(); tempFile?.delete() }
}
