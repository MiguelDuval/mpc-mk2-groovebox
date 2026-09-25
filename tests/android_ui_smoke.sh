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
adb shell am start -W -n "$ACTIVITY"

dump_ui() {
  adb exec-out uiautomator dump /dev/tty > "$DUMP"
  if ! grep -q '<hierarchy' "$DUMP"; then
    echo "ERROR: uiautomator did not return a UI hierarchy"
    cat "$DUMP"
    adb shell dumpsys activity activities | tail -n 80 || true
    exit 1
  fi
}

assert_text() {
  local expected="$1"
  if ! grep -Fq "text=\"$expected\"" "$DUMP"; then
    echo "ERROR: UI text not found: $expected"
    cat "$DUMP"
    exit 1
  fi
}

dump_ui
assert_text "MPC Studio MkII Groovebox — Hardware Bring-Up"
assert_text "Sample target pad: 1"
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

