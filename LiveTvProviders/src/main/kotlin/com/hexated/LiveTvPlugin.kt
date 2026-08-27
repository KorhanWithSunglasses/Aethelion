package com.hexated

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class LiveTvPlugin : Plugin() {
    override fun load(context: Context) {
        // Register Live TV and IPTV Providers
        registerMainAPI(UlusalTvProvider())
        registerMainAPI(CanliYayinProvider())
    }
}
