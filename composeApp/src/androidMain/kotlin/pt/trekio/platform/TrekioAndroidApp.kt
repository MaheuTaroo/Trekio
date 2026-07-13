package pt.trekio.platform

import android.app.Application
import android.content.Context

class TrekioAndroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}
