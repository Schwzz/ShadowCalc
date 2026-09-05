package com.shadowcalc.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.shadowcalc.app.databinding.ActivityHomeBinding

data class HomeItem(val name: String, val iconRes: Int, val action: () -> Unit)

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var securityManager: SecurityManager
    private var isDecoy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        isDecoy = intent.getBooleanExtra("decoy_mode", false)

        setupGrid()
        setupBrowserSearch()
        setupFab()
    }

    private fun setupGrid() {
        val items = if (isDecoy) {
            listOf(
                HomeItem("Images", R.drawable.ic_image) {},
                HomeItem("Videos", R.drawable.ic_video) {},
                HomeItem("Files", R.drawable.ic_file) {},
                HomeItem("Notes", R.drawable.ic_note) {},
                HomeItem("Settings", R.drawable.ic_settings) {}
            )
        } else {
            listOf(
                HomeItem("Images", R.drawable.ic_image) { startActivity(Intent(this, ImageVaultActivity::class.java)) },
                HomeItem("Videos", R.drawable.ic_video) { startActivity(Intent(this, VideoVaultActivity::class.java)) },
                HomeItem("Audio", R.drawable.ic_audio) { startActivity(Intent(this, AudioVaultActivity::class.java)) },
                HomeItem("Files", R.drawable.ic_file) { startActivity(Intent(this, FileManagerActivity::class.java)) },
                HomeItem("Notes", R.drawable.ic_note) { startActivity(Intent(this, NotesActivity::class.java)) },
                HomeItem("Browser", R.drawable.ic_browser) { startActivity(Intent(this, BrowserActivity::class.java)) },
                HomeItem("Downloads", R.drawable.ic_download) { startActivity(Intent(this, DownloadsActivity::class.java)) },
                HomeItem("Trash", R.drawable.ic_trash) { startActivity(Intent(this, TrashActivity::class.java)) },
                HomeItem("Settings", R.drawable.ic_settings) { startActivity(Intent(this, SettingsActivity::class.java)) }
            )
        }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = HomeAdapter(items)
        binding.btnLock.setOnClickListener { finishAffinity() }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val vaultManager = VaultManager(this, securityManager)
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
        }
    }
}
