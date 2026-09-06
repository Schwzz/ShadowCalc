package com.shadowcalc.app

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shadowcalc.app.databinding.ActivityVaultBinding
import java.io.File

class DownloadsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVaultBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var vaultManager: VaultManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        vaultManager = VaultManager(this, securityManager)
        binding.tvTitle.text = "Downloads"
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.visibility = View.GONE
        refresh()
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val files = vaultManager.getDownloads().sortedByDescending { it.lastModified() }
        binding.tvCount.text = "${files.size} downloads"
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = DownloadAdapter(files)
        binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
    }

    private inner class DownloadAdapter(private val files: List<File>) : RecyclerView.Adapter<DownloadAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: android.widget.TextView = v.findViewById(R.id.tvName)
            val url: android.widget.TextView = v.findViewById(R.id.tvUrl)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(android.view.LayoutInflater.from(p.context).inflate(R.layout.item_download, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val file = files[i]
            h.name.text = file.name
            h.url.text = formatSize(file.length())
            h.itemView.setOnClickListener { /* open */ }
            h.itemView.setOnLongClickListener {
                AlertDialog.Builder(this@DownloadsActivity, R.style.DarkAlertDialog)
                    .setTitle("Delete?")
                    .setPositiveButton("Delete") { _, _ -> file.delete(); refresh() }
                    .setNegativeButton("Cancel", null).show()
                true
            }
        }
        override fun getItemCount() = files.size
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
