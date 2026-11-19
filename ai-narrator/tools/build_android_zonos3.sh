#!/usr/bin/env bash
# Standalone, separately signed ARM32 companion build. Never put this ELF in Nuvio's APK.
set -euo pipefail
ROOT="$(git rev-parse --show-toplevel)"
NDK="${ANDROID_NDK_HOME:-}"
if [[ -z "$NDK" || ! -f "$NDK/build/cmake/android.toolchain.cmake" ]]; then
  echo "Set ANDROID_NDK_HOME to a valid Android NDK" >&2; exit 2
fi
for name in NUVIO_RELEASE_STORE_FILE NUVIO_RELEASE_KEY_ALIAS NUVIO_RELEASE_KEY_PASSWORD NUVIO_RELEASE_STORE_PASSWORD; do
  if [[ -z "${!name:-}" ]]; then
    echo "Missing $name: ARM32 companion MUST use the same signer as Nuvio" >&2; exit 2
  fi
done
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
UPSTREAM_SHA=6ff97cac0c684732de5f2bc6e3671aa783c15054
git clone --filter=blob:none https://github.com/Zyphra/ZONOS3-Mini.git "$WORK/src"
git -C "$WORK/src" checkout --detach "$UPSTREAM_SHA"
git -C "$WORK/src" submodule update --init --recursive --depth=1
cmake -S "$WORK/src/servers/ggml" -B "$WORK/build" \
  -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=armeabi-v7a -DANDROID_PLATFORM=android-28 \
  -DCMAKE_BUILD_TYPE=Release -DGGML_NATIVE=OFF -DGGML_CUDA=OFF \
  -DGGML_VULKAN=OFF -DGGML_OPENMP=OFF
cmake --build "$WORK/build" --config Release --target zonos3-server --parallel 2
BIN="$WORK/build/zonos3-server"
test -x "$BIN" || { echo "No native worker was built" >&2; exit 1; }
if command -v readelf >/dev/null; then
  readelf -h "$BIN" | grep -q 'Machine:.*ARM'
  readelf -h "$BIN" | grep -q 'Type:.*DYN'
fi
OUTPUT="$ROOT/ai-narrator/worker-arm32/app/src/main/jniLibs/armeabi-v7a"
mkdir -p "$OUTPUT"
cp "$BIN" "$OUTPUT/libzonos3_server.so"
chmod 755 "$OUTPUT/libzonos3_server.so"
"$ROOT/gradlew" -p "$ROOT/ai-narrator/worker-arm32" :app:assembleRelease --no-daemon
APK="$ROOT/ai-narrator/worker-arm32/app/build/outputs/apk/release/app-release.apk"
test -s "$APK" || { echo "No signed companion APK" >&2; exit 1; }
sha256sum "$APK"
echo "Companion APK: $APK"
echo "Confirm APK signature, Android 32-bit ABI, device execution, memory and audio BEFORE publishing."
