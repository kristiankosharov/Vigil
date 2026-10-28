package app.vigil

import android.app.Application
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else ReleaseTree())
    }
}

/**
 * Release logging: only warnings and errors, sent to Crashlytics. Every message becomes a breadcrumb
 * in the next crash report; an error with an exception is also reported as a non-fatal. Messages
 * never contain tokens or personal data (CLAUDE.md, Logging), so they are safe to send.
 */
private class ReleaseTree : Timber.Tree() {
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun isLoggable(tag: String?, priority: Int) = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        crashlytics.log("${tag ?: "-"}: $message")
        if (priority >= Log.ERROR && t != null) crashlytics.recordException(t)
    }
}
