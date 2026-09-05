package com.shadowcalc.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.shadowcalc.app.databinding.ActivityImageViewerBinding
import java.io.File

class ImageViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageViewerBinding
    private lateinit var vaultManager: VaultManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vaultManager = VaultManager(this)
        val path = intent.getStringExtra("path")
        if (path != null) {
            val decrypted = vaultManager.decryptFile(File(path))
            decrypted?.let { Glide.with(this).load(it).into(binding.imageView) }
        }
        binding.btnClose.setOnClickListener { finish() }
    }
}
