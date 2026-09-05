package com.shadowcalc.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shadowcalc.app.databinding.ActivityVaultBinding
import java.io.File

class IntrudersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVaultBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var vaultManager: VaultManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        vaultManager = VaultManager(this, securityManager)
        binding.tvTitle.text = "Intruders"
        binding.btnAdd.visibility = View.GONE
        binding.btnBack.setOnClickListener { finish() }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val files = vaultManager.getIntruderPhotos()
        binding.recyclerView.adapter = IntruderAdapter(files)
        binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
    }

    private inner class IntruderAdapter(private val files: List<File>) : RecyclerView.Adapter<IntruderAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.ivThumb)
            val del: ImageView = v.findViewById(R.id.btnDelete)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_media, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val file = files[i]
            Glide.with(h.img).load(file).placeholder(R.drawable.ic_camera).centerCrop().into(h.img)
            h.del.setOnClickListener {
                AlertDialog.Builder(this@IntrudersActivity, R.style.DarkAlertDialog)
                    .setTitle("Delete photo?")
                    .setPositiveButton("Delete") { _, _ -> vaultManager.deleteIntruderPhoto(file); refresh() }
                    .setNegativeButton("Cancel", null).show()
            }
        }
        override fun getItemCount() = files.size
    }
}
