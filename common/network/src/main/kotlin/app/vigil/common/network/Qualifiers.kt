package app.vigil.common.network

import javax.inject.Qualifier

/** The API base URL; provided by :app from BuildConfig (one per flavor). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl

/** True in debug builds; provided by :app. Gate debug-only behaviour on this outside :app. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsDebugBuild

/** The default client for feature APIs: has the bearer `Auth` plugin. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Authenticated

/** No `Auth` plugin: only login, register and token refresh, so a refresh never triggers another refresh. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Unauthenticated
