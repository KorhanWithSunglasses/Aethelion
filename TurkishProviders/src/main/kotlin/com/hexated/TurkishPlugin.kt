package com.hexated

import android.content.Context
import android.util.Log
import com.hexated.extractors.*
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.utils.ExtractorApi

@CloudstreamPlugin
class TurkishPlugin : Plugin() {
    override fun load(context: Context) {
        val providers = listOf<MainAPI>(
            HDFilmCehennemiProvider(),
            DiziPalProvider(),
            FilmMakinesiProvider(),
            FullHDFilmizleseneProvider(),
            DiziBoxProvider(),
            JetFilmIzleProvider(),
            DiziWatchProvider(),
            FilmModuProvider(),
            SezonlukDiziProvider(),
            DiziMomProvider(),
            DizillaProvider(),
            SetFilmIzleProvider(),
            WebteIzleProvider(),
            DizilabProvider()
        )

        for (provider in providers) {
            try {
                registerMainAPI(provider)
            } catch (t: Throwable) {
                Log.e("TurkishPlugin", "Error registering provider ${provider.name}: ${t.message}", t)
            }
        }

        val extractors = listOf<ExtractorApi>(
            CloseLoad(),
            RapidVid(),
            VidMoxy(),
            TRsTX(),
            Sobreatsesuyp(),
            TurboImgz(),
            PixelDrain(),
            Vidmoly(),
            Rapidame(),
            Streamwish(),
            FileLions()
        )

        for (extractor in extractors) {
            try {
                registerExtractorAPI(extractor)
            } catch (t: Throwable) {
                Log.e("TurkishPlugin", "Error registering extractor ${extractor.name}: ${t.message}", t)
            }
        }
    }
}
