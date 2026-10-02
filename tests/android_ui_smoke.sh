#!/usr/bin/env bash
set -euo pipefail

APK="android/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="com.miguelduval.mpcmk2groovebox.debug"
ACTIVITY="$PACKAGE/com.miguelduval.mpcmk2groovebox.MainActivity"
DUMP="/tmp/mpc-groovebox-ui.xml"
MAIN_ACTIVITY_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MainActivity.java"
NATIVE_ENGINE_SOURCE="src/NativeEngine.cpp"
TRACK_EDIT_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcTrackEditView.java"

echo "Running MPC Main UI source preflight..."
for required in \
  "MIXER STRIP" \
  "nativeAudioGetPadSampleName" \
  "nativeAudioSetPadSampleName" \
  "BAR %03d  BEAT %d  TICK %03d" \
  "SEQ\\n" \
  "TRANSPOSE\\n—" \
  "Main Track View quick sample waveform" \
  "Main Track View monitor state" \
  "Main Track View length mode" \
  "Main Track View velocity state" \
  "Main Track View selected layer" \
  "Main Track View record sample" \
  "Main Track View browse samples" \
  "DRUM • TYPE" \
  "Main Track Edit" \
  "MPC Function Bar SEQ REC ARM" \
  "MPC Main sequence REC ARM" \
  "MPC Function Bar TRACK previous next" \
  "HARDWARE_FOCUS_SEQUENCE_START" \
  "HARDWARE_FOCUS_SEQUENCE_END" \
  "HARDWARE_FOCUS_SEQUENCE_BPM" \
  "HARDWARE_FOCUS_SEQUENCE_BARS" \
  "MPC condensed Mixer Strip show or hide" \
  "MPC condensed Mixer Strip" \
  "openMainTrackEditContext" \
  "openMainArrangementGridContext" \
  "setOnDoubleTapListener" \
  "MpcTrackEditView"
  "hardwareFocus = drumTrack ? 10 : 0;"
  "hardwareFocus == 10"; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: MainActivity source contract missing: $required"
    exit 1
  fi
done

echo "Running Track Edit workspace source preflight..."
for required in \
  "Track Edit Global tab" \
  "Track Edit Samples tab" \
  "Track Edit AMP ENV tab" \
  "Track Edit bottom tab bar" \
  "Track Edit Track context" \
  "Track Edit Pad context" \
  "Track Edit LFO tab" \
  "Track Edit Modulations tab" \
  "Track Edit Effects tab" \
  "Track Edit Edit All Layers RESERVED" \
  "Track Edit Samples waveform" \
  "LAYER %d/8"; do
  if ! grep -Fq -- "$required" "$TRACK_EDIT_SOURCE"; then
    echo "ERROR: Track Edit source contract missing: $required"
    exit 1
  fi
done

for required in \
  "MainActivity_nativeAudioGetPadSampleName" \
  "MainActivity_nativeAudioSetPadSampleName"; do
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
adb install -r "$APK"

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
  if ! adb shell dumpsys activity activities 2>/dev/null |
      grep -Fq "$expected"; then
    echo "ERROR: expected Activity was not present in dumpsys activity."
    dump_debug_state
    exit 1
  fi
}

wait_for_log_marker "UI_READY" 30 2
wait_for_log_marker "UI_ONLY_COMPLETE" 30 2
assert_activity_present "com.miguelduval.mpcmk2groovebox.debug/com.miguelduval.mpcmk2groovebox.MainActivity"
echo "UI-only startup diagnostic passed."

echo "Launching full application..."
adb shell am force-stop "$PACKAGE"
adb shell am start -n "$ACTIVITY"

wait_for_log_marker "UI_READY" 30 2
wait_for_log_marker "STARTUP_BEGIN" 30 2
wait_for_log_marker "NATIVE_INFO_END" 30 2
wait_for_log_marker "BUNDLED_SAMPLE_END" 60 2
wait_for_log_marker "MIDI_BRIDGE_END" 30 2
wait_for_log_marker "STARTUP_COMPLETE" 30 2
assert_activity_present "com.miguelduval.mpcmk2groovebox.debug/com.miguelduval.mpcmk2groovebox.MainActivity"

echo "Android emulator startup smoke test passed."

