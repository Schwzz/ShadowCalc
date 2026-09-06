package com.shadowcalc.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.shadowcalc.app.databinding.ActivityGalleryBinding
import java.io.File

class GalleryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGalleryBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var vaultManager: VaultManager
    private var actionMode: ActionMode? = null
    private val selectedFiles = mutableSetOf<File>()
    private var isSelectionMode = false
    private var currentTab = "all"
    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { importFile(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        vaultManager = VaultManager(this, securityManager)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.setOnClickListener { pickMedia.launch("*/*") }
        binding.btnSelect.setOnClickListener {
            if (!isSelectionMode) startSelectionMode()
        }

        setupTabs()
        currentTab = intent.getStringExtra("tab") ?: "all"
        selectTab(currentTab)
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("All Media"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Photos"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Videos"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Audio"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = when (tab?.position) {
                    0 -> "all"
                    1 -> "photos"
                    2 -> "videos"
                    3 -> "audio"
                    else -> "all"
                }
                refresh()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun selectTab(tabName: String) {
        val position = when (tabName) {
            "all" -> 0
            "photos" -> 1
            "videos" -> 2
            "audio" -> 3
            else -> 0
        }
        binding.tabLayout.getTabAt(position)?.select()
    }

    private fun refresh() {
        selectedFiles.clear()
        isSelectionMode = false
        actionMode?.finish()

        val files = when (currentTab) {
            "photos" -> vaultManager.getImages()
            "videos" -> vaultManager.getVideos()
            "audio" -> vaultManager.getAudio()
            else -> vaultManager.getImages() + vaultManager.getVideos() + vaultManager.getAudio()
        }.sortedByDescending { it.lastModified() }

        binding.tvCount.text = "${files.size} items"

        if (currentTab == "audio") {
            binding.recyclerView.layoutManager = LinearLayoutManager(this)
            binding.recyclerView.adapter = AudioAdapter(files)
        } else {
            val spanCount = if (currentTab == "videos") 2 else 3
            binding.recyclerView.layoutManager = GridLayoutManager(this, spanCount)
            binding.recyclerView.adapter = MediaAdapter(files, currentTab)
        }

        binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun importFile(uri: android.net.Uri) {
        val type = contentResolver.getType(uri) ?: ""
        val vaultType = when {
            type.startsWith("image/") -> "image"
            type.startsWith("video/") -> "video"
            type.startsWith("audio/") -> "audio"
            else -> "file"
        }
        if (vaultManager.encryptAndStore(uri, this, vaultType)) {
            Toast.makeText(this, "File hidden", Toast.LENGTH_SHORT).show()
            refresh()
        } else {
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startSelectionMode() {
        if (isSelectionMode) return
        isSelectionMode = true
        binding.btnSelect.text = "Cancel"
        binding.btnSelect.setOnClickListener {
            actionMode?.finish()
        }
        actionMode = startSupportActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                menuInflater.inflate(R.menu.menu_selection, menu)
                return true
            }
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                when (item?.itemId) {
                    R.id.action_delete -> confirmDeleteSelected()
                    R.id.action_unhide -> confirmUnhideSelected()
                }
                return true
            }
            override fun onDestroyActionMode(mode: ActionMode?) {
                isSelectionMode = false
                selectedFiles.clear()
                binding.btnSelect.text = "Select"
                binding.btnSelect.setOnClickListener { if (!isSelectionMode) startSelectionMode() }
                (binding.recyclerView.adapter as? RecyclerView.Adapter<*>)?.notifyDataSetChanged()
            }
        })
    }

    private fun confirmDeleteSelected() {
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Move ${selectedFiles.size} item(s) to Trash?")
            .setPositiveButton("Trash") { _, _ ->
                selectedFiles.forEach { file ->
                    val type = when {
                        file.parentFile?.name == "images" -> "image"
                        file.parentFile?.name == "videos" -> "video"
                        file.parentFile?.name == "audio" -> "audio"
                        else -> "file"
                    }
                    vaultManager.moveToTrash(file, type)
                }
                refresh()
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun confirmUnhideSelected() {
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Restore ${selectedFiles.size} item(s)?")
            .setPositiveButton("Restore") { _, _ ->
                var success = 0
                selectedFiles.forEach { file ->
                    val type = when {
                        file.parentFile?.name == "images" -> "image"
                        file.parentFile?.name == "videos" -> "video"
                        file.parentFile?.name == "audio" -> "audio"
                        else -> "file"
                    }
                    if (vaultManager.unhideToPublic(this, file, type)) success++
                }
                Toast.makeText(this, "$success restored", Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun updateActionModeTitle() {
        actionMode?.title = "${selectedFiles.size} selected"
    }

    private inner class MediaAdapter(private val files: List<File>, private val tabType: String) : RecyclerView.Adapter<MediaAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.ivThumb)
            val check: ImageView = v.findViewById(R.id.ivCheck)
            val overlay: View = v.findViewById(R.id.vOverlay)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_media, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val file = files[i]
            val isImage = file.parentFile?.name == "images" || tabType == "photos"
            val isVideo = file.parentFile?.name == "videos" || tabType == "videos"

            if (isImage) {
                val decrypted = vaultManager.decryptFile(file)
                decrypted?.let { Glide.with(h.img).load(it).placeholder(R.drawable.ic_image).centerCrop().into(h.img) }
                    ?: h.img.setImageResource(R.drawable.ic_image)
            } else if (isVideo) {
                h.img.setImageResource(R.drawable.ic_video)
            } else {
                h.img.setImageResource(R.drawable.ic_image)
            }

            val isSelected = selectedFiles.contains(file)
            h.check.visibility = if (isSelected) View.VISIBLE else View.GONE
            h.overlay.visibility = if (isSelected) View.VISIBLE else View.GONE

            h.itemView.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(file)
                    notifyItemChanged(i)
                } else {
                    when {
                        isImage -> startActivity(Intent(this@GalleryActivity, ImageViewerActivity::class.java).putExtra("path", file.absolutePath))
                        isVideo -> startActivity(Intent(this@GalleryActivity, VideoPlayerActivity::class.java).putExtra("path", file.absolutePath))
                    }
                }
            }
            h.itemView.setOnLongClickListener {
                if (!isSelectionMode) startSelectionMode()
                toggleSelection(file)
                notifyItemChanged(i)
                true
            }
        }
        override fun getItemCount() = files.size

        private fun toggleSelection(file: File) {
            if (selectedFiles.contains(file)) selectedFiles.remove(file) else selectedFiles.add(file)
            updateActionModeTitle()
            if (selectedFiles.isEmpty()) actionMode?.finish()
        }
    }

    private inner class AudioAdapter(private val files: List<File>) : RecyclerView.Adapter<AudioAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvName)
            val check: ImageView = v.findViewById(R.id.ivCheck)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_file, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val file = files[i]
            h.name.text = file.name.removePrefix("file_").removeSuffix(".enc")
            val isSelected = selectedFiles.contains(file)
            h.check.visibility = if (isSelected) View.VISIBLE else View.GONE
            h.itemView.alpha = if (isSelected) 0.6f else 1.0f

            h.itemView.setOnClickListener {
                if (isSelectionMode) { toggleSelection(file); notifyItemChanged(i) }
                else { startActivity(Intent(this@GalleryActivity, AudioPlayerActivity::class.java).putExtra("path", file.absolutePath)) }
            }
            h.itemView.setOnLongClickListener {
                if (!isSelectionMode) startSelectionMode()
                toggleSelection(file); notifyItemChanged(i); true
            }
        }
        override fun getItemCount() = files.size
        private fun toggleSelection(file: File) {
            if (selectedFiles.contains(file)) selectedFiles.remove(file) else selectedFiles.add(file)
            updateActionModeTitle(); if (selectedFiles.isEmpty()) actionMode?.finish()
        }
    }
}
