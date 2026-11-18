#!/usr/bin/env bash
# Standalone smoke run (needs kotlinc on PATH). The authoritative run is Gradle:
#   ./gradlew :app:testFullDebugUnitTest --tests 'com.nuvio.tv.fork.ai.subtitles.*'
set -euo pipefail
cd "$(dirname "$0")/../.."
command -v kotlinc >/dev/null || { echo "kotlinc not found; use the Gradle command above" >&2; exit 2; }
output="$(mktemp -d)"
trap 'rm -rf "$output"' EXIT
kotlinc app/src/main/java/com/nuvio/tv/fork/ai/subtitles/*.kt tools/ai-subtitles-tests/CoreContractTests.kt -include-runtime -d "$output/contracts.jar"
java -jar "$output/contracts.jar"
