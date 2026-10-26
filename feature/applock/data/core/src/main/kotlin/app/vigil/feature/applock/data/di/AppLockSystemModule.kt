package app.vigil.feature.applock.data.di

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import app.vigil.feature.applock.data.session.BiometricAvailability
import app.vigil.feature.applock.data.session.DeviceClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/** The device facts the app lock depends on: time, boot count and biometrics. */
@Module
@InstallIn(SingletonComponent::class)
internal object AppLockSystemModule {
    @Provides
    fun provideTimeSource(): TimeSource = TimeSource.Monotonic

    @Provides
    fun provideDeviceClock(@ApplicationContext context: Context): DeviceClock = object : DeviceClock {
        override fun sinceBoot() = SystemClock.elapsedRealtime().milliseconds

        override fun bootCount() = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
    }

    @Provides
    fun provideBiometricAvailability(@ApplicationContext context: Context) = BiometricAvailability {
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }
}
