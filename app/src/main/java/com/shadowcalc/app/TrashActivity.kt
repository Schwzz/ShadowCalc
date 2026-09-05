package com.shadowcalc.app

import android.os.Bundle
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
import com.shadowcalc.app.databinding.ActivityTrashBinding
import java.io.File

class TrashActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTrashBinding
    private lateinit var vaultManager: VaultManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vaultManager = VaultManager(this)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnEmpty.setOnClickListener {
            AlertDialog.Builder(this, R.style.DarkAlertDialog)
                .setTitle("Empty Trash?").setMessage("Permanently delete all items?")
                .setPositiveButton("Empty") { _, _ -> vaultManager.emptyTrash(); refresh() }
                .setNegativeButton("Cancel", null).show()
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        refresh()
    }

    private fun refresh() {
        val all = vaultManager.getTrash("image") + vaultManager.getTrash("video") + vaultManager.getTrash("file")
        binding.recyclerView.adapter = TrashAdapter(all.sortedByDescending { it.lastModified() })
        binding.tvEmpty.visibility = if (all.isEmpty()) View.VISIBLE else View.GONE
    }

    private inner class TrashAdapter(private val files: List<File>) : RecyclerView.Adapter<TrashAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvName)
            val btnRestore: ImageView = v.findViewById(R.id.btnRestore)
            val btnDelete: ImageView = v.findViewById(R.id.btnDelete)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_trash, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val file = files[i]
            h.name.text = file.name
            val type = when {
                file.parentFile?.name == "images" -> "image"
                file.parentFile?.name == "videos" -> "video"
                else -> "file"
            }
            h.btnRestore.setOnClickListener { vaultManager.restoreFromTrash(file, type); Toast.makeText(this@TrashActivity, "Restored", Toast.LENGTH_SHORT).show(); refresh() }
            h.btnDelete.setOnClickListener {
                AlertDialog.Builder(this@TrashActivity, R.style.DarkAlertDialog)
                    .setTitle("Delete forever?").setPositiveButton("Delete") { _, _ -> vaultManager.permanentDelete(file); refresh() }
                    .setNegativeButton("Cancel", null).show()
            }
        }
        override fun getItemCount() = files.size
    }
}
