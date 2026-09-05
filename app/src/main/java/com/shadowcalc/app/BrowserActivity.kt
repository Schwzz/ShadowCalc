package com.shadowcalc.app

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.MotionEvent
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.shadowcalc.app.databinding.ActivityBrowserBinding
import java.io.File

class BrowserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBrowserBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var vaultManager: VaultManager
    private var dX = 0f
    private var dY = 0f
    private val braveUserAgent = "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 Brave/120.0.0.0"
    private val shortcuts = mutableListOf(
        Shortcut("Google", "https://www.google.com", R.drawable.ic_browser),
        Shortcut("Facebook", "https://www.facebook.com", R.drawable.ic_browser),
        Shortcut("Amazon", "https://www.amazon.com", R.drawable.ic_browser),
        Shortcut("Reddit", "https://www.reddit.com", R.drawable.ic_browser),
        Shortcut("Twitter", "https://twitter.com", R.drawable.ic_browser),
        Shortcut("Wikipedia", "https://www.wikipedia.org", R.drawable.ic_browser)
    )

    data class Shortcut(val name: String, val url: String, val iconRes: Int)

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

        // Privacy: disable third-party cookies
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
                updateFabVisibility(url)
                binding.etUrl.setText(url)
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressBar.visibility = View.GONE
                binding.etUrl.setText(url)
                updateFabVisibility(url)
                injectVideoDetector()
            }
        }

        binding.webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            if (mimeType?.startsWith("video/") == true) {
                showResolutionPicker(url, userAgent, contentDisposition, mimeType)
            } else {
                downloadToPublic(url, userAgent, contentDisposition, mimeType)
            }
        }

        // Top bar actions
        binding.btnExit.setOnClickListener { finish() }
        binding.btnGo.setOnClickListener { navigateToUrl() }
        binding.etUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                navigateToUrl(); true
            } else false
        }

        // Bottom nav
        binding.btnNavBack.setOnClickListener { if (binding.webView.canGoBack()) binding.webView.goBack() }
        binding.btnNavForward.setOnClickListener { if (binding.webView.canGoForward()) binding.webView.goForward() }
        binding.btnNavHome.setOnClickListener { showHomePage() }
        binding.btnNavTabs.setOnClickListener { Toast.makeText(this, "Tab switcher: 1 tab", Toast.LENGTH_SHORT).show() }
        binding.btnNavDownloads.setOnClickListener { startActivity(Intent(this, DownloadsActivity::class.java)) }
        binding.btnNavMenu.setOnClickListener { showBrowserMenu() }

        // How-to banner
        binding.bannerHowTo.setOnClickListener {
            AlertDialog.Builder(this, R.style.DarkAlertDialog)
                .setTitle("How to Download Videos")
                .setMessage("1. Navigate to a video page (YouTube, Vimeo, etc.)\n2. Wait for the floating download button to appear\n3. Tap it and select your preferred resolution\n4. The video will be saved directly to your hidden vault.")
                .setPositiveButton("Got it", null)
                .show()
        }

        // Floating download button
        binding.fabDownload.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { dX = v.x - event.rawX; dY = v.y - event.rawY }
                MotionEvent.ACTION_MOVE -> { v.animate().x(event.rawX + dX).y(event.rawY + dY).setDuration(0).start() }
                MotionEvent.ACTION_UP -> {
                    val url = binding.webView.url
                    if (url != null) triggerVideoDownload(url)
                }
            }
            true
        }

        // Load search query or home
        val searchQuery = intent.getStringExtra("search_query")
        if (!searchQuery.isNullOrEmpty()) {
            binding.webView.loadUrl("https://search.brave.com/search?q=${Uri.encode(searchQuery)}")
            binding.etUrl.setText("https://search.brave.com/search?q=$searchQuery")
        } else {
            showHomePage()
        }
    }

    private fun showHomePage() {
        binding.webView.loadUrl("about:blank")
        binding.layoutHome.visibility = View.VISIBLE
        binding.fabDownload.visibility = View.GONE
        setupShortcuts()
    }

    private fun setupShortcuts() {
        // In a real app, this would be a RecyclerView. For V4 we use a simplified approach.
        binding.gridShortcuts.removeAllViews()
        shortcuts.forEach { shortcut ->
            val btn = android.widget.Button(this).apply {
                text = shortcut.name
                setTextColor(getColor(R.color.text_primary))
                background = getDrawable(R.drawable.bg_card)
                setOnClickListener {
                    binding.layoutHome.visibility = View.GONE
                    binding.webView.loadUrl(shortcut.url)
                }
            }
            binding.gridShortcuts.addView(btn, android.widget.GridLayout.LayoutParams().apply {
                width = 0
                height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                rowSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED)
                setMargins(8, 8, 8, 8)
            })
        }
    }

    private fun navigateToUrl() {
        var url = binding.etUrl.text.toString()
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

    private fun updateFabVisibility(url: String?) {
        val isVideoPage = url != null && (
            url.contains("youtube.com/watch") || url.contains("youtu.be/") ||
            url.contains(".mp4") || url.contains(".m3u8") || url.contains(".webm") ||
            url.contains("vimeo.com") || url.contains("dailymotion.com") ||
            url.contains("tiktok.com") || url.contains("facebook.com/watch")
        )
        binding.fabDownload.visibility = if (isVideoPage) View.VISIBLE else View.GONE
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
                val label = resolutions[which]
                downloadVideoToVault(url, userAgent, label)
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
        // Clear session cache on exit
        binding.webView.clearCache(true)
        CookieManager.getInstance().removeSessionCookies(null)
        binding.webView.destroy()
    }
}
