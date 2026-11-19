#!/usr/bin/env bash
# Noncommitting upstream merge probe, using the fork's existing release-merge conflict policy.
set -euo pipefail

TAG="${1:?Pass an official NuvioTV tag}"
if [[ ! "$TAG" =~ ^[A-Za-z0-9][A-Za-z0-9._-]*$ ]]; then
  echo "Invalid upstream tag" >&2
  exit 2
fi

ROOT="$(git rev-parse --show-toplevel)"
TEMP="$(mktemp -d "${RUNNER_TEMP:-/tmp}/ai-narrator-upstream.XXXXXX")"
REPORT="$ROOT/upstream-compatibility-report.txt"
cleanup() {
  git -C "$TEMP" merge --abort >/dev/null 2>&1 || true
  git -C "$ROOT" worktree remove --force "$TEMP" >/dev/null 2>&1 || true
  rmdir "$TEMP" >/dev/null 2>&1 || true
}
trap cleanup EXIT

{
  echo "UPSTREAM COMPATIBILITY — noncommitting isolated test"
  echo "Feature HEAD: $(git rev-parse HEAD)"
  echo "Official NuvioTV tag: $TAG"
  echo "No branch update or release publication"
} > "$REPORT"

git worktree add --detach "$TEMP" HEAD
git -C "$TEMP" fetch --no-tags https://github.com/NuvioMedia/NuvioTV.git "refs/tags/$TAG"

set +e
git -C "$TEMP" merge --no-ff --no-commit FETCH_HEAD > "$TEMP/ai-merge.log" 2>&1
merge_status=$?
set -e

# Match fork-owned workflow preference in mirror-nuviotv-release.yml.
git -C "$TEMP" restore --source=HEAD --staged --worktree .github/workflows

if (( merge_status != 0 )); then
  mapfile -t conflicts < <(git -C "$TEMP" diff --name-only --diff-filter=U)
  {
    echo "Merge exit status: $merge_status"
    echo "Conflicted files: ${#conflicts[@]}"
    printf '  %s\n' "${conflicts[@]}"
  } >> "$REPORT"

  # The existing AutoSync special-case: upstream build.gradle wins,
  # then reinsert only the known fork-owned Reshaped Gradle hook.
  if [[ "${#conflicts[@]}" -eq 1 && "${conflicts[0]}" == "app/build.gradle.kts" ]]; then
    git -C "$TEMP" checkout --theirs -- app/build.gradle.kts
    hook='apply(from = "reshaped.gradle") // Nuvio RS hook: fork build settings'
    if ! grep -qxF "$hook" "$TEMP/app/build.gradle.kts"; then
      printf '\n%s\n' "$hook" >> "$TEMP/app/build.gradle.kts"
    fi
    git -C "$TEMP" add app/build.gradle.kts
    echo "Special-case app/build.gradle.kts resolved using existing upstream rule" >> "$REPORT"
  else
    echo "RESULT: CONFLICT — review required; no automatic resolution attempted" >> "$REPORT"
    cat "$TEMP/ai-merge.log" >> "$REPORT"
    cat "$REPORT"
    exit 1
  fi
fi

if [[ -n "$(git -C "$TEMP" diff --name-only --diff-filter=U)" ]]; then
  echo "RESULT: UNRESOLVED CONFLICT" >> "$REPORT"
  exit 1
fi
echo "MERGE: CLEAN or resolved strictly with current fork rule" >> "$REPORT"
{
  echo "Changed paths:"
  git -C "$TEMP" status --short
  echo "Checks:"
} >> "$REPORT"
set +e
(
  cd "$TEMP"
  python3 ai-narrator/tools/check_contracts.py
  ./gradlew :app:compileFullDebugKotlin :app:testFullDebugUnitTest --tests '*Narrator*Test*' --no-daemon
) >> "$REPORT" 2>&1
test_status=$?
set -e
echo "BUILD_AND_CONTRACT_EXIT=$test_status" >> "$REPORT"
tail -n 70 "$REPORT"
exit "$test_status"
