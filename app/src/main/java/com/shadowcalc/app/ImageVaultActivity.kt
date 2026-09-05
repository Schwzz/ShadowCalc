package com.shadowcalc.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shadowcalc.app.databinding.ActivityVaultBinding
import java.io.File

class ImageVaultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVaultBinding
    private lateinit var vaultManager: VaultManager
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { importFile(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vaultManager = VaultManager(this)
        binding.tvTitle.text = "Images"
        binding.btnAdd.setOnClickListener { pickImage.launch("image/*") }
        binding.btnBack.setOnClickListener { finish() }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
    }
    override fun onResume() { super.onResume(); refresh() }
    private fun refresh() {
        val files = vaultManager.getImages()
        binding.recyclerView.adapter = ImageAdapter(files)
        binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
    }
    private fun importFile(uri: android.net.Uri) {
        if (vaultManager.encryptAndStore(uri, this, "image")) { Toast.makeText(this, "Image hidden", Toast.LENGTH_SHORT).show(); refresh() }
        else { Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show() }
    }
    private inner class ImageAdapter(private val files: List<File>) : RecyclerView.Adapter<ImageAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.ivThumb)
            val del: ImageView = v.findViewById(R.id.btnDelete)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_media, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val file = files[i]
            val decrypted = vaultManager.decryptFile(file)
            decrypted?.let { Glide.with(h.img).load(it).placeholder(R.drawable.ic_image).centerCrop().into(h.img) }
            h.itemView.setOnClickListener { startActivity(Intent(this@ImageVaultActivity, ImageViewerActivity::class.java).putExtra("path", file.absolutePath)) }
            h.del.setOnClickListener {
                AlertDialog.Builder(this@ImageVaultActivity, R.style.DarkAlertDialog)
                    .setTitle("Move to Trash?").setPositiveButton("Trash") { _, _ -> vaultManager.moveToTrash(file, "image"); refresh() }
                    .setNegativeButton("Cancel", null).show()
            }
        }
        override fun getItemCount() = files.size
    }
}
