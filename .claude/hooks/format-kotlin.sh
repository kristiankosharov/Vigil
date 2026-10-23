#!/usr/bin/env bash
# PostToolUse hook (Write|Edit) for Kotlin files:
# 1. Marks this session as "Claude changed Kotlin", so the Stop hook runs Konsist only after
#    Claude's own edits (never because of edits you make in the IDE).
# 2. Formats the file with ktlint, pinned as `ktlint` in gradle/libs.versions.toml. Violations ktlint
#    can't fix go back to Claude (exit 2 + stderr); everything else is silent.
# If ktlint is missing or not the pinned version, formatting is skipped and you and Claude are told
# once per session (scripts/check.sh format still catches formatting in CI).
# Needs jq (built into macOS 15+).

input=$(cat)
file=$(jq -r '.tool_input.file_path // .tool_response.filePath // empty' <<<"$input")

case "$file" in
  *.kt|*.kts) ;;
  *) exit 0 ;;
esac
[[ -f "$file" ]] || exit 0

root="${CLAUDE_PROJECT_DIR:-.}"
state="$root/build/check-logs"
session=$(jq -r '.session_id // "unknown"' <<<"$input" | tr -cd 'A-Za-z0-9_-')
mkdir -p "$state" && touch "$state/kotlin-edited-$session"

# Shows $1 to the user and to Claude, then stops; only the first time in this session.
warn_once() {
  local flag="$state/ktlint-warned-$session"
  if [[ ! -f "$flag" ]]; then
    touch "$flag"
    jq -n --arg m "$1" '{systemMessage: $m, hookSpecificOutput: {hookEventName: "PostToolUse", additionalContext: $m}}'
  fi
  exit 0
}

pinned=$(sed -n 's/^ktlint *= *"\([^"]*\)".*/\1/p' "$root/gradle/libs.versions.toml" 2>/dev/null)
bin=$(command -v ktlint) ||
  warn_once "ktlint is not installed, so edited Kotlin files are not formatted. Install ktlint ${pinned:-(version pinned in gradle/libs.versions.toml)}."

if [[ -n "$pinned" ]]; then
  # `ktlint --version` starts a JVM, so cache the answer until the ktlint binary changes.
  cache="$state/ktlint-version"
  if [[ ! -f "$cache" || "$bin" -nt "$cache" ]]; then
    ktlint --version 2>/dev/null | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1 >"$cache"
  fi
  installed=$(<"$cache")
  [[ "$installed" == "$pinned" ]] ||
    warn_once "ktlint ${installed:-?} is installed, but gradle/libs.versions.toml pins $pinned, so edited Kotlin files are not formatted. Install ktlint $pinned."
fi

if ! output=$(ktlint --format --relative "$file" 2>&1); then
  {
    echo "ktlint could not auto-fix these formatting problems in ${file#"$root"/}:"
    grep -v '^[[:space:]]*$' <<<"$output" | head -20
  } >&2
  exit 2
fi
exit 0
