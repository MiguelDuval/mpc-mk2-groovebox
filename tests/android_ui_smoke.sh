#!/usr/bin/env bash
set -euo pipefail

APK="android/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="com.miguelduval.mpcmk2groovebox.debug"
ACTIVITY="$PACKAGE/com.miguelduval.mpcmk2groovebox.MainActivity"
DUMP="/tmp/mpc-groovebox-ui.xml"

test -f "$APK"

echo "Installing APK..."
adb install -r "$APK"

echo "Launching $ACTIVITY..."
adb shell am force-stop "$PACKAGE"
adb shell am start -n "$ACTIVITY"
sleep 2

dump_ui() {
  adb shell uiautomator dump /data/local/tmp/mpc-groovebox-ui.xml >/dev/null || return 1
  adb exec-out cat /data/local/tmp/mpc-groovebox-ui.xml > "$DUMP" || return 1
  if ! grep -q '<hierarchy' "$DUMP"; then
    return 1
  fi
  return 0
}

assert_text() {
  local expected="$1"
  if ! grep -Fq "text=\"$expected\"" "$DUMP"; then
    echo "ERROR: UI text not found: $expected"
    cat "$DUMP"
    exit 1
  fi
}

for attempt in $(seq 1 15); do
  if dump_ui && grep -Fq 'text="MPC Studio MkII Groovebox — Hardware Bring-Up"' "$DUMP"; then
    break
  fi
  sleep 1
done

dump_ui
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
        r'")(?=[^>]*bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]")'
    )
    match = re.search(pattern, current)
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
    r'<node\b(?=[^>]*text="Reset")(?=[^>]*bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]")',
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
match = re.search(r'<node\b(?=[^>]*text="2")(?=[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]")', xml)
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

