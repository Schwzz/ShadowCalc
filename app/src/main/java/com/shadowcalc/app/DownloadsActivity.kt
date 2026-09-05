package com.shadowcalc.app

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shadowcalc.app.databinding.ActivityDownloadsBinding
import java.io.File

class DownloadsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDownloadsBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var vaultManager: VaultManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        vaultManager = VaultManager(this, securityManager)
        binding.btnBack.setOnClickListener { finish() }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        refresh()
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val publicDownloads = getPublicDownloadedFiles()
        val vaultDownloads = vaultManager.getDownloads()
        val all = publicDownloads.map { it to true } + vaultDownloads.map { it to false }
        binding.recyclerView.adapter = DownloadAdapter(all)
        binding.tvEmpty.visibility = if (all.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun getPublicDownloadedFiles(): List<File> {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return dir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    private inner class DownloadAdapter(private val files: List<Pair<File, Boolean>>) : RecyclerView.Adapter<DownloadAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvName)
            val size: TextView = v.findViewById(R.id.tvSize)
            val del: ImageView = v.findViewById(R.id.btnDelete)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_download, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val (file, isPublic) = files[i]
            h.name.text = file.name + if (!isPublic) " (Vault)" else ""
            h.size.text = formatSize(file.length())
            h.del.setOnClickListener {
                AlertDialog.Builder(this@DownloadsActivity, R.style.DarkAlertDialog)
                    .setTitle("Delete file?")
                    .setPositiveButton("Delete") { _, _ ->
                        if (isPublic) file.delete() else vaultManager.permanentDelete(file)
                        refresh()
                    }
                    .setNegativeButton("Cancel", null).show()
            }
        }
        override fun getItemCount() = files.size

        private fun formatSize(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
            return "${bytes / (1024 * 1024)} MB"
        }
    }
}
