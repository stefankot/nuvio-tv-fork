# Optimization report — AI subtitle core

**Date:** 2026-11-18. **Scope:** isolated Kotlin core delivered as an archive, not a tested Android feature; uploaded to a dedicated isolated GitHub branch.

## Changes

| Area | Original implementation | Optimized implementation | Compatibility |
| --- | --- | --- | --- |
| Auxiliary-language choice | Candidate filters, sorting, repeated language extraction | Single pass through candidates; priority map built once per selection; deterministic tie handling | Tested against the former comparator on 750 randomized lists |
| Translation response validation | Temporary ID lists, zipping, line splitting and repeated Unicode counting | Direct index checks, a single readability scan per cue and no acceptance of partially invalid replies | Preserves per-cue ID, structural, markup, flag and safety validation; tests cover Unicode |
| SRT/WebVTT export | `String.format` for all start/end timestamps | Direct append to `StringBuilder`, with identical padding and stable chronological ordering | Tested 100-hour timestamp, millisecond precision and source ordering |
| Persistent audio quota | Single global JVM monitor, list comparison for write detection, unbuffered writes | 64 bounded striped JVM monitors plus existing per-title cross-process locks, explicit dirty flag, buffered writes and atomic NIO replacement | Existing binary format and quota accounting retained; unchanged SENT/snapshot do not rewrite ledger |

The 60-second quota, 20-second snippet maximum, conservative charging after unknown uploads, and original media timing are unchanged. New implementation refuses to save if the filesystem does not support atomic replacement; there is no non-atomic fallback.

## Test results

- Kotlin/JVM standalone contract tests: **16/16 PASS** (`./tools/ai-subtitles-tests/run.sh`).
- Read-only upstream-lock schema check: **PASS** (`./scripts/ai-subtitles/check-upstreams --offline`). This does not verify remote HEADs.
- JUnit 4 test file expanded with 4 cases but **not run under Gradle** (repository not locally available).
- No Android build, device, ExoPlayer or MPV integration tests.

### JVM microbenchmark, sample observation (not a performance guarantee)

JRE 21, Kotlin compiler 1.9, `-Xms128m -Xmx512m`, one execution per version, 3 warmups and 7 measured rounds. Same generated data and benchmark source in each version.

| Operation | Previous median | New median | Ratio |
| --- | ---: | ---: | ---: |
| Export 6000 cues to both SRT and WebVTT | 27 ms | 6 ms | 4.5× faster |
| Validate 6000 cues | 14 ms | 6 ms | 2.3× faster |
| Select sources from 6000 candidates | 4 ms | 3 ms | 1.3× faster |

Microbenchmarks have JVM warmup and allocation noise. They do not measure network, model latency, UI rendering or device performance. Run `./tools/ai-subtitles-tests/benchmark.sh` on the optimized archive for an additional local sample.

## Assumptions and gaps

1. The Android app's minimum SDK and filesystem's atomic-rename support have not been checked by an Android build. NIO filesystem APIs generally require Android API 26 or later; verify actual app configuration before integration.
2. Atomic file replacement plus file fsync does not by itself establish parent-directory durability after sudden power loss. Crash/power-cut tests and platform-specific durability validation remain necessary.
3. The translation response parser, API provider, sidecar renderer adapter, MPV, and real audio transport have not been integrated.
4. No integration branches, worktrees or inherited Nuvio code were altered; the isolated feature branch contains this prototype.
