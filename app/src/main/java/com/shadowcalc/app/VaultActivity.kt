package com.shadowcalc.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shadowcalc.app.databinding.ActivityVaultBinding
import java.io.File

class VaultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVaultBinding
    private lateinit var vaultManager: VaultManager
    private var currentTab = "images"

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { importFile(it, "image") }
    }
    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { importFile(it, "video") }
    }
    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { importFile(it, "file") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vaultManager = VaultManager(this)

        binding.btnImages.setOnClickListener { switchTab("images") }
        binding.btnVideos.setOnClickListener { switchTab("videos") }
        binding.btnFiles.setOnClickListener { switchTab("files") }
        binding.btnBrowser.setOnClickListener {
            startActivity(Intent(this, BrowserActivity::class.java))
        }
        binding.btnAdd.setOnClickListener { showAddDialog() }
        binding.btnBack.setOnClickListener { finish() }

        switchTab("images")
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun switchTab(tab: String) {
        currentTab = tab
        binding.tvTitle.text = tab.replaceFirstChar { it.uppercase() }
        refreshList()
    }

    private fun refreshList() {
        val files = when (currentTab) {
            "images" -> vaultManager.getImages()
            "videos" -> vaultManager.getVideos()
            else -> vaultManager.getFiles()
        }
        binding.recyclerView.layoutManager = GridLayoutManager(this, if (currentTab == "files") 1 else 3)
        binding.recyclerView.adapter = VaultAdapter(files, currentTab)
        binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAddDialog() {
        val options = when (currentTab) {
            "images" -> arrayOf("Import Image")
            "videos" -> arrayOf("Import Video")
            else -> arrayOf("Import File")
        }
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Add to Vault")
            .setItems(options) { _, which ->
                when (currentTab) {
                    "images" -> pickImage.launch("image/*")
                    "videos" -> pickVideo.launch("video/*")
                    else -> pickFile.launch("*/*")
                }
            }
            .show()
    }

    private fun importFile(uri: Uri, type: String) {
        if (vaultManager.encryptAndStore(uri, this, type)) {
            Toast.makeText(this, "Hidden successfully", Toast.LENGTH_SHORT).show()
            refreshList()
        } else {
            Toast.makeText(this, "Failed to hide file", Toast.LENGTH_SHORT).show()
        }
    }

    private inner class VaultAdapter(
        private val files: List<File>,
        private val type: String
    ) : RecyclerView.Adapter<VaultAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ImageView = view.findViewById(R.id.ivThumb)
            val textView: TextView = view.findViewById(R.id.tvName)
            val btnDelete: ImageView = view.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layout = if (type == "files") R.layout.item_file else R.layout.item_media
            val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val file = files[position]
            if (type == "images") {
                val decrypted = vaultManager.decryptFile(file)
                decrypted?.let {
                    Glide.with(holder.imageView).load(it).placeholder(R.drawable.ic_image).into(holder.imageView)
                }
                holder.textView.text = "Image ${position + 1}"
            } else if (type == "videos") {
                holder.imageView.setImageResource(R.drawable.ic_video)
                holder.textView.text = "Video ${position + 1}"
            } else {
                holder.textView.text = file.name
            }

            holder.itemView.setOnClickListener {
                if (type == "images") {
                    val intent = Intent(this@VaultActivity, ImageViewerActivity::class.java)
                    intent.putExtra("file_path", file.absolutePath)
                    startActivity(intent)
                } else if (type == "videos") {
                    val intent = Intent(this@VaultActivity, VideoPlayerActivity::class.java)
                    intent.putExtra("file_path", file.absolutePath)
                    startActivity(intent)
                } else {
                    shareFile(file)
                }
            }

            holder.btnDelete.setOnClickListener {
                AlertDialog.Builder(this@VaultActivity, R.style.DarkAlertDialog)
                    .setTitle("Delete?")
                    .setMessage("This will permanently remove the file.")
                    .setPositiveButton("Delete") { _, _ ->
                        vaultManager.deleteFile(file)
                        refreshList()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        override fun getItemCount() = files.size

        private fun shareFile(file: File) {
            val uri = FileProvider.getUriForFile(this@VaultActivity, "${packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Open with"))
        }
    }
}
