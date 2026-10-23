# Encrypted DataStore

Rules: `.claude/rules/datastore.md`. Once these exist in `:common:datastore`, the source files win.

## `:common:datastore`: serializer, lazy key, qualifier, factory

```kotlin
/** kotlinx.serialization JSON serializer for typed DataStore. Parse failures become CorruptionException. */
class JsonSerializer<T>(
    private val serializer: KSerializer<T>,
    override val defaultValue: T,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) : Serializer<T> {
    override suspend fun readFrom(input: InputStream): T =
        try {
            json.decodeFromString(serializer, input.readBytes().decodeToString())
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot read ${serializer.descriptor.serialName}", e)
        }

    override suspend fun writeTo(t: T, output: OutputStream) {
        output.write(json.encodeToString(serializer, t).encodeToByteArray())
    }
}

/**
 * Creating the Keystore-backed keyset is slow, so it is deferred until the first encrypt/decrypt,
 * which DataStore always runs on its own IO scope (never on the main thread).
 *
 * Key setup failures are rethrown as IOException on purpose. AeadSerializer turns
 * GeneralSecurityException into CorruptionException, and the corruption handler would then wipe
 * the store. Android Keystore can fail temporarily, so a failed setup must be a retryable I/O
 * error (AppError.Local.Storage), never "corrupt data". Only a real decryption failure of the
 * file itself is corruption. `lazy` does not cache a thrown exception, so the next access retries.
 */
internal class LazyAead(create: () -> Aead) : Aead {
    private val delegate by lazy {
        try {
            create()
        } catch (e: GeneralSecurityException) {
            throw IOException("DataStore key unavailable", e)
        } catch (e: ProviderException) {          // java.security.ProviderException (Keystore, unchecked)
            throw IOException("DataStore key unavailable", e)
        }
    }

    override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray =
        delegate.encrypt(plaintext, associatedData)
    override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray =
        delegate.decrypt(ciphertext, associatedData)
}

/** Qualifies the Tink AEAD used for DataStore encryption, so it never clashes with another `Aead` binding. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DataStoreAead

// di/DataStoreCryptoModule.kt
@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreCryptoModule {
    @Provides @Singleton @DataStoreAead
    fun provideAead(@ApplicationContext context: Context): Aead = LazyAead {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, "datastore_keyset", "datastore_keyset_prefs")
            .withKeyTemplate(KeyTemplate.createFrom(PredefinedAeadParameters.AES256_GCM))
            .withMasterKeyUri("android-keystore://datastore_master_key")
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }
}

/** The only way to create a DataStore in this project. */
fun <T> Context.encryptedDataStore(
    fileName: String,
    serializer: KSerializer<T>,
    defaultValue: T,
    aead: Aead,
    scope: CoroutineScope,
): DataStore<T> = DataStoreFactory.create(
    serializer = AeadSerializer(
        aead = aead,
        wrappedSerializer = JsonSerializer(serializer, defaultValue),
        associatedData = fileName.encodeToByteArray(),   // binds ciphertext to this file
    ),
    corruptionHandler = ReplaceFileCorruptionHandler { defaultValue },
    scope = scope,
    produceFile = { dataStoreFile(fileName) },
)
```

## In a feature (`:feature:users:data:core`)

```kotlin
// local/UsersPreferences.kt
@Serializable
internal data class UsersPreferences(
    val sortOrder: SortOrder = SortOrder.Name,
    val showInactive: Boolean = false,
)

// di/UsersDataStoreModule.kt
@Module
@InstallIn(SingletonComponent::class)
internal object UsersDataStoreModule {
    @Provides @Singleton
    fun provideUsersPreferencesStore(
        @ApplicationContext context: Context,
        @DataStoreAead aead: Aead,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<UsersPreferences> = context.encryptedDataStore(
        fileName = "users_preferences.json",
        serializer = UsersPreferences.serializer(),
        defaultValue = UsersPreferences(),
        aead = aead,
        scope = scope,
    )
}

// in UsersSettingsRepositoryImpl
override fun observeSortOrder(): Flow<AppResult<SortOrderData>> =
    store.data.map { it.sortOrder.toData() }.distinctUntilChanged().asStoreResult()

override suspend fun setSortOrder(order: SortOrderData): AppResult<Unit> =
    safeStoreCall { store.updateData { it.copy(sortOrder = order.toLocal()) } }.map { }
```
