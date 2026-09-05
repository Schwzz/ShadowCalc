package com.shadowcalc.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.shadowcalc.app.databinding.ActivityImageViewerBinding
import java.io.File

class ImageViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageViewerBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var vaultManager: VaultManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
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
        Glide.with(this).load(decrypted).into(binding.imageView)
        binding.btnBack.setOnClickListener { finish() }
    }
}