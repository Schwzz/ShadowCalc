package com.shadowcalc.app

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shadowcalc.app.databinding.ActivityVaultBinding
import java.io.File

class FileManagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVaultBinding
    private lateinit var vaultManager: VaultManager
    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { importFile(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vaultManager = VaultManager(this)
        binding.tvTitle.text = "Files"
        binding.btnAdd.setOnClickListener { pickFile.launch("*/*") }
        binding.btnBack.setOnClickListener { finish() }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }
    override fun onResume() { super.onResume(); refresh() }
    private fun refresh() {
        val files = vaultManager.getFiles()
        binding.recyclerView.adapter = FileAdapter(files)
        binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
    }
    private fun importFile(uri: Uri) {
        if (vaultManager.encryptAndStore(uri, this, "file")) { Toast.makeText(this, "File hidden", Toast.LENGTH_SHORT).show(); refresh() }
        else { Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show() }
    }
    private inner class FileAdapter(private val files: List<File>) : RecyclerView.Adapter<FileAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvName)
            val del: ImageView = v.findViewById(R.id.btnDelete)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_file, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val file = files[i]
            h.name.text = file.name
            h.itemView.setOnClickListener {
                val decrypted = vaultManager.decryptFile(file) ?: return@setOnClickListener
                val temp = File(cacheDir, "temp_" + System.currentTimeMillis() + "_" + file.name.removeSuffix(".enc"))
                temp.writeBytes(decrypted)
                val uri = FileProvider.getUriForFile(this@FileManagerActivity, "${packageName}.provider", temp)
                val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                try { startActivity(intent) } catch (_: Exception) { Toast.makeText(this@FileManagerActivity, "No app to open file", Toast.LENGTH_SHORT).show() }
            }
            h.del.setOnClickListener {
                AlertDialog.Builder(this@FileManagerActivity, R.style.DarkAlertDialog)
                    .setTitle("Move to Trash?").setPositiveButton("Trash") { _, _ -> vaultManager.moveToTrash(file, "file"); refresh() }
                    .setNegativeButton("Cancel", null).show()
            }
        }
        override fun getItemCount() = files.size
    }
}
