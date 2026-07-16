package pt.trekio.platform

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import pt.trekio.repos.SettingsRepository

class TrekioAndroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        val language = SettingsRepository().getLanguage()

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.tag),
        )
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}
