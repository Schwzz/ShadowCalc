package com.shadowcalc.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shadowcalc.app.databinding.ActivitySearchBinding
import java.io.File

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var vaultManager: VaultManager
    private lateinit var noteManager: NoteManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        vaultManager = VaultManager(this, securityManager)
        noteManager = NoteManager(this, securityManager)

        val query = intent.getStringExtra("query") ?: ""
        binding.tvQuery.text = "Results for "$query""
        binding.btnBack.setOnClickListener { finish() }

        performSearch(query)
    }

    private fun performSearch(query: String) {
        val results = mutableListOf<SearchResult>()
        val q = query.lowercase()

        vaultManager.getImages().forEach { f ->
            if (f.name.lowercase().contains(q)) results.add(SearchResult(f.name, "Image", f, "image"))
        }
        vaultManager.getVideos().forEach { f ->
            if (f.name.lowercase().contains(q)) results.add(SearchResult(f.name, "Video", f, "video"))
        }
        vaultManager.getAudio().forEach { f ->
            if (f.name.lowercase().contains(q)) results.add(SearchResult(f.name, "Audio", f, "audio"))
        }
        vaultManager.getFiles().forEach { f ->
            if (f.name.lowercase().contains(q)) results.add(SearchResult(f.name, "File", f, "file"))
        }
        noteManager.loadNotes().forEach { n ->
            if (n.title.lowercase().contains(q) || n.content.lowercase().contains(q))
                results.add(SearchResult(n.title, "Note", null, "note", n))
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = SearchAdapter(results)
        binding.tvEmpty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
    }

    data class SearchResult(val name: String, val type: String, val file: File?, val fileType: String, val note: Note? = null)

    private inner class SearchAdapter(private val list: List<SearchResult>) : RecyclerView.Adapter<SearchAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.ivIcon)
            val name: TextView = v.findViewById(R.id.tvName)
            val type: TextView = v.findViewById(R.id.tvType)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_search, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val r = list[i]
            h.name.text = r.name
            h.type.text = r.type
            val iconRes = when (r.type) {
                "Image" -> R.drawable.ic_image
                "Video" -> R.drawable.ic_video
                "Audio" -> R.drawable.ic_audio
                "File" -> R.drawable.ic_file
                "Note" -> R.drawable.ic_note
                else -> R.drawable.ic_file
            }
            h.icon.setImageResource(iconRes)

            h.itemView.setOnClickListener {
                when (r.type) {
                    "Image" -> startActivity(Intent(this@SearchActivity, ImageViewerActivity::class.java).putExtra("path", r.file!!.absolutePath))
                    "Video" -> startActivity(Intent(this@SearchActivity, VideoPlayerActivity::class.java).putExtra("path", r.file!!.absolutePath))
                    "Audio" -> startActivity(Intent(this@SearchActivity, AudioPlayerActivity::class.java).putExtra("path", r.file!!.absolutePath))
                    "File" -> openFile(r.file!!)
                    "Note" -> startActivity(Intent(this@SearchActivity, NotesActivity::class.java))
                }
            }
        }
        override fun getItemCount() = list.size

        private fun openFile(file: File) {
            val decrypted = vaultManager.decryptFile(file) ?: return
            val temp = File(cacheDir, "temp_" + System.currentTimeMillis() + "_" + file.name.removeSuffix(".enc"))
            temp.writeBytes(decrypted)
            val uri = FileProvider.getUriForFile(this@SearchActivity, "${packageName}.provider", temp)
            val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try { startActivity(intent) } catch (_: Exception) {}
        }
    }
}
