package com.shadowcalc.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shadowcalc.app.databinding.ActivityDownloadsBinding
import java.io.File

class DownloadsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDownloadsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        loadDownloads()
    }

    private fun loadDownloads() {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val files = dir.listFiles()?.filter { it.length() > 0 }?.sortedByDescending { it.lastModified() } ?: emptyList()
        binding.recyclerView.adapter = DownloadAdapter(files)
        binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
    }

    private inner class DownloadAdapter(private val files: List<File>) : RecyclerView.Adapter<DownloadAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvName)
            val size: TextView = v.findViewById(R.id.tvSize)
            val btnOpen: ImageView = v.findViewById(R.id.btnOpen)
            val btnDelete: ImageView = v.findViewById(R.id.btnDelete)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_download, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val file = files[i]
            h.name.text = file.name
            h.size.text = formatSize(file.length())
            h.btnOpen.setOnClickListener {
                val uri = FileProvider.getUriForFile(this@DownloadsActivity, "${packageName}.provider", file)
                val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, getMimeType(file.name)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                try { startActivity(intent) } catch (_: Exception) { Toast.makeText(this@DownloadsActivity, "Cannot open file", Toast.LENGTH_SHORT).show() }
            }
            h.btnDelete.setOnClickListener { file.delete(); loadDownloads() }
        }
        override fun getItemCount() = files.size

        private fun formatSize(size: Long): String {
            return when {
                size > 1024 * 1024 * 1024 -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
                size > 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
                size > 1024 -> String.format("%.2f KB", size / 1024.0)
                else -> "$size B"
            }
        }
        private fun getMimeType(name: String): String {
            return when {
                name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") -> "video/*"
                name.endsWith(".mp3") || name.endsWith(".wav") -> "audio/*"
                name.endsWith(".jpg") || name.endsWith(".png") -> "image/*"
                name.endsWith(".pdf") -> "application/pdf"
                else -> "*/*"
            }
        }
    }
}
