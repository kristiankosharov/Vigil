package app.vigil.common.security.di

import app.vigil.common.security.BiometricKeys
import app.vigil.common.security.BiometricKeysImpl
import app.vigil.common.security.DevicePepper
import app.vigil.common.security.DevicePepperImpl
import app.vigil.common.security.PinKeyDerivation
import app.vigil.common.security.PinKeyDerivationImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface SecurityKeysModule {
    @Binds
    fun bindDevicePepper(impl: DevicePepperImpl): DevicePepper

    @Binds
    fun bindPinKeyDerivation(impl: PinKeyDerivationImpl): PinKeyDerivation

    @Binds
    fun bindBiometricKeys(impl: BiometricKeysImpl): BiometricKeys
}
