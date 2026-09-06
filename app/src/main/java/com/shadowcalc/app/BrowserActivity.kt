package com.shadowcalc.app

import android.content.Intent
import android.view.ViewGroup
import android.widget.ImageView
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shadowcalc.app.databinding.ActivityBrowserBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BrowserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBrowserBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var vaultManager: VaultManager
    private val braveUserAgent = "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    private val shortcuts = listOf(
        Shortcut("Google", "https://www.google.com", R.drawable.ic_browser),
        Shortcut("GitHub", "https://github.com", R.drawable.ic_browser),
        Shortcut("Reddit", "https://www.reddit.com", R.drawable.ic_browser),
        Shortcut("Hacker News", "https://news.ycombinator.com", R.drawable.ic_browser),
        Shortcut("DuckDuckGo", "https://duckduckgo.com", R.drawable.ic_browser),
        Shortcut("Wikipedia", "https://www.wikipedia.org", R.drawable.ic_browser)
    )
    private val recentSites = mutableListOf<RecentSite>()

    data class Shortcut(val name: String, val url: String, val iconRes: Int)
    data class RecentSite(val name: String, val url: String, val time: Long)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        vaultManager = VaultManager(this, securityManager)

        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
            userAgentString = braveUserAgent
            cacheMode = WebSettings.LOAD_NO_CACHE
            setSupportZoom(true)
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(binding.webView, false)
        }

        binding.webView.webChromeClient = WebChromeClient()
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return AdBlocker.isAd(request?.url.toString())
            }
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.progressBar.visibility = View.VISIBLE
                updateShieldBadge(url)
                binding.etUrl.setText(url)
                binding.layoutHome.visibility = View.GONE
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressBar.visibility = View.GONE
                binding.etUrl.setText(url)
                updateShieldBadge(url)
                updateVideoBadge(url)
                injectVideoDetector()
                addToRecentSites(url)
            }
        }

        binding.webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            if (mimeType?.startsWith("video/") == true) {
                showResolutionPicker(url, userAgent, contentDisposition, mimeType)
            } else {
                downloadToPublic(url, userAgent, contentDisposition, mimeType)
            }
        }

        setupTopBar()
        setupBottomNav()
        setupHomePage()

        val searchQuery = intent.getStringExtra("search_query")
        if (!searchQuery.isNullOrEmpty()) {
            navigateToUrl("https://search.brave.com/search?q=${Uri.encode(searchQuery)}")
        } else {
            showHomePage()
        }
    }

    private fun setupTopBar() {
        binding.btnExit.setOnClickListener { finish() }
        binding.btnGo.setOnClickListener { navigateToUrl(binding.etUrl.text.toString()) }
        binding.etUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                navigateToUrl(binding.etUrl.text.toString()); true
            } else false
        }
    }

    private fun setupBottomNav() {
        binding.btnNavBack.setOnClickListener { if (binding.webView.canGoBack()) binding.webView.goBack() }
        binding.btnNavForward.setOnClickListener { if (binding.webView.canGoForward()) binding.webView.goForward() }
        binding.btnNavHome.setOnClickListener { showHomePage() }
        binding.btnNavDownloads.setOnClickListener { startActivity(Intent(this, DownloadsActivity::class.java)) }
        binding.btnNavMenu.setOnClickListener { showBrowserMenu() }
    }

    private fun setupHomePage() {
        binding.recyclerShortcuts.layoutManager = GridLayoutManager(this, 4)
        binding.recyclerShortcuts.adapter = ShortcutAdapter(shortcuts)

        binding.recyclerRecent.layoutManager = LinearLayoutManager(this)
        loadRecentSites()
    }

    private fun showHomePage() {
        binding.webView.loadUrl("about:blank")
        binding.layoutHome.visibility = View.VISIBLE
        binding.cardVideoBadge.visibility = View.GONE
        loadRecentSites()
    }

    private fun navigateToUrl(input: String) {
        var url = input
        if (url.isBlank()) return
        binding.layoutHome.visibility = View.GONE
        if (!url.startsWith("http")) {
            if (url.contains(".") && !url.contains(" ")) {
                url = "https://$url"
            } else {
                url = "https://search.brave.com/search?q=${Uri.encode(url)}"
            }
        }
        binding.webView.loadUrl(url)
    }

    private fun updateShieldBadge(url: String?) {
        val isSecure = url != null && (url.startsWith("https://") || url.startsWith("about:blank"))
        binding.ivShield.setImageResource(if (isSecure) R.drawable.ic_lock else R.drawable.ic_browser)
        binding.tvShieldStatus.text = if (isSecure) "Shield Active" else "Not Secure"
        binding.tvShieldStatus.setTextColor(getColor(if (isSecure) R.color.accent else R.color.error))
    }

    private fun updateVideoBadge(url: String?) {
        val isVideoPage = url != null && (
            url.contains("youtube.com/watch") || url.contains("youtu.be/") ||
            url.contains(".mp4") || url.contains(".m3u8") || url.contains(".webm") ||
            url.contains("vimeo.com") || url.contains("dailymotion.com") ||
            url.contains("tiktok.com") || url.contains("facebook.com/watch")
        )
        binding.cardVideoBadge.visibility = if (isVideoPage) View.VISIBLE else View.GONE
        if (isVideoPage) {
            binding.cardVideoBadge.setOnClickListener { triggerVideoDownload(url!!) }
        }
    }

    private fun injectVideoDetector() {
        val js = """
            (function(){
                if (window._shadowVideoInjected) return;
                window._shadowVideoInjected = true;
                var videos = document.querySelectorAll('video');
                videos.forEach(function(v){
                    v.addEventListener('play', function(){
                        if (v.src && v.src.startsWith('http')) {
                            console.log('SHADOW_VIDEO_DETECTED:' + v.src);
                        }
                    });
                });
            })()
        """.trimIndent()
        binding.webView.evaluateJavascript(js, null)
    }

    private fun triggerVideoDownload(url: String) {
        binding.webView.evaluateJavascript(
            "(function(){var v=document.querySelector('video');if(v&&v.src)return v.src;var s=document.querySelectorAll('source');for(var i=0;i<s.length;i++)if(s[i].src)return s[i].src;return '';})()"
        ) { result ->
            val videoUrl = result?.trim('"') ?: ""
            if (videoUrl.isNotEmpty() && videoUrl.startsWith("http")) {
                showResolutionPicker(videoUrl, binding.webView.settings.userAgentString, "", "video/mp4")
            } else {
                showResolutionPicker(url, binding.webView.settings.userAgentString, "", "video/mp4")
            }
        }
    }

    private fun showResolutionPicker(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) {
        val resolutions = arrayOf("1080p (~2GB/hr)", "720p (~1GB/hr)", "480p (~500MB/hr)", "360p (~300MB/hr)")
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Download Video")
            .setItems(resolutions) { _, which ->
                downloadVideoToVault(url, userAgent, resolutions[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun downloadVideoToVault(url: String, userAgent: String?, label: String) {
        Toast.makeText(this, "Downloading $label to vault...", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val connection = java.net.URL(url).openConnection()
                connection.setRequestProperty("User-Agent", userAgent ?: braveUserAgent)
                val cookies = CookieManager.getInstance().getCookie(url)
                if (cookies != null) connection.setRequestProperty("Cookie", cookies)
                connection.connect()
                val bytes = connection.inputStream.use { it.readBytes() }
                val fileName = "video_" + System.currentTimeMillis() + ".mp4"
                val success = vaultManager.saveDownloadedVideo(bytes, fileName)
                runOnUiThread {
                    if (success) Toast.makeText(this, "Saved to vault: $fileName", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Download error: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun downloadToPublic(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimeType ?: "*/*")
            request.addRequestHeader("User-Agent", userAgent ?: "")
            val cookies = CookieManager.getInstance().getCookie(url)
            if (cookies != null) request.addRequestHeader("cookie", cookies)
            request.setDescription("Downloading...")
            val fileName = if (!contentDisposition.isNullOrEmpty()) {
                URLUtil.guessFileName(url, contentDisposition, mimeType)
            } else {
                val name = Uri.parse(url).lastPathSegment ?: "download"
                if (name.contains(".")) name else "$name.mp4"
            }
            request.setTitle(fileName)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(this, "Downloading: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBrowserMenu() {
        val items = arrayOf("Refresh", "Clear Cache & Cookies", "Share URL", "Find in Page")
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> binding.webView.reload()
                    1 -> {
                        binding.webView.clearCache(true)
                        CookieManager.getInstance().removeAllCookies(null)
                        Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, binding.webView.url ?: "")
                        }
                        startActivity(Intent.createChooser(share, "Share URL"))
                    }
                    3 -> binding.webView.showFindDialog("", true)
                }
            }
            .show()
    }

    private fun addToRecentSites(url: String?) {
        if (url == null || url == "about:blank") return
        val name = try { Uri.parse(url).host ?: url } catch (_: Exception) { url }
        recentSites.removeAll { it.url == url }
        recentSites.add(0, RecentSite(name, url, System.currentTimeMillis()))
        if (recentSites.size > 10) recentSites.removeLast()
        saveRecentSites()
        loadRecentSites()
    }

    private fun saveRecentSites() {
        val prefs = getSharedPreferences("browser_prefs", Context.MODE_PRIVATE)
        val json = recentSites.joinToString("\n") { "${it.name}|${it.url}|${it.time}" }
        prefs.edit().putString("recent_sites", json).apply()
    }

    private fun loadRecentSites() {
        val prefs = getSharedPreferences("browser_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("recent_sites", "") ?: ""
        recentSites.clear()
        json.split("\n").forEach { line ->
            val parts = line.split("|")
            if (parts.size == 3) {
                recentSites.add(RecentSite(parts[0], parts[1], parts[2].toLongOrNull() ?: 0))
            }
        }
        binding.recyclerRecent.adapter = RecentAdapter(recentSites)
        binding.tvRecentEmpty.visibility = if (recentSites.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onBackPressed() {
        if (binding.layoutHome.visibility == View.VISIBLE) {
            finish()
        } else if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.webView.clearCache(true)
        CookieManager.getInstance().removeSessionCookies(null)
        binding.webView.destroy()
    }

    private inner class ShortcutAdapter(private val items: List<Shortcut>) : RecyclerView.Adapter<ShortcutAdapter.VH>() {
        inner class VH(v: android.view.View) : RecyclerView.ViewHolder(v) {
            val name: android.widget.TextView = v.findViewById(R.id.tvShortcutName)
            val icon: ImageView = v.findViewById(R.id.ivShortcutIcon)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(android.view.LayoutInflater.from(p.context).inflate(R.layout.item_shortcut, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val item = items[i]
            h.name.text = item.name
            h.icon.setImageResource(item.iconRes)
            h.itemView.setOnClickListener { navigateToUrl(item.url) }
        }
        override fun getItemCount() = items.size
    }

    private inner class RecentAdapter(private val items: List<RecentSite>) : RecyclerView.Adapter<RecentAdapter.VH>() {
        inner class VH(v: android.view.View) : RecyclerView.ViewHolder(v) {
            val name: android.widget.TextView = v.findViewById(R.id.tvRecentName)
            val url: android.widget.TextView = v.findViewById(R.id.tvRecentUrl)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(android.view.LayoutInflater.from(p.context).inflate(R.layout.item_recent, p, false))
        override fun onBindViewHolder(h: VH, i: Int) {
            val item = items[i]
            h.name.text = item.name
            h.url.text = item.url
            h.itemView.setOnClickListener { navigateToUrl(item.url) }
        }
        override fun getItemCount() = items.size
    }
}
