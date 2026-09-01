package com.hexated

import android.content.Context
import android.util.Log
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class LiveTvPlugin : Plugin() {
    override fun load(context: Context) {
        val providers = listOf<MainAPI>(
            UlusalTvProvider(),
            CanliYayinProvider()
        )

        for (provider in providers) {
            try {
                registerMainAPI(provider)
            } catch (t: Throwable) {
                Log.e("LiveTvPlugin", "Error registering provider ${provider.name}: ${t.message}", t)
            }
        }
    }
}
