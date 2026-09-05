package com.shadowcalc.app

object AdBlocker {
    private val adDomains = setOf(
        "googleadservices.com", "googlesyndication.com", "google-analytics.com",
        "doubleclick.net", "facebook.com/tr", "adsystem.amazon.com",
        "advertising.com", "adnxs.com", "adsrvr.org", "taboola.com",
        "outbrain.com", "scorecardresearch.com", "moatads.com",
        "ads.yahoo.com", "advertising.yahoo.com", "adsafeprotected.com"
    )

    fun isAd(url: String?): Boolean {
        if (url == null) return false
        return adDomains.any { url.contains(it) }
    }
}