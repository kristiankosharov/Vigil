package app.vigil

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else ReleaseTree())
    }
}

/** Release logging: only warnings and errors, forwarded to the crash reporter once one is added. */
private class ReleaseTree : Timber.Tree() {
    override fun isLoggable(tag: String?, priority: Int) = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // TODO(crash-reporting): forward to the crash reporter when one is added.
    }
}
