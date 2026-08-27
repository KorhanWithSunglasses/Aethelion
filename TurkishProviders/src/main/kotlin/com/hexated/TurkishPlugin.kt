package com.hexated

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class TurkishPlugin : Plugin() {
    override fun load(context: Context) {
        // Register all Phase 1 Core Turkish Providers
        registerMainAPI(HDFilmCehennemiProvider())
        registerMainAPI(DiziWatchProvider())
        registerMainAPI(FilmModuProvider())
        registerMainAPI(SezonlukDiziProvider())
        registerMainAPI(DiziPalProvider())
        registerMainAPI(DiziMomProvider())
        registerMainAPI(FilmMakinesiProvider())
        registerMainAPI(DizillaProvider())
        registerMainAPI(SetFilmIzleProvider())
        registerMainAPI(WebteIzleProvider())
        registerMainAPI(DiziBoxProvider())
        registerMainAPI(DizilabProvider())
    }
}
