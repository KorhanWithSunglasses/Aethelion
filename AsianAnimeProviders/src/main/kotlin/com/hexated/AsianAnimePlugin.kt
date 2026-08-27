package com.hexated

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class AsianAnimePlugin : Plugin() {
    override fun load(context: Context) {
        // Register Turkish Anime & Asian Drama Providers
        registerMainAPI(TurkanimeProvider())
        registerMainAPI(AnimecixProvider())
        registerMainAPI(AnizmProvider())
        registerMainAPI(KoreanTurkProvider())
        registerMainAPI(AsyaLogProvider())
        registerMainAPI(DramacoolProvider())
        registerMainAPI(CizgiMaxProvider())
    }
}
