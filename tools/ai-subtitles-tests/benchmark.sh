#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
output="$(mktemp -d)"
trap 'rm -rf "$output"' EXIT
kotlinc app/src/main/java/com/nuvio/tv/fork/ai/subtitles/*.kt tools/ai-subtitles-tests/Benchmark.kt -include-runtime -d "$output/bench.jar"
java -Xms128m -Xmx512m -jar "$output/bench.jar"
