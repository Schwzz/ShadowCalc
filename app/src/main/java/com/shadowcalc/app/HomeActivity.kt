package com.shadowcalc.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.shadowcalc.app.databinding.ActivityHomeBinding

data class HomeItem(val name: String, val iconRes: Int, val action: () -> Unit)

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val items = listOf(
            HomeItem("Images", R.drawable.ic_image) { startActivity(Intent(this, ImageVaultActivity::class.java)) },
            HomeItem("Videos", R.drawable.ic_video) { startActivity(Intent(this, VideoVaultActivity::class.java)) },
            HomeItem("Files", R.drawable.ic_file) { startActivity(Intent(this, FileManagerActivity::class.java)) },
            HomeItem("Notes", R.drawable.ic_note) { startActivity(Intent(this, NotesActivity::class.java)) },
            HomeItem("Browser", R.drawable.ic_browser) { startActivity(Intent(this, BrowserActivity::class.java)) },
            HomeItem("Downloads", R.drawable.ic_download) { startActivity(Intent(this, DownloadsActivity::class.java)) },
            HomeItem("Trash", R.drawable.ic_trash) { startActivity(Intent(this, TrashActivity::class.java)) },
            HomeItem("Settings", R.drawable.ic_settings) { startActivity(Intent(this, SettingsActivity::class.java)) }
        )
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = HomeAdapter(items)
        binding.btnLock.setOnClickListener { finish() }
    }
}
