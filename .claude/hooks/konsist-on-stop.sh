#!/usr/bin/env bash
# Stop hook: when Claude finishes a turn after changing Kotlin files in this session, run the
# Konsist architecture tests quietly. On violations, block the stop and hand the failing rules and
# offending files back to Claude so it fixes them before ending the turn.
# The "Kotlin changed" marker is written by format-kotlin.sh (PostToolUse), so your own IDE edits
# never trigger this. The marker is removed only after a passing run.
# Skips: no Kotlin edited by Claude, no Gradle project yet, or a re-run caused by this hook itself
# (stop_hook_active) so it can never loop; the marker stays, so the next turn checks again.

input=$(cat)
[[ "$(jq -r '.stop_hook_active // false' <<<"$input")" == "true" ]] && exit 0

cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
session=$(jq -r '.session_id // "unknown"' <<<"$input" | tr -cd 'A-Za-z0-9_-')
marker="build/check-logs/kotlin-edited-$session"
[[ -f "$marker" ]] || exit 0
[[ -x ./gradlew && -x ./scripts/check.sh ]] || exit 0

if result=$(./scripts/check.sh konsist 2>&1); then
  rm -f "$marker"
  exit 0
fi

jq -n --arg r "Konsist architecture rules failed. Fix these violations (the code is wrong, not the test), then finish:
$result" '{decision: "block", reason: $r}'
exit 0
