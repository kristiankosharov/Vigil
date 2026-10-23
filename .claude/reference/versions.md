# Versions current on 2026-10-23

`gradle/libs.versions.toml` is the source of truth; this is the state when the project was created.
Re-check every version on its official release page before use; always take the latest stable.

- Gradle 9.8.2 (wrapper), Gradle daemon on Java 21 (`gradle/gradle-daemon-jvm.properties`; detekt
  1.23 can't run on Java 25), compile toolchain Java 17.
- AGP 9.4.1, Kotlin 2.4.21, KSP 2.3.12, compileSdk/targetSdk 37, minSdk 26.
- Compose BOM 2026.09.00, Navigation 3 1.2.0, Lifecycle 2.11.0 (incl. lifecycle-process),
  hilt-lifecycle-viewmodel-compose 1.4.0, Fragment 1.9.1, Biometric 1.1.0 (1.4 is still alpha).
- Room 3 3.0.3 (`androidx.room3`, plugin `androidx.room3`), sqlite-bundled 2.7.1, DataStore +
  datastore-tink 1.3.0, Tink 1.24.0, Paging 3.5.3.
- Hilt 2.60.1, Ktor 3.6.0, kotlinx-serialization 1.12.0, coroutines 1.11.0,
  collections-immutable 0.5.2, Timber 5.0.1.
- Kotest 6.2.6 (JUnit Platform 1.13.4), MockK 1.14.11, Turbine 1.2.1, Molecule 2.2.0, Konsist 0.18.1.
- detekt 1.23.8 (detekt 2 is still alpha; no detekt-formatting), compose-rules 0.6.8, ktlint 1.8.0
  (CLI; releases: github.com/ktlint/ktlint).

Keep the `ktlint = "x.y.z"` line in `libs.versions.toml` on one line in exactly that form: the hook
and `scripts/check.sh` read it with a simple pattern.
