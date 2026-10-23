---
name: release-check
description: Run the full CI check order, verify the R8 release build, fix R8/kotlinx.serialization keep-rule problems, and prepare release artifacts. Use before opening a pull request, before a release, or when a release build fails or crashes.
---

# CI and release checks

Rules: `.claude/rules/build.md`.

## CI order (every pull request, fail fast)

Locally, run everything quietly in this order:

```bash
scripts/check.sh all
```

That runs ktlint (pinned version) → detekt → Konsist → unit tests (Android modules via
`testDebugUnitTest` / `testDevDebugUnitTest`, pure Kotlin modules via `test`) →
`:app:lintProdRelease` → `:app:assembleProdRelease`. CI runs the same script.

Nightly or before a release, with an emulator running:

```bash
scripts/check.sh android
```

Without a device it prints that the instrumented tests did **not** run; report that, don't treat it
as a pass.

- CI installs the ktlint version pinned in `gradle/libs.versions.toml` (release `ktlint` binary from
  github.com/ktlint/ktlint) and uses the JDK from the convention plugin's toolchain, the
  configuration cache and the build cache.
- Room schema JSON changes are committed with their migration.
- CI never prints secrets; signing and API keys come only from CI secret variables.

## R8 problems

1. Reproduce with `scripts/check.sh release` and run the release build. Locally it is unsigned
   (no signing env vars), which doesn't change R8's output.
2. Read the stack trace with the build's `mapping.txt` (retrace).
3. Add a rule only when a `@Serializable` class is looked up by reflection (runtime `KType`,
   polymorphic `NavKey`s), in the `consumer-rules.pro` of the module that owns the class, scoped to
   our package:

   ```proguard
   # :feature:x:data:core/consumer-rules.pro (only if needed)
   -if @kotlinx.serialization.Serializable class app.vigil.feature.x.**
   -keepclassmembers class <1> {
       static <1>$Companion Companion;
       kotlinx.serialization.KSerializer serializer(...);
   }
   ```

4. Never `-keep class app.vigil.** { *; }` or `-dontobfuscate`. Don't copy rules that
   kotlinx.serialization, Ktor, Room, Hilt or Navigation already ship.

## Release artifacts

- Upload the R8 `mapping.txt` of every release build with the release.
- Release signing config comes only from environment variables (`RELEASE_KEYSTORE_PATH`, …; see
  `.claude/reference/app.md`).
