package com.shadowcalc.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private val handler = Handler(Looper.getMainLooper())
    private var autoLockRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        isDecoy = intent.getBooleanExtra("decoy_mode", false)

        setupGrid()
        setupSearch()
        setupAutoLock()
    }

    override fun onResume() {
        super.onResume()
        resetAutoLock()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetAutoLock()
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
            mutableListOf(
                HomeItem("Images", R.drawable.ic_image) { startActivity(Intent(this, ImageVaultActivity::class.java)) },
                HomeItem("Videos", R.drawable.ic_video) { startActivity(Intent(this, VideoVaultActivity::class.java)) },
                HomeItem("Audio", R.drawable.ic_audio) { startActivity(Intent(this, AudioVaultActivity::class.java)) },
                HomeItem("Files", R.drawable.ic_file) { startActivity(Intent(this, FileManagerActivity::class.java)) },
                HomeItem("Notes", R.drawable.ic_note) { startActivity(Intent(this, NotesActivity::class.java)) },
                HomeItem("Passwords", R.drawable.ic_lock) { startActivity(Intent(this, PasswordsActivity::class.java)) },
                HomeItem("Browser", R.drawable.ic_browser) { startActivity(Intent(this, BrowserActivity::class.java)) },
                HomeItem("Downloads", R.drawable.ic_download) { startActivity(Intent(this, DownloadsActivity::class.java)) },
                HomeItem("Intruders", R.drawable.ic_camera) { startActivity(Intent(this, IntrudersActivity::class.java)) },
                HomeItem("Trash", R.drawable.ic_trash) { startActivity(Intent(this, TrashActivity::class.java)) },
                HomeItem("Settings", R.drawable.ic_settings) { startActivity(Intent(this, SettingsActivity::class.java)) }
            )
        }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = HomeAdapter(items)
        binding.btnLock.setOnClickListener { finishAffinity() }
    }

    private fun setupSearch() {
        if (isDecoy) {
            binding.etSearch.visibility = View.GONE
            return
        }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    startActivity(Intent(this, SearchActivity::class.java).putExtra("query", query))
                }
                true
            } else false
        }
    }

    private fun setupAutoLock() {
        val minutes = securityManager.getAutoLockMinutes()
        if (minutes > 0) {
            autoLockRunnable = Runnable {
                finishAffinity()
            }
        }
    }

    private fun resetAutoLock() {
        autoLockRunnable?.let {
            handler.removeCallbacks(it)
            val minutes = securityManager.getAutoLockMinutes()
            if (minutes > 0) {
                handler.postDelayed(it, minutes * 60 * 1000L)
            }
        }
    }
}
