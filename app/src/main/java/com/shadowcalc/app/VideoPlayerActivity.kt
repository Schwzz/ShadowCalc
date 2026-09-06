package com.shadowcalc.app

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.shadowcalc.app.databinding.ActivityVaultBinding
import java.io.File

class VideoPlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var vaultManager: VaultManager
    private lateinit var securityManager: SecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        val binding = ActivityVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        vaultManager = VaultManager(this, securityManager)
        binding.tvTitle.text = "Video"
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.visibility = android.view.View.GONE
        binding.recyclerView.visibility = android.view.View.GONE
        binding.tvEmpty.visibility = android.view.View.GONE
        binding.tvCount.visibility = android.view.View.GONE

        val path = intent.getStringExtra("path") ?: return finish()
        val file = File(path)
        val decrypted = vaultManager.decryptFile(file)
        decrypted?.let {
            val playerView = PlayerView(this)
            playerView.layoutParams = android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
            playerView.useController = true
            binding.root.addView(playerView)
            player = ExoPlayer.Builder(this).build().apply {
                setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(it)))
                prepare()
                play()
            }
            playerView.player = player
        } ?: finish()
    }

    override fun onPause() { super.onPause(); player?.pause() }
    override fun onDestroy() { super.onDestroy(); player?.release(); player = null }
}
