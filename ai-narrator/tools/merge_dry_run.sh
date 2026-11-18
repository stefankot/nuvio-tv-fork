#!/usr/bin/env bash
set -euo pipefail
TAG="${1:?Official NuvioTV tag required}"
if [[ ! "$TAG" =~ ^[a-zA-Z0-9._-]+$ ]]; then echo "Invalid tag"; exit 2; fi
ROOT="$(git rev-parse --show-toplevel)"
TEMP="$(mktemp -d "${RUNNER_TEMP:-/tmp}/ai-narrator-merge.XXXXXX")"
REPORT="$ROOT/upstream-compatibility-report.txt"
cleanup() {
  git -C "$TEMP" merge --abort >/dev/null 2>&1 || true
  git -C "$ROOT" worktree remove --force "$TEMP" >/dev/null 2>&1 || true
  rmdir "$TEMP" >/dev/null 2>&1 || true
}
trap cleanup EXIT
{
  echo "AI narrator upstream compatibility dry run"
  echo "Feature HEAD: $(git rev-parse HEAD)"
  echo "Official upstream tag: $TAG"
} > "$REPORT"
git fetch --no-tags https://github.com/NuvioMedia/NuvioTV.git "refs/tags/$TAG:refs/tags/ai-narrator-check-$TAG"
git worktree add --detach "$TEMP" HEAD
set +e
git -C "$TEMP" merge --no-commit --no-ff "refs/tags/ai-narrator-check-$TAG" > "$TEMP/ai-merge.log" 2>&1
status=$?
set -e
if (( status != 0 )); then
  {
    echo "RESULT: MERGE FAILED / MANUAL REVIEW REQUIRED"
    echo "Conflicted files:"
    git -C "$TEMP" diff --name-only --diff-filter=U || true
    cat "$TEMP/ai-merge.log"
  } >> "$REPORT"
  cat "$REPORT"
  exit 1
fi
echo "RESULT: MERGE CLEAN, compiling merged worktree without commit" >> "$REPORT"
set +e
(cd "$TEMP" && ./gradlew :app:compileFullDebugKotlin :app:testFullDebugUnitTest --no-daemon) >> "$REPORT" 2>&1
status=$?
set -e
echo "BUILD_RESULT=$status" >> "$REPORT"
tail -n 50 "$REPORT"
exit "$status"
