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
import com.shadowcalc.app.databinding.ActivityVaultBinding
import java.io.File

class VideoVaultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVaultBinding
    private lateinit var vaultManager: VaultManager
    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { importFile(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vaultManager = VaultManager(this)
        binding.tvTitle.text = "Videos"
        binding.btnAdd.setOnClickListener { pickVideo.launch("video/*") }
        binding.btnBack.setOnClickListener { finish() }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
    }
    override fun onResume() { super.onResume(); refresh() }
    private fun refresh() {
        val files = vaultManager.getVideos()
        binding.recyclerView.adapter = VideoAdapter(files)
        binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
    }
    private fun importFile(uri: android.net.Uri) {
        if (vaultManager.encryptAndStore(uri, this, "video")) { Toast.makeText(this, "Video hidden", Toast.LENGTH_SHORT).show(); refresh() }
        else { Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show() }
    }
    private inner class VideoAdapter(private val files: List<File>) : RecyclerView.Adapter<VideoAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val thumb: ImageView = v.findViewById(R.id.ivThumb)
            val del: ImageView = v.findViewById(R.id.btnDelete)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_media, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val file = files[i]
            h.thumb.setImageResource(R.drawable.ic_video)
            h.itemView.setOnClickListener { startActivity(Intent(this@VideoVaultActivity, VideoPlayerActivity::class.java).putExtra("path", file.absolutePath)) }
            h.del.setOnClickListener {
                AlertDialog.Builder(this@VideoVaultActivity, R.style.DarkAlertDialog)
                    .setTitle("Move to Trash?").setPositiveButton("Trash") { _, _ -> vaultManager.moveToTrash(file, "video"); refresh() }
                    .setNegativeButton("Cancel", null).show()
            }
        }
        override fun getItemCount() = files.size
    }
}
