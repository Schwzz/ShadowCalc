package com.shadowcalc.app

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.MotionEvent
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shadowcalc.app.databinding.ActivityBrowserBinding

class BrowserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBrowserBinding
    private var dX = 0f
    private var dY = 0f

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
        }

        binding.webView.webChromeClient = WebChromeClient()
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return AdBlocker.isAd(request?.url.toString())
            }
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.progressBar.visibility = View.VISIBLE
                updateFabVisibility(url)
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressBar.visibility = View.GONE
                binding.etUrl.setText(url)
                updateFabVisibility(url)
            }
        }

        binding.webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            downloadFile(url, userAgent, contentDisposition, mimeType)
        }

        binding.btnGo.setOnClickListener {
            var url = binding.etUrl.text.toString()
            if (url.isBlank()) return@setOnClickListener
            if (!url.startsWith("http")) url = "https://$url"
            binding.webView.loadUrl(url)
        }
        binding.btnBack.setOnClickListener { if (binding.webView.canGoBack()) binding.webView.goBack() else finish() }
        binding.btnForward.setOnClickListener { if (binding.webView.canGoForward()) binding.webView.goForward() }
        binding.btnRefresh.setOnClickListener { binding.webView.reload() }
        binding.btnHome.setOnClickListener { binding.webView.loadUrl("https://duckduckgo.com") }

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

        binding.webView.loadUrl("https://duckduckgo.com")
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

    private fun triggerVideoDownload(url: String) {
        binding.webView.evaluateJavascript(
            "(function(){var v=document.querySelector('video');if(v&&v.src)return v.src;var s=document.querySelectorAll('source');for(var i=0;i<s.length;i++)if(s[i].src)return s[i].src;return '';})()"
        ) { result ->
            val videoUrl = result?.trim('"') ?: ""
            if (videoUrl.isNotEmpty() && videoUrl.startsWith("http")) {
                downloadFile(videoUrl, binding.webView.settings.userAgentString, "", "video/mp4")
            } else {
                downloadFile(url, binding.webView.settings.userAgentString, "", "video/mp4")
            }
        }
    }

    private fun downloadFile(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) {
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

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) binding.webView.goBack()
        else super.onBackPressed()
    }
}
