package com.shadowcalc.app

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shadowcalc.app.databinding.ActivityVaultBinding
import java.io.File
import java.util.Timer
import java.util.TimerTask

class AudioPlayerActivity : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var timer: Timer? = null
    private lateinit var vaultManager: VaultManager
    private lateinit var securityManager: SecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        vaultManager = VaultManager(this, securityManager)
        binding.tvTitle.text = "Audio"
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.tvCount.visibility = View.GONE

        val path = intent.getStringExtra("path") ?: return finish()
        val file = File(path)
        val decrypted = vaultManager.decryptFile(file)
        decrypted?.let {
            val container = android.widget.LinearLayout(this)
            container.orientation = android.widget.LinearLayout.VERTICAL
            container.gravity = android.view.Gravity.CENTER
            container.layoutParams = android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)

            val title = android.widget.TextView(this)
            title.text = file.name.removePrefix("file_").removeSuffix(".enc")
            title.textSize = 18f
            title.setTextColor(getColor(R.color.text_primary))
            title.gravity = android.view.Gravity.CENTER
            container.addView(title)

            val seekBar = SeekBar(this)
            seekBar.layoutParams = android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(32, 32, 32, 32) }
            container.addView(seekBar)

            val btnPlay = android.widget.Button(this)
            btnPlay.text = "Play"
            btnPlay.setOnClickListener {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) { mp.pause(); btnPlay.text = "Play" }
                    else { mp.start(); btnPlay.text = "Pause" }
                }
            }
            container.addView(btnPlay)

            binding.root.addView(container)

            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(it.absolutePath)
                    prepare()
                    seekBar.max = duration
                    start()
                    btnPlay.text = "Pause"
                }
                timer = Timer().apply {
                    scheduleAtFixedRate(object : TimerTask() {
                        override fun run() {
                            runOnUiThread { mediaPlayer?.let { mp -> seekBar.progress = mp.currentPosition } }
                        }
                    }, 0, 500)
                }
                seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) mediaPlayer?.seekTo(progress)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot play audio", Toast.LENGTH_SHORT).show()
                finish()
            }
        } ?: finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
