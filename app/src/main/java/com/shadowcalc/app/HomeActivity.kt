package com.shadowcalc.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.shadowcalc.app.databinding.ActivityHomeBinding
import java.io.File

data class HomeItem(val name: String, val iconRes: Int, val count: Int = 0, val action: () -> Unit)

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var vaultManager: VaultManager
    private var isDecoy = false
    private val lockReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == AutoLockManager.ACTION_LOCK_VAULT) {
                finishAffinity()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        vaultManager = VaultManager(this, securityManager)
        isDecoy = intent.getBooleanExtra("decoy_mode", false)

        setupStorageMeter()
        setupGrid()
        setupBrowserSearch()
        setupFab()
        setupLockButton()

        AutoLockManager.getInstance().setVaultOpen(true)
        registerReceiver(lockReceiver, android.content.IntentFilter(AutoLockManager.ACTION_LOCK_VAULT))
    }

    override fun onResume() {
        super.onResume()
        refreshStorageMeter()
        refreshGridCounts()
        AutoLockManager.getInstance().resetTimer()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        AutoLockManager.getInstance().resetTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        AutoLockManager.getInstance().setVaultOpen(false)
        try { unregisterReceiver(lockReceiver) } catch (_: Exception) {}
    }

    private fun setupStorageMeter() {
        val breakdown = vaultManager.getStorageBreakdown()
        val totalUsed = breakdown.values.sum()
        val totalMB = totalUsed / (1024 * 1024)
        val maxMB = 1024 * 10 // Assume 10GB max for visualization

        binding.tvStorageLabel.text = "Storage Used"
        binding.tvStorageValue.text = formatSize(totalUsed)

        val progressPercent = ((totalUsed.toFloat() / (maxMB * 1024 * 1024)) * 100).toInt().coerceAtMost(100)
        binding.progressStorage.progress = progressPercent

        binding.tvPhotoSize.text = "Photos: ${formatSize(breakdown["image"] ?: 0)}"
        binding.tvVideoSize.text = "Videos: ${formatSize(breakdown["video"] ?: 0)}"
        binding.tvAudioSize.text = "Audio: ${formatSize(breakdown["audio"] ?: 0)}"
        binding.tvOtherSize.text = "Other: ${formatSize(breakdown["file"] ?: 0)}"
    }

    private fun refreshStorageMeter() {
        setupStorageMeter()
    }

    private fun refreshGridCounts() {
        setupGrid()
    }

    private fun setupGrid() {
        val imageCount = vaultManager.getImages().size
        val videoCount = vaultManager.getVideos().size
        val audioCount = vaultManager.getAudio().size
        val fileCount = vaultManager.getFiles().size
        val noteCount = NoteManager(this, securityManager).loadNotes().size
        val trashCount = vaultManager.getAllTrash().size

        val items = if (isDecoy) {
            listOf(
                HomeItem("Photos", R.drawable.ic_image, 0) {},
                HomeItem("Videos", R.drawable.ic_video, 0) {},
                HomeItem("Files", R.drawable.ic_file, 0) {},
                HomeItem("Notes", R.drawable.ic_note, 0) {},
                HomeItem("Settings", R.drawable.ic_settings, 0) {}
            )
        } else {
            listOf(
                HomeItem("Photos", R.drawable.ic_image, imageCount) { startActivity(Intent(this, GalleryActivity::class.java).putExtra("tab", "photos")) },
                HomeItem("Videos", R.drawable.ic_video, videoCount) { startActivity(Intent(this, GalleryActivity::class.java).putExtra("tab", "videos")) },
                HomeItem("Audio", R.drawable.ic_audio, audioCount) { startActivity(Intent(this, GalleryActivity::class.java).putExtra("tab", "audio")) },
                HomeItem("Files", R.drawable.ic_file, fileCount) { startActivity(Intent(this, FileManagerActivity::class.java)) },
                HomeItem("Notes", R.drawable.ic_note, noteCount) { startActivity(Intent(this, NotesActivity::class.java)) },
                HomeItem("Browser", R.drawable.ic_browser, 0) { startActivity(Intent(this, BrowserActivity::class.java)) },
                HomeItem("Downloads", R.drawable.ic_download, 0) { startActivity(Intent(this, DownloadsActivity::class.java)) },
                HomeItem("Trash", R.drawable.ic_trash, trashCount) { startActivity(Intent(this, TrashActivity::class.java)) },
                HomeItem("Settings", R.drawable.ic_settings, 0) { startActivity(Intent(this, SettingsActivity::class.java)) }
            )
        }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = HomeAdapter(items)
    }

    private fun setupBrowserSearch() {
        if (isDecoy) {
            binding.etBrowserSearch.visibility = View.GONE
            return
        }
        binding.etBrowserSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etBrowserSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    val intent = Intent(this, BrowserActivity::class.java)
                    intent.putExtra("search_query", query)
                    startActivity(intent)
                }
                true
            } else false
        }
    }

    private fun setupFab() {
        if (isDecoy) {
            binding.fabAdd.visibility = View.GONE
            return
        }
        binding.fabAdd.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(Intent.createChooser(intent, "Select files to hide"), 1001)
        }
    }

    private fun setupLockButton() {
        binding.btnLock.setOnClickListener { finishAffinity() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            data?.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) {
                    val uri = clip.getItemAt(i).uri
                    val type = contentResolver.getType(uri) ?: ""
                    val vaultType = when {
                        type.startsWith("image/") -> "image"
                        type.startsWith("video/") -> "video"
                        type.startsWith("audio/") -> "audio"
                        else -> "file"
                    }
                    vaultManager.encryptAndStore(uri, this, vaultType)
                }
            } ?: data?.data?.let { uri ->
                val type = contentResolver.getType(uri) ?: ""
                val vaultType = when {
                    type.startsWith("image/") -> "image"
                    type.startsWith("video/") -> "video"
                    type.startsWith("audio/") -> "audio"
                    else -> "file"
                }
                vaultManager.encryptAndStore(uri, this, vaultType)
            }
            refreshGridCounts()
            refreshStorageMeter()
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
