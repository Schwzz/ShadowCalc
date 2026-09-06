package com.shadowcalc.app

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shadowcalc.app.databinding.ActivityVaultBinding
import java.io.File

class TrashActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVaultBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var vaultManager: VaultManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        vaultManager = VaultManager(this, securityManager)
        binding.tvTitle.text = "Trash"
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.visibility = View.GONE
        refresh()
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val files = vaultManager.getAllTrash().sortedByDescending { it.lastModified() }
        binding.tvCount.text = "${files.size} items"
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = TrashAdapter(files)
        binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
    }

    private inner class TrashAdapter(private val files: List<File>) : RecyclerView.Adapter<TrashAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: android.widget.TextView = v.findViewById(R.id.tvName)
            val type: android.widget.TextView = v.findViewById(R.id.tvType)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(android.view.LayoutInflater.from(p.context).inflate(R.layout.item_trash, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val file = files[i]
            h.name.text = file.name
            h.type.text = file.parentFile?.name ?: "unknown"
            h.itemView.setOnClickListener {
                AlertDialog.Builder(this@TrashActivity, R.style.DarkAlertDialog)
                    .setTitle("Restore or permanently delete?")
                    .setPositiveButton("Restore") { _, _ ->
                        val type = file.parentFile?.name ?: "file"
                        vaultManager.restoreFromTrash(file, type)
                        refresh()
                    }
                    .setNegativeButton("Delete Forever") { _, _ ->
                        file.delete(); refresh()
                    }
                    .setNeutralButton("Cancel", null).show()
            }
        }
        override fun getItemCount() = files.size
    }
}
