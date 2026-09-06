package com.shadowcalc.app

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.shadowcalc.app.databinding.ActivityVaultBinding
import java.io.File

class ImageViewerActivity : AppCompatActivity() {
    private lateinit var vaultManager: VaultManager
    private lateinit var securityManager: SecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        val binding = com.shadowcalc.app.databinding.ActivityVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        vaultManager = VaultManager(this, securityManager)
        binding.tvTitle.text = "Image"
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.visibility = android.view.View.GONE
        binding.recyclerView.visibility = android.view.View.GONE
        binding.tvEmpty.visibility = android.view.View.GONE
        binding.tvCount.visibility = android.view.View.GONE

        val path = intent.getStringExtra("path") ?: return finish()
        val file = File(path)
        val decrypted = vaultManager.decryptFile(file)
        decrypted?.let {
            val imageView = android.widget.ImageView(this)
            imageView.layoutParams = android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
            imageView.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            Glide.with(this).load(it).into(imageView)
            binding.root.addView(imageView)
        } ?: finish()
    }
}
