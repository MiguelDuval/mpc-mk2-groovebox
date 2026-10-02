#!/usr/bin/env bash
set -euo pipefail

APK="android/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="com.miguelduval.mpcmk2groovebox.debug"
ACTIVITY="$PACKAGE/com.miguelduval.mpcmk2groovebox.MainActivity"
DUMP="/tmp/mpc-groovebox-ui.xml"

MAIN_ACTIVITY_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MainActivity.java"
NATIVE_ENGINE_SOURCE="src/NativeEngine.cpp"

echo "Running MPC Main UI source preflight..."
for required in   "MIXER STRIP"   "nativeAudioGetPadSampleName"   "nativeAudioSetPadSampleName"   "BAR %03d  BEAT %d  TICK %03d"   "SEQ\\n"   "TRANSPOSE\\n—"   "Main Track View quick sample waveform"   "Main Track View monitor state"   "Main Track View length mode"   "Main Track View velocity state"   "Main Track View selected layer"   "Main Mixer Strip level"   "MPC condensed Mixer Strip show or hide"   "MPC condensed Mixer Strip"   "openMainTrackEditContext"   "openMainArrangementGridContext"   "setOnDoubleTapListener"; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: MainActivity source contract missing: $required"
    exit 1
  fi
done

for required in   "MainActivity_nativeAudioGetPadSampleName"   "MainActivity_nativeAudioSetPadSampleName"; do
  if ! grep -Fq -- "$required" "$NATIVE_ENGINE_SOURCE"; then
    echo "ERROR: Native sample-name JNI contract missing: $required"
    exit 1
  fi
done

test -f "$APK"

dump_debug_state() {
  echo "===== ADB STATE ====="
  adb shell pidof "$PACKAGE" || true
  adb shell dumpsys activity activities | tail -n 120 || true
  echo "===== WINDOW STATE ====="
  adb shell dumpsys window windows | tail -n 120 || true
  echo "===== RELEVANT LOGCAT ====="
  adb logcat -d -t 400 | grep -E 'ANR|system_server|ActivityTaskManager|WindowManager|AndroidRuntime|mpcmk2groovebox' | tail -n 160 || true
}

echo "Installing APK..."
install_ok=false
for attempt in $(seq 1 3); do
  if adb install -r "$APK"; then
    install_ok=true
    break
  fi

  echo "APK install attempt $attempt failed; reconnecting ADB..."
  adb reconnect offline >/dev/null 2>&1 || true
  sleep 3
done

if [ "$install_ok" != true ]; then
  echo "ERROR: APK installation failed after 3 attempts."
  dump_debug_state
  exit 1
fi

echo "Launching UI-only startup diagnostic..."
adb shell am force-stop "$PACKAGE"
adb shell am start -n "$ACTIVITY" --es mpc.groovebox.smoke.mode ui-only

wait_for_log_marker() {
  local marker="$1"
  local attempts="$2"
  local interval="$3"

  for attempt in $(seq 1 "$attempts"); do
    if adb logcat -d -t 500 2>/dev/null | grep -Fq "MpcGroovebox: $marker"; then
      echo "Log marker appeared: $marker"
      return 0
    fi
    sleep "$interval"
  done

  echo "ERROR: log marker did not appear: $marker"
  adb logcat -d -t 800 2>/dev/null | grep -F "MpcGroovebox" | tail -n 120 || true
  dump_debug_state
  return 1
}

assert_activity_present() {
  local expected="$1"