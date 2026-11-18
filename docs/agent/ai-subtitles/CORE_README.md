# AI subtitles — optimized isolated core prototype for Nuvio TV fork

This prototype is **not a built or integrated Android feature**. It is a small, JVM-tested Kotlin slice designed for later inclusion into a **new isolated worktree** of `stefankot/nuvio-tv-fork` based on verified `origin/final`.

No files from Hossam or other upstream forks were copied. No inherited files in the target repository have been changed; the prototype is stored on a dedicated non-integrated GitHub branch.

## Run

```sh
./tools/ai-subtitles-tests/run.sh
./tools/ai-subtitles-tests/benchmark.sh
./scripts/ai-subtitles/check-upstreams --offline
```

The accompanying JUnit 4 test file under `app/src/test/` can be run with the project’s existing Gradle test dependency after independent worktree integration; it has not been run under Android Gradle.

The optional `check-ai-subtitles-hooks` script is only meaningful after files are placed in an isolated clone/worktree with full history. Live drift checks use `GITHUB_TOKEN` in the environment; the script does not write or fetch source code.

## Do not assume

- The audio ledger alone authorizes uploading: the real sender must reserve before *every* transmission and account for user consent/cost, network retries, and provider requirements.
- The validator parses JSON: it verifies already parsed typed output. The API boundary must strictly parse and limit the JSON response before calling it.
- Sidecar can automatically handle MPV: its renderer is separate and must be audited.
- SRT/VTT text-only export preserves original ASS/VTT styling, comment metadata or accessibility annotations: it does not.
- 16 passing JVM checks imply Android compilation or TV behaviour: they do not.
- Atomic disk replacement requires filesystem support; there is no unsafe non-atomic fallback.

See `OPTIMIZATION_REPORT.md` for the change list, measured JVM sample and limitations; also see `docs/agent/ai-subtitles/STATE.md` and `UPSTREAM_LOCK.json`.
