package com.shadowcalc.app

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shadowcalc.app.databinding.ActivityVaultBinding
import java.io.File

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVaultBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var vaultManager: VaultManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVaultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        vaultManager = VaultManager(this, securityManager)
        binding.tvTitle.text = "Search"
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.visibility = View.GONE
        binding.etSearch.visibility = View.VISIBLE
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search(binding.etSearch.text.toString())
                true
            } else false
        }
        refresh(emptyList())
    }

    private fun search(query: String) {
        val allFiles = vaultManager.getImages() + vaultManager.getVideos() + vaultManager.getAudio() + vaultManager.getFiles()
        val results = allFiles.filter { it.name.contains(query, ignoreCase = true) }
        refresh(results)
    }

    private fun refresh(files: List<File>) {
        binding.tvCount.text = "${files.size} results"
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = SearchAdapter(files)
        binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
    }

    private inner class SearchAdapter(private val files: List<File>) : RecyclerView.Adapter<SearchAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: android.widget.TextView = v.findViewById(R.id.tvName)
            val type: android.widget.TextView = v.findViewById(R.id.tvType)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(android.view.LayoutInflater.from(p.context).inflate(R.layout.item_search, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val file = files[i]
            h.name.text = file.name
            h.type.text = file.parentFile?.name ?: "unknown"
        }
        override fun getItemCount() = files.size
    }
}
