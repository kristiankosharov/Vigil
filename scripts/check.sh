#!/usr/bin/env bash
# Quiet project checks for Claude Code and humans.
# Prints one line per step (✓/✗). On failure it prints only what is needed to fix it (failing tests
# with their messages, compiler/detekt/lint findings; max 40 lines per step) and the path of the
# full log, so Gradle output never floods the session.
#
# Usage:
#   scripts/check.sh format                 # ktlint (version pinned in gradle/libs.versions.toml)
#   scripts/check.sh detekt                 # static analysis
#   scripts/check.sh konsist                # architecture rules
#   scripts/check.sh tests [:module ...]    # unit tests (all modules, or only the given ones)
#   scripts/check.sh android [:module ...]  # instrumented tests on a connected device or emulator
#   scripts/check.sh release                # lint + R8 release build (unsigned without signing env vars)
#   scripts/check.sh all                    # format, detekt, konsist, tests, release (CI order)
#
# Exit code is non-zero if any step failed. A skipped step (no device for `android`) says so and
# does not fail.

set -uo pipefail
cd "$(dirname "$0")/.." || exit 1

if [[ ! -x ./gradlew ]]; then
  echo "✗ ./gradlew not found — run this from the Android project root" >&2
  exit 1
fi

LOG_DIR="build/check-logs"
mkdir -p "$LOG_DIR"
STATUS=0
MAX_LINES=40

