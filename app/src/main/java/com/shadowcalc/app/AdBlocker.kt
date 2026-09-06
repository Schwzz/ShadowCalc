package com.shadowcalc.app

import java.net.URL

object AdBlocker {
    private val adDomains = setOf(
        "googleadservices.com", "googlesyndication.com", "google-analytics.com",
        "doubleclick.net", "adservice.google.com", "facebook.com/tr",
        "amazon-adsystem.com", "adsystem.amazon.com", "outbrain.com",
        "taboola.com", "ads.yahoo.com", "advertising.com",
        "adsrvr.org", "adsystem.com", "adsafeprotected.com",
        "moatads.com", "scorecardresearch.com", "quantserve.com",
        "googletagmanager.com", "googletagservices.com", "googleads.g.doubleclick.net"
    )

    fun isAd(url: String?): Boolean {
        if (url == null) return false
        return try {
            val host = URL(url).host.lowercase()
            adDomains.any { host.contains(it) || it.contains(host) }
        } catch (e: Exception) {
            false
        }
    }

    fun getBlockedDomains(): Set<String> = adDomains
}
