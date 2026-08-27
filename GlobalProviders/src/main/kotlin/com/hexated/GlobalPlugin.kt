package com.hexated

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class GlobalPlugin : Plugin() {
    override fun load(context: Context) {
        // Register Global Providers
        registerMainAPI(SFlixProvider())
        registerMainAPI(LookMovieProvider())
        registerMainAPI(VidsrcProvider())
        registerMainAPI(GogoanimeProvider())
        registerMainAPI(AnimePaheProvider())
    }
}
