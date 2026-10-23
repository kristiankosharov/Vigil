package app.vigil.common.datastore

import javax.inject.Qualifier

/** Qualifies the Tink AEAD used for DataStore encryption, so it never clashes with another `Aead` binding. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DataStoreAead
