#!/usr/bin/env bash
set -euo pipefail

APK="android/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="com.miguelduval.mpcmk2groovebox.debug"
ACTIVITY="$PACKAGE/com.miguelduval.mpcmk2groovebox.MainActivity"
DUMP="/tmp/mpc-groovebox-ui.xml"
MAIN_ACTIVITY_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MainActivity.java"
UI_STATE_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcUiState.java"
NAVIGATION_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcNavigationController.java"
NATIVE_ENGINE_SOURCE="src/NativeEngine.cpp"
TRACK_EDIT_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcTrackEditView.java"
SHELL_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcShell.java"

echo "Running MPC Main UI source preflight..."
for required in \
  "MPC shell mixer strips" \
  "navigationController.setCompactMixerState(" \
  "state.compactMixerVisible()" \
  "state.compactMixerPadMode()" \
  "compactMixerPadModeForDisplay()" \
  "compactMixerStripModeAvailable()" \
  "MPC shell Track record mute and solo state" \
  "MPC shell track mixer strip" \
  "Main Mode selected sequence" \
  "Main Mode selected track" \
  "Main Mode selected program" \
  "nativeAudioGetPadSampleName" \
  "nativeAudioSetPadSampleName" \
  "BAR %03d  BEAT %d  TICK %03d" \
  'sequenceType.setText("SEQ")' \

  "Main Track View quick sample waveform" \
  "Main Track View monitor state" \
  "Main Track View length mode" \
  "Main Track View velocity state" \
  "Main Track View selected layer" \
  "Main Mode sequence tempo source • SEQ • Global unavailable" \
  "Main Track / Arrangement view switcher" \
  "Main Mode Track identity header" \
  "MPC Toolbar Menu" \
  "Main Mode sequence header" \
  "Main Mode BPM" \
  "Main Time Signature field • tap for editor" \
  "MPC_TIME_SIGNATURE_HIGHLIGHT" \
  "Timing Correct" \
  "Time Signature value" \
  "Time Signature numerator" \
  "Time Signature denominator" \
  "Main Track View header" \
  "Main Arrangement View header" \
  "Main Arrangement Edit RESERVED" \
  "Main Track View record sample" \
  "Main Track View browse samples" \
  "DRUM • TYPE" \
  "Main Track Edit" \
  "trackProgramHeader.addView(trackEditHeader," \
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




  "private LinearLayout buildCompactMixerTabs()" \
  "MPC condensed Mixer Strip" \
  "LVL" \
  "FX" \
  "SEND" \
  "I/O" \
  "+ NEW TRACK" \
  "openMainTrackEditContext" \
  "openMainArrangementGridContext" \
  "setOnDoubleTapListener" \
  "MpcTrackEditView" \
  "hardwareFeedbackView.setVisibility" \
  "bottomStatus.setVisibility" \
  "uiAuditSmokeMode ? View.VISIBLE : View.GONE" \
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

if ! grep -Fq -- "uiAuditSmokeMode ? View.VISIBLE : View.GONE" "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: normal Main must not expose diagnostic footer as permanent UI"
  exit 1
fi

if grep -Fq -- "private boolean compactMixerVisible" "$MAIN_ACTIVITY_SOURCE" || \
   grep -Fq -- "private boolean compactMixerPadMode" "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: compact Mixer presentation state leaked back into MainActivity-local booleans"
  exit 1
fi

if grep -Fq -- "sequenceAuxFields" "$MAIN_ACTIVITY_SOURCE" || \
   grep -Fq -- "sequenceFields.addView(bpm, weight())" "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: obsolete stacked Main Sequence field layout is still present"
  exit 1
fi

if grep -Fq -- "trackWorkspaceHeader" "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Track Edit must not use a separate blank workspace action header"
  exit 1
fi

if ! grep -Fq -- 'trackProgramHeader.addView(trackEditHeader,' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Track Edit pencil must stay in the Track identity header"
  exit 1
fi

if ! grep -Fq -- 'arrangementHeader.addView(' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Arrangement contextual action header must remain present"
  exit 1
fi

if grep -Fq -- 'Main Sequence Edit RESERVED' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main must not expose an invented Sequence Edit affordance"
  exit 1
fi

if ! grep -Fq -- 'MPC_FLAT_RADIUS_DP = 0' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: MPC Main/shell flat-chrome radius contract is missing"
  exit 1
fi

if ! grep -Fq -- 'compactContextField(' "$MAIN_ACTIVITY_SOURCE" ||    ! grep -Fq -- 'buildCompactMixerTabs()' "$MAIN_ACTIVITY_SOURCE" ||    ! grep -Fq -- 'MPC_FLAT_RADIUS_DP));' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: compact Mixer/Shortcut chrome flat styling contract is missing"
  exit 1
fi

if ! grep -Fq -- '"TRANSPOSE\\n—"' "$MAIN_ACTIVITY_SOURCE" &&    ! grep -Fq -- '"TRANSPOSE\\\\n—"' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main TRANSPOSE unavailable state contract is missing"
  exit 1
fi

