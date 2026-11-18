# AI subtitles — isolated prototype state

**Date:** 2026-11-18 (Europe/Warsaw). **Status:** optimized, isolated, independently JVM-tested prototype uploaded to `feature/ai-subtitles/review-464b04df-20261118-2014`; **not integrated into the Android application**.

## Confirmed facts

- GitHub connector reports `stefankot/nuvio-tv-fork` private, write access granted, default branch `main`.
- On 2026-11-18 `main=01ea2ed1860616d758d575154ee08cbad7b43520`, `final=464b04df900738a24733a274c9c0daa601fc51cb` (connector branch endpoint).
- `final` contains `ForkHooks` and `PlayerSidecarSubtitles.commitPreparedSidecarSubtitle` supporting buffer-preserving cue replacement guarded by source URL and optional attachment generation. Independent per-block revision control is **not** established.
- `PlayerSubtitleCueParser` is a lenient SRT/VTT sync parser and does not preserve all formatting metadata.
- No `AGENTS.md` found in repository root (GitHub contents 404); nested policies not exhaustively checked.
- Container has no checkout of this repository. `git ls-remote https://github.com/stefankot/nuvio-tv-fork.git` exited 128 due to DNS failure. Cannot inspect local `git status`, worktree ownership, uncommitted files, or run Android build.

## What was prepared (without modifying inherited application files)

- Pure Kotlin contracts under `app/src/main/java/com/nuvio/tv/fork/ai/subtitles/`:
  - `AuxiliaryLanguagePolicy.kt`: strict PL source categories, RU priority, Western choice constrained to verified candidates; explicit missing category.
  - `TranslationOutputContract.kt`: rejects wrong/duplicate/missing cue IDs, malformed text flags and ASS overrides; readability warnings separate from structural errors.
  - `OrdinaryAudioBudget.kt`: per-material persistent reservations before network upload, 60 s hard total, 20 s per sample, conservative treatment of unknown transmission and parallel locking.
  - `SubtitleExport.kt`: text-only UTF-8 SRT and WebVTT output preserving original start/end times (no ASS support).
- Read-only drift detection and hook checks are included but not executed against a local repository.
- `app/src/test/java/com/nuvio/tv/fork/ai/subtitles/AiSubtitleContractsTest.kt`: JUnit 4 tests prepared for the repository’s existing JUnit dependency, **not executed under Gradle**.
- `UPSTREAM_LOCK.json`: pinned base and reference-only donors; no donor code copied. Licences are intentionally `null` pending audit.
- No keys, model configuration, cost authorization, translation requests, publication, or upstream edits.

## Evidence and test reproduction

```sh
./tools/ai-subtitles-tests/run.sh
# Kotlin/JVM 1.9.0, JRE 21
# TOTAL PASS: 16
./scripts/ai-subtitles/check-upstreams --offline
# Returns 0, validates pinned metadata only
```

Additional optimization tests verify exact prior-language-selection tie rules on 750 randomized cases, Unicode code points, stable timestamp formatting and write-free read-only audio operations. The tests exercise strict category rules, invalid response rejection, SRT/VTT timing, persistence, overlapping/duplicate transmissions, single-JVM concurrency and fail-closed ledger corruption. They do **not** validate Android SDK compatibility, fsync directory durability across power loss, Gradle build, UI integration, Exo/MPV, real AI API, network/audio extraction, cost accounting, or export through Android's document picker.

## Blockers / next actions

1. Obtain a **safe independent local worktree or clone** of the private repository, inspect `AGENTS.md` recursively and existing worktrees/status before copying code. Do not use direct GitHub branch writes to bypass the worktree requirement.
2. Run `check-ai-subtitles-hooks` and `git diff --check`, actual Gradle unit tests and Android TV instrumentation in the independent checkout.
3. Confirm a dedicated session-bound sidecar adapter before any per-block render; `commitPreparedSidecarSubtitle` alone does not prevent stale block-level revisions.
4. Add actual model provider behind explicit spending policy, SRT/VTT parsing preserving metadata, semantic review, durable text cache, UI/ETA, real audio extraction/consent, Android Keystore storage, MPV support decision, OpenSubtitles export/publication review.
5. Revalidate upstream SHAs and licenses and test base updates on two revisions. Do not create a PR before real integration tests.

**Changed upstream files:** 0. **Unreviewed edits:** 0. **GitHub writes:** upload to a new dedicated feature branch only. **Full feature readiness:** not established.

## Optimization addendum (2026-11-18)

- Implemented single-pass source selection and readability checks, direct timestamp formatting and striped per-title JVM synchronization for the audio ledger. Binary ledger format stays `ASB1`.
- Added atomic NIO replacement for ledger writes, read-only/idempotent no-write paths, JUnit test cases and a self-contained benchmark. See `OPTIMIZATION_REPORT.md`.
- Standalone JVM tests **16/16 PASS**; JVM sample medians (previous → optimized): combined SRT+VTT export 27 → 6 ms, validation 14 → 6 ms, language choice 4 → 3 ms for 6000 items.
- Filesystem durability after abrupt loss of power, Android minSdk/Gradle compatibility and integration remain unverified.
- All changes are in this independent prototype and its dedicated review branch. **GitHub writes: dedicated feature branch only**; inherited source edits: 0.
