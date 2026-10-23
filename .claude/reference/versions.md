# Versions current on 2026-10-23

Re-check every version on its official release page before use; always take the latest stable.

```toml
[versions]
agp = "9.0.1"                 # or newer 9.x
kotlin = "2.3.20"             # or newer
composeBom = "2026.09.00"
navigation3 = "1.1.5"
room3 = "3.0.1"
ktor = "3.4.1"
hilt = "2.60.1"
kotest = "6.2.4"
datastore = "1.3.0"           # first stable with datastore-tink (AeadSerializer), 2026-10-21
detekt = "1.23.8"             # latest stable; detekt 2 is not stable yet. No detekt-formatting.
ktlint = "1.8.0"              # CLI, pinned: the format hook and scripts/check.sh read this line.
                              # Releases: github.com/ktlint/ktlint (moved from pinterest/ktlint)
# ksp, lifecycle, hilt-lifecycle-viewmodel-compose, kotlinx-serialization, kotlinx-coroutines,
# kotlinx-collections-immutable, mockk, turbine, molecule, konsist, paging, tink-android,
# timber, compose-rules: latest stable
```

Keep the `ktlint = "x.y.z"` line on one line in exactly this form: the hook and `scripts/check.sh`
read it with a simple pattern.
