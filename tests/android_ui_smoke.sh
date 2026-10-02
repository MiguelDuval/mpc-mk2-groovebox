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
  "MPC shell mixer strips" \
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
  "Main Mode sequence tempo source • SEQ • Global unavailable" \
  "Main Track / Arrangement view switcher" \
  "MPC Toolbar Menu" \
  "Timing Correct" \
  "Time Signature value" \
  "Time Signature numerator" \
  "Time Signature denominator" \
  "Main Track View header" \
  "Main Arrangement View header" \
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
  "double-tap for numeric entry" \
  "Timing Correct enabled field" \
  "Timing Correct time division" \
  "Timing Correct swing" \
  "Timing Correct reserved fields" \
  "Time Signature value" \
  "Time Signature numerator" \
  "Time Signature denominator" \
  "showTimingCorrectDialog" \
  "showTimeSignatureDialog" \
  "showMpcNumericEntry" \
  "MPC condensed Mixer Strip show or hide" \
  "MPC condensed Mixer Strip Track or Pad selector" \
  "MPC condensed Mixer Strip showing Track" \
  "MPC condensed Mixer Strip showing Pad" \
  "MPC condensed Mixer Strip" \
  "openMainTrackEditContext" \
  "openMainArrangementGridContext" \
  "setOnDoubleTapListener" \
  "MpcTrackEditView" \
  "mpcShortcutLabel" \
  "mpcShortcutButton" \
  "hardwareFocus = drumTrack ? 10 : 0;" \
  "hardwareFocus == 10" \
  "buttonLedOnState" \
  "syncHardwareLevelModeLeds" \
  "syncHardwareMuteModeLed" \
  "syncPersistentHardwareModeLeds"; do
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