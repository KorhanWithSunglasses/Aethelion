package com.hexated

import android.content.Context
import com.hexated.extractors.*
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class TurkishPlugin : Plugin() {
    override fun load(context: Context) {
        // Register all Core Turkish Providers
        registerMainAPI(HDFilmCehennemiProvider())
        registerMainAPI(DiziPalProvider())
        registerMainAPI(FilmMakinesiProvider())
        registerMainAPI(FullHDFilmizleseneProvider())
        registerMainAPI(DiziBoxProvider())
        registerMainAPI(JetFilmIzleProvider())
        registerMainAPI(DiziWatchProvider())
        registerMainAPI(FilmModuProvider())
        registerMainAPI(SezonlukDiziProvider())
        registerMainAPI(DiziMomProvider())
        registerMainAPI(DizillaProvider())
        registerMainAPI(SetFilmIzleProvider())
        registerMainAPI(WebteIzleProvider())
        registerMainAPI(DizilabProvider())

        // Register all Custom Turkish Extractors
        registerExtractorAPI(CloseLoad())
        registerExtractorAPI(RapidVid())
        registerExtractorAPI(VidMoxy())
        registerExtractorAPI(TRsTX())
        registerExtractorAPI(Sobreatsesuyp())
        registerExtractorAPI(TurboImgz())
        registerExtractorAPI(PixelDrain())
        registerExtractorAPI(Vidmoly())
        registerExtractorAPI(Rapidame())
        registerExtractorAPI(Streamwish())
        registerExtractorAPI(FileLions())
    }
}
