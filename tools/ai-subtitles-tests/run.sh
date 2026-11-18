#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
mkdir -p /tmp/nuvio-ai-test-output
kotlinc app/src/main/java/com/nuvio/tv/fork/ai/subtitles/*.kt tools/ai-subtitles-tests/CoreContractTests.kt -include-runtime -d /tmp/nuvio-ai-test-output/contracts.jar
java -jar /tmp/nuvio-ai-test-output/contracts.jar
