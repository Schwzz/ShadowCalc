package com.shadowcalc.app

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shadowcalc.app.databinding.ActivityAudioPlayerBinding
import java.io.File

class AudioPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAudioPlayerBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var vaultManager: VaultManager
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        vaultManager = VaultManager(this, securityManager)
        val path = intent.getStringExtra("path") ?: return finish()
        val file = File(path)
        val decrypted = vaultManager.decryptFile(file)
        if (decrypted == null) {
            Toast.makeText(this, "Cannot decrypt", Toast.LENGTH_SHORT).show()
            return finish()
        }
        val temp = File(cacheDir, "temp_audio_" + System.currentTimeMillis() + ".mp3")
        temp.writeBytes(decrypted)
        binding.tvTitle.text = file.name.removeSuffix(".enc").removePrefix("file_")
        binding.btnBack.setOnClickListener { finish() }

        mediaPlayer = MediaPlayer().apply {
            setDataSource(temp.absolutePath)
            prepare()
            binding.seekBar.max = duration
            binding.tvTotal.text = formatTime(duration)
            start()
            binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
        }

        binding.btnPlayPause.setOnClickListener {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    binding.btnPlayPause.setImageResource(R.drawable.ic_play)
                } else {
                    it.start()
                    binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
                }
            }
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        runnable = Runnable {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    binding.seekBar.progress = it.currentPosition
                    binding.tvCurrent.text = formatTime(it.currentPosition)
                }
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
        mediaPlayer?.release()
        mediaPlayer = null
    }
}