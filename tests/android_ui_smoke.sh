#!/usr/bin/env bash
set -euo pipefail

APK="android/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="com.miguelduval.mpcmk2groovebox.debug"
ACTIVITY="$PACKAGE/com.miguelduval.mpcmk2groovebox.MainActivity"
DUMP="/tmp/mpc-groovebox-ui.xml"

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

