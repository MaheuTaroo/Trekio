/**
 * The code inside this file has been extracted from the
 * codebase for the Alert-KMP library. It can be found at
 * https://github.com/KhubaibKhan4/Alert-KMP; last read
 * was on 21/07/2026, on commit with hash 7817665
 */

package pt.trekio.misc

import android.app.Application
import android.content.Context
import androidx.startup.Initializer

internal class AppContextInitializer : Initializer<Context> {
    override fun create(context: Context): Context {
        AppContext.setUp(context.applicationContext)
        return AppContext.get()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

internal object AppContext {
    private lateinit var application: Application

    fun setUp(context: Context) {
        application = context as Application
    }

    fun get(): Context =
        if (AppContext::application.isInitialized.not()) {
            throw Exception("Context is not initialized.")
        } else {
            application.applicationContext
        }
}
