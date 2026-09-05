package com.shadowcalc.app

import android.content.Intent
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shadowcalc.app.databinding.ActivityVaultBinding
import java.io.File

class AudioVaultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVaultBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var vaultManager: VaultManager
    private val pickAudio = registerForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { importFile(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        vaultManager = VaultManager(this, securityManager)
        binding.tvTitle.text = "Audio"
        binding.btnAdd.setOnClickListener { pickAudio.launch("audio/*") }
        binding.btnBack.setOnClickListener { finish() }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }
    override fun onResume() { super.onResume(); refresh() }
    private fun refresh() {
        val files = vaultManager.getAudio()
        binding.recyclerView.adapter = AudioAdapter(files)
        binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
    }
    private fun importFile(uri: android.net.Uri) {
        if (vaultManager.encryptAndStore(uri, this, "audio")) { Toast.makeText(this, "Audio hidden", Toast.LENGTH_SHORT).show(); refresh() }
        else { Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show() }
    }
    private inner class AudioAdapter(private val files: List<File>) : RecyclerView.Adapter<AudioAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvName)
            val del: ImageView = v.findViewById(R.id.btnDelete)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_file, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val file = files[i]
            h.name.text = file.name
            h.itemView.setOnClickListener {
                startActivity(Intent(this@AudioVaultActivity, AudioPlayerActivity::class.java).putExtra("path", file.absolutePath))
            }
            h.del.setOnClickListener {
                AlertDialog.Builder(this@AudioVaultActivity, R.style.DarkAlertDialog)
                    .setTitle("Move to Trash?").setPositiveButton("Trash") { _, _ -> vaultManager.moveToTrash(file, "audio"); refresh() }
                    .setNegativeButton("Cancel", null).show()
            }
        }
        override fun getItemCount() = files.size
    }
}
