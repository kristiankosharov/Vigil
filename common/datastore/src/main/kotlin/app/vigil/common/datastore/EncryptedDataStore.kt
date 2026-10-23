package app.vigil.common.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.datastore.tink.AeadSerializer
import com.google.crypto.tink.Aead
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer

/**
 * The only way to create a DataStore in this project: typed, JSON, encrypted with the
 * [DataStoreAead] key. [fileName] is permanent (`<feature>_<name>.json`): it is also the AEAD
 * associated data, so renaming the file makes the old data unreadable. Decryption or parse failures
 * reset the store to [defaultValue].
 */
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
        associatedData = fileName.encodeToByteArray(), // binds the ciphertext to this file
    ),
    corruptionHandler = ReplaceFileCorruptionHandler { defaultValue },
    scope = scope,
    produceFile = { dataStoreFile(fileName) },
)
