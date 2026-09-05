package com.shadowcalc.app

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shadowcalc.app.databinding.ActivityVideoPlayerBinding
import java.io.File

class VideoPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoPlayerBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var vaultManager: VaultManager
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private var isPlaying = false

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

        binding.btnClose.setOnClickListener { finish() }
        binding.btnPlayPause.setOnClickListener { togglePlayPause() }
        binding.btnSkipBack.setOnClickListener { skip(-5000) }
        binding.btnSkipForward.setOnClickListener { skip(5000) }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.videoView.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.videoView.setOnPreparedListener { mp ->
            binding.seekBar.max = mp.duration
            binding.tvTotal.text = formatTime(mp.duration)
            binding.videoView.start()
            isPlaying = true
            binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
            startProgressUpdate()
        }

        binding.videoView.setOnCompletionListener {
            isPlaying = false
            binding.btnPlayPause.setImageResource(R.drawable.ic_play)
        }

        // Tap to toggle controls visibility
        binding.videoView.setOnClickListener {
            val vis = if (binding.controlsOverlay.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            binding.controlsOverlay.visibility = vis
        }
    }

    private fun togglePlayPause() {
        if (isPlaying) {
            binding.videoView.pause()
            isPlaying = false
            binding.btnPlayPause.setImageResource(R.drawable.ic_play)
        } else {
            binding.videoView.start()
            isPlaying = true
            binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
        }
    }

    private fun skip(ms: Int) {
        val newPos = (binding.videoView.currentPosition + ms).coerceIn(0, binding.seekBar.max)
        binding.videoView.seekTo(newPos)
    }

    private fun startProgressUpdate() {
        runnable = Runnable {
            if (binding.videoView.isPlaying) {
                binding.seekBar.progress = binding.videoView.currentPosition
                binding.tvCurrent.text = formatTime(binding.videoView.currentPosition)
            }
            handler.postDelayed(runnable!!, 500)
        }
        handler.post(runnable!!)
    }

    private fun formatTime(ms: Int): String {
        val sec = ms / 1000
        val min = sec / 60
        val s = sec % 60
        return String.format("%02d:%02d", min, s)
    }

    override fun onDestroy() {
        super.onDestroy()
        runnable?.let { handler.removeCallbacks(it) }
        binding.videoView.stopPlayback()
    }
}