if ! grep -Fq -- 'new String[]{"LVL", "FX", "SEND", "I/O"}' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: compact Mixer tab vocabulary contract is missing"
  exit 1
fi
if ! grep -Fq -- '(active ? " active" : " unavailable")' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: compact Mixer active/unavailable state policy is missing"
  exit 1
fi



if ! grep -Fq -- 'MPC_TIME_SIGNATURE_HIGHLIGHT' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Time Signature yellow highlight contract is missing"
  exit 1
fi

if ! grep -Fq -- 'trackWorkspace.addView(trackDetailRow,' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Track state row must remain in the workspace"
  exit 1
fi

echo "Running Main Track/Arrangement header styling preflight..."
main_view_switch_start=$(grep -n -m1 'private void setMainTrackArrangementView(boolean arrangementSelected)' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
main_view_switch_end=$(grep -n -m1 'private int selectedPadIndexForUi()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$main_view_switch_start" || -z "$main_view_switch_end" || "$main_view_switch_end" -le "$main_view_switch_start" ]]; then
  echo "ERROR: Main Track/Arrangement style method boundary is missing"
  exit 1
fi
main_view_switch_block=$(sed -n "${main_view_switch_start},$((main_view_switch_end - 1))p" "$MAIN_ACTIVITY_SOURCE")
if ! grep -Fq -- 'MPC presents Track / Arrangement as contextual headers' <<<"$main_view_switch_block"; then
  echo "ERROR: Main Track/Arrangement header fidelity contract is missing"
  exit 1
fi
if grep -Fq -- 'trackVisible ? ACCENT : SURFACE_2' <<<"$main_view_switch_block" ||    grep -Fq -- 'trackVisible ? SURFACE_2 : ACCENT' <<<"$main_view_switch_block"; then
  echo "ERROR: Main Track/Arrangement headers must not regress to cyan-card active styling"
  exit 1
fi

echo "Running Track View shell ownership preflight..."
track_view_start=$(grep -n -m1 'private void showTrackViewPage()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
track_view_end=$(grep -n -m1 'private void showSequencePage()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$track_view_start" || -z "$track_view_end" || "$track_view_end" -le "$track_view_start" ]]; then
  echo "ERROR: Track View source boundary is missing"
  exit 1
fi
track_view_block=$(sed -n "${track_view_start},$((track_view_end - 1))p" "$MAIN_ACTIVITY_SOURCE")
if ! grep -Fq -- 'MPC Track View context header' <<<"$track_view_block"; then
  echo "ERROR: Track View context header contract is missing"
  exit 1
fi
if ! grep -Fq -- 'MPC_FLAT_RADIUS_DP' <<<"$track_view_block"; then
  echo "ERROR: Track View channel-strip flat chrome contract is missing"
  exit 1
fi
for obsolete in 'actionButton("PREV"' 'actionButton("NEXT"' 'v -> showArrangePage()'; do
  if grep -Fq -- "$obsolete" <<<"$track_view_block"; then
    echo "ERROR: Track View must not duplicate shell navigation control: $obsolete"
    exit 1
  fi
done
if grep -Fq -- 'SELECTED TRACK • shell Function Bar' <<<"$track_view_block"; then
  echo "ERROR: Track View must not render a duplicate instructional footer"
  exit 1
fi

if ! grep -Fq -- 'compactMixerStripModeToggle = actionButton("□  ▦",' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Track lower-right selector must expose the single-pad / four-squares pair"
  exit 1
fi

if ! grep -Fq -- '0.33f' "$MAIN_ACTIVITY_SOURCE" || \
   ! grep -Fq -- '0.67f' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Sequence/Track workspace proportions drifted from the MPC density target"
  exit 1
fi

track_detail_line=$(grep -n -m1 'trackWorkspace.addView(trackDetailRow,' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
track_canvas_line=$(grep -n -m1 'trackWorkspace.addView(quickTrack,' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$track_detail_line" || -z "$track_canvas_line" || "$track_detail_line" -le "$track_canvas_line" ]]; then
  echo "ERROR: Main Track state row must remain below the performance canvas"
  exit 1
fi

echo "Running MPC shell geometry preflight..."
for required in \
  "TOOLBAR_HEIGHT_DP = 44" \
  "SHORTCUT_RAIL_WIDTH_DP = 48" \
  "CHANNEL_STRIP_WIDTH_DP = 210" \
  "FUNCTION_BAR_HEIGHT_DP = 40"; do
  if ! grep -Fq -- "$required" "$SHELL_SOURCE"; then
    echo "ERROR: MPC shell geometry contract missing: $required"
    exit 1
  fi
done

echo "Running MPC UI state/navigation source preflight..."
for required in \
  "compactMixerVisible" \
  "compactMixerPadMode" \
  "withCompactMixerState" \
  "setCompactMixerState" \
  "Exactly five shortcuts are required"; do
  if ! grep -Fq -- "$required" "$UI_STATE_SOURCE" && \
     ! grep -Fq -- "$required" "$NAVIGATION_SOURCE"; then
    echo "ERROR: UI state/navigation contract missing: $required"
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