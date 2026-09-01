package com.hexated

import android.content.Context
import android.util.Log
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class AsianAnimePlugin : Plugin() {
    override fun load(context: Context) {
        val providers = listOf<MainAPI>(
            TurkanimeProvider(),
            AnimecixProvider(),
            AnizmProvider(),
            KoreanTurkProvider(),
            AsyaLogProvider(),
            DramacoolProvider(),
            CizgiMaxProvider()
        )

        for (provider in providers) {
            try {
                registerMainAPI(provider)
            } catch (t: Throwable) {
                Log.e("AsianAnimePlugin", "Error registering provider ${provider.name}: ${t.message}", t)
            }
        }
    }
}
