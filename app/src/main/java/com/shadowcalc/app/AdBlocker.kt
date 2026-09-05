package com.shadowcalc.app

object AdBlocker {
    private val adDomains = setOf("googleadservices.com","googlesyndication.com","google-analytics.com","doubleclick.net",
        "adservice.google.com","googletagmanager.com","facebook.com/tr","connect.facebook.net","amazon-adsystem.com",
        "ads.yahoo.com","advertising.com","adnxs.com","adsystem.com","moatads.com","outbrain.com","taboola.com",
        "scorecardresearch.com","quantserve.com","googletagservices.com","googleads.g.doubleclick.net")
    fun isAd(url: String): Boolean = adDomains.any { url.lowercase().contains(it) }
}
