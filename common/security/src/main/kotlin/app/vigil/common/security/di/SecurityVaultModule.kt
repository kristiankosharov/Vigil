package app.vigil.common.security.di

import app.vigil.common.security.Vault
import app.vigil.common.security.VaultImpl
import app.vigil.common.security.VaultKeyControl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** One VaultImpl (@Singleton) behind both interfaces: features seal, the app lock opens and closes. */
@Module
@InstallIn(SingletonComponent::class)
internal interface SecurityVaultModule {
    @Binds
    fun bindVault(impl: VaultImpl): Vault

    @Binds
    fun bindVaultKeyControl(impl: VaultImpl): VaultKeyControl
}
