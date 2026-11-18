#!/usr/bin/env bash
# Experimental Android arm64 cross-build; weights are downloaded only by user in app.
set -euo pipefail
ROOT="$(git rev-parse --show-toplevel)"
NDK="${ANDROID_NDK_HOME:-}"
if [[ -z "$NDK" || ! -f "$NDK/build/cmake/android.toolchain.cmake" ]]; then
  echo "Set ANDROID_NDK_HOME to a valid NDK" >&2
  exit 2
fi
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
UPSTREAM_SHA="6ff97cac0c684732de5f2bc6e3671aa783c15054"
git clone --filter=blob:none https://github.com/Zyphra/ZONOS3-Mini.git "$WORK/src"
git -C "$WORK/src" checkout --detach "$UPSTREAM_SHA"
git -C "$WORK/src" submodule update --init --recursive --depth=1
cmake -S "$WORK/src/servers/ggml" -B "$WORK/build" \
  -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-28 \
  -DCMAKE_BUILD_TYPE=Release -DGGML_NATIVE=OFF -DGGML_CUDA=OFF \
  -DGGML_VULKAN=OFF -DGGML_OPENMP=OFF
cmake --build "$WORK/build" --config Release --target zonos3-server --parallel 2
BINARY="$WORK/build/zonos3-server"
if [[ ! -x "$BINARY" ]]; then
  echo "No executable zonos3-server produced" >&2
  exit 1
fi
# A standalone Android PIE executable must be packed inside the APK's nativeLibDir.
# Android disallows execution from writable filesDir on recent releases.
OUTPUT="$ROOT/ai-narrator/jniLibs/arm64-v8a"
mkdir -p "$OUTPUT"
cp "$BINARY" "$OUTPUT/libzonos3_server.so"
echo "Output: $OUTPUT/libzonos3_server.so"
echo "Packaging and real ARM64 Android execution STILL REQUIRE device validation."
