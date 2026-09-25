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

dump_ui_once() {
  rm -f "$DUMP"
  timeout 12s adb shell uiautomator dump /data/local/tmp/mpc-groovebox-ui.xml >/dev/null 2>&1 || return 1
  timeout 12s adb exec-out cat /data/local/tmp/mpc-groovebox-ui.xml > "$DUMP" 2>/dev/null || return 1
  grep -q '<hierarchy' "$DUMP"
}

wait_for_log_marker "UI_READY" 30 2
wait_for_log_marker "UI_ONLY_COMPLETE" 30 2
if ! dump_ui_once; then
  echo "ERROR: final UI-only hierarchy dump failed."
  dump_debug_state
  exit 1
fi
assert_text "MPC Studio MkII Groovebox — Hardware Bring-Up"
assert_text "Startup diagnostic: UI-only; native/MIDI deferred"
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

if ! dump_ui_once; then
  echo "ERROR: final full-application hierarchy dump failed."
  dump_debug_state
  exit 1
fi
assert_text "MPC Studio MkII Groovebox — Hardware Bring-Up"
assert_text "Sample target pad: 1"
assert_text "Pad 1 tuning: +0.00 st"
assert_text "-1 st"
assert_text "Reset"
assert_text "+1 st"
assert_text "Load WAV Sample"
assert_text "Start Sampler"
assert_text "Stop Audio"

for pad in $(seq 1 16); do
  assert_text "$pad"
done

echo "All 16 pad selectors are present."

python3 - "$DUMP" <<'PY'
import re
import subprocess
import sys
import time

dump = sys.argv[1]
xml = open(dump, encoding="utf-8").read()

def tap_text(text):
    current = open(dump, encoding="utf-8").read()
    pattern = (
        r'<node\b(?=[^>]*text="' + re.escape(text) +
        r'")(?=[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]")'
    )
    match = re.search(pattern, current, re.IGNORECASE)
    if not match:
        raise SystemExit(f"ERROR: could not locate {text!r} bounds")
    left, top, right, bottom = map(int, match.groups())
    x = (left + right) // 2
    y = (top + bottom) // 2
    print(f"Clicking {text!r} at {x},{y}")
    subprocess.run(["adb", "shell", "input", "tap", str(x), str(y)], check=True)
    time.sleep(0.3)

tap_text("+1 st")
PY

dump_ui
assert_text "Pad 1 tuning: +1.00 st"

python3 - "$DUMP" <<'PY'
import re
import subprocess
import sys
import time

dump = sys.argv[1]
xml = open(dump, encoding="utf-8").read()
match = re.search(
    r'<node\b(?=[^>]*text="(?i:Reset)")(?=[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]")',
    xml,
)
if not match:
    raise SystemExit("ERROR: could not locate Reset bounds")
left, top, right, bottom = map(int, match.groups())
x = (left + right) // 2
y = (top + bottom) // 2
print(f"Clicking Reset at {x},{y}")
subprocess.run(["adb", "shell", "input", "tap", str(x), str(y)], check=True)
time.sleep(0.3)
PY

dump_ui
assert_text "Pad 1 tuning: +0.00 st"

python3 - "$DUMP" <<'PY'
import re
import subprocess
import sys
import time

dump = sys.argv[1]
xml = open(dump, encoding="utf-8").read()
match = re.search(r'<node\b(?=[^>]*text="(?i:2)")(?=[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]")', xml)
if not match:
    raise SystemExit("ERROR: could not locate pad 2 bounds")
left, top, right, bottom = map(int, match.groups())
x = (left + right) // 2
y = (top + bottom) // 2
print(f"Clicking pad 2 at {x},{y}")
subprocess.run(["adb", "shell", "input", "tap", str(x), str(y)], check=True)
time.sleep(0.4)
PY

dump_ui
assert_text "Sample target pad: 2"

echo "Android emulator smoke test passed."