# Failing tests from the JUnit XML reports written after the file $1: "✗ Class > test" plus the
# message, without stack frames. Gradle's console output never contains these messages; the XML does.
test_failures() {
  local files=() f
  while IFS= read -r -d '' f; do files+=("$f"); done < <(
    find . \( -path '*/build/test-results/*' -o -path '*/build/outputs/androidTest-results/*' \) \
      -name 'TEST-*.xml' -newer "$1" -print0 2>/dev/null
  )
  [[ ${#files[@]} -gt 0 ]] || return 0
  awk '
    function attr(line, key) {
      if (!match(line, " " key "=\"[^\"]*\"")) return ""
      return substr(line, RSTART + length(key) + 3, RLENGTH - length(key) - 4)
    }
    {
      line = $0
      if (index(line, "<testcase ")) { name = attr(line, "name"); cls = attr(line, "classname") }
      if (line ~ /<(failure|error)[ >]/) {
        print "✗ " cls " > " name
        sub(/.*<(failure|error)[^>]*>/, "", line)
        infail = 1; shown = 0
      }
      if (!infail) next
      done = (line ~ /<\/(failure|error)>/)
      sub(/<\/(failure|error)>.*/, "", line)
      if (line ~ /^[[:space:]]*at /) { infail = 0; next }      # stack trace: message is complete
      if (line != "" && shown < 12) { print "    " line; shown++ }
      if (done) infail = 0
    }' "${files[@]}" \
    | sed -e 's/&lt;/</g' -e 's/&gt;/>/g' -e 's/&quot;/"/g' -e "s/&apos;/'/g" -e 's/&#10;/ /g' -e 's/&amp;/\&/g'
}

# Compiler, detekt and lint findings from a Gradle log (test failures come from test_failures).
log_findings() {
  grep -E '^e: |^w: .*deprecated|error:|: Error: |Execution failed for task|^> |Lint found|Analysis failed|\.(kts?|xml):[0-9]+(:[0-9]+)?[: ]' "$1" \
    | grep -vE '^> (Task |There were failing tests|Run with|Get more help)|^[[:space:]]+at ' \
    | awk '!seen[$0]++'
}

run() {
  local name=$1
  shift
  local log="$LOG_DIR/$name.log" start="$LOG_DIR/.$name.start"
  touch "$start"
  if ./gradlew --console=plain --continue "$@" >"$log" 2>&1; then
    echo "✓ $name"
  else
    echo "✗ $name (full log: $log)"
    { test_failures "$start"; log_findings "$log"; } | sed "s|$PWD/||g" | head -"$MAX_LINES"
    STATUS=1
  fi
}

# Module path (":feature:users:ui") → its directory's build file mentions the pure Kotlin plugin.
is_jvm_module() {
  local dir=${1#:}
  dir=${dir//://}
  [[ -f "$dir/build.gradle.kts" ]] && grep -q 'jvm.library' "$dir/build.gradle.kts"
}

# Android modules use testDebugUnitTest (testDevDebugUnitTest in :app); pure Kotlin modules use test.
test_task_for() {
  if [[ "$1" == ":app" ]]; then
    echo ":app:testDevDebugUnitTest"
  elif is_jvm_module "$1"; then
    echo "$1:test"
  else
    echo "$1:testDebugUnitTest"
  fi
}

# Every pure Kotlin module except :konsist (they have no testDebugUnitTest task, so the Android test
# tasks skip them; Konsist has its own step).
jvm_test_tasks() {
  grep -rl --include=build.gradle.kts 'jvm.library' . 2>/dev/null \
    | grep -v '/build/' | grep -v '^./build-logic/' \
    | sed -e 's|^\./||' -e 's|/build.gradle.kts$||' -e 's|/|:|g' -e 's|^|:|' -e 's|$|:test|' \
    | grep -v '^:konsist:'
}

tests() {
  local tasks=() m t
  if [[ $# -gt 0 ]]; then
    for m in "$@"; do tasks+=("$(test_task_for "$m")"); done
  else
    tasks=(testDebugUnitTest testDevDebugUnitTest)
    while IFS= read -r t; do [[ -n "$t" ]] && tasks+=("$t"); done < <(jvm_test_tasks)
  fi
  run tests "${tasks[@]}"
}

android() {
  local adb m tasks=()
  adb=$(command -v adb || echo "${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb")
  if [[ ! -x "$adb" ]] || ! "$adb" devices 2>/dev/null | awk 'NR > 1 && $2 == "device"' | grep -q .; then
    echo "⚠ android: skipped — no device or emulator connected, so instrumented tests did NOT run"
    return
  fi
  if [[ $# -gt 0 ]]; then
    for m in "$@"; do
      if [[ "$m" == ":app" ]]; then
        tasks+=(":app:connectedDevDebugAndroidTest")
      elif is_jvm_module "$m"; then
        echo "  ($m is pure Kotlin: no instrumented tests)"
      else
        tasks+=("$m:connectedDebugAndroidTest")
      fi
    done
    [[ ${#tasks[@]} -gt 0 ]] || return
  else
    tasks=(connectedDebugAndroidTest :app:connectedDevDebugAndroidTest)
  fi
  run android "${tasks[@]}"
}

ktlint_version() {
  ktlint --version 2>/dev/null | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1
}

format() {
  local log="$LOG_DIR/format.log" pinned installed
  pinned=$(sed -n 's/^ktlint *= *"\([^"]*\)".*/\1/p' gradle/libs.versions.toml 2>/dev/null)
  if ! command -v ktlint >/dev/null 2>&1; then
    echo "✗ format: ktlint is not installed (need ${pinned:-the version pinned in gradle/libs.versions.toml})"
    STATUS=1
    return
  fi
  installed=$(ktlint_version)
  if [[ -n "$pinned" && "$installed" != "$pinned" ]]; then
    echo "✗ format: ktlint $installed is installed, but gradle/libs.versions.toml pins $pinned"
    STATUS=1
    return
  fi
  if ktlint --relative '**/*.kt' '**/*.kts' '!**/build/**' >"$log" 2>&1; then
    echo "✓ format"
  else
    echo "✗ format (full log: $log; fix a file with: ktlint --format <file>)"
    grep -E '\.kts?:[0-9]+' "$log" | head -"$MAX_LINES"
    STATUS=1
  fi
}

step=${1:-all}
[[ $# -gt 0 ]] && shift

case "$step" in
  format)  format ;;
  detekt)  run detekt detekt ;;
  konsist) run konsist :konsist:test ;;
  tests)   tests "$@" ;;
  android) android "$@" ;;
  release) run lint :app:lintProdRelease; run release-build :app:assembleProdRelease ;;
  all)
    format
    run detekt detekt
    run konsist :konsist:test
    tests
    run lint :app:lintProdRelease
    run release-build :app:assembleProdRelease
    ;;
  *)
    echo "Unknown step: $step (use format | detekt | konsist | tests [:module ...] | android [:module ...] | release | all)" >&2
    exit 2
    ;;
esac

exit $STATUS
