#!/usr/bin/env bash
set -euo pipefail

APK="android/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="com.miguelduval.mpcmk2groovebox.debug"
ACTIVITY="$PACKAGE/com.miguelduval.mpcmk2groovebox.MainActivity"
DEVICE_DUMP="/sdcard/window_dump.xml"
DUMP="/tmp/mpc-groovebox-ui.xml"
SMOKE_MODE_EXTRA="mpc.groovebox.smoke.mode"
MAIN_ACTIVITY_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MainActivity.java"
ARRANGE_VIEW_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcArrangeView.java"
BROWSER_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcBrowserView.java"
BROWSER_FILTER_ICON_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcBrowserFilterIconDrawable.java"
UI_STATE_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcUiState.java"
NAVIGATION_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcNavigationController.java"
NATIVE_ENGINE_SOURCE="src/NativeEngine.cpp"
TRACK_EDIT_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcTrackEditView.java"
SHELL_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcShell.java"

echo "Running reserved shortcut state ownership preflight..."
reserved_nav_start=$(grep -n -m1 'private void navigateToMode(MpcUiState.Mode mode)' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
reserved_nav_end=$(grep -n -m1 'private void updateMpcShellState()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$reserved_nav_start" || -z "$reserved_nav_end" || "$reserved_nav_end" -le "$reserved_nav_start" ]]; then
  echo "ERROR: reserved shortcut navigation method boundary is missing"
  exit 1
fi
reserved_nav_block=$(sed -n "${reserved_nav_start},$((reserved_nav_end - 1))p" "$MAIN_ACTIVITY_SOURCE")
if ! grep -Fq -- 'navigationController.navigate(mode);' <<<"$reserved_nav_block"; then
  echo "ERROR: reserved shortcut activation must preserve its semantic mode"
  exit 1
fi
echo "Running MPC shortcut config source hygiene preflight..."
shortcut_config_start=$(grep -n -m1 'private void showShortcutConfigPage()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$shortcut_config_start" ]]; then
  echo "ERROR: Shortcut config source boundary is missing"
  exit 1
fi
shortcut_config_end=$(grep -n -m1 'private void navigateBackFromShell()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$shortcut_config_end" || "$shortcut_config_end" -le "$shortcut_config_start" ]]; then
  shortcut_config_end=$((shortcut_config_start + 240))
fi
shortcut_config_block=$(sed -n "${shortcut_config_start},$((shortcut_config_end - 1))p" "$MAIN_ACTIVITY_SOURCE")
if grep -Fq -- 'availableModes' <<<"$shortcut_config_block"; then
  echo "ERROR: Shortcut config must use the canonical shortcut catalog, not the obsolete availableModes list"
  exit 1
fi
echo "Running MPC default shortcut fidelity preflight..."
for required in \
  'DEFAULT_SHORTCUTS' \
  'MpcUiState.Mode.BROWSER' \
  'MpcUiState.Mode.CHANNEL_MIXER' \
  'MpcUiState.Mode.PAD_MIXER' \
  'MpcUiState.Mode.SOUNDS' \
  'MpcUiState.Mode.XYFX'; do
  if ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcModeRegistry.java" && \
     ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcUiState.java"; then
    echo "ERROR: MPC default shortcut fidelity contract missing: $required"
    exit 1
  fi
done

for required in \
  'case BROWSER: return "BROWSER";' \
  'case CHANNEL_MIXER: return "CH MIX";' \
  'case PAD_MIXER: return "PAD MIX";' \
  'case SOUNDS: return "SOUNDS";' \
  'case XYFX: return "XY";'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: MPC Shortcut Rail visible-label contract missing: $required"
    exit 1
  fi
done
if grep -Eq -- 'case (BROWSER|CHANNEL_MIXER|PAD_MIXER|SOUNDS|XYFX): return "[^"]*[⌂☷⌕▦▥✎∿●≡▤✣♫▶╬☰]"' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: canonical MPC Shortcut Rail must not use Unicode glyphs as visible labels"
  exit 1
fi

echo "Running MPC factory shortcut reset preflight..."
if ! grep -Fq -- 'void resetDefaultShortcuts()' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcNavigationController.java" ||    ! grep -Fq -- 'navigationController.resetDefaultShortcuts();' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: factory shortcut reset must use the dedicated default-reset contract"
  exit 1
fi
if grep -Fq -- 'navigationController.setShortcuts(' "$MAIN_ACTIVITY_SOURCE" &&    grep -Fq -- 'MpcModeRegistry.defaultShortcuts()' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main shortcut reset must not call strict setShortcuts(defaultShortcuts())"
  exit 1
fi

echo "Running MPC Pull-Down Menu preflight..."
for required in   "MpcPullDownPanelView"   "buildPullDownOverlay(root)"   "installPullDownGesture()"   "showPullDown()"   "hidePullDown()"   "CURRENT CONTROL"   "MPC Pull-Down Menu"   "sequenceTransportView.setContentDescription("   "swipe down for MPC Pull-Down Menu"; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE" &&      ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcPullDownPanelView.java"; then
    echo "ERROR: MPC Pull-Down Menu shell contract missing: $required"
    exit 1
  fi
done
if ! grep -Fq -- 'pageIndex == 0' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcPullDownPanelView.java" ||    ! grep -Fq -- 'setPage(pageIndex + 1)' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcPullDownPanelView.java" ||    ! grep -Fq -- 'setPage(0)' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: MPC Pull-Down must expose both Control and Q-Link pages"
  exit 1
fi

echo "Running MPC Main UI source preflight..."
for required in \
  "MPC shell mixer strips" \
  "navigationController.setCompactMixerState(" \
  "state.compactMixerVisible()" \
  "state.compactMixerPadMode()" \
  "compactMixerPadModeForDisplay()" \
  "compactMixerStripModeAvailable()" \
  "MPC shell Track record mute and solo state" \
  "Main Mode selected sequence" \
  "Main Mode selected track" \
  "nativeAudioGetPadSampleName" \
  "nativeAudioSetPadSampleName" \
  "BAR %03d  BEAT %d  TICK %03d" \
  'sequenceType.setText("SEQ")' \
  "Main Track View quick sample waveform" \
  "Main Sequence transpose state" \
  "Main Track View monitor state" \
  "Main Track View length mode" \
  "Main Track View velocity state" \
  "Main Sequence transpose state" \
  'TRANSPOSE\n—' \
  "Main Track View selected layer" \
  "Main Mode sequence tempo source • SEQ • Global unavailable" \
  "Main Track visual hierarchy • Track / Program / workspace header" \
  "Main Mode selected track" \
  "MPC Toolbar Menu" \
  "MPC Project Browser" \
  "MPC_TOOLBAR_BG" \
  "pageTitle.setVisibility(View.GONE)" \
  "Main Mode sequence header" \
  "Main Mode Track Program context" \
  "MPC_PANEL_DARK" \
  "MPC_FLAT_RADIUS_DP));" \
  "Main Mode selected program" \
  "Main Mode program ownership status" \
  "buildMainTrackTypeIconStrip" \
  "Main Mode selected Track Type icon" \
  'TRACK_TYPE_CHANGE_RESERVED_STATUS' \
  "buildMpcMenuTile" \
  "styleMpcMenuFooterButton" \
  "MPC Main BPM field • double-tap for numeric entry" \
  "MPC Main BARS field • double-tap for numeric entry" \
  "MPC Main LOOP START field • double-tap for numeric entry" \
  "MPC Main LOOP END field • double-tap for numeric entry" \
  "Main Time Signature field • tap for editor" \
  "MPC_TIME_SIGNATURE_HIGHLIGHT" \
  "Timing Correct" \
  "Time Signature value" \
  "Time Signature numerator" \
  "Time Signature denominator" \
  "Main Track View header" \
  "Main Arrangement View header" \
  "Main Track View record sample" \
  "Main Track View browse samples" \
  "Main Track View quick sample editor • controller-first selected Pad" \
  'mainTrackSampleEmptyActions = row();' \
  'mainTrackSampleAuditionButton = mainActionButton(' \
  'FrameLayout sampleSurface = new FrameLayout(this);' \
  'Gravity.CENTER' \
  'Gravity.RIGHT | Gravity.CENTER_VERTICAL' \
  'quickTrack.addView(sampleColumn,' \
  'mainTrackTypeField = buildMainTrackTypeIconStrip();' \
  "Main Track Edit" \
  "trackContextHeader.addView(trackEditHeader," \
  "MPC Function Bar REC ARM" \
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
  "mpcShortcutLabel" \
  "mpcShortcutButton" \
  "buttonLedOnState" \
  "syncHardwareLevelModeLeds" \
  "syncHardwareMuteModeLed" \
  "syncPersistentHardwareModeLeds"; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: MainActivity source contract missing: $required"
    exit 1
  fi
done

# Main Track Type fidelity also needs the Pad-mode strip source window.
# Keep this extraction before the first Pad-mode assertion; this script runs
# with set -u, so referencing it earlier makes the CI fail before the real UI
# preflight can execute.
mixer_strip_source="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainMixerStripView.java"
pad_mode_start=$(grep -n -m1 'if (padMode)' "$mixer_strip_source" | cut -d: -f1)
if [[ -z "$pad_mode_start" ]]; then
  echo "ERROR: Main XL mixer pad-mode branch is missing"
  exit 1
fi
pad_mode_block=$(sed -n "$pad_mode_start,$((pad_mode_start + 24))p" "$mixer_strip_source")

if grep -Eq -- 'private int hardwareFocus([[:space:]]|=)' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Data Dial focus must not have an independent MainActivity hardwareFocus field"
  exit 1
fi
for required in   'private int hardwareFocusId()'   'navigationController.state().dataDialFocus()'   'MpcUiState.DataDialFocus.SEQUENCE_BPM'   'MpcUiState.DataDialFocus.SEQUENCE_BARS'   'MpcUiState.DataDialFocus.TRACK'   'MpcUiState.DataDialFocus.PROGRAM'   'MpcUiState.DataDialFocus.TRACK_TYPE'   'MpcUiState.DataDialFocus.SAMPLE_LAYER'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: semantic Data Dial owner contract missing: $required"
    exit 1
  fi
done

if ! grep -Fq -- "hardwareFeedbackView.setVisibility(View.GONE);" "$MAIN_ACTIVITY_SOURCE" ||    ! grep -Fq -- "bottomStatus.setVisibility(View.GONE);" "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: ui-audit mode must keep diagnostic footer out of the visual evidence surface"
  exit 1
fi

if grep -Fq -- "private boolean compactMixerVisible =" "$MAIN_ACTIVITY_SOURCE" || \
   grep -Fq -- "private boolean compactMixerPadMode =" "$MAIN_ACTIVITY_SOURCE"; then
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

if ! grep -Fq -- 'trackContextHeader.addView(trackEditHeader,' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Track Edit pencil must stay in the Track identity header"
  exit 1
fi
main_identity_start=$(grep -n -m1 'private void showMainPage()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
main_identity_type=$(grep -n -m1 'TextView trackName = mainField("TRACK")' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
main_identity_view=$(grep -n -m1 'mainTrackViewButton = mainSectionToggle(' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$main_identity_start" || -z "$main_identity_type" || -z "$main_identity_view" || "$main_identity_type" -le "$main_identity_start" || "$main_identity_view" -le "$main_identity_type" ]]; then
  echo "ERROR: Main Track identity band source boundary is missing"
  exit 1
fi
main_identity_block=$(sed -n "$main_identity_start,$((main_identity_view - 1))p" "$MAIN_ACTIVITY_SOURCE")
if grep -Fq -- 'mainField("PROGRAM")' <<<"$main_identity_block" || grep -Fq -- 'Main Mode selected program' <<<"$main_identity_block"; then
  echo "ERROR: MPC3 Main Track identity band must not expose a duplicate Program field"
  exit 1
fi
if grep -Fq -- 'mainField("PROGRAM")' <<<"$main_identity_block" || grep -Fq -- 'Main Mode selected program' <<<"$main_identity_block"; then
  echo "ERROR: MPC3 Main Track identity band must not expose a duplicate Program field"
  exit 1
fi
if grep -Fq -- 'mainProgramField' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: obsolete Main Program field presentation pointer remains"
  exit 1
fi
if ! grep -Fq -- 'mainTrackTypeField = buildMainTrackTypeIconStrip();' <<<"$main_identity_block" || \
   ! grep -Fq -- 'trackContextHeader.addView(' <<<"$main_identity_block" || \
   ! grep -Fq -- 'mainTrackTypeField,' <<<"$main_identity_block"; then
  echo "ERROR: unified Main Track Type icon cluster ownership contract is missing"
  exit 1
fi

if ! grep -Fq -- 'mainArrangementViewButton = mainSectionToggle(' "$MAIN_ACTIVITY_SOURCE" || \
   ! grep -Fq -- 'trackArrangementToggle.addView(mainArrangementViewButton,' "$MAIN_ACTIVITY_SOURCE" || \
   ! grep -Fq -- 'trackContextHeader.addView(trackArrangementToggle,' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Arrangement action must stay inside the unified Track/Arrangement segmented header"
  exit 1
fi

main_track_type_line=$(grep -n -m1 'mainTrackTypeField,' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
main_track_name_line=$(grep -n -m1 'trackName,' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$main_track_type_line" || -z "$main_track_name_line" || "$main_track_type_line" -ge "$main_track_name_line" ]]; then
  echo "ERROR: Main Track Type icon must remain beside the Track identity before the Track name"
  exit 1
fi

if ! grep -Fq -- 'Main Sequence Edit/Copy RESERVED until semantic backend exists' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Sequence pencil affordance must remain visible as truthful reserved UI"
  exit 1
fi

if ! grep -Fq -- 'MPC_FLAT_RADIUS_DP = 0' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: MPC Main/shell flat-chrome radius contract is missing"
  exit 1
fi
if ! grep -Fq -- 'private static final int MPC_TOOLBAR_BG' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: MPC One toolbar visual contract is missing"
  exit 1
fi
if ! grep -Fq -- 'pageTitle.setVisibility(View.GONE);' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: legacy duplicate Main page title must remain hidden in the MPC toolbar"
  exit 1
fi
if ! grep -Fq -- 'buildMainTrackTypeIconStrip()' "$MAIN_ACTIVITY_SOURCE" || \
   ! grep -Fq -- 'refreshMainTrackTypeVisuals()' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Track type icon cluster contract is missing"
  exit 1
fi
echo "Running MPC Main Track Type single-icon fidelity preflight..."
for required in \
  'MPC_MAIN_TRACK_TYPE_ICON_WIDTH_DP = 38' \
  'refreshMainTrackTypeVisuals();' \
  'buildMainTrackTypeIconStrip()' \
  'trackTypeIconDrawable(' \
  'TRACKTYPE_ICON' \
  'TRACK_TYPE_CHANGE_RESERVED_STATUS' \
  'cleanTrackDisplayName(' ; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: Main Track Type fidelity presentation contract missing: $required"
    exit 1
  fi
done
if ! grep -Fq -- 'buildTrackStrip(' <<<"$pad_mode_block"; then
  echo "ERROR: Main Pad-mode XL strip must pair the selected Pad with its selected Track"
  exit 1
fi
if ! grep -Fq -- 'PAD • SELECTED' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainMixerStripView.java"; then
  echo "ERROR: Main Pad-mode XL strip selected-Pad presentation contract is missing"
  exit 1
fi
if grep -Fq -- 'focusLabel' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainMixerStripView.java" || \
   grep -Fq -- 'DIAL •' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainMixerStripView.java"; then
  echo "ERROR: XL Mixer Strip must not reintroduce the obsolete standalone Data Dial row"
  exit 1
fi
if ! grep -Fq -- 'indicator.setBackgroundColor("LVL".equals(tabName) ? RED' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainMixerStripView.java"; then
  echo "ERROR: MPC XL mixer tabs must use a flat active LVL underline"
  exit 1
fi
for required in \
  'MpcTrackTypeIconDrawable' \
  'MpcTrackTypeIconDrawable.Type.DRUM' \
  'MpcTrackTypeIconDrawable.Type.KEYGROUP' \
  'MpcTrackTypeIconDrawable.Type.PLUGIN' \
  'MpcTrackTypeIconDrawable.Type.MIDI' \
  'MpcTrackTypeIconDrawable.Type.AUDIO' \
  'MpcTrackTypeIconDrawable.Type.CV' \
  'setForeground(icon)' \
  'Main Track Type ' \
  'setContentDescription("Main Mode selected Track Type icon' \
  'TRACKTYPE_ICON'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE" && \
     ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcTrackTypeIconDrawable.java"; then
    echo "ERROR: MPC Main Track Type six-icon presentation contract missing: $required"
    exit 1
  fi
done
track_type_map_start=$(grep -n -m1 'private MpcTrackTypeIconDrawable trackTypeIconDrawable(String active)' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
track_type_map_end=$(awk -v start="$track_type_map_start" 'NR > start && /^    private / { print NR; exit }' "$MAIN_ACTIVITY_SOURCE")
if [[ -z "$track_type_map_start" || -z "$track_type_map_end" || "$track_type_map_end" -le "$track_type_map_start" ]]; then
  echo "ERROR: Main Track Type native-label mapping source boundary is missing"
  exit 1
fi
track_type_map_block=$(sed -n "${track_type_map_start},$((track_type_map_end - 1))p" "$MAIN_ACTIVITY_SOURCE")
for required in 'case "AUDIO":' 'type = MpcTrackTypeIconDrawable.Type.AUDIO;' 'case "DRUM":' 'type = MpcTrackTypeIconDrawable.Type.DRUM;' 'case "CV":'; do
  if ! grep -Fq -- "$required" <<<"$track_type_map_block"; then
    echo "ERROR: native Main Track Type vocabulary mapping missing: $required"
    exit 1
  fi
done
if grep -Fq -- 'case "CLIP":' <<<"$track_type_map_block" || \
   grep -Fq -- 'Type.CLIP' "$MAIN_ACTIVITY_SOURCE" || \
   grep -Fq -- 'Type.CLIP' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcTrackTypeIconDrawable.java" || \
   grep -Fq -- 'private void clip(' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcTrackTypeIconDrawable.java"; then
  echo "ERROR: Clip/Arrangement vocabulary must not stand in for the AUDIO Main Track Type"
  exit 1
fi

mixer_track_type_map_start=$(grep -n -m1 'private MpcTrackTypeIconDrawable trackTypeIcon(String trackType)' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainMixerStripView.java" | cut -d: -f1)
mixer_track_type_map_end=$(awk -v start="$mixer_track_type_map_start" 'NR > start && /^    private / { print NR; exit }' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainMixerStripView.java")
if [[ -z "$mixer_track_type_map_start" || -z "$mixer_track_type_map_end" || "$mixer_track_type_map_end" -le "$mixer_track_type_map_start" ]]; then
  echo "ERROR: XL mixer Track Type icon mapping source boundary is missing"
  exit 1
fi
mixer_track_type_map_block=$(sed -n "${mixer_track_type_map_start},$((mixer_track_type_map_end - 1))p" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainMixerStripView.java")
for required in 'case "AUDIO":' 'type = MpcTrackTypeIconDrawable.Type.AUDIO;' 'case "DRUM":' 'type = MpcTrackTypeIconDrawable.Type.DRUM;' 'case "CV":'; do
  if ! grep -Fq -- "$required" <<<"$mixer_track_type_map_block"; then
    echo "ERROR: XL mixer Track Type icon vocabulary mapping missing: $required"
    exit 1
  fi
done
if grep -Fq -- 'case "CLIP":' <<<"$mixer_track_type_map_block" || \
   grep -Fq -- 'Type.CLIP' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainMixerStripView.java"; then
  echo "ERROR: XL mixer strip must not refer to the obsolete CLIP Track Type icon"
  exit 1
fi

echo "Running Track View mute round-trip preflight..."
for required in \
  'verifyTrackViewMuteToggleAndRestoreForAudit(' \
  'completeTrackViewMuteToggleAndRestoreForAudit(' \
  'UI_TRACK_VIEW_MUTED_CAPTURE_READY:' \
  'mpc-track-view-muted-capture-release' \
  'UI_TRACK_VIEW_MUTE_ROUNDTRIP_COMPLETE:'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: Track View mute round-trip contract missing: $required"
    exit 1
  fi
done
track_view_audit_start=$(grep -n -m1 'private void runUiAuditAfterTrackView()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
track_view_audit_end=$(awk -v start="$track_view_audit_start" 'NR > start && /^    private / { print NR; exit }' "$MAIN_ACTIVITY_SOURCE")
if [[ -z "$track_view_audit_start" || -z "$track_view_audit_end" || "$track_view_audit_end" -le "$track_view_audit_start" ]]; then
  echo "ERROR: Track View audit source boundary is missing"
  exit 1
fi
track_view_audit_block=$(sed -n "${track_view_audit_start},$((track_view_audit_end - 1))p" "$MAIN_ACTIVITY_SOURCE")
if ! grep -Fq -- 'verifyTrackViewMuteToggleAndRestoreForAudit(' <<<"$track_view_audit_block"; then
  echo "ERROR: Track View audit must invoke the mute round-trip on its real workspace"
  exit 1
fi

echo "Running Arrangement-event-to-Grid navigation preflight..."
for required in \
  'focusSequenceGridAtTick(tick)' \
  'private int focusSequenceGridAtTick(long tick)' \
  'MpcSequenceZoomPolicy.stepIndexForTick(' \
  'MpcSequenceZoomPolicy.gridStartForStep(' \
  'ARRANGE EVENT • GRID FOCUSED • STEP'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: Arrangement event must open Grid at its corresponding tick/step: $required"
    exit 1
  fi
done

echo "Running Arrangement event → Grid gesture integration preflight..."
for required in \
  'verifyArrangeLoopDragAndRestoreForAudit()' \
  'dispatchArrangementLoopDragForAudit(' \
  'loopEndHandlePointForAudit(' \
  'UI_ARRANGE_LOOP_GESTURE_COMPLETE' \
  'verifyArrangeTrackSelectionAndRestoreForAudit()' \
  'trackLanePointForAudit(' \
  'selectedTrackForAudit()' \
  'dispatchArrangementSingleTapForAudit(' \
  'restoreArrangeTrackSelectionAfterAudit(' \
  'UI_ARRANGE_TRACK_SELECTION_COMPLETE:' \
  'UI_ARRANGE_TRACK_SELECTION_SINGLE_TRACK_COMPLETE:' \
  'alternateTrackChangeUnavailable=true' \
  'arrangementDataUnchanged=true' \
  'prepareArrangementEventGestureAudit()' \
  'eventCenterForAudit(' \
  'dispatchArrangementEventDoubleTapForAudit(' \
  'dispatchArrangementTouchForAudit(' \
  'UI_ARRANGEMENT_EVENT_SEEDED:' \
  'nativeSequenceMoveArrangementEvent(' \
  'Arrangement move selected event one grid earlier' \
  'Arrangement move selected event one grid later' \
  'UI_ARRANGEMENT_EVENT_GESTURE_DISPATCHED:' \
  'lastArrangementFocusTick' \
  'openGridFromArrangementEvent(trackIndex, tick)' \
  'UI_ARRANGEMENT_GRID_FOCUS_COMPLETE' \
  'UI_TRACK_VIEW_MUTED_CAPTURE_READY:' \
  'mpc-track-view-muted-capture-release' \
  'UI_ARRANGEMENT_GRID_FOCUS_CAPTURE_READY' \
  'mpc-arrange-grid-focus-capture-release' \
  'UI_ARRANGEMENT_BACK_CONTEXT_COMPLETE:' \
  'UI_ARRANGEMENT_BACK_CAPTURE_READY' \
  'mpc-arrange-back-capture-release' \
  'UI_ARRANGEMENT_GRID_BACK_REENTRY_COMPLETE:' \
  'UI_ARRANGEMENT_GRID_BACK_REENTRY_CAPTURE_READY' \
  'mpc-arrange-grid-reentry-capture-release' \
  'captureArrangeNavigationPatternSnapshot' \
  'firstStepForTest()'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE" "$ARRANGE_VIEW_SOURCE"; then
    echo "ERROR: Arrangement event gesture integration contract missing: $required"
    exit 1
  fi
done
arrange_audit_start=$(grep -n -m1 'private void runUiAuditAfterArrangement()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
arrange_audit_end=$(awk -v start="$arrange_audit_start" 'NR > start && /^    private / { print NR; exit }' "$MAIN_ACTIVITY_SOURCE")
if [[ -z "$arrange_audit_start" || -z "$arrange_audit_end" || "$arrange_audit_end" -le "$arrange_audit_start" ]]; then
  echo "ERROR: Arrange gesture audit source boundaries are missing"
  exit 1
fi
arrange_audit_block=$(sed -n "${arrange_audit_start},$((arrange_audit_end - 1))p" "$MAIN_ACTIVITY_SOURCE")
if grep -Fq -- 'openGridFromArrangementEvent(' <<<"$arrange_audit_block"; then
  echo "ERROR: Arrange gesture audit must dispatch the GestureDetector path, not call the focus route directly"
  exit 1
fi

if grep -Fq -- 'findViewWithContentDescription(\n                content, "Main Mode track type selector")' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Track Type must not retain a second selector lookup after unified field migration"
  exit 1
fi
if ! grep -Fq -- 'Main Mode selected program' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: central MPC Program context contract is missing"
  exit 1
fi

if ! grep -Fq -- 'compactContextField(' "$MAIN_ACTIVITY_SOURCE" ||    ! grep -Fq -- 'buildCompactMixerTabs()' "$MAIN_ACTIVITY_SOURCE" ||    ! grep -Fq -- 'MPC_FLAT_RADIUS_DP));' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: compact Mixer/Shortcut chrome flat styling contract is missing"
  exit 1
fi

if ! grep -Fq -- 'transpose.setText("TRANSPOSE' "$MAIN_ACTIVITY_SOURCE" || \
   ! grep -Fq -- 'TRANSPOSE • unavailable in current Sequence backend' "$MAIN_ACTIVITY_SOURCE"; then
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

if ! grep -Fq -- 'TextView layerDetail = mainMetric("LAYER");' "$MAIN_ACTIVITY_SOURCE" || \
   ! grep -Fq -- 'layerDetail.setOnClickListener' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main LAYER must remain a single focused field, not a duplicated +/- control group"
  exit 1
fi
if ! grep -Fq -- 'MpcUiState.DataDialFocus.SAMPLE_LAYER' "$MAIN_ACTIVITY_SOURCE" || ! grep -Fq -- 'LAYER • DATA DIAL / +/-' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main LAYER field must retain semantic Data Dial focus entry"
  exit 1
fi
if ! grep -Fq -- 'Button loop = mainActionButton("",' "$MAIN_ACTIVITY_SOURCE" ||
   ! grep -Fq -- 'nativeSequenceSetLoopEnabled(' "$MAIN_ACTIVITY_SOURCE" ||
   ! grep -Fq -- 'loop.setContentDescription(' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Sequence Loop must be a dedicated semantic toggle button"
  exit 1
fi
if grep -Fq -- 'sequenceFields.addView(loop, weight());' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Sequence Loop must not consume a full parameter-field slot"
  exit 1
fi
if ! grep -Fq -- 'trackWorkspace.addView(trackDetailRow,' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Track state row must remain in the workspace"
  exit 1
fi

main_layer_start=$(grep -n -m1 'TextView layerDetail = mainMetric("LAYER");' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$main_layer_start" ]]; then
  echo "ERROR: Main LAYER field source boundary is missing"
  exit 1
fi
main_layer_block=$(sed -n "${main_layer_start},$((main_layer_start + 38))p" "$MAIN_ACTIVITY_SOURCE")
if grep -Fq -- 'LAYER\n%d/8' <<<"$main_layer_block"; then
  echo "ERROR: Main LAYER presentation must match MPC vocabulary and show the current layer index without a synthetic /8 suffix"
  exit 1
fi
if ! grep -Fq -- '"LAYER\n%d"' <<<"$main_layer_block"; then
  echo "ERROR: Main LAYER presentation contract must render the selected layer as the MPC-style single index"
  exit 1
fi

echo "Running MPC Main iconography regression preflight..."
for required in   'MpcMainIconDrawable'   'Mode.PENCIL'   'Mode.LOOP'   'Mode.PLAY'   'Mode.MENU'   'trackEditHeader.setForeground(new MpcMainIconDrawable'   'loop.setForeground(new MpcMainIconDrawable'   'mainTrackSampleAuditionButton.setForeground(new MpcMainIconDrawable'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE" &&      ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainIconDrawable.java"; then
    echo "ERROR: MPC Main controls must use deterministic vector iconography instead of Unicode text glyphs: $required"
    exit 1
  fi
done
if ! grep -Fq -- 'case LOOP:' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainIconDrawable.java" || \
   ! grep -Fq -- 'drawLoop(' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainIconDrawable.java"; then
  echo "ERROR: Main Loop icon is missing"
  exit 1
fi

echo "Running MPC Toolbar chrome preflight..."
for required in   'private static final int MPC_TOOLBAR_BG = Color.rgb(17, 19, 22);'   'bar.setBackgroundColor(MPC_TOOLBAR_BG)'   'private static final int MPC_SELECTION_RED = Color.rgb(224, 30, 61);'   'MPC Toolbar Menu'   'MPC Project Browser'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: MPC3 graphite Toolbar contract missing: $required"
    exit 1
  fi
done
for required in   'private static final int BG = Color.rgb(17, 19, 22);'   'toolbar.setBackgroundColor(BG)'; do
  if ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcShell.java"; then
    echo "ERROR: outer MPC shell graphite Toolbar host contract missing: $required"
    exit 1
  fi
done
if grep -Fq -- 'bar.setBackgroundColor(Color.rgb(224, 30, 61))' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: MPC3 Main Toolbar must not be hard-coded red"
  exit 1
fi
if ! grep -Fq -- 'compactTrackContext.setBackground(strokeBackground(
                MPC_SELECTION_RED,' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: MPC3 red selection accent contract is missing"
  exit 1
fi

echo "Running MPC3 Toolbar geometry preflight..."
for required in \
  'MPC_TOOLBAR_INSET_DP = 6' \
  'MPC_TOOLBAR_CONTROL_HEIGHT_DP = 34' \
  'MPC_TOOLBAR_GAP_DP = 2' \
  'MPC_TOOLBAR_MENU_WIDTH_DP = 38' \
  'MPC_TOOLBAR_PROJECT_IDENTITY_WIDTH_DP = 106' \
  'MPC_TOOLBAR_PROJECT_BROWSER_WIDTH_DP = 26' \
  'MPC_TOOLBAR_TIMING_WIDTH_DP = 60' \
  'MPC_TOOLBAR_METRO_WIDTH_DP = 58' \
  'MPC_TOOLBAR_AUTO_WIDTH_DP = 48' \
  'MPC_TOOLBAR_IO_WIDTH_DP = 40' \
  'bar.setContentDescription("MPC One Main Toolbar")' \
  'BAR  001    BEAT  1    TICK  000' \
  'MPC Toolbar Menu' \
  'MPC Toolbar MIDI IN' \
  'MPC Toolbar MIDI OUT' \
  'midiInTopStatus' \
  'midiOutTopStatus' \
  'MPC 3.9 keeps the Toolbar status-oriented' \
  'hardware-first operation, while the final cells show MIDI In/Out' \
  'view.setText("IN".equals(view.getTag()) ? "IN" : "OUT")'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: MPC3 Toolbar geometry/content contract missing: $required"
    exit 1
  fi
done

echo "Running MPC shortcut pictography preflight..."
SHORTCUT_ITEM_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcShortcutRailItemView.java"
for required in   'MpcShortcutIconDrawable'   'new MpcShortcutIconDrawable('   'dp(MPC_SHORTCUT_SELECTION_WIDTH_DP)'   'setSelectedState(boolean selected)'   'setSelected(selected)'   'case BROWSER:'   'case GRID:'   'case STEP:'   'case TRACK_VIEW:'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE" &&      ! grep -Fq -- "$required" "$SHORTCUT_ITEM_SOURCE" &&      ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcShortcutIconDrawable.java"; then
    echo "ERROR: deterministic MPC shortcut icon contract missing: $required"
    exit 1
  fi
done
if grep -Fq -- 'button.setTextColor(selected ? TEXT : MUTED)' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: shortcut selection must not rely on Unicode glyph color alone"
  exit 1
fi
if grep -Fq -- 'selected ? MPC_SELECTED : BG' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: shortcut rail must not use a full-width red selected tile"
  exit 1
fi
for required in 'MPC_SHORTCUT_SELECTION_WIDTH_DP = 3' 'canvas.drawRect(' 'Color.rgb(224, 30, 61)' 'graphite surface' ; do
  if ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcShortcutIconDrawable.java" && \
     ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: MPC shortcut selection-rail visual contract missing: $required"
    exit 1
  fi
done

echo "Running MPC One Main geometry preflight..."
for required in \
  'MPC_MAIN_CONTENT_GUTTER_DP = 4' \
  'MPC_MAIN_SECTION_GAP_DP = 2' \
  'MPC_MAIN_FIELD_HEIGHT_DP = 40' \
  'MPC_MAIN_METRIC_HEIGHT_DP = 36' \
  'MPC_MAIN_TRACK_STATE_HEIGHT_DP = 40' \
  'MPC_MAIN_RADIUS_DP = 0' \
  'private Button mainActionButton(' \
  'page.setPadding(dp(MPC_MAIN_CONTENT_GUTTER_DP), dp(2)' \
  'sequenceCard.setPadding(dp(4), dp(4), dp(4), dp(MPC_MAIN_SECTION_GAP_DP));' \
  'sequenceCard.addView(sequenceHeader' \
  'sequenceCard.addView(sequenceFields' \
  'trackProgramSection.addView(trackContextHeader' \
  'trackWorkspace.addView(trackDetailRow' \
  'MPC_MAIN_FIELD_HEIGHT_DP' \
  'MPC_MAIN_METRIC_HEIGHT_DP' \
  'MPC_MAIN_TRACK_STATE_HEIGHT_DP'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: MPC One Main geometry contract missing: $required"
    exit 1
  fi
done

main_header_start=$(grep -n -m1 'trackProgramSection.addView(trackContextHeader' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$main_header_start" ]]; then
  echo "ERROR: Main Track header geometry anchor is missing"
  exit 1
fi
main_header_block=$(sed -n "$main_header_start,$((main_header_start + 8))p" "$MAIN_ACTIVITY_SOURCE")
if grep -Fq -- 'dp(44)' <<<"$main_header_block"; then
  echo "ERROR: Main Track context header still uses the pre-fidelity 44dp geometry"
  exit 1
fi

echo "Running MPC One Main visual hierarchy preflight..."
for required in \
  'mainSectionToggle(' \
  'Main Track Arrangement segmented control' \
  'Main Mode Track workspace' \
  'Main Mode selected program' \
  'Main Program create button reserved' \
  'programCreateButton' \
  'Main Track visual hierarchy' \
  'MPC_MAIN_WORKSPACE_WEIGHT' \
  'MPC_MAIN_TRACK_HEADER_HEIGHT_DP = 36' \
  'new LinearLayout.LayoutParams(0, dp(MPC_MAIN_TRACK_HEADER_HEIGHT_DP), 1.0f)'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: MPC One Main visual hierarchy contract missing: $required"
    exit 1
  fi
done

echo "Running Main workspace state-preservation preflight..."
main_page_start=$(grep -n -m1 'private void showMainPage()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
main_page_end=$(grep -n -m1 'private void installMainNumericEntry' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$main_page_start" || -z "$main_page_end" || "$main_page_end" -le "$main_page_start" ]]; then
  echo "ERROR: Main page source boundary is missing"
  exit 1
fi
main_page_block=$(sed -n "${main_page_start},$((main_page_end - 1))p" "$MAIN_ACTIVITY_SOURCE")
if grep -Fq -- 'navigationController.setDataDialFocus(MpcUiState.DataDialFocus.NONE);' <<<"$main_page_block"; then
  echo "ERROR: Main page rebuild must not unconditionally clear semantic Data Dial focus"
  exit 1
fi
for required in   'previousFocus'   'previousSubcontext'   'isMainWorkspaceDataDialFocus(previousFocus)'   'preserveMainContext'; do
  if ! grep -Fq -- "$required" <<<"$main_page_block"; then
    echo "ERROR: Main workspace focus-preservation contract missing: $required"
    exit 1
  fi
done
if ! grep -Fq -- 'private boolean isMainWorkspaceDataDialFocus(' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main-valid Data Dial focus policy helper is missing"
  exit 1
fi
if ! grep -Fq -- 'previousMainArrangementView' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main rebuild must preserve Track/Arrangement presentation state"
  exit 1
fi
if ! grep -Fq -- 'setMainTrackArrangementView(previousMainArrangementView);' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main rebuild must restore the prior Track/Arrangement presentation"
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
if grep -Fq -- 'showMainPage();' <<<"$main_view_switch_block"; then
  echo "ERROR: Track/Arrangement sibling switch must not rebuild Main page"
  exit 1
fi
if ! grep -Fq -- 'mainTrackArrangementHost.getChildAt(0)' <<<"$main_view_switch_block" || \
   ! grep -Fq -- 'mainTrackArrangementHost.getChildAt(1)' <<<"$main_view_switch_block"; then
  echo "ERROR: Track/Arrangement switch must operate on one shared workspace host"
  exit 1
fi
if ! grep -Fq -- 'MPC presents Track / Arrangement as a compact segmented context' <<<"$main_view_switch_block"; then
  echo "ERROR: Main Track/Arrangement header fidelity contract is missing"
  exit 1
fi
if grep -Fq -- 'trackVisible ? ACCENT : SURFACE_2' <<<"$main_view_switch_block" || \
   grep -Fq -- 'trackVisible ? SURFACE_2 : ACCENT' <<<"$main_view_switch_block"; then
  echo "ERROR: Main Track/Arrangement headers must not regress to cyan-card active styling"
  exit 1
fi

echo "Running Main Function Bar geometry preflight..."
rec_arm_start=$(grep -n -m1 'private void addSequenceRecArmFunction(' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
rec_arm_end=$(grep -n -m1 'private void addTrackStepperFunction(' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$rec_arm_start" || -z "$rec_arm_end" || "$rec_arm_end" -le "$rec_arm_start" ]]; then
  echo "ERROR: Main REC ARM Function Bar source boundary is missing"
  exit 1
fi
rec_arm_block=$(sed -n "${rec_arm_start},$((rec_arm_end - 1))p" "$MAIN_ACTIVITY_SOURCE")
if ! grep -Fq -- 'MPC Function Bar REC ARM' <<<"$rec_arm_block" || ! grep -Fq -- 'MPC Main sequence record scope' <<<"$rec_arm_block" || ! grep -Fq -- 'MPC_FLAT_RADIUS_DP));' <<<"$rec_arm_block"; then
  echo "ERROR: Main REC ARM flat-chrome contract is missing"
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
if ! grep -Fq -- 'MPC Track View focused track header' <<<"$track_view_block"; then
  echo "ERROR: Track View context header contract is missing"
  exit 1
fi
if ! grep -Fq -- 'MPC_FLAT_RADIUS_DP' <<<"$track_view_block"; then
  echo "ERROR: Track View channel-strip flat chrome contract is missing"
  exit 1
fi
for required in   'MPC Track View focused Track field'   'Track View track I/O unavailable'   'Track View track key range unavailable'   'Track View track monitor unavailable'   'Track View track level unavailable'   'Track View track pan unavailable'   'Track View track solo unavailable'   'Track View track MIDI filter unavailable'   'nativeSequenceToggleTrackMute(trackIndex)'; do
  if ! grep -Fq -- "$required" <<<"$track_view_block"; then
    echo "ERROR: Track View strip control contract missing: $required"
    exit 1
  fi
done
for required in 'strip.setOnClickListener(v -> {' 'String trackIdentityStatus ='; do
  if ! grep -Fq -- "$required" <<<"$track_view_block"; then
    echo "ERROR: Track View selection/identity contract missing: $required"
    exit 1
  fi
done
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

if ! grep -Fq -- 'Track −/+ is a high-frequency Main action' "$MAIN_ACTIVITY_SOURCE" || ! grep -Fq -- 'mainTrackArrangementHost.getChildAt(1).getVisibility()' "$MAIN_ACTIVITY_SOURCE" || ! grep -Fq -- 'MpcUiState.DataDialFocus.TRACK' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Track −/+ must preserve local view context and restore Track Data Dial focus"
  exit 1
fi

if ! grep -Fq -- 'private String cleanTrackDisplayName(String status)' "$MAIN_ACTIVITY_SOURCE" || ! grep -Fq -- 'trackName.setText(String.format(' "$MAIN_ACTIVITY_SOURCE" || ! grep -Fq -- 'Locale.ROOT, "%d  %s"' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Track identity must expose track number and clean user-facing name"
  exit 1
fi

if ! grep -Fq -- 'Track identity is intentionally compact' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Track identity source contract is missing"
  exit 1
fi

echo "Running Main Track-state row fidelity preflight..."
for required in   'trackDetailRow.addView(monitorDetail, weight());'   'trackDetailRow.addView(lengthDetail, weight());'   'trackDetailRow.addView(velocityDetail, weight());'   'layerDetail,'   'selectedLayer + 1'   'setBottomStatus("LAYER • DATA DIAL / +/-")'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: MPC Main Track-state row field contract missing: $required"
    exit 1
  fi
done
main_track_state_line=$(grep -n -m1 'trackWorkspace.addView(trackDetailRow' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
main_track_canvas_line=$(grep -n -m1 'trackWorkspace.addView(quickTrack' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$main_track_state_line" || -z "$main_track_canvas_line" || "$main_track_state_line" -le "$main_track_canvas_line" ]]; then
  echo "ERROR: Main Track-state row must be rendered below the Track waveform/canvas"
  exit 1
fi
if grep -Fq -- 'LinearLayout layerControls = row();' "$MAIN_ACTIVITY_SOURCE" ||    grep -Fq -- 'layerControls.addView(layerDownButton' "$MAIN_ACTIVITY_SOURCE" ||    grep -Fq -- 'layerControls.addView(layerUpButton' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main LAYER must remain one field, not a nested +/- control group"
  exit 1
fi
if ! grep -Fq -- 'layerDetail.setOnClickListener' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main LAYER field must own the shared Data Dial focus entry"
  exit 1
fi

echo "Running Main waveform layer-indicator preflight..."
if ! grep -Fq -- 'public void setLayerIndicator(int layer)' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/WaveformView.java" || \
   ! grep -Fq -- 'drawLayerIndicator(canvas, left, right, top);' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/WaveformView.java" || \
   ! grep -Fq -- 'mainTrackWaveform.setLayerIndicator(selectedLayer);' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main waveform eight-layer indicator contract is missing"
  exit 1
fi

echo "Running Main Track/Pad selector placement preflight..."
if ! grep -Fq -- 'mixerSelectorLp.bottomMargin = dp(44)' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Track/Pad selector must reserve the Track-state row below the canvas"
  exit 1
fi
if ! grep -Fq -- 'mainSequenceTransposeField = transpose;' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main TRANSPOSE ownership must remain in the Sequence section"
  exit 1
fi
echo "Running Main Track/Pad selector placement preflight..."
if grep -Fq -- 'trackDetailRow.addView(compactMixerStripModeToggle' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Track/Pad selector must not live inside the MONITOR/LENGTH/VELOCITY/LAYER state row"
  exit 1
fi

for required in   'compactMixerStripModeToggle = mainActionButton("",'   'Gravity.RIGHT | Gravity.BOTTOM'   'FrameLayout.LayoutParams mixerSelectorLp = new FrameLayout.LayoutParams('   'mixerSelectorLp.rightMargin = dp(4)'   'mixerSelectorLp.bottomMargin = dp(44)'   'mainTrackArrangementHost.addView(compactMixerStripModeToggle, mixerSelectorLp)'   'MPC 3.9 places the Track/Pad channel-strip selector in the'   'lower-right corner of the Track/Arrangement section'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: MPC Track/Pad selector lower-right placement contract missing: $required"
    exit 1
  fi
done

if ! grep -Fq -- 'compactMixerStripModeToggle = mainActionButton("",' "$MAIN_ACTIVITY_SOURCE" || \
   ! grep -Fq -- 'compactMixerStripModeToggle.setForeground(new MpcMixerStripIconDrawable' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Track lower-right selector must expose the deterministic single-pad / four-squares icon"
  exit 1
fi

if ! grep -Fq -- 'trackName.setOnClickListener(v -> focusMainTrackField())' "$MAIN_ACTIVITY_SOURCE" ||    ! grep -Fq -- 'private void focusMainTrackField()' "$MAIN_ACTIVITY_SOURCE" ||    ! grep -Fq -- 'showTrackSelectPage();' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Track tap must enter the canonical 4x4 Track Select context"
  exit 1
fi

if ! grep -Fq -- 'Main Sequence Select list' "$MAIN_ACTIVITY_SOURCE" ||    ! grep -Fq -- 'nativeSequenceSelect(index)' "$MAIN_ACTIVITY_SOURCE" ||    ! grep -Fq -- 'showMainPage();' "$MAIN_ACTIVITY_SOURCE" ||    ! grep -Fq -- 'MpcUiState.DataDialFocus.SEQUENCE' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Sequence Select must return to Main with Sequence Data Dial focus"
  exit 1
fi
if grep -Fq -- 'mainTrackSampleActionButton.setText("SAMPLE EDIT");' "$MAIN_ACTIVITY_SOURCE" ||    grep -Fq -- 'Main Track View sample edit' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Track canvas must not carry a permanent SAMPLE EDIT duplicate"
  exit 1
fi
if ! grep -Fq -- 'this::openMainTrackEditContext' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Track sample must retain waveform double-tap Track Edit entry"
  exit 1
fi

sequence_select_start=$(grep -n -m1 'private void showSequenceSelectPage()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
track_select_start=$(grep -n -m1 'private void showTrackSelectPage()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
program_select_start=$(grep -n -m1 'private void showProgramSelectPage()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$sequence_select_start" || -z "$track_select_start" || -z "$program_select_start" || \
      "$track_select_start" -le "$sequence_select_start" || "$program_select_start" -le "$track_select_start" ]]; then
  echo "ERROR: Main selector source boundaries are missing"
  exit 1
fi
sequence_select_block=$(sed -n "$sequence_select_start,$((track_select_start - 1))p" "$MAIN_ACTIVITY_SOURCE")
track_select_block=$(sed -n "$track_select_start,$((program_select_start - 1))p" "$MAIN_ACTIVITY_SOURCE")
if grep -Fq -- '"SEQUENCE EDIT"' <<<"$sequence_select_block"; then
  echo "ERROR: Sequence Select must not expose a duplicate SEQUENCE EDIT action"
  exit 1
fi
if grep -Fq -- '"TRACK VIEW"' <<<"$track_select_block"; then
  echo "ERROR: Track Select must not expose a duplicate TRACK VIEW action"
  exit 1
fi

if grep -Fq -- 'mainProgramField' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: obsolete Main Program presentation pointer remains"
  exit 1
fi
if ! grep -Fq -- 'nativeSequenceSetTrackProgram(' "$MAIN_ACTIVITY_SOURCE" ||    ! grep -Fq -- 'MpcUiState.DataDialFocus.PROGRAM' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Program semantics must remain available without a duplicate Main Program field"
  exit 1
fi

program_select_start=$(grep -n -m1 'private void showProgramSelectPage()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
program_select_end=$(grep -n -m1 'private void showBrowserPage()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$program_select_start" || -z "$program_select_end" || "$program_select_end" -le "$program_select_start" ]]; then
  echo "ERROR: Main Program Select source boundary is missing"
  exit 1
fi
program_select_block=$(sed -n "${program_select_start},$((program_select_end - 1))p" "$MAIN_ACTIVITY_SOURCE")
if ! grep -Fq -- 'nativeSequenceSetTrackProgram(' <<<"$program_select_block" ||    ! grep -Fq -- 'showMainPage();' <<<"$program_select_block" ||    ! grep -Fq -- 'MpcUiState.DataDialFocus.PROGRAM' <<<"$program_select_block"; then
  echo "ERROR: Program Select must apply to the selected Track and return to Main with Program focus"
  exit 1
fi

if ! grep -Fq -- 'PROGRAM SELECT • DRUM TRACK REQUIRED' "$MAIN_ACTIVITY_SOURCE" ||    ! grep -Fq -- 'programSelectPageVisible' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Program Data Dial routing contract is missing"
  exit 1
fi

if grep -Fq -- 'MpcUiState.Subcontext.TRACK_TYPE_SELECT' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Track Type must not enter a selector state without a real conversion backend"
  exit 1
fi
if ! grep -Fq -- 'TRACK_TYPE_CHANGE_RESERVED_STATUS' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Track Type Data Dial must remain truthful/reserved instead of routing to another screen"
  exit 1
fi

for required in   'MPC_FLAT_RADIUS_DP));'   'group.setBackground(strokeBackground('   'b.setBackground(strokeBackground('; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: Main MPC flat-chrome contract missing: $required"
    exit 1
  fi
done

echo "Running Main Mixer default-state preflight..."
if ! grep -Fq -- 'assertTrue(state.compactMixerVisible());' "android/app/src/test/java/com/miguelduval/mpcmk2groovebox/MpcUiStateTest.java"; then
  echo "ERROR: Main mixer strips default-visibility regression test is missing"
  exit 1
fi

echo "Running compact context sizing preflight..."
if grep -Fq -- 'ViewGroup.LayoutParams.MATCH_PARENT, dp(304)' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: persistent MPC context rail must not reserve the obsolete fixed 304dp height"
  exit 1
fi
if ! grep -Fq -- 'ViewGroup.LayoutParams.WRAP_CONTENT));' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: persistent MPC context rail must remain content-sized"
  exit 1
fi

echo "Running Main section framing preflight..."
for required in \
  'sequenceCard.setBackground(strokeBackground(' \
  'trackProgramSection.setBackground(strokeBackground(' \
  'Main Track visual hierarchy • Track / Program / workspace section' \
  'Main Sequence Loop button'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: Main section framing contract missing: $required"
    exit 1
  fi
done

if grep -Fq -- 'Main Arrangement Edit RESERVED' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: redundant Main Arrangement-edit header control remains"
  exit 1
fi

echo "Running Main workspace density preflight..."
main_geometry_start=$(grep -n -m1 'page.addView(sequenceCard, new LinearLayout.LayoutParams(' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
main_geometry_end=$(grep -n -m1 'page.addView(trackProgramSection, new LinearLayout.LayoutParams(' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$main_geometry_start" || -z "$main_geometry_end" || "$main_geometry_end" -le "$main_geometry_start" ]]; then
  echo "ERROR: Main Sequence/Track layout boundary is missing"
  exit 1
fi
main_geometry_block=$(sed -n "${main_geometry_start},$((main_geometry_end + 2))p" "$MAIN_ACTIVITY_SOURCE")
if ! grep -Fq -- 'ViewGroup.LayoutParams.WRAP_CONTENT' <<<"$main_geometry_block"; then
  echo "ERROR: Main Sequence band must be content-sized instead of consuming a proportional workspace weight"
  exit 1
fi
if grep -Fq -- '0.33f' <<<"$main_geometry_block" || grep -Fq -- '0.67f' <<<"$main_geometry_block"; then
  echo "ERROR: Main Sequence/Track layout must not reserve dead proportional space"
  exit 1
fi
if ! grep -Fq -- 'ViewGroup.LayoutParams.MATCH_PARENT, 0, 1' <<<"$main_geometry_block"; then
  echo "ERROR: Main Track/Arrangement workspace must own the remaining vertical space"
  exit 1
fi

track_detail_line=$(grep -n -m1 'trackWorkspace.addView(trackDetailRow,' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
track_canvas_line=$(grep -n -m1 'trackWorkspace.addView(quickTrack,' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$track_detail_line" || -z "$track_canvas_line" || "$track_canvas_line" -ge "$track_detail_line" ]]; then
  echo "ERROR: Main Track state row must remain directly below the performance canvas"
  exit 1
fi

echo "Running Main Track/Arrangement horizontal canvas preflight..."
if grep -Fq -- 'padColumn.addView(buildMiniMainPadGrid()' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main canonical Track/Arrangement workspace must not embed the Android 4x4 pad grid"
  exit 1
fi
if grep -Fq -- 'quickTrack.addView(padColumn' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main canonical Track/Arrangement workspace must not allocate a software pad column"
  exit 1
fi
# The Track state row is intentionally rendered immediately above the performance canvas
# (see the positive ordering assertion above). Do not assert the inverse here.

echo "Running MPC Pad Mixer Java nesting preflight..."
for required in   'interface FaderCallback'   'private final class Fader'; do
  if ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcPadMixerView.java"; then
    echo "ERROR: Pad Mixer Fader nesting contract missing: $required"
    exit 1
  fi
done
if grep -Fq -- 'interface Callback {' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcPadMixerView.java"; then
  echo "ERROR: Pad Mixer Fader must not declare a static member interface inside an inner class"
  exit 1
fi

echo "Running MPC Pad Mixer format-string safety preflight..."
if grep -Fq -- 'PAN %+0.2f' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcPadMixerView.java" ||    grep -Fq -- 'TUNE %+0.1f' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcPadMixerView.java"; then
  echo "ERROR: Pad Mixer must not use Java Formatter +0 flag without an explicit width"
  exit 1
fi
for required in 'PAN %+.2f' 'TUNE %+.1f'; do
  if ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcPadMixerView.java"; then
    echo "ERROR: Pad Mixer safe signed format contract missing: $required"
    exit 1
  fi
done

echo "Running MPC Browser flat-chrome preflight..."
echo "Running MPC Browser information-architecture preflight..."
if grep -Fq -- 'PROVIDER • ANDROID DOCUMENTS • OPEN PICKER BELOW' "$BROWSER_SOURCE" \
    || grep -Fq -- 'PROVIDER • ANDROID DOCUMENTS • OPEN PICKER BELOW' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Browser still presents Document Picker as its primary provider"
  exit 1
fi
for required in 'providerState = info(context,' 'LOCAL AUDIO • NOT SCANNED'; do
  if ! grep -Fq -- "$required" "$BROWSER_SOURCE"; then
    echo "ERROR: Browser initial local-provider state missing: $required"
    exit 1
  fi
done
for required in 'REQUEST_SCAN_LOCAL_AUDIO' 'REQUEST_OPEN_DOCUMENT_TREE' 'ACTION_OPEN_DOCUMENT_TREE' 'takePersistableUriPermission(' 'READ_MEDIA_AUDIO' 'READ_EXTERNAL_STORAGE' 'MpcLocalAudioCatalog.queryWavFiles' 'MpcDocumentTreeCatalog.queryWavFiles' 'nativeAudioLoadSampleForPadLayer(' 'MediaPlayer' 'void assignSelectedBrowserFile()' 'browserPickerTargetPad = Math.max(0, Math.min(15, selectedPad))' 'browserPickerTargetLayer = Math.max(0, Math.min(7, selectedLayer))' 'final int targetPad = browserPickerTargetPad >= 0' 'final int targetLayer = browserPickerTargetLayer >= 0'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE" \
      && ! grep -Fq -- "$required" "android/app/src/main/AndroidManifest.xml" \
      && ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcLocalAudioCatalog.java"; then
    echo "ERROR: Browser local WAV provider/workflow contract missing: $required"
    exit 1
  fi
done
echo "Running MPC Browser workspace composition preflight..."
for required in   'body.addView(center, new LayoutParams(0, -1, 1))'   'center.addView(scroll, new LayoutParams('   'ViewGroup.LayoutParams.MATCH_PARENT, 0, 1)'   'scroll.setContentDescription("MPC Browser results list")'   'TARGET • PAD'   'Browser target context • state only'; do
  if ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcBrowserView.java"; then
    echo "ERROR: MPC Browser workspace composition contract missing: $required"
    exit 1
  fi
done
if grep -Fq -- 'paramsWidth(context, 190)' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcBrowserView.java"; then
  echo "ERROR: Browser must not reserve the legacy 190dp duplicate target card"
  exit 1
fi
if grep -Fq -- 'targetPanel' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcBrowserView.java"; then
  echo "ERROR: Browser must not keep a separate Android-style target panel"
  exit 1
fi

if ! grep -Fq -- '"PROJECTS", "PATTERNS", "KITS", "PLUGIN PRESETS", "SAMPLES", "ALL"' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcBrowserView.java"; then
  echo "ERROR: MPC Browser six file-type filters are not aligned with the canonical filter taxonomy"
  exit 1
fi

for required in   '"PLACES", "CONTENT", "EXPANSIONS"'   'void onNavigationItemSelected(String section, String item);'   'void onNavigateUp();'   'void onChooseFolder();'   'void onLocalScanRequested();'   'void onFileSelected(MpcBrowserFile file);'   'void onAuditionSelectedFile();'   'void setSection(String section)'   'void setLocalFiles(List<MpcBrowserFile> files, String status)'   'matchesBrowserQuery('   'addFileResult('   'MPC Browser scan local audio files'   'SCAN LOCAL AUDIO…'   'NO LOCAL WAV FILES'   'button.setContentDescription('   '"MPC Browser navigation " + section + " " + item'   'search.setContentDescription("MPC Browser search files")'   'clear.setContentDescription("MPC Browser clear search")'   'destination.setContentDescription("MPC Browser target Pad and Layer")'   'currentSample.setContentDescription("MPC Browser current sample")'   'DRUMS'   'INSTRUMENTS'   'OPEN STORAGE…'   'CHOOSE FOLDER…'   'setUserFolderFiles('   'MPC Browser open Android storage picker'   'MPC Browser choose a local folder for " + activeLocationItem'   'CURRENT SAMPLE'; do
  if ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcBrowserView.java"; then
    echo "ERROR: MPC Browser information-architecture contract missing: $required"
    exit 1
  fi
done
for required in   'MPC_FLAT_RADIUS_DP = 0'   'MPC_BROWSER_SELECTED'   'MPC Browser'   'setCornerRadius(dp(getContext(), MPC_FLAT_RADIUS_DP))'   'selected ? MPC_BROWSER_SELECTED : SURFACE_2'; do
  if ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcBrowserView.java"; then
    echo "ERROR: MPC Browser flat-chrome contract missing: $required"
    exit 1
  fi
done

echo "Running MPC 4x4 Menu iconography preflight..."
menu_tile_source_start=$(grep -n -m1 'private Button buildMpcMenuTile' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
menu_footer_start=$(grep -n -m1 'private Button styleMpcMenuFooterButton' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$menu_tile_source_start" || -z "$menu_footer_start" || "$menu_footer_start" -le "$menu_tile_source_start" ]]; then
  echo "ERROR: MPC Menu tile source boundary is missing"
  exit 1
fi
menu_tile_block=$(sed -n "${menu_tile_source_start},$((menu_footer_start - 1))p" "$MAIN_ACTIVITY_SOURCE")
for required in 'MpcShortcutIconDrawable(entry.mode, 0.0f)' 'setCompoundDrawablesWithIntrinsicBounds(' 'setCompoundDrawablePadding('; do
  if ! grep -Fq -- "$required" <<<"$menu_tile_block"; then
    echo "ERROR: MPC 4x4 Menu tile vector icon contract missing: $required"
    exit 1
  fi
done
if grep -Fq -- 'glyph + "\n" + title' <<<"$menu_tile_block"; then
  echo "ERROR: MPC 4x4 Menu must not compose visible tiles from Unicode glyph text"
  exit 1
fi
echo "Running MPC Menu Function Bar ownership preflight..."
menu_start=$(grep -n -m1 'private void showMenuPage()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
shortcut_start=$(grep -n -m1 'private void showShortcutConfigPage()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$menu_start" || -z "$shortcut_start" || "$shortcut_start" -le "$menu_start" ]]; then
  echo "ERROR: Menu source boundary is missing"
  exit 1
fi
menu_block=$(sed -n "${menu_start},$((shortcut_start - 1))p" "$MAIN_ACTIVITY_SOURCE")
if grep -Fq -- 'LinearLayout system = row();' <<<"$menu_block"; then
  echo "ERROR: Menu must not render a second internal bottom command bar"
  exit 1
fi
for required in   'case MENU:'   'NEW PROJECT'   'SAVE'   'EDIT SHORTCUTS'   'if (mode == MpcUiState.Mode.PROJECT)'   'showProjectPage()'   'MPC Project Open'   'MPC Project Save As'   'MPC Project Save'   'nativeProjectSerialize()'   'nativeProjectLoad(byte[] data)'   'nativeProjectGetSummary()'   'ACTION_CREATE_DOCUMENT'   'ACTION_OPEN_DOCUMENT'   'PROJECT_SAVE_COMPLETE'   'PROJECT_OPEN_COMPLETE'   'loaded.startsWith("PROJECT_LOADED")'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: Menu contextual Function Bar contract missing: $required"
    exit 1
  fi
done

echo "Running MPC Pad Mixer presentation preflight..."
echo "Running MPC Pad Mixer Function Bar preflight..."
echo "Running MPC Main XL Mixer Strip preflight..."
for required in   'MpcMainMixerStripView'   'MPC Main XL Mixer Strips'   'MPC XL'   'TRACK • SELECTED'   'PAD • SELECTED'   'OUTPUT 1/2'   'MpcVerticalMeter'   'MpcPanSlider'   'addIdentityHeader('   'addProgramBand('   'MPC XL level meter and white-line fader'   'MPC XL selected track pan slider reserved'   'mixerStripVisible()'   'onMixerStripVisibilityChanged'   'MPC Main mixer strips show or hide'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE" &&      ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainMixerStripView.java"; then
    echo "ERROR: Main XL Mixer Strip architecture contract missing: $required"
    exit 1
  fi
done
if grep -Fq -- 'modeLabel' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainMixerStripView.java"; then
  echo "ERROR: XL Mixer Strip must not reserve a textual MIXER header"
  exit 1
fi
for required in 'visibilityButton = button(context, "", 10);' 'MpcMixerStripIconDrawable.Mode.PERSONAL_CHANNEL_STRIP' 'MPC Main mixer strips show or hide' 'header.addView(' 'dp(context, 26), dp(context, 20)'; do
  if ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainMixerStripView.java"; then
    echo "ERROR: XL Mixer Strip compact top visibility control contract missing: $required"
    exit 1
  fi
done

echo "Running MPC 3.9 Main XL mixer strip semantic preflight..."
mixer_strip_source="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainMixerStripView.java"
pad_mode_start=$(grep -n -m1 'if (padMode)' "$mixer_strip_source" | cut -d: -f1)
if [[ -z "$pad_mode_start" ]]; then
  echo "ERROR: Main XL mixer pad-mode branch is missing"
  exit 1
fi
pad_mode_block=$(sed -n "$pad_mode_start,$((pad_mode_start + 24))p" "$mixer_strip_source")
if ! grep -Fq -- 'buildPadStrip(' <<<"$pad_mode_block" ||    ! grep -Fq -- 'buildTrackStrip(' <<<"$pad_mode_block"; then
  echo "ERROR: MPC 3.9 Pad Mixer Strip must pair the selected Pad with its selected Track"
  exit 1
fi
if grep -Fq -- 'strips.addView(buildOutputStrip(' <<<"$pad_mode_block"; then
  echo "ERROR: MPC 3.9 Pad Mixer Strip must not show Main Output as the right strip"
  exit 1
fi

if ! grep -Fq -- 'new MpcMainMixerStripView(this,' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main XL Mixer Strip must be composed by the application shell"
  exit 1
fi
if ! grep -Fq -- 'assertTrue(state.compactMixerVisible())' "android/app/src/test/java/com/miguelduval/mpcmk2groovebox/MpcUiStateTest.java"; then
  echo "ERROR: Main Mixer default visibility regression test is missing"
  exit 1
fi

echo "Running MPC Pad Mixer Data Dial preflight..."
echo "Running MPC Pad Mixer hardware handler preflight..."
for required in   'MpcUiState.Mode.PAD_MIXER'   'MpcUiState.DataDialFocus.PAD_MIXER_LEVEL'   'MpcUiState.DataDialFocus.PAD_MIXER_PAN'   'MpcUiState.DataDialFocus.PAD_MIXER_TUNE'   'cyclePadMixerDialFocus()'   'MpcPadMixerView.ControlFocus.LEVEL'   'MpcPadMixerView.ControlFocus.PAN'   'MpcPadMixerView.ControlFocus.TUNE'   'onControlFocus'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE" &&      ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcUiState.java" &&      ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcPadMixerView.java"; then
    echo "ERROR: Pad Mixer hardware focus handler contract missing: $required"
    exit 1
  fi
done

for required in   'PAD_MIXER_LEVEL'   'PAD_MIXER_PAN'   'PAD_MIXER_TUNE'   'case PAD_MIXER:'   'cyclePadMixerDialFocus()'   'nativeAudioSetPadLevel(selectedPad'   'nativeAudioSetPadPan(selectedPad'   'nativeAudioSetPadTuning(selectedPad'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE" &&      ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcUiState.java"; then
    echo "ERROR: Pad Mixer Data Dial semantic contract missing: $required"
    exit 1
  fi
done
for required in   'enum ControlFocus'   'onControlFocus'   'setControlFocus'   'LEVEL'   'PAN'   'TUNE'; do
  if ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcPadMixerView.java"; then
    echo "ERROR: Pad Mixer control-focus UI contract missing: $required"
    exit 1
  fi
done

if ! grep -Fq -- 'case PAD_MIXER:' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Pad Mixer Function Bar context is missing"
  exit 1
fi
pad_mixer_function_start=$(grep -n -m1 'case PAD_MIXER:' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
pad_mixer_function_end=$(
  awk -v start="$pad_mixer_function_start"     'NR > start && /case SAMPLE_EDIT:/ { print NR; exit }'     "$MAIN_ACTIVITY_SOURCE"
)
if [[ -z "$pad_mixer_function_end" || "$pad_mixer_function_end" -le "$pad_mixer_function_start" ]]; then
  echo "ERROR: Pad Mixer Function Bar source boundary is missing"
  exit 1
fi
pad_mixer_function_block=$(sed -n "${pad_mixer_function_start},$((pad_mixer_function_end - 1))p" "$MAIN_ACTIVITY_SOURCE")
for required in   'PAD −'   'PAD +'   'TRACK EDIT'   'MAIN'   'BROWSER'   'navigationController.setSelectedPad(selectedPad)'; do
  if ! grep -Fq -- "$required" <<<"$pad_mixer_function_block"; then
    echo "ERROR: Pad Mixer Function Bar contract missing: $required"
    exit 1
  fi
done

echo "Running MPC Pad Mixer ownership/presentation preflight..."
mix_start=$(grep -n -m1 'private void showMixPage()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
mix_midi=$(grep -n -m1 'private void showMidiPage()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$mix_start" || -z "$mix_midi" || "$mix_midi" -le "$mix_start" ]]; then
  echo "ERROR: Pad Mixer source boundary is missing"
  exit 1
fi
mix_block=$(sed -n "${mix_start},$((mix_midi - 1))p" "$MAIN_ACTIVITY_SOURCE")

# MainActivity owns semantic wiring; MpcPadMixerView owns Pad Mixer presentation.
for required in \
  'new MpcPadMixerView(' \
  'MpcPadMixerView.ControlFocus.LEVEL' \
  'MpcUiState.DataDialFocus.PAD_MIXER_LEVEL' \
  'nativeAudioSetPadLevel(' \
  'nativeAudioSetPadPan(' \
  'nativeAudioSetPadTuning(' \
  'nativeAudioGetPadLevel(' \
  'nativeAudioGetPadPan(' \
  'nativeAudioGetPadTuning(' \
  'selectedPad' \
  'PAD %02d'; do
  if ! grep -Fq -- "$required" <<<"$mix_block"; then
    echo "ERROR: MPC Pad Mixer MainActivity semantic contract missing: $required"
    exit 1
  fi
done

pad_mixer_view_source="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcPadMixerView.java"
for required in \
  'setContentDescription("MPC Pad Mixer")' \
  'TextView title = text(context, "PAD MIXER"' \
  'private static final int VISIBLE_STRIPS = 8;' \
  'vertical fader' \
  'interface Listener {' \
  'enum ControlFocus' \
  'onControlFocus' \
  'setControlFocus' \
  'LEVEL' \
  'PAN' \
  'TUNE'; do
  if ! grep -Fq -- "$required" "$pad_mixer_view_source"; then
    echo "ERROR: MPC Pad Mixer presentation contract missing: $required"
    exit 1
  fi
done
if grep -Fq -- 'for (int pad = 0; pad < 4; pad++)' "$pad_mixer_view_source"; then
  echo "ERROR: Pad Mixer must not regress to the obsolete four-strip diagnostic layout"
  exit 1
fi

echo "Running MPC Shortcut Rail fidelity preflight..."
SHORTCUT_MAIN_SOURCE="$MAIN_ACTIVITY_SOURCE"
SHORTCUT_ITEM_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcShortcutRailItemView.java"
for required in \
  'private final MpcUiState.Mode mode' \
  'MpcShortcutIconDrawable' \
  'TextView labelView' \
  'setSelectedState(boolean selected)' \
  'selectionIndicator' \
  'setContentDescription("MPC shortcut " + accessibleLabel)' \
  'setOnClickListener' \
  'setFocusable(true)'; do
  if ! grep -Fq -- "$required" "$SHORTCUT_ITEM_SOURCE"; then
    echo "ERROR: MPC Shortcut Rail item contract missing: $required"
    exit 1
  fi
done
for required in \
  'private final MpcShortcutRailItemView[] shortcutButtons' \
  'BROWSER' \
  'CHANNEL MIXER' \
  'PAD MIXER' \
  'SOUNDS' \
  'XY' \
  'new MpcShortcutRailItemView(' \
  'setSelectedState(' \
  'item.mode() == active'; do
  if ! grep -Fq -- "$required" "$SHORTCUT_MAIN_SOURCE"; then
    echo "ERROR: MPC Shortcut Rail icon+label integration contract missing: $required"
    exit 1
  fi
done
if grep -Fq -- 'private final Button[] shortcutButtons' "$SHORTCUT_MAIN_SOURCE"; then
  echo "ERROR: MPC Shortcut Rail must not use generic Android Button-only presentation"
  exit 1
fi

echo "Running MPC Main section framing fidelity preflight..."
if ! grep -Fq -- "MPC_MAIN_SECTION_DIVIDER_DP = 2" "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: MPC Main section divider geometry constant missing"
  exit 1
fi
main_page_start=$(grep -n -m1 'private void showMainPage()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
main_numeric_start=$(grep -n -m1 'private void installMainNumericEntry' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$main_page_start" || -z "$main_numeric_start" || "$main_numeric_start" -le "$main_page_start" ]]; then
  echo "ERROR: Main page source boundary is missing"
  exit 1
fi
main_page_block=$(sed -n "${main_page_start},$((main_numeric_start - 1))p" "$MAIN_ACTIVITY_SOURCE")
for required in   "sequenceCard.setBackground(strokeBackground("   "trackProgramSection.setBackground(strokeBackground("   "MPC_PANEL_DARK, Color.TRANSPARENT, MPC_MAIN_RADIUS_DP"   "mainSectionDivider.setBackgroundColor(MPC_SELECTION_RED)"   "MPC Main Sequence / Track section divider"; do
  if ! grep -Fq -- "$required" <<<"$main_page_block"; then
    echo "ERROR: MPC Main Sequence/Track framing contract missing: $required"
    exit 1
  fi
done
if grep -Fq -- "MPC_PANEL, MPC_PANEL_BORDER, MPC_MAIN_RADIUS_DP" <<<"$main_page_block"; then
  echo "ERROR: Main Sequence/Track zones must not regress to heavy outer card framing"
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

echo "Running MPC Pull-Down deterministic iconography preflight..."
pull_down_source="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcPullDownPanelView.java"
main_icon_source="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainIconDrawable.java"
for required in \
  'MpcMainIconDrawable.Mode.CLOSE' \
  'MpcMainIconDrawable.Mode.PREVIOUS' \
  'MpcMainIconDrawable.Mode.NEXT' \
  'private Button iconButton(' \
  'Swipe up to close • next page'; do
  if ! grep -Fq -- "$required" "$pull_down_source"; then
    echo "ERROR: MPC Pull-Down deterministic iconography contract missing: $required"
    exit 1
  fi
done
for required in 'CLOSE' 'PREVIOUS' 'NEXT' 'drawClose(' 'drawChevron(' ; do
  if ! grep -Fq -- "$required" "$main_icon_source"; then
    echo "ERROR: MPC Main deterministic icon drawable mode missing: $required"
    exit 1
  fi
done
if grep -Eq '[×‹›]' "$pull_down_source"; then
  echo "ERROR: MPC Pull-Down must not use Unicode close/chevron glyphs in visible chrome"
  exit 1
fi

echo "Running MPC One mixer iconography regression preflight..."
mixer_main_source="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainMixerStripView.java"
for required in   'MpcMixerStripIconDrawable'   'Mode.PERSONAL_CHANNEL_STRIP'   'Mode.TRACK_PAD_SELECTOR'   'visibilityButton = button(context, "", 10);'   'visibilityButton.setForeground(new MpcMixerStripIconDrawable'   'compactMixerStripModeToggle.setText("")'   'compactMixerStripModeToggle.setForeground(new MpcMixerStripIconDrawable'; do
  if ! grep -Fq -- "$required" "$SHELL_SOURCE" &&      ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE" &&      ! grep -Fq -- "$required" "$mixer_main_source"; then
    echo "ERROR: MPC One mixer controls must use deterministic iconography instead of Unicode/Android text glyphs: $required"
    exit 1
  fi
done
if ! grep -Fq -- 'TRACK_PAD_SELECTOR' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMixerStripIconDrawable.java"; then
  echo "ERROR: Track/Pad selector icon drawable is missing"
  exit 1
fi

echo "Running MPC XL channel-strip restore affordance preflight..."
for required in \
  'channelStripRestoreButton.setText("")' \
  'channelStripRestoreButton.setForeground(new MpcMixerStripIconDrawable(' \
  'MpcMixerStripIconDrawable.Mode.PERSONAL_CHANNEL_STRIP' \
  'channelStripRestoreButton.setContentDescription('; do
  if ! grep -Fq -- "$required" "$SHELL_SOURCE"; then
    echo "ERROR: collapsed XL Channel Strip restore affordance contract missing: $required"
    exit 1
  fi
done
if ! grep -Fq -- 'MPC XL Channel Strip show' "$SHELL_SOURCE"; then
  echo "ERROR: collapsed XL Channel Strip restore affordance must describe the actual show action"
  exit 1
fi

if grep -Fq -- '"MPC XL Channel Strip restore"' "$SHELL_SOURCE"; then
  echo "ERROR: hidden XL Channel Strip restore control must expose the actual show action to accessibility"
  exit 1
fi

if grep -Fq -- 'channelStripRestoreButton.setText("›")' "$SHELL_SOURCE"; then
  echo "ERROR: collapsed XL Channel Strip must not use a visible Unicode chevron restore glyph"
  exit 1
fi

echo "Running MPC XL channel-strip collapse/focus regression preflight..."
for required in   'void setChannelStripVisible(boolean visible)'   'contextArea.getLayoutParams()'   'contextParams.width = visible'   ': 0;'   'channelStripRestoreButton'   'channelStripRestoreButton.setVisibility('   'visible ? View.GONE : View.VISIBLE'; do
  if ! grep -Fq -- "$required" "$SHELL_SOURCE"; then
    echo "ERROR: XL Channel Strip must collapse its full 210dp shell column while preserving a restore affordance: $required"
    exit 1
  fi
done
if ! grep -Fq -- 'mpcShell.setChannelStripVisible(visible)' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main shell visibility state must drive XL Channel Strip column geometry"
  exit 1
fi
if ! grep -Fq -- 'setChannelStripRestoreListener' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: hidden XL Channel Strip must retain a shell-level restore path"
  exit 1
fi

for required in   'final boolean levelFocus = "MIX LEVEL".equals(dialFocus)'   'final boolean panFocus = "MIX PAN".equals(dialFocus)'   'final boolean tuneFocus = "MIX TUNE".equals(dialFocus)'   'meter.setDialFocus(levelFocus)'   'pan.setDialFocus(panFocus)'   'MPC Main selected pad tuning'   'tuneValue.setBackground(stroke('   'tuneValue.setContentDescription("MPC Main selected pad tuning")'; do
  if ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainMixerStripView.java"; then
    echo "ERROR: XL Channel Strip Data Dial focus must be visibly projected into Level/Pan/Tune controls: $required"
    exit 1
  fi
done
if ! grep -Fq -- 'void setDialFocus(boolean focused)' "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainMixerStripView.java"; then
  echo "ERROR: XL Channel Strip custom controls must expose an explicit Data Dial focus state"
  exit 1
fi

echo "Running native JNI bridge source preflight..."
for required in \
  "Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetLauncherContext" \
  "Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetStepEditContext" \
  "Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeStepEditParameterNext" \
  "Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeStepEditParameterDelta"; do
  if ! grep -Fq -- "$required" "$NATIVE_ENGINE_SOURCE"; then
    echo "ERROR: Native JNI bridge contract missing: $required"
    exit 1
  fi
done

echo "Running persistent Program context source preflight..."
for required in \
  "compactProgramContext" \
  "MPC shell program context" \
  "nativeSequenceGetTrackProgram(trackIndex)"; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: Persistent compact Program context contract missing: $required"
    exit 1
  fi
done

echo "Running MPC Main visible mixer hierarchy preflight..."
for required in   'compactContextPanel.setVisibility(View.GONE)'   'compactMixerPanel.setVisibility(View.GONE)'   'area.addView(mainMixerStripView,'   'refreshMpcMainMixerStripView()'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: legacy context rail must be hidden behind the visible Main Mixer Strip migration layer: $required"
    exit 1
  fi
done
for required in   'compactContextCaption("SEQUENCE")'   'compactContextCaption("PROGRAM")'   'compactContextCaption("DATA DIAL")'   'private TextView compactContextField('   'private void setCompactContextFocus('   'private String compactDialFocusLabel('   '"DIAL\n" + compactDialFocusLabel(dialFocus)'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: semantic context refresh dependencies are missing: $required"
    exit 1
  fi
done


echo "Running persistent MPC context rail source preflight..."
for required in \
  "compactContextPanel" \
  "compactSequenceContext" \
  "compactDialContext" \
  "compactSequenceOverviewView" \
  "\"MPC shell compact track program context\"" \
  "\"MPC shell track context\"" \
  "focusMainTrackField()" \
  "showSequenceSelectPage();" \
  "showProgramSelectPage();" \
  "\"MPC shell sequence context\"" \
  "\"MPC shell Data Dial focus\"" \
  "\"MPC shell sequence overview\"" \
  "compactSequenceOverviewView.setState("; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: Persistent MPC context rail contract missing: $required"
    exit 1
  fi
done

context_visibility_start=$(grep -n -m1 'private void applyCompactMixerVisibility()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
context_visibility_end=$(grep -n -m1 'private void refreshMpcCompactContext()' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$context_visibility_start" || -z "$context_visibility_end" || "$context_visibility_end" -le "$context_visibility_start" ]]; then
  echo "ERROR: compact Mixer visibility method boundary is missing"
  exit 1
fi
context_visibility_block=$(sed -n "${context_visibility_start},$((context_visibility_end - 1))p" "$MAIN_ACTIVITY_SOURCE")
for forbidden in \
  "compactTrackContext.setVisibility" \
  "compactProgramContext.setVisibility" \
  "compactPadContext.setVisibility" \
  "compactSequenceContext.setVisibility" \
  "compactDialContext.setVisibility" \
  "compactSequenceOverviewView.setVisibility"; do
  if grep -Fq -- "$forbidden" <<<"$context_visibility_block"; then
    echo "ERROR: persistent MPC context must not be hidden with Mixer Strip: $forbidden"
    exit 1
  fi
done
if ! grep -Fq -- 'compactMixerPanel.setVisibility(View.GONE);' <<<"$context_visibility_block"; then
  echo "ERROR: legacy condensed mixer panel must remain hidden; Main uses MpcMainMixerStripView"
  exit 1
fi
if grep -Fq -- 'visible ? View.VISIBLE : View.GONE' <<<"$context_visibility_block"; then
  echo "ERROR: persisted Mixer Strip visibility must not re-expose the legacy duplicate panel"
  exit 1
fi
for required in 'mpcShell.setChannelStripVisible(visible);' 'compactMixerToggle.setText(' ; do
  if ! grep -Fq -- "$required" <<<"$context_visibility_block"; then
    echo "ERROR: Mixer Strip show/hide state wiring regressed: $required"
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
  "Track Edit layer pan center" \
  "LAYER %d/8" \
  "layer tuning" \
  'Track Edit " + semanticName + " value' \
  'Track Edit " + semanticName + " increase' \
  'Track Edit " + semanticName + " decrease'; do
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

for required in \
  "nativeAudioGetPadLayerTuning(selectedPad, selectedLayer)" \
  "nativeAudioSetPadLayerTuning(" \
  "refreshTrackEditView()"; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: Track Edit sampler parameter routing missing: $required"
    exit 1
  fi
done

echo "Running startup visual evidence ordering preflight..."
startup_ready_line=$(grep -n -m1 'startupComplete = true;' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
startup_shell_refresh_line=$(grep -n 'updateMpcShellState();' "$MAIN_ACTIVITY_SOURCE" | awk -F: -v start="$startup_ready_line" '$1 > start { print $1; exit }')
startup_complete_log_line=$(grep -n -m1 'Log.i(TAG, "STARTUP_COMPLETE");' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$startup_ready_line" || -z "$startup_shell_refresh_line" || -z "$startup_complete_log_line" \
    || "$startup_shell_refresh_line" -ge "$startup_complete_log_line" ]]; then
  echo "ERROR: persistent MPC shell must refresh after startup readiness and before STARTUP_COMPLETE"
  exit 1
fi
startup_capture_line=$(grep -n -m1 'echo "Capturing startup UI screenshot..."' "$0" | cut -d: -f1)
startup_settle_line=$(grep -n -m1 'echo "Allowing startup path to settle..."' "$0" | cut -d: -f1)
if [[ -z "$startup_capture_line" || -z "$startup_settle_line" || "$startup_capture_line" -ge "$startup_settle_line" ]]; then
  echo "ERROR: startup Main screenshot must be captured before post-launch settling can let the UI audit navigate away from Main"
  exit 1
fi
echo "Running Pull-Down semantic routing preflight..."
for required in   'void onTimeCounter();'   'void onTempo();'   'listener.onTimeCounter();'   'listener.onTempo();'   'showPullDownLocateDialog();'   'showPullDownTempoDialog();'   'nativeSequenceLocateMoveTicks(delta)'   'nativeSequenceSetTempo(bpm)'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"       && ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcPullDownPanelView.java"; then
    echo "ERROR: Pull-Down semantic routing contract missing: $required"
    exit 1
  fi
done
require_source_contains() {
  local source="$1"
  local required="$2"
  echo "Checking source contract: $required"
  if ! timeout --signal=TERM --kill-after=1s 5s grep -Fq -- "$required" "$source"; then
    echo "ERROR: source contract missing or grep timed out: $required ($source)"
    exit 1
  fi
}

echo "Running MPC Browser product-scope preflight..."
if grep -Fqi -- 'splice' "$BROWSER_SOURCE"; then
  echo "ERROR: excluded commercial Splice destination is present in the product Browser"
  exit 1
fi
for local_category in '"DRUMS"' '"INSTRUMENTS"' '"SAMPLES"' '"DEMOS"' '"MY FILES"'; do
  if ! grep -Fq -- "$local_category" "$BROWSER_SOURCE"; then
    echo "ERROR: expected generic/local Browser category missing: $local_category"
    exit 1
  fi
done
echo "Running MPC Browser visual/runtime contract preflight..."
require_source_contains "$BROWSER_SOURCE" 'MpcBrowserFilterIconDrawable'
require_source_contains "$BROWSER_SOURCE" 'button.setContentDescription("MPC Browser filter " + filter)'
require_source_contains "$BROWSER_SOURCE" 'MPC Browser current location: PLACE • INTERNAL'
require_source_contains "$BROWSER_SOURCE" 'row.setOnClickListener(v -> {'
require_source_contains "$BROWSER_SOURCE" 'listener.onOpenStorage();'
require_source_contains "$BROWSER_SOURCE" 'MPC Browser open Android storage picker'
require_source_contains "$BROWSER_SOURCE" 'ANDROID DOCUMENT PICKER • DIRECT WAV LOAD'
require_source_contains "$BROWSER_SOURCE" 'listener.onChooseFolder();'
require_source_contains "$BROWSER_SOURCE" 'MPC Browser choose a local folder for " + activeLocationItem'
require_source_contains "$BROWSER_SOURCE" 'PERSIST A LOCAL FOLDER FOR BROWSING'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'public void onOpenStorage()'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'public void onChooseFolder()'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'openBrowserDocumentTreePicker();'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'Button load = addFunction("LOAD", true, v -> openWavPicker());'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'MPC Browser LOAD source file with Android Document Picker'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'restoreUserDocumentTree(browserView)'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'PREF_USER_TREE_URI'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'Button audition = addFunction("AUDITION", browserHasSelectedFile,'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'Browser search state propagation'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'Browser context did not survive view recreation'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'Browser Data Dial delta did not expose provider limitation'
require_source_contains "$BROWSER_SOURCE" 'MPC Browser provider status'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'Browser restore did not return to Main'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'Browser CLEAR did not clear persisted query'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'MPC Browser audition unavailable: no source item selected'
require_source_contains "$BROWSER_SOURCE" 'void auditionSelectedFile()'
require_source_contains "$BROWSER_SOURCE" 'button.setContentDescription("Browser " + section + " tab")'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'up.setContentDescription("Browser UP from " + browserLocation)'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'MPC Browser LOAD source file with Android Document Picker'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'MPC Browser BACK to previous workspace'

echo "Running clean visual-evidence audit preflight..."
for required in   'hardwareFeedbackView.setVisibility(View.GONE);'   'bottomStatus.setVisibility(View.GONE);'   'truthful Main screenshot'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: clean visual evidence contract missing: $required"
    exit 1
  fi
done
echo "Running MPC Main XL strip value preflight..."
MAIN_MIXER_STRIP_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainMixerStripView.java"
require_source_contains "$MAIN_MIXER_STRIP_SOURCE" 'right.setSingleLine(true)'
require_source_contains "$MAIN_MIXER_STRIP_SOURCE" 'right.setEllipsize(TextUtils.TruncateAt.END)'
require_source_contains "$MAIN_MIXER_STRIP_SOURCE" 'compactProgramDisplayName(fullProgramName)'
require_source_contains "$MAIN_MIXER_STRIP_SOURCE" 'addProgramBand(strip, context, "MAIN", "OUTPUT", "MAIN OUTPUT")'
require_source_contains "$MAIN_MIXER_STRIP_SOURCE" 'MPC Main selected track program: '
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'Main XL program value wrapped or loses full program name'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'Main Output label repeats OUTPUT or lost value'

echo "Running MPC Browser text overflow preflight..."
require_source_contains "$BROWSER_SOURCE" 'destination.setSingleLine(true)'
require_source_contains "$BROWSER_SOURCE" 'results.addView(row, paramsMatch(getContext(), 40))'
require_source_contains "$BROWSER_SOURCE" 'new LayoutParams(0, dp(context, 34), 1.0f)'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'Browser scan/storage actions are clipped or not actionable'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'browserStorageAction.getHeight() <= dp(36)'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'Browser storage picker action not hittable'
require_source_contains "$BROWSER_SOURCE" 'currentSample.setSingleLine(true)'
require_source_contains "$BROWSER_SOURCE" 'main.setSingleLine(true)'
require_source_contains "$BROWSER_SOURCE" 'sub.setSingleLine(true)'
require_source_contains "$BROWSER_SOURCE" 'TextUtils.TruncateAt.END'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'Browser text wrapped after layout'

echo "Running MPC Browser geometry regression preflight..."
require_source_contains "$BROWSER_SOURCE" 'filterBar.addView(filters, new LayoutParams(0, -1, 1))'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'functionBar.setWeightSum(visibleChildren)'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'Browser Function Bar unequal action widths'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'Browser Options button has no hittable bounds'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'getInsetsIgnoringVisibility('
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'view.setPadding(left, 0, right, bottom)'
require_source_contains "$0" 'Browser Options has zero/empty Android accessibility bounds'
require_source_contains "$0" 'Function Bar action hit widths are unequal'

echo "Running Arrangement Pattern placement Function Bar preflight..."
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'nativeSequenceAddArrangementPatternPlacement'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'nativeSequenceMoveArrangementPatternPlacement'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'nativeSequenceDeleteArrangementPatternPlacement'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'nativeSequenceDuplicateArrangementPatternPlacement'
require_source_contains "$0" 'Arrangement CLIP DUP → UNDO → REDO → UNDO restored exact source placement state'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'Arrangement place Pattern at empty target tick'
require_source_contains "$MAIN_ACTIVITY_SOURCE" 'Arrangement move Pattern placement at tick'
require_source_contains "$0" 'Pattern placement real-touch PLACE → MOVE ± GRID'
require_source_contains "$0" 'UI_ARRANGE_PLACEMENT_STATE: action=(TARGET|SPLIT|DELETE|PLACE|MOVE|DUPLICATE|UNDO|REDO)'
echo "Running MPC Browser Options preflight..."
for required in   'MpcBrowserChromeIconDrawable'   'MPC Browser Options'   'MPC Browser Option Show file size'   'MPC Browser Options close'   'void onOptionsRequested();'   'showBrowserOptionsDialog'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"       && ! grep -Fq -- "$required" "$BROWSER_SOURCE"; then
    echo "ERROR: MPC Browser Options contract missing: $required"
    exit 1
  fi
done
echo "Running MPC Preferences taxonomy preflight..."
for required in   'buildPreferencesCategoryBar("AUDIO")'   'MPC Preferences AUDIO'   'MPC Preferences MIDI / SYNC'   'MPC Preferences SEQUENCER'   'showPreferencesReservedPage'   'RESERVED / UNAVAILABLE'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: MPC Preferences taxonomy contract missing: $required"
    exit 1
  fi
done
echo "Running Android runtime startup smoke..."
echo "Running canonical Main shell visibility preflight..."
for required in   'compactContextPanel.setVisibility(View.GONE);'   'compactMixerPanel.setVisibility(View.GONE);'   'header.setVisibility(View.GONE);'   'mainMixerStripView = new MpcMainMixerStripView'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: canonical Main shell visibility contract missing: $required"
    exit 1
  fi
done
if grep -Fq -- '"MPC shell compact track program context"' "$MAIN_ACTIVITY_SOURCE"     && grep -Fq -- 'compactContextPanel.setVisibility(View.VISIBLE)' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: legacy Main context dashboard must stay hidden"
  exit 1
fi
echo "Running Android runtime startup smoke..."
echo "Running MPC Main UI audit contract preflight..."
for forbidden in   'shortcut.setContentDescription("MPC shortcut " + mode.label())'; do
  if grep -Fq -- "$forbidden" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: stale factory shortcut accessibility override detected: $forbidden"
    exit 1
  fi
done
for required in   'String[] mainShellExpectedDescriptions'   'MPC One Main Toolbar'   'MPC shortcut CHANNEL MIXER'   'MPC shortcut SOUNDS'   'MPC shortcut XY'   'MPC Main XL Mixer Strips'   'Main Track visual hierarchy • Track / Program / workspace section'   'clickMpcToolbarMenuForAudit()'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: current MPC Main UI audit contract missing: $required"
    exit 1
  fi
done
for required in   'UI_AUDIT_TIMEOUT_SECONDS=360'   'audit_deadline=$((SECONDS + UI_AUDIT_TIMEOUT_SECONDS))'   'adb_bounded()'   'timeout --signal=TERM --kill-after=3s'   'UI_MAIN_CAPTURE_READY'   'mpc-main-capture-release'   'mpc-groovebox-main-ready-uiautomator.xml'   'UI_BROWSER_CAPTURE_READY'   'mpc-browser-capture-release'   'mpc-groovebox-browser-uiautomator.xml'   'UI_STORAGE_PICKER_LAUNCH_REQUESTED'   'UI_STORAGE_PICKER_RETURNED'   'mpc-groovebox-storage-picker.png'   'UI_TRACK_VIEW_MUTED_CAPTURE_READY:'   'mpc-track-view-muted-capture-release'   'mpc-groovebox-track-view-muted.png'   'mpc-groovebox-track-view-muted-uiautomator.xml'   'UI_INTERACTION_COMPLETE'   'UI_HIERARCHY_FAILED:'   'UI_INTERACTION_FAILED:'   'uiautomator dump' 'tap_ui_picker_result_row()'; do
  if ! grep -Fq -- "$required" "$0"; then
    echo "ERROR: runtime smoke synchronization contract missing: $required"
    exit 1
  fi
done
adb_bounded() {
  local seconds="$1"
  shift
  timeout --signal=TERM --kill-after=3s "${seconds}s" adb "$@"
}

# Keep UI dumping available before startup checks run; Bash executes top-level
# statements in order, so functions declared later cannot be called earlier.

# The Arrangement/Grid capture handshake calls this before generic UI helpers
# are declared later in the script. Use the real hierarchy, and require one
# enabled clickable target with positive bounds before issuing a device tap.
tap_visible_description_early() {
  local hierarchy_path="$1"
  local expected_description="$2"
  local coordinates tap_x tap_y
  if ! coordinates="$(python3 - "$hierarchy_path" "$expected_description" <<'PY'
import sys
import xml.etree.ElementTree as ET

path, expected = sys.argv[1], sys.argv[2]
nodes = list(ET.parse(path).getroot().iter("node"))
found = [n for n in nodes if n.attrib.get("content-desc", "") == expected]
if len(found) != 1:
    raise SystemExit(f"expected exactly one accessibility target {expected!r}, got {len(found)}")
node = found[0]
if node.attrib.get("clickable") != "true" or node.attrib.get("enabled") != "true":
    raise SystemExit(f"accessibility target is not enabled/clickable: {node.attrib}")
parts = node.attrib.get("bounds", "").replace("][", ",").strip("[]").split(",")
if len(parts) != 4 or not all(p.isdigit() for p in parts):
    raise SystemExit(f"invalid accessibility target bounds: {node.attrib}")
left, top, right, bottom = map(int, parts)
if right <= left or bottom <= top:
    raise SystemExit(f"non-positive accessibility target bounds: {node.attrib}")
print((left + right) // 2, (top + bottom) // 2)
PY
)"; then
    echo "ERROR: could not derive a valid early UI target: $expected_description"
    return 1
  fi
  read -r tap_x tap_y <<< "$coordinates"
  if ! adb_bounded 10 shell input tap "$tap_x" "$tap_y"; then
    echo "ERROR: could not tap $expected_description at $tap_x,$tap_y"
    return 1
  fi
}


tap_visible_text_early() {
  local hierarchy_path="$1"
  local expected_text="$2"
  local coordinates tap_x tap_y
  if ! coordinates="$(python3 - "$hierarchy_path" "$expected_text" <<'PY'
import sys
import xml.etree.ElementTree as ET
path, expected = sys.argv[1], sys.argv[2]
nodes = list(ET.parse(path).getroot().iter("node"))
found = [n for n in nodes
         if n.attrib.get("text", "") == expected
         and n.attrib.get("class", "").endswith(".Button")]
if len(found) != 1:
    raise SystemExit(f"expected one button with text {expected!r}, got {len(found)}")
node = found[0]
if node.attrib.get("clickable") != "true" or node.attrib.get("enabled") != "true":
    raise SystemExit(f"button is not enabled/clickable: {node.attrib}")
parts = node.attrib.get("bounds", "").replace("][", ",").strip("[]").split(",")
if len(parts) != 4 or not all(p.isdigit() for p in parts):
    raise SystemExit(f"invalid button bounds: {node.attrib}")
left, top, right, bottom = map(int, parts)
if right <= left or bottom <= top:
    raise SystemExit(f"non-positive button bounds: {node.attrib}")
print((left + right) // 2, (top + bottom) // 2)
PY
)"; then
    echo "ERROR: could not derive enabled Step Function Bar button: $expected_text"
    return 1
  fi
  read -r tap_x tap_y <<< "$coordinates"
  if ! adb_bounded 10 shell input tap "$tap_x" "$tap_y"; then
    echo "ERROR: could not tap Step Function Bar button $expected_text at $tap_x,$tap_y"
    return 1
  fi
}

dump_device_ui() {
  local remote_path="$1"
  local host_path="$2"
  local log_path="/tmp/mpc-groovebox-uiautomator-dump.log"
  if ! adb_bounded 20 shell uiautomator dump "$remote_path" > "$log_path" 2>&1; then
    cat "$log_path" || true
    echo "ERROR: could not dump emulator UI hierarchy: $remote_path"
    return 1
  fi
  if ! adb_bounded 10 exec-out cat "$remote_path" > "$host_path"; then
    echo "ERROR: could not retrieve emulator UI hierarchy: $remote_path"
    return 1
  fi
  if ! test -s "$host_path"; then
    echo "ERROR: emulator UI hierarchy is empty: $host_path"
    return 1
  fi
}

echo "Installing debug APK (hard limit 120s)..."
if ! adb_bounded 120 install -r "$APK"; then
  echo "ERROR: adb install failed or timed out after 120s"
  exit 1
fi

# Prepare the real SAF tree fixture before Activity startup, then seed only the
# app's saved *location hint* (not a grant). The normal ACTION_OPEN_DOCUMENT_TREE
# flow must still grant access through Android's "Use this folder" action.
TREE_FOLDER_NAME="AAA-MPC-Browser-Tree"
TREE_SAMPLE_NAME="Browser-Tree-Only.wav"
TREE_FOLDER_DEVICE="/sdcard/$TREE_FOLDER_NAME"
TREE_SAMPLE_DEVICE="$TREE_FOLDER_DEVICE/$TREE_SAMPLE_NAME"
TREE_MEDIA_QUERY="/tmp/mpc-browser-tree-not-in-mediastore.txt"
FAVORITE_FOLDER_NAME="BBB-MPC-Browser-Favorite"
FAVORITE_SAMPLE_NAME="Browser-Favorite-Only.wav"
FAVORITE_FOLDER_DEVICE="/sdcard/$FAVORITE_FOLDER_NAME"
FAVORITE_SAMPLE_DEVICE="$FAVORITE_FOLDER_DEVICE/$FAVORITE_SAMPLE_NAME"
FAVORITE_MEDIA_QUERY="/tmp/mpc-browser-favorite-not-in-mediastore.txt"
TREE_PICKER_XML="/tmp/mpc-groovebox-browser-tree-picker-uiautomator.xml"
TREE_PICKER_REMOTE="/sdcard/mpc-groovebox-browser-tree-picker-uiautomator.xml"
TREE_BROWSER_XML="/tmp/mpc-groovebox-browser-tree-results-uiautomator.xml"
TREE_BROWSER_REMOTE="/sdcard/mpc-groovebox-browser-tree-results-uiautomator.xml"
TREE_RESTORED_XML="/tmp/mpc-groovebox-browser-tree-restored-uiautomator.xml"
TREE_RESTORED_REMOTE="/sdcard/mpc-groovebox-browser-tree-restored-uiautomator.xml"
FAVORITE_TREE_PICKER_XML="/tmp/mpc-groovebox-browser-favorite-tree-picker-uiautomator.xml"
FAVORITE_TREE_PICKER_REMOTE="/sdcard/mpc-groovebox-browser-favorite-tree-picker-uiautomator.xml"
FAVORITE_TREE_RESULTS_XML="/tmp/mpc-groovebox-browser-favorite-tree-results-uiautomator.xml"
FAVORITE_TREE_RESULTS_REMOTE="/sdcard/mpc-groovebox-browser-favorite-tree-results-uiautomator.xml"
FAVORITE_TREE_RESTORED_XML="/tmp/mpc-groovebox-browser-favorite-tree-restored-uiautomator.xml"
FAVORITE_TREE_RESTORED_REMOTE="/sdcard/mpc-groovebox-browser-favorite-tree-restored-uiautomator.xml"
FAVORITE_PREFS_HOST="/tmp/mpc-browser-favorite-prefs.xml"
TREE_PREFS_HOST="/tmp/mpc-browser-storage-seed.xml"

if ! adb_bounded 10 shell mkdir -p "$TREE_FOLDER_DEVICE"; then
  echo "ERROR: could not create user-folder test fixture"; exit 1
fi
if ! adb_bounded 10 shell touch "$TREE_FOLDER_DEVICE/.nomedia"; then
  echo "ERROR: could not protect the tree fixture from MediaStore indexing"; exit 1
fi
cat > "$TREE_PREFS_HOST" <<EOF
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
  <string name="user_tree_uri">content://com.android.externalstorage.documents/tree/primary%3A${TREE_FOLDER_NAME}</string>
  <string name="user_tree_name">${TREE_FOLDER_NAME}</string>
</map>
EOF
TREE_PREFS_B64="$(base64 < "$TREE_PREFS_HOST" | tr -d '\r\n')"
if [[ -z "$TREE_PREFS_B64" ]]; then
  echo "ERROR: could not encode the ungranted initial Browser tree location"; exit 1
fi
# adb shell joins its arguments into a remote command line. Pass the remote
# shell script as one quoted argument; otherwise sh -c receives only "mkdir"
# and treats "-p" as its positional argument, producing "mkdir: Needs 1 argument".
if ! adb_bounded 10 shell "run-as $PACKAGE sh -c 'mkdir -p shared_prefs && echo $TREE_PREFS_B64 | base64 -d > shared_prefs/mpc_browser_storage.xml'"; then
  echo "ERROR: could not seed the ungranted initial Browser tree location"; exit 1
fi

echo "Clearing logcat and launching MainActivity..."
if ! adb_bounded 10 logcat -c; then
  echo "ERROR: adb logcat clear failed or timed out"
  exit 1
fi
if ! adb_bounded 10 shell am force-stop "$PACKAGE"; then
  echo "ERROR: adb force-stop failed or timed out"
  exit 1
fi
if ! adb_bounded 60 shell am start -W -n "$ACTIVITY" --es "$SMOKE_MODE_EXTRA" "ui-audit" \
    2>&1 | tee /tmp/mpc-groovebox-am-start.txt; then
  echo "ERROR: MainActivity launch failed or timed out after 60s"
  exit 1
fi

echo "Allowing startup path to settle..."
sleep 5

# Android 12+ shows a one-time system-owned immersive-mode confirmation over
# fullscreen Activities. Dismiss it explicitly before startup/Main captures so
# the product UI assertions and evidence inspect the app, not android:id/ok.
IMMERSIVE_CLING_XML="/tmp/mpc-groovebox-immersive-cling-uiautomator.xml"
IMMERSIVE_CLING_REMOTE="/sdcard/mpc-groovebox-immersive-cling-uiautomator.xml"
if ! dump_device_ui "$IMMERSIVE_CLING_REMOTE" "$IMMERSIVE_CLING_XML"; then
  echo "ERROR: could not inspect Android immersive-mode confirmation state"
  exit 1
fi
if grep -Fq -- 'resource-id="android:id/immersive_cling_title"' "$IMMERSIVE_CLING_XML"; then
  echo "Android immersive confirmation detected; acknowledging system-owned GOT IT action..."
  immersive_ok_coordinates="$(python3 - "$IMMERSIVE_CLING_XML" <<'PY'
import sys
import xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
for node in root.iter("node"):
    if (node.attrib.get("resource-id") == "android:id/ok"
            and node.attrib.get("text", "").strip() == "GOT IT"
            and node.attrib.get("clickable") == "true"
            and node.attrib.get("enabled") == "true"):
        raw = node.attrib.get("bounds", "")
        try:
            first, second = raw[1:-1].split("][")
            left, top = map(int, first.split(","))
            right, bottom = map(int, second.split(","))
        except ValueError:
            continue
        if right > left and bottom > top:
            print((left + right) // 2, (top + bottom) // 2)
            raise SystemExit(0)
raise SystemExit(2)
PY
  )" || {
    echo "ERROR: immersive confirmation was present but its enabled GOT IT target was not found"
    exit 1
  }
  read -r immersive_ok_x immersive_ok_y <<< "$immersive_ok_coordinates"
  if ! adb_bounded 10 shell cmd input touchscreen tap "$immersive_ok_x" "$immersive_ok_y"; then
    echo "ERROR: could not acknowledge Android immersive-mode confirmation"
    exit 1
  fi
  immersive_cling_dismissed=0
  for immersive_attempt in {1..6}; do
    sleep 1
    if ! dump_device_ui "$IMMERSIVE_CLING_REMOTE" "$IMMERSIVE_CLING_XML"; then
      echo "ERROR: could not verify immersive-mode confirmation dismissal"
      exit 1
    fi
    if ! grep -Fq -- 'resource-id="android:id/immersive_cling_title"' "$IMMERSIVE_CLING_XML"; then
      immersive_cling_dismissed=1
      break
    fi
  done
  if [[ "$immersive_cling_dismissed" != "1" ]]; then
    echo "ERROR: Android immersive-mode confirmation remained visible after GOT IT"
    exit 1
  fi
  echo "Android immersive-mode confirmation dismissed and verified."
fi

echo "Capturing startup UI screenshot after the system-overlay check..."
if ! adb_bounded 15 exec-out screencap -p > /tmp/mpc-groovebox-startup.png; then
  echo "ERROR: startup screenshot capture failed or timed out after 15s"
  exit 1
fi

echo "Waiting for application-side UI audit to complete..."
UI_AUDIT_TIMEOUT_SECONDS=360
ui_audit_complete=0
main_runtime_capture_done=0
browser_runtime_capture_done=0
storage_picker_capture_done=0
arrangement_grid_focus_capture_done=0
arrangement_back_capture_done=0
arrangement_grid_reentry_capture_done=0
track_view_mute_capture_done=0
step_touch_capture_done=0
step_touch_acceptance_complete=0
arrangement_loop_touch_capture_done=0
arrangement_loop_touch_acceptance_complete=0
audit_deadline=$((SECONDS + UI_AUDIT_TIMEOUT_SECONDS))
audit_poll=0
while (( SECONDS < audit_deadline )); do
  log_snapshot="$(adb_bounded 5 logcat -d -v brief 2>/dev/null || true)"

  if grep -Fq -- "UI_ARRANGE_LOOP_TOUCH_ACCEPTANCE_COMPLETE:" <<<"$log_snapshot"; then
    arrangement_loop_touch_acceptance_complete=1
  fi

  if [ "$arrangement_loop_touch_capture_done" -eq 0 ] \
      && grep -Fq -- "UI_ARRANGE_LOOP_TOUCH_CAPTURE_READY:" <<<"$log_snapshot"; then
    echo "Capturing live Arrangement loop-brace state before external touch..."
    ARRANGE_LOOP_BEFORE_PNG="/tmp/mpc-groovebox-arrange-loop-before.png"
    ARRANGE_LOOP_BEFORE_XML="/tmp/mpc-groovebox-arrange-loop-before-uiautomator.xml"
    ARRANGE_LOOP_BEFORE_REMOTE="/sdcard/mpc-groovebox-arrange-loop-before-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_LOOP_BEFORE_REMOTE" "$ARRANGE_LOOP_BEFORE_XML"; then
      echo "ERROR: Arrange loop initial accessibility hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_LOOP_BEFORE_PNG"; then
      echo "ERROR: Arrange loop initial screenshot failed"; exit 1
    fi
    loop_ready_line="$(grep -F -- 'UI_ARRANGE_LOOP_TOUCH_CAPTURE_READY:' <<<"$log_snapshot" | tail -n 1)"
    read -r loop_original_start loop_original_end loop_target_end loop_bars loop_visible_bars loop_viewport_start \
      < <(sed -nE 's/.*start=([0-9]+) end=([0-9]+) targetEnd=([0-9]+) bars=([0-9]+) visibleBars=([0-9]+) viewportStart=([0-9]+).*/\1 \2 \3 \4 \5 \6/p' <<<"$loop_ready_line")
    if [[ -z "$loop_original_start" || -z "$loop_original_end" || -z "$loop_target_end" \
        || -z "$loop_bars" || -z "$loop_visible_bars" || -z "$loop_viewport_start" ]]; then
      echo "ERROR: could not parse live Arrangement loop baseline: $loop_ready_line"; exit 1
    fi
    if (( loop_target_end == loop_original_end || loop_target_end < loop_original_start \
        || loop_target_end > loop_bars )); then
      echo "ERROR: app reported an invalid Arrangement loop drag target: $loop_ready_line"; exit 1
    fi
    arrange_density="$(adb_bounded 10 shell wm density)" || {
      echo "ERROR: could not read Android density for Arrange loop hit testing"; exit 1;
    }
    read -r arrange_event_track arrange_event_tick arrange_event_note arrange_event_grid arrange_ticks_per_bar \
      < <(sed -nE 's/.*eventTrack=([0-9]+) eventTick=([0-9]+) eventNote=([0-9]+) gridTicks=([0-9]+) ticksPerBar=([0-9]+).*/\1 \2 \3 \4 \5/p' <<<"$loop_ready_line")
    if [[ -z "$arrange_event_track" || -z "$arrange_event_tick" || -z "$arrange_event_note" \
        || -z "$arrange_event_grid" || -z "$arrange_ticks_per_bar" ]]; then
      echo "ERROR: cannot parse seeded Arrangement event: $loop_ready_line"; exit 1
    fi
    ARRANGE_EVENT_SELECTED_PNG="/tmp/mpc-groovebox-arrange-event-selected.png"
    ARRANGE_EVENT_SELECTED_XML="/tmp/mpc-groovebox-arrange-event-selected-uiautomator.xml"
    ARRANGE_EVENT_SELECTED_REMOTE="/sdcard/mpc-groovebox-arrange-event-selected-uiautomator.xml"
    arrange_event_points="$(python3 - \
      "$ARRANGE_LOOP_BEFORE_XML" "$arrange_density" "$arrange_event_track" \
      "$arrange_event_tick" "$arrange_event_grid" "$arrange_ticks_per_bar" \
      "$loop_visible_bars" "$loop_viewport_start" <<'PY'
import re, sys
import xml.etree.ElementTree as ET
path, density_text, track_s, tick_s, grid_s, tpb_s, visible_s, viewport_s = sys.argv[1:]
track,tick,grid,tpb,visible,viewport=map(int,(track_s,tick_s,grid_s,tpb_s,visible_s,viewport_s))
nodes=list(ET.parse(path).getroot().iter("node"))
views=[n for n in nodes if n.attrib.get("content-desc")=="MPC linear arrangement editor"]
if len(views)!=1: raise AssertionError(f"expected one Arrangement canvas, got {len(views)}")
m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",views[0].attrib.get("bounds",""))
if m is None: raise AssertionError("invalid Arrangement canvas bounds")
left,top,right,bottom=map(int,m.groups())
ds=re.findall(r"Override density:\s*(\d+)",density_text)
if not ds: ds=re.findall(r"Physical density:\s*(\d+)",density_text)
if not ds: raise AssertionError("could not parse Android density")
density=int(ds[-1])/160.0
width,height=right-left,bottom-top
header,ruler,lane=138*density,34*density,46*density
if track<0 or visible<1 or tpb<1 or grid<1: raise AssertionError("invalid Arrangement hit-test input")
bar_width=(width-header)/visible
sbar=tick/tpb-(viewport-1); ebar=(tick+grid)/tpb-(viewport-1)
x1=max(header+1*density,header+sbar*bar_width)
x2=min(width-1*density,max(header+sbar*bar_width+2*density,header+ebar*bar_width))
x=round(left+(x1+x2)/2); y=round(top+ruler+track*lane+21.5*density)
if not(left<x<right and top<y<bottom and ruler+track*lane+21.5*density<height):
 raise AssertionError(f"event tap {(x,y)} outside Arrangement canvas")
print(x,y)
PY
    )" || { echo "ERROR: could not derive event touch from live hierarchy"; exit 1; }
    read -r arrange_event_x arrange_event_y <<< "$arrange_event_points"
    if ! adb_bounded 10 shell input tap "$arrange_event_x" "$arrange_event_y"; then
      echo "ERROR: could not select seeded Arrangement event"; exit 1
    fi
    sleep 0.3
    if ! dump_device_ui "$ARRANGE_EVENT_SELECTED_REMOTE" "$ARRANGE_EVENT_SELECTED_XML"; then
      echo "ERROR: Arrangement selected-event hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_EVENT_SELECTED_PNG"; then
      echo "ERROR: Arrangement selected-event screenshot failed"; exit 1
    fi
    if ! python3 - "$ARRANGE_EVENT_SELECTED_XML" "$arrange_event_track" "$arrange_event_tick" "$arrange_event_note" <<'PY'
import sys, xml.etree.ElementTree as ET
path,track_s,tick_s,note_s=sys.argv[1:]; track,tick,note=map(int,(track_s,tick_s,note_s))
nodes=list(ET.parse(path).getroot().iter("node"))
info=[n for n in nodes if n.attrib.get("content-desc")=="Arrangement editing guidance"]
expected=f"EVENT • TRACK {track+1:02d} • TICK {tick} • NOTE {note:03d}"
if len(info)!=1 or info[0].attrib.get("text")!=expected: raise AssertionError(f"selected event mismatch: {[n.attrib.get('text') for n in info]!r}, expected={expected!r}")
for desc in ("Arrangement move selected event one grid earlier","Arrangement move selected event one grid later"):
 found=[n for n in nodes if n.attrib.get("content-desc")==desc]
 if len(found)!=1 or found[0].attrib.get("enabled")!="true" or found[0].attrib.get("clickable")!="true": raise AssertionError(f"move button unavailable: {desc}")
print(f"Arrangement event selected: track={track+1}, tick={tick}, note={note}.")
PY
    then
      echo "ERROR: Arrangement selection did not expose native event identity"; cat "$ARRANGE_EVENT_SELECTED_XML"; exit 1
    fi
    if ! tap_visible_description_early "$ARRANGE_EVENT_SELECTED_XML" "Arrangement move selected event one grid later"; then
      echo "ERROR: EVENT + control not actionable"; exit 1
    fi
    sleep 0.35
    ARRANGE_EVENT_MOVED_PNG="/tmp/mpc-groovebox-arrange-event-moved.png"
    ARRANGE_EVENT_MOVED_XML="/tmp/mpc-groovebox-arrange-event-moved-uiautomator.xml"
    ARRANGE_EVENT_MOVED_REMOTE="/sdcard/mpc-groovebox-arrange-event-moved-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_EVENT_MOVED_REMOTE" "$ARRANGE_EVENT_MOVED_XML"; then
      echo "ERROR: Arrangement moved-state hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_EVENT_MOVED_PNG"; then
      echo "ERROR: Arrangement moved-state screenshot failed"; exit 1
    fi
    if ! python3 - "$ARRANGE_EVENT_MOVED_XML" "$arrange_event_track" "$((arrange_event_tick + arrange_event_grid))" "$arrange_event_note" <<'PY'
import sys, xml.etree.ElementTree as ET
path,track_s,tick_s,note_s=sys.argv[1:]; track,tick,note=map(int,(track_s,tick_s,note_s))
nodes=list(ET.parse(path).getroot().iter("node"))
info=[n for n in nodes if n.attrib.get("content-desc")=="Arrangement editing guidance"]
expected=f"EVENT • TRACK {track+1:02d} • TICK {tick} • NOTE {note:03d}"
if len(info)!=1 or info[0].attrib.get("text")!=expected: raise AssertionError(f"native event did not move: {[n.attrib.get('text') for n in info]!r}, expected={expected!r}")
print(f"Arrangement MOVE + GRID verified at tick {tick}, note {note}.")
PY
    then
      echo "ERROR: MOVE + GRID did not change native event"; cat "$ARRANGE_EVENT_MOVED_XML"; exit 1
    fi
    if ! tap_visible_description_early "$ARRANGE_EVENT_MOVED_XML" "Arrangement move selected event one grid earlier"; then
      echo "ERROR: EVENT − control not actionable"; exit 1
    fi
    sleep 0.35
    ARRANGE_EVENT_RESTORED_PNG="/tmp/mpc-groovebox-arrange-event-restored.png"
    ARRANGE_EVENT_RESTORED_XML="/tmp/mpc-groovebox-arrange-event-restored-uiautomator.xml"
    ARRANGE_EVENT_RESTORED_REMOTE="/sdcard/mpc-groovebox-arrange-event-restored-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_EVENT_RESTORED_REMOTE" "$ARRANGE_EVENT_RESTORED_XML"; then
      echo "ERROR: Arrangement restored-state hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_EVENT_RESTORED_PNG"; then
      echo "ERROR: Arrangement restored-state screenshot failed"; exit 1
    fi
    if ! python3 - "$ARRANGE_EVENT_RESTORED_XML" "$arrange_event_track" "$arrange_event_tick" "$arrange_event_note" <<'PY'
import sys, xml.etree.ElementTree as ET
path,track_s,tick_s,note_s=sys.argv[1:]; track,tick,note=map(int,(track_s,tick_s,note_s))
nodes=list(ET.parse(path).getroot().iter("node"))
info=[n for n in nodes if n.attrib.get("content-desc")=="Arrangement editing guidance"]
expected=f"EVENT • TRACK {track+1:02d} • TICK {tick} • NOTE {note:03d}"
if len(info)!=1 or info[0].attrib.get("text")!=expected: raise AssertionError(f"native event not restored: {[n.attrib.get('text') for n in info]!r}, expected={expected!r}")
print(f"Arrangement MOVE + GRID → MOVE − GRID restored tick {tick}, note {note}.")
PY
    then
      echo "ERROR: original native event position was not restored"; cat "$ARRANGE_EVENT_RESTORED_XML"; exit 1
    fi
    echo "Arrangement single-event MOVE ± GRID → native-position restoration passed."

    # Duplicate the selected event through a real UI touch; validate the native Pattern
    # projection on every step and restore the original fixture using the real Undo action.
    arrangement_event_state_line() {
      local action="$1"
      adb_bounded 5 logcat -d -v brief 2>/dev/null \
        | grep -F -- "UI_ARRANGE_EVENT_STATE: action=${action} " \
        | tail -n 1 || true
    }
    arrangement_placement_state_line() {
      local action="$1"
      adb_bounded 5 logcat -d -v brief 2>/dev/null \
        | grep -F -- "UI_ARRANGE_PLACEMENT_STATE: action=${action} " \
        | tail -n 1 || true
    }
    arrangement_insertion_target_line() {
      adb_bounded 5 logcat -d -v brief 2>/dev/null \
        | grep -F -- "UI_ARRANGE_INSERTION_TARGET: " \
        | tail -n 1 || true
    }
    arrange_event_target_tick=$((arrange_event_tick + arrange_event_grid))
    arrange_event_baseline_line="$(arrangement_event_state_line MOVE)"
    if [[ -z "$arrange_event_baseline_line" ]]; then
      echo "ERROR: no native Arrangement snapshot was logged after restoring MOVE ± GRID"; exit 1
    fi
    arrange_event_baseline_data="$(python3 - "$arrange_event_baseline_line" \
      "$arrange_event_track" "$arrange_event_tick" "$arrange_event_note" \
      "$arrange_event_target_tick" <<'PY'
import re, sys
line, track_s, source_s, note_s, target_s = sys.argv[1:]
track, source, note, target = map(int, (track_s, source_s, note_s, target_s))
m = re.search(r"UI_ARRANGE_EVENT_STATE: action=MOVE track=(\d+) tick=(\d+) note=(\d+) data=(.*)$", line)
if not m or tuple(map(int, m.group(1,2,3))) != (track,source,note):
    raise AssertionError(f"baseline native state mismatch: {line!r}")
data = m.group(4)
length_text, sep, rest = data.partition("|")
if not sep or int(length_text) <= target:
    raise AssertionError(f"target tick is outside the native Pattern: {data!r}")
events = [tuple(map(int, item.split(","))) for item in rest.split(";") if item]
if sum(1 for t,_,n in events if t == source and n == note) != 1:
    raise AssertionError("source event is not unique in native Pattern state")
if any(t == target and n == note for t,_,n in events):
    raise AssertionError("target event is already occupied in the baseline fixture")
print(data)
PY
    )" || { echo "ERROR: baseline native Arrangement Pattern validation failed"; exit 1; }

    # Reusable validator: all musical events must match baseline except for one clone
    # at source+grid; undo must restore the serialized Pattern exactly.
    validate_arrangement_duplicate_state() {
      local action="$1" xml_path="$2" native_line="$3" selection_tick="$4"
      if ! python3 - "$action" "$xml_path" "$native_line" "$arrange_event_baseline_data" \
          "$arrange_event_track" "$arrange_event_tick" "$arrange_event_target_tick" \
          "$arrange_event_note" "$selection_tick" <<'PY'
import re, sys, xml.etree.ElementTree as ET
action, path, line, baseline, track_s, source_s, target_s, note_s, selected_s = sys.argv[1:]
track,source,target,note,selected = map(int,(track_s,source_s,target_s,note_s,selected_s))
m = re.search(r"UI_ARRANGE_EVENT_STATE: action=(\w+) track=(\d+) tick=(\d+) note=(\d+) data=(.*)$", line)
if not m or m.group(1) != action:
    raise AssertionError(f"missing authoritative {action} state marker: {line!r}")
if tuple(map(int,m.group(2,3,4))) != (track,selected,note):
    raise AssertionError(f"{action} selected identity mismatch: {m.groups()[1:4]}")
data = m.group(5)
def decode(value):
    length_s, sep, rest = value.partition("|")
    if not sep: raise AssertionError(f"invalid native Pattern serialization {value!r}")
    return int(length_s), [tuple(map(int,e.split(","))) for e in rest.split(";") if e]
base_len, base_events = decode(baseline)
current_len, events = decode(data)
if base_len != current_len: raise AssertionError("Pattern length changed during event duplication")
if action == "UNDO":
    if data != baseline: raise AssertionError("Undo did not restore exact native Pattern serialization")
else:
    if len(events) != len(base_events)+1:
        raise AssertionError(f"{action} should add exactly one event: {len(base_events)}→{len(events)}")
    source_events=[e for e in events if e[0]==source and e[2]==note]
    clones=[e for e in events if e[0]==target and e[2]==note]
    if len(source_events)!=1 or len(clones)!=1:
        raise AssertionError(f"{action} source/clone mismatch: source={source_events}, clone={clones}")
    if source_events[0][1] != clones[0][1]:
        raise AssertionError("duplicated event duration differs from source")
    if [e for e in events if e != clones[0]] != base_events:
        raise AssertionError(f"{action} changed other native Pattern events")
nodes=list(ET.parse(path).getroot().iter("node"))
info=[n for n in nodes if n.attrib.get("content-desc")=="Arrangement editing guidance"]
expected=f"EVENT • TRACK {track+1:02d} • TICK {selected} • NOTE {note:03d}"
if len(info)!=1 or info[0].attrib.get("text") != expected:
    raise AssertionError(f"{action} accessibility selection mismatch: expected {expected!r}")
print(f"Arrangement {action}: authoritative Pattern and selected UI event verified.")
PY
      then
        echo "ERROR: Arrangement ${action} state failed validation"
        cat "$xml_path"
        return 1
      fi
    }

    if ! tap_visible_description_early "$ARRANGE_EVENT_RESTORED_XML" \
      "Arrangement duplicate selected event one grid later"; then
      echo "ERROR: Arrangement DUP control not actionable"; exit 1
    fi
    sleep 0.35
    ARRANGE_EVENT_DUPLICATED_PNG="/tmp/mpc-groovebox-arrange-event-duplicated.png"
    ARRANGE_EVENT_DUPLICATED_XML="/tmp/mpc-groovebox-arrange-event-duplicated-uiautomator.xml"
    ARRANGE_EVENT_DUPLICATED_REMOTE="/sdcard/mpc-groovebox-arrange-event-duplicated-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_EVENT_DUPLICATED_REMOTE" "$ARRANGE_EVENT_DUPLICATED_XML"; then
      echo "ERROR: duplicated Arrangement hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_EVENT_DUPLICATED_PNG"; then
      echo "ERROR: duplicated Arrangement screenshot failed"; exit 1
    fi
    arrangement_event_duplicate_line="$(arrangement_event_state_line DUPLICATE)"
    if [[ -z "$arrangement_event_duplicate_line" ]]; then
      echo "ERROR: DUP did not log its authoritative native Pattern state"; exit 1
    fi
    validate_arrangement_duplicate_state DUPLICATE "$ARRANGE_EVENT_DUPLICATED_XML" \
      "$arrangement_event_duplicate_line" "$arrange_event_target_tick" || exit 1

    if ! tap_visible_description_early "$ARRANGE_EVENT_DUPLICATED_XML" \
      "Arrangement undo latest sequencer edit"; then
      echo "ERROR: Arrangement UNDO control not actionable"; exit 1
    fi
    sleep 0.35
    ARRANGE_EVENT_DUP_UNDO_PNG="/tmp/mpc-groovebox-arrange-event-duplicate-undo.png"
    ARRANGE_EVENT_DUP_UNDO_XML="/tmp/mpc-groovebox-arrange-event-duplicate-undo-uiautomator.xml"
    ARRANGE_EVENT_DUP_UNDO_REMOTE="/sdcard/mpc-groovebox-arrange-event-duplicate-undo-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_EVENT_DUP_UNDO_REMOTE" "$ARRANGE_EVENT_DUP_UNDO_XML"; then
      echo "ERROR: duplicate-UNDO Arrangement hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_EVENT_DUP_UNDO_PNG"; then
      echo "ERROR: duplicate-UNDO Arrangement screenshot failed"; exit 1
    fi
    arrangement_event_undo_line="$(arrangement_event_state_line UNDO)"
    if [[ -z "$arrangement_event_undo_line" ]]; then
      echo "ERROR: UNDO did not log native Pattern state"; exit 1
    fi
    validate_arrangement_duplicate_state UNDO "$ARRANGE_EVENT_DUP_UNDO_XML" \
      "$arrangement_event_undo_line" "$arrange_event_tick" || exit 1

    if ! tap_visible_description_early "$ARRANGE_EVENT_DUP_UNDO_XML" \
      "Arrangement redo latest sequencer edit"; then
      echo "ERROR: Arrangement REDO control not actionable"; exit 1
    fi
    sleep 0.35
    ARRANGE_EVENT_DUP_REDO_PNG="/tmp/mpc-groovebox-arrange-event-duplicate-redo.png"
    ARRANGE_EVENT_DUP_REDO_XML="/tmp/mpc-groovebox-arrange-event-duplicate-redo-uiautomator.xml"
    ARRANGE_EVENT_DUP_REDO_REMOTE="/sdcard/mpc-groovebox-arrange-event-duplicate-redo-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_EVENT_DUP_REDO_REMOTE" "$ARRANGE_EVENT_DUP_REDO_XML"; then
      echo "ERROR: duplicate-REDO Arrangement hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_EVENT_DUP_REDO_PNG"; then
      echo "ERROR: duplicate-REDO Arrangement screenshot failed"; exit 1
    fi
    arrangement_event_redo_line="$(arrangement_event_state_line REDO)"
    if [[ -z "$arrangement_event_redo_line" ]]; then
      echo "ERROR: REDO did not log native Pattern state"; exit 1
    fi
    validate_arrangement_duplicate_state REDO "$ARRANGE_EVENT_DUP_REDO_XML" \
      "$arrangement_event_redo_line" "$arrange_event_target_tick" || exit 1

    if ! tap_visible_description_early "$ARRANGE_EVENT_DUP_REDO_XML" \
      "Arrangement undo latest sequencer edit"; then
      echo "ERROR: final duplicate restore UNDO control not actionable"; exit 1
    fi
    sleep 0.35
    ARRANGE_EVENT_DUP_RESTORED_PNG="/tmp/mpc-groovebox-arrange-event-duplicate-restored.png"
    ARRANGE_EVENT_DUP_RESTORED_XML="/tmp/mpc-groovebox-arrange-event-duplicate-restored-uiautomator.xml"
    ARRANGE_EVENT_DUP_RESTORED_REMOTE="/sdcard/mpc-groovebox-arrange-event-duplicate-restored-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_EVENT_DUP_RESTORED_REMOTE" "$ARRANGE_EVENT_DUP_RESTORED_XML"; then
      echo "ERROR: final duplicate-restore Arrangement hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_EVENT_DUP_RESTORED_PNG"; then
      echo "ERROR: final duplicate-restore Arrangement screenshot failed"; exit 1
    fi
    arrangement_event_final_undo_line="$(arrangement_event_state_line UNDO)"
    if [[ -z "$arrangement_event_final_undo_line" ]]; then
      echo "ERROR: final duplicate-restore UNDO did not log native Pattern state"; exit 1
    fi
    validate_arrangement_duplicate_state UNDO "$ARRANGE_EVENT_DUP_RESTORED_XML" \
      "$arrangement_event_final_undo_line" "$arrange_event_tick" || exit 1
    echo "Arrangement duplicate touch → native Pattern → UNDO → REDO → exact restoration passed."

    # Copy and paste the selected event into the next grid cell through real UI actions.
    # The native clipboard owns the full event value; copy itself must not mutate the Pattern.
    if ! tap_visible_description_early "$ARRANGE_EVENT_DUP_RESTORED_XML" \
      "Arrangement copy selected event"; then
      echo "ERROR: Arrangement COPY control not actionable"; exit 1
    fi
    sleep 0.25
    ARRANGE_EVENT_COPIED_PNG="/tmp/mpc-groovebox-arrange-event-copied.png"
    ARRANGE_EVENT_COPIED_XML="/tmp/mpc-groovebox-arrange-event-copied-uiautomator.xml"
    ARRANGE_EVENT_COPIED_REMOTE="/sdcard/mpc-groovebox-arrange-event-copied-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_EVENT_COPIED_REMOTE" "$ARRANGE_EVENT_COPIED_XML"; then
      echo "ERROR: Arrangement copied-state hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_EVENT_COPIED_PNG"; then
      echo "ERROR: Arrangement copied-state screenshot failed"; exit 1
    fi
    arrangement_event_copy_line="$(arrangement_event_state_line COPY)"
    if [[ -z "$arrangement_event_copy_line" ]]; then
      echo "ERROR: COPY did not log native Pattern state"; exit 1
    fi
    if ! python3 - "$ARRANGE_EVENT_COPIED_XML" "$arrangement_event_copy_line" \
      "$arrange_event_baseline_data" "$arrange_event_track" "$arrange_event_tick" \
      "$arrange_event_note" <<'PY'
import re, sys, xml.etree.ElementTree as ET
path,line,baseline,track_s,tick_s,note_s=sys.argv[1:]
m=re.search(r"UI_ARRANGE_EVENT_STATE: action=COPY track=(\d+) tick=(\d+) note=(\d+) data=(.*)$",line)
if not m or m.group(4)!=baseline: raise AssertionError("COPY mutated the native Arrangement Pattern")
track,tick,note=map(int,(track_s,tick_s,note_s))
if tuple(map(int,m.group(1,2,3)))!=(track,tick,note): raise AssertionError("COPY identity mismatch")
nodes=list(ET.parse(path).getroot().iter("node"))
copy=[n for n in nodes if n.attrib.get("content-desc")=="Arrangement copy selected event"]
paste=[n for n in nodes if n.attrib.get("content-desc")=="Arrangement paste copied event one grid later"]
if len(copy)!=1 or copy[0].attrib.get("enabled")!="true": raise AssertionError("COPY is no longer actionable")
if len(paste)!=1 or paste[0].attrib.get("enabled")!="true": raise AssertionError("successful COPY did not enable PASTE")
print("Arrangement COPY captured the selected event without mutating the native Pattern.")
PY
    then
      echo "ERROR: Arrangement COPY did not preserve the source Pattern or enable PASTE"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_EVENT_COPIED_XML" \
      "Arrangement paste copied event one grid later"; then
      echo "ERROR: Arrangement PASTE control not actionable after COPY"; exit 1
    fi
    sleep 0.35
    ARRANGE_EVENT_PASTED_PNG="/tmp/mpc-groovebox-arrange-event-pasted.png"
    ARRANGE_EVENT_PASTED_XML="/tmp/mpc-groovebox-arrange-event-pasted-uiautomator.xml"
    ARRANGE_EVENT_PASTED_REMOTE="/sdcard/mpc-groovebox-arrange-event-pasted-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_EVENT_PASTED_REMOTE" "$ARRANGE_EVENT_PASTED_XML"; then
      echo "ERROR: Arrangement pasted-state hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_EVENT_PASTED_PNG"; then
      echo "ERROR: Arrangement pasted-state screenshot failed"; exit 1
    fi
    arrangement_event_paste_line="$(arrangement_event_state_line PASTE)"
    if [[ -z "$arrangement_event_paste_line" ]]; then
      echo "ERROR: PASTE did not log authoritative native Pattern state"; exit 1
    fi
    validate_arrangement_duplicate_state PASTE "$ARRANGE_EVENT_PASTED_XML" \
      "$arrangement_event_paste_line" "$arrange_event_target_tick" || exit 1

    if ! tap_visible_description_early "$ARRANGE_EVENT_PASTED_XML" \
      "Arrangement undo latest sequencer edit"; then
      echo "ERROR: PASTE UNDO control not actionable"; exit 1
    fi
    sleep 0.3
    ARRANGE_EVENT_PASTE_UNDO_PNG="/tmp/mpc-groovebox-arrange-event-paste-undo.png"
    ARRANGE_EVENT_PASTE_UNDO_XML="/tmp/mpc-groovebox-arrange-event-paste-undo-uiautomator.xml"
    ARRANGE_EVENT_PASTE_UNDO_REMOTE="/sdcard/mpc-groovebox-arrange-event-paste-undo-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_EVENT_PASTE_UNDO_REMOTE" "$ARRANGE_EVENT_PASTE_UNDO_XML"; then
      echo "ERROR: PASTE UNDO hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_EVENT_PASTE_UNDO_PNG"; then
      echo "ERROR: PASTE UNDO screenshot failed"; exit 1
    fi
    arrangement_event_paste_undo_line="$(arrangement_event_state_line UNDO)"
    if [[ -z "$arrangement_event_paste_undo_line" ]]; then
      echo "ERROR: PASTE UNDO did not log native Pattern state"; exit 1
    fi
    validate_arrangement_duplicate_state UNDO "$ARRANGE_EVENT_PASTE_UNDO_XML" \
      "$arrangement_event_paste_undo_line" "$arrange_event_tick" || exit 1

    if ! tap_visible_description_early "$ARRANGE_EVENT_PASTE_UNDO_XML" \
      "Arrangement redo latest sequencer edit"; then
      echo "ERROR: PASTE REDO control not actionable"; exit 1
    fi
    sleep 0.3
    ARRANGE_EVENT_PASTE_REDO_PNG="/tmp/mpc-groovebox-arrange-event-paste-redo.png"
    ARRANGE_EVENT_PASTE_REDO_XML="/tmp/mpc-groovebox-arrange-event-paste-redo-uiautomator.xml"
    ARRANGE_EVENT_PASTE_REDO_REMOTE="/sdcard/mpc-groovebox-arrange-event-paste-redo-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_EVENT_PASTE_REDO_REMOTE" "$ARRANGE_EVENT_PASTE_REDO_XML"; then
      echo "ERROR: PASTE REDO hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_EVENT_PASTE_REDO_PNG"; then
      echo "ERROR: PASTE REDO screenshot failed"; exit 1
    fi
    arrangement_event_paste_redo_line="$(arrangement_event_state_line REDO)"
    if [[ -z "$arrangement_event_paste_redo_line" ]]; then
      echo "ERROR: PASTE REDO did not log native Pattern state"; exit 1
    fi
    validate_arrangement_duplicate_state REDO "$ARRANGE_EVENT_PASTE_REDO_XML" \
      "$arrangement_event_paste_redo_line" "$arrange_event_target_tick" || exit 1

    if ! tap_visible_description_early "$ARRANGE_EVENT_PASTE_REDO_XML" \
      "Arrangement undo latest sequencer edit"; then
      echo "ERROR: final PASTE cleanup UNDO not actionable"; exit 1
    fi
    sleep 0.3
    ARRANGE_EVENT_PASTE_RESTORED_PNG="/tmp/mpc-groovebox-arrange-event-paste-restored.png"
    ARRANGE_EVENT_PASTE_RESTORED_XML="/tmp/mpc-groovebox-arrange-event-paste-restored-uiautomator.xml"
    ARRANGE_EVENT_PASTE_RESTORED_REMOTE="/sdcard/mpc-groovebox-arrange-event-paste-restored-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_EVENT_PASTE_RESTORED_REMOTE" "$ARRANGE_EVENT_PASTE_RESTORED_XML"; then
      echo "ERROR: final PASTE cleanup hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_EVENT_PASTE_RESTORED_PNG"; then
      echo "ERROR: final PASTE cleanup screenshot failed"; exit 1
    fi
    arrangement_event_paste_final_undo_line="$(arrangement_event_state_line UNDO)"
    if [[ -z "$arrangement_event_paste_final_undo_line" ]]; then
      echo "ERROR: final PASTE cleanup UNDO did not log native Pattern state"; exit 1
    fi
    validate_arrangement_duplicate_state UNDO "$ARRANGE_EVENT_PASTE_RESTORED_XML" \
      "$arrangement_event_paste_final_undo_line" "$arrange_event_tick" || exit 1
    echo "Arrangement COPY → PASTE → UNDO → REDO → exact restoration passed."

    # CUT must remove only the selected native Pattern event, then Undo/Redo must be reversible.
    validate_arrangement_cut_state() {
      local action="$1" xml_path="$2" native_line="$3"
      if ! python3 - "$action" "$xml_path" "$native_line" "$arrange_event_baseline_data" \
          "$arrange_event_track" "$arrange_event_tick" "$arrange_event_note" <<'PY'
import re, sys, xml.etree.ElementTree as ET
action,path,line,baseline,track_s,tick_s,note_s=sys.argv[1:]
track,tick,note=map(int,(track_s,tick_s,note_s))
m=re.search(r"UI_ARRANGE_EVENT_STATE: action=(CUT|REDO) track=(\d+) tick=(\d+) note=(\d+) data=(.*)$",line)
if not m or m.group(1)!=action or tuple(map(int,m.group(2,3,4)))!=(track,tick,note):
    raise AssertionError(f"{action} native-state identity mismatch: {line!r}")
def decode(value):
    length_s,sep,rest=value.partition("|")
    if not sep: raise AssertionError(f"invalid Pattern serialization: {value!r}")
    return int(length_s),[tuple(map(int,e.split(","))) for e in rest.split(";") if e]
base_len,base=decode(baseline); current_len,current=decode(m.group(5))
removed=[e for e in base if e[0]==tick and e[2]==note]
expected=[e for e in base if not(e[0]==tick and e[2]==note)]
if len(removed)!=1 or current_len!=base_len or current!=expected:
    raise AssertionError(f"{action} must remove only the unique source: {base!r} -> {current!r}")
nodes=list(ET.parse(path).getroot().iter("node"))
info=[n for n in nodes if n.attrib.get("content-desc")=="Arrangement editing guidance"]
if len(info)!=1 or info[0].attrib.get("text")!="Select event • tap empty timeline to set PASTE target":
    raise AssertionError(f"{action} should clear selection of the deleted event")
for desc in ("Arrangement cut selected event","Arrangement copy selected event",
             "Arrangement duplicate selected event one grid later"):
    found=[n for n in nodes if n.attrib.get("content-desc")==desc]
    if len(found)!=1 or found[0].attrib.get("enabled")!="false":
        raise AssertionError(f"{action} left an edit action enabled without a selection: {desc}")
print(f"Arrangement {action}: exact source deletion and cleared selection verified.")
PY
      then
        echo "ERROR: Arrangement $action native-state/hierarchy verification failed"
        return 1
      fi
    }

    if ! tap_visible_description_early "$ARRANGE_EVENT_PASTE_RESTORED_XML" \
      "Arrangement cut selected event"; then
      echo "ERROR: Arrangement CUT Function Bar action not actionable"; exit 1
    fi
    sleep 0.3
    ARRANGE_EVENT_CUT_PNG="/tmp/mpc-groovebox-arrange-event-cut.png"
    ARRANGE_EVENT_CUT_XML="/tmp/mpc-groovebox-arrange-event-cut-uiautomator.xml"
    ARRANGE_EVENT_CUT_REMOTE="/sdcard/mpc-groovebox-arrange-event-cut-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_EVENT_CUT_REMOTE" "$ARRANGE_EVENT_CUT_XML"; then
      echo "ERROR: Arrangement CUT hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_EVENT_CUT_PNG"; then
      echo "ERROR: Arrangement CUT screenshot failed"; exit 1
    fi
    arrangement_event_cut_line="$(arrangement_event_state_line CUT)"
    if [[ -z "$arrangement_event_cut_line" ]]; then
      echo "ERROR: CUT did not log authoritative native Pattern state"; exit 1
    fi
    validate_arrangement_cut_state CUT "$ARRANGE_EVENT_CUT_XML" "$arrangement_event_cut_line" || exit 1

    if ! tap_visible_description_early "$ARRANGE_EVENT_CUT_XML" \
      "Arrangement undo latest sequencer edit"; then
      echo "ERROR: CUT UNDO action not available"; exit 1
    fi
    sleep 0.3
    ARRANGE_EVENT_CUT_UNDO_PNG="/tmp/mpc-groovebox-arrange-event-cut-undo.png"
    ARRANGE_EVENT_CUT_UNDO_XML="/tmp/mpc-groovebox-arrange-event-cut-undo-uiautomator.xml"
    ARRANGE_EVENT_CUT_UNDO_REMOTE="/sdcard/mpc-groovebox-arrange-event-cut-undo-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_EVENT_CUT_UNDO_REMOTE" "$ARRANGE_EVENT_CUT_UNDO_XML"; then
      echo "ERROR: CUT UNDO hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_EVENT_CUT_UNDO_PNG"; then
      echo "ERROR: CUT UNDO screenshot failed"; exit 1
    fi
    arrangement_event_cut_undo_line="$(arrangement_event_state_line UNDO)"
    if [[ -z "$arrangement_event_cut_undo_line" ]]; then
      echo "ERROR: CUT UNDO did not log native Pattern state"; exit 1
    fi
    validate_arrangement_duplicate_state UNDO "$ARRANGE_EVENT_CUT_UNDO_XML" \
      "$arrangement_event_cut_undo_line" "$arrange_event_tick" || exit 1

    if ! tap_visible_description_early "$ARRANGE_EVENT_CUT_UNDO_XML" \
      "Arrangement redo latest sequencer edit"; then
      echo "ERROR: CUT REDO action not available"; exit 1
    fi
    sleep 0.3
    ARRANGE_EVENT_CUT_REDO_PNG="/tmp/mpc-groovebox-arrange-event-cut-redo.png"
    ARRANGE_EVENT_CUT_REDO_XML="/tmp/mpc-groovebox-arrange-event-cut-redo-uiautomator.xml"
    ARRANGE_EVENT_CUT_REDO_REMOTE="/sdcard/mpc-groovebox-arrange-event-cut-redo-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_EVENT_CUT_REDO_REMOTE" "$ARRANGE_EVENT_CUT_REDO_XML"; then
      echo "ERROR: CUT REDO hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_EVENT_CUT_REDO_PNG"; then
      echo "ERROR: CUT REDO screenshot failed"; exit 1
    fi
    arrangement_event_cut_redo_line="$(arrangement_event_state_line REDO)"
    if [[ -z "$arrangement_event_cut_redo_line" ]]; then
      echo "ERROR: CUT REDO did not log native Pattern state"; exit 1
    fi
    validate_arrangement_cut_state REDO "$ARRANGE_EVENT_CUT_REDO_XML" "$arrangement_event_cut_redo_line" || exit 1

    if ! tap_visible_description_early "$ARRANGE_EVENT_CUT_REDO_XML" \
      "Arrangement undo latest sequencer edit"; then
      echo "ERROR: final CUT restoration UNDO action not available"; exit 1
    fi
    sleep 0.3
    ARRANGE_EVENT_CUT_RESTORED_PNG="/tmp/mpc-groovebox-arrange-event-cut-restored.png"
    ARRANGE_EVENT_CUT_RESTORED_XML="/tmp/mpc-groovebox-arrange-event-cut-restored-uiautomator.xml"
    ARRANGE_EVENT_CUT_RESTORED_REMOTE="/sdcard/mpc-groovebox-arrange-event-cut-restored-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_EVENT_CUT_RESTORED_REMOTE" "$ARRANGE_EVENT_CUT_RESTORED_XML"; then
      echo "ERROR: final CUT restoration hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_EVENT_CUT_RESTORED_PNG"; then
      echo "ERROR: final CUT restoration screenshot failed"; exit 1
    fi
    arrangement_event_cut_final_undo_line="$(arrangement_event_state_line UNDO)"
    if [[ -z "$arrangement_event_cut_final_undo_line" ]]; then
      echo "ERROR: final CUT UNDO did not log native Pattern state"; exit 1
    fi
    validate_arrangement_duplicate_state UNDO "$ARRANGE_EVENT_CUT_RESTORED_XML" \
      "$arrangement_event_cut_final_undo_line" "$arrange_event_tick" || exit 1
    echo "Arrangement CUT → UNDO → REDO → UNDO restored exact native Pattern state."

    # Repeat CUT, choose a genuine empty timeline point, and PASTE exactly at that
    # snapped cursor. Undo/Redo must return to the cut Pattern/cursor and then restore
    # the original Pattern by undoing CUT. No event selection is fabricated at the cursor.
    if ! tap_visible_description_early "$ARRANGE_EVENT_CUT_RESTORED_XML" \
      "Arrangement cut selected event"; then
      echo "ERROR: second Arrangement CUT not actionable after fixture restoration"; exit 1
    fi
    sleep 0.3
    ARRANGE_CURSOR_CUT_PNG="/tmp/mpc-groovebox-arrange-event-cursor-cut.png"
    ARRANGE_CURSOR_CUT_XML="/tmp/mpc-groovebox-arrange-event-cursor-cut-uiautomator.xml"
    ARRANGE_CURSOR_CUT_REMOTE="/sdcard/mpc-groovebox-arrange-event-cursor-cut-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_CURSOR_CUT_REMOTE" "$ARRANGE_CURSOR_CUT_XML"; then
      echo "ERROR: second CUT hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_CURSOR_CUT_PNG"; then
      echo "ERROR: second CUT screenshot failed"; exit 1
    fi
    arrangement_cursor_cut_line="$(arrangement_event_state_line CUT)"
    if [[ -z "$arrangement_cursor_cut_line" ]]; then
      echo "ERROR: second CUT did not log authoritative Pattern state"; exit 1
    fi
    validate_arrangement_cut_state CUT "$ARRANGE_CURSOR_CUT_XML" \
      "$arrangement_cursor_cut_line" || exit 1
    if ! python3 - "$ARRANGE_CURSOR_CUT_XML" <<'PY'
import sys, xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter("node"))
paste=[n for n in nodes if n.attrib.get("content-desc")=="Arrangement paste copied event one grid later"]
if len(paste)!=1 or paste[0].attrib.get("enabled")!="false":
    raise AssertionError("PASTE must stay disabled after CUT until an empty target is chosen")
print("CUT clears event selection; PASTE remains disabled without a target cursor.")
PY
    then
      echo "ERROR: PASTE enabled without an event selection or insertion cursor"; exit 1
    fi

    arrange_cursor_target="$(python3 - "$ARRANGE_CURSOR_CUT_XML" \
      "$arrange_density" "$arrange_event_baseline_data" "$arrange_event_track" \
      "$arrange_event_tick" "$arrange_event_note" "$arrange_event_grid" \
      "$arrange_ticks_per_bar" "$loop_visible_bars" "$loop_viewport_start" <<'PY'
import re, sys, xml.etree.ElementTree as ET
path,density_text,data,track_s,source_s,note_s,grid_s,tpb_s,visible_s,viewport_s=sys.argv[1:]
track,source,note,grid,tpb,visible,viewport=map(int,(track_s,source_s,note_s,grid_s,tpb_s,visible_s,viewport_s))
length_s,sep,rest=data.partition("|")
if not sep: raise AssertionError("invalid Pattern snapshot")
length=int(length_s)
events=[tuple(map(int,e.split(","))) for e in rest.split(";") if e]
source_events=[e for e in events if e[0]==source and e[2]==note]
if len(source_events)!=1: raise AssertionError(f"source event not unique: {source_events!r}")
occupied=[e for e in events if not(e[0]==source and e[2]==note)]
ds=re.findall(r"Override density:\s*(\d+)",density_text)
if not ds: ds=re.findall(r"Physical density:\s*(\d+)",density_text)
if not ds: raise AssertionError("cannot read display density")
density=int(ds[-1])/160.0
nodes=list(ET.parse(path).getroot().iter("node"))
views=[n for n in nodes if n.attrib.get("content-desc")=="MPC linear arrangement editor"]
if len(views)!=1: raise AssertionError("Arrangement canvas missing")
m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",views[0].attrib.get("bounds",""))
if not m: raise AssertionError("Arrangement bounds invalid")
left,top,right,bottom=map(int,m.groups())
width=right-left
header,ruler,lane=138*density,34*density,46*density
bar_width=(width-header)/visible
visible_start=max(0,(viewport-1)*tpb)
visible_end=min(length,(viewport+visible-1)*tpb)
target=None
for candidate in range(0,length,grid):
    if candidate<=0 or candidate==source or candidate+grid>=length:
        continue
    if not(visible_start <= candidate < visible_end):
        continue
    # Leave half a grid of clearance so pixel rounding cannot hit an adjacent note.
    if any(et <= candidate+grid/2 and et+dur >= candidate-grid/2
           for et,dur,*_ in occupied):
        continue
    x=round(left+header+(candidate/tpb-(viewport-1))*bar_width)
    y=round(top+ruler+track*lane+21.5*density)
    if not(left+header < x < right and top+ruler+track*lane < y < top+ruler+(track+1)*lane):
        continue
    target=candidate
    break
if target is None: raise AssertionError("no visible, grid-snapped empty Pattern target found")
x=round(left+header+(target/tpb-(viewport-1))*bar_width)
y=round(top+ruler+track*lane+21.5*density)
print(target,x,y)
PY
    )" || { echo "ERROR: no valid empty snapped Pattern position for cursor paste"; exit 1; }
    read -r arrange_cursor_tick arrange_cursor_x arrange_cursor_y <<< "$arrange_cursor_target"
    if ! adb_bounded 10 shell input tap "$arrange_cursor_x" "$arrange_cursor_y"; then
      echo "ERROR: empty-timeline paste target touch failed"; exit 1
    fi
    sleep 0.3
    ARRANGE_PASTE_CURSOR_PNG="/tmp/mpc-groovebox-arrange-event-paste-cursor.png"
    ARRANGE_PASTE_CURSOR_XML="/tmp/mpc-groovebox-arrange-event-paste-cursor-uiautomator.xml"
    ARRANGE_PASTE_CURSOR_REMOTE="/sdcard/mpc-groovebox-arrange-event-paste-cursor-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_PASTE_CURSOR_REMOTE" "$ARRANGE_PASTE_CURSOR_XML"; then
      echo "ERROR: paste cursor hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_PASTE_CURSOR_PNG"; then
      echo "ERROR: paste cursor screenshot failed"; exit 1
    fi
    arrangement_cursor_line="$(arrangement_insertion_target_line)"
    if [[ -z "$arrangement_cursor_line" ]]; then
      echo "ERROR: empty timeline touch did not create native-addressed insertion cursor"; exit 1
    fi
    if ! python3 - "$ARRANGE_PASTE_CURSOR_XML" "$arrangement_cursor_line" \
      "$arrange_event_baseline_data" "$arrange_event_track" "$arrange_event_tick" \
      "$arrange_event_note" "$arrange_cursor_tick" "$arrange_event_grid" <<'PY'
import re, sys, xml.etree.ElementTree as ET
path,line,baseline,track_s,source_s,note_s,target_s,grid_s=sys.argv[1:]
track,source,note,target,grid=map(int,(track_s,source_s,note_s,target_s,grid_s))
m=re.search(r"UI_ARRANGE_INSERTION_TARGET: track=(\d+) tick=(\d+) grid=(\d+) data=(.*)$",line)
if not m or tuple(map(int,m.group(1,2,3)))!=(track,target,grid):
    raise AssertionError(f"cursor state mismatch: {line!r}")
def decode(value):
    length_s,sep,rest=value.partition("|")
    if not sep: raise AssertionError(f"invalid Pattern serialization: {value!r}")
    return int(length_s),[tuple(map(int,e.split(","))) for e in rest.split(";") if e]
base_len,base=decode(baseline); current_len,current=decode(m.group(4))
expected=[e for e in base if not(e[0]==source and e[2]==note)]
if current_len!=base_len or current!=expected:
    raise AssertionError("setting insertion cursor mutated native Pattern state")
nodes=list(ET.parse(path).getroot().iter("node"))
info=[n for n in nodes if n.attrib.get("content-desc")==f"Arrangement paste target Track {track+1:02d} tick {target}"]
if len(info)!=1 or info[0].attrib.get("text")!=f"PASTE TARGET • TRACK {track+1:02d} • TICK {target}":
    raise AssertionError("cursor target is not exposed with a truthful accessible target label")
paste=[n for n in nodes if n.attrib.get("content-desc")==f"Arrangement paste copied event at target tick {target}"]
if len(paste)!=1 or paste[0].attrib.get("enabled")!="true" or paste[0].attrib.get("clickable")!="true":
    raise AssertionError("clipboard + valid cursor did not enable Function Bar PASTE")
for desc in ("Arrangement cut selected event","Arrangement copy selected event",
             "Arrangement duplicate selected event one grid later"):
    found=[n for n in nodes if n.attrib.get("content-desc")==desc]
    if len(found)!=1 or found[0].attrib.get("enabled")!="false":
        raise AssertionError(f"event-only action was enabled for insertion cursor: {desc}")
print(f"Empty target cursor is snapped to tick {target}; Pattern is unchanged and PASTE is enabled.")
PY
    then
      echo "ERROR: insertion cursor label, native state, or Function Bar enablement failed"; exit 1
    fi

    # A strict interior cursor splits the real native clip placement into two adjacent
    # source ranges, without changing MIDI notes. Undo/Redo must reproduce exact clip state.
    arrangement_placement_target_line="$(arrangement_placement_state_line TARGET)"
    if [[ -z "$arrangement_placement_target_line" ]]; then
      echo "ERROR: insertion cursor did not capture the native Pattern placement baseline"; exit 1
    fi
    if ! tap_visible_description_early "$ARRANGE_PASTE_CURSOR_XML" \
      "Arrangement split Pattern placement at target tick $arrange_cursor_tick"; then
      echo "ERROR: Pattern clip SPLIT Function Bar action not actionable"; exit 1
    fi
    sleep 0.3
    ARRANGE_PLACEMENT_SPLIT_PNG="/tmp/mpc-groovebox-arrange-placement-split.png"
    ARRANGE_PLACEMENT_SPLIT_XML="/tmp/mpc-groovebox-arrange-placement-split-uiautomator.xml"
    ARRANGE_PLACEMENT_SPLIT_REMOTE="/sdcard/mpc-groovebox-arrange-placement-split-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_PLACEMENT_SPLIT_REMOTE" "$ARRANGE_PLACEMENT_SPLIT_XML"; then
      echo "ERROR: placement split accessibility hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_PLACEMENT_SPLIT_PNG"; then
      echo "ERROR: placement split screenshot failed"; exit 1
    fi
    arrangement_placement_split_line="$(arrangement_placement_state_line SPLIT)"
    if [[ -z "$arrangement_placement_split_line" ]]; then
      echo "ERROR: SPLIT did not log authoritative native placement state"; exit 1
    fi
    if ! python3 - "$arrangement_placement_target_line" "$arrangement_placement_split_line" \
      "$arrange_event_track" "$arrange_cursor_tick" <<'PY'
import re, sys
target_line, split_line, track_s, tick_s = sys.argv[1:]
track, tick = map(int, (track_s, tick_s))
pattern = re.compile(
    r"UI_ARRANGE_PLACEMENT_STATE: action=(TARGET|SPLIT) track=(\d+) tick=(\d+) "
    r"placements=(.*?) pattern=(.*)$")
before=pattern.search(target_line)
after=pattern.search(split_line)
if not before or not after or before.group(1)!="TARGET" or after.group(1)!="SPLIT":
    raise AssertionError("placement state log format/action mismatch")
if tuple(map(int,before.group(2,3)))!=(track,tick) or tuple(map(int,after.group(2,3)))!=(track,tick):
    raise AssertionError("placement cursor identity mismatch")
if before.group(5)!=after.group(5):
    raise AssertionError("splitting a Pattern placement mutated authoritative MIDI notes")
def decode(value):
    length_s, sep, rest = value.partition("|")
    if not sep: raise AssertionError(f"invalid placement projection: {value!r}")
    rows=[]
    for item in rest.split(";"):
        if not item: continue
        fields=item.split(",")
        if len(fields)!=5: raise AssertionError(f"invalid placement row: {item!r}")
        rows.append((fields[0],fields[1],*map(int,fields[2:])))
    return int(length_s),rows
before_len, base=decode(before.group(4))
after_len, split=decode(after.group(4))
if before_len!=after_len or len(split)!=len(base)+1:
    raise AssertionError(f"SPLIT must add exactly one clip placement: {base!r} -> {split!r}")
containing=[p for p in base if p[2] < tick < p[2]+p[3]]
if len(containing)!=1: raise AssertionError(f"target must lie inside exactly one original clip: {containing!r}")
source=containing[0]
left=[p for p in split if p[0]==source[0]]
right=[p for p in split if p[0]!=source[0] and p[2]==tick]
if len(left)!=1 or len(right)!=1: raise AssertionError("split source/right placement identities are ambiguous")
left,right=left[0],right[0]
if left[1]!=source[1] or left[2]!=source[2] or left[3]!=tick-source[2] or left[4]!=source[4]:
    raise AssertionError(f"left clip source range incorrect: {source!r} -> {left!r}")
if right[1]!=source[1] or right[3]!=source[2]+source[3]-tick:
    raise AssertionError(f"right clip source range incorrect: {source!r} -> {right!r}")
if right[4]!=source[4]+tick-source[2] or right[0] in {p[0] for p in base}:
    raise AssertionError(f"right clip must use a unique id and continuous source offset: {right!r}")
expected=[p for p in base if p!=source]+[left,right]
expected.sort(key=lambda p:(p[2],p[0]))
if split!=expected: raise AssertionError(f"unrelated placements changed: {base!r} -> {split!r}")
for i,p in enumerate(split):
    for q in split[i+1:]:
        if p[2] < q[2]+q[3] and q[2] < p[2]+p[3]:
            raise AssertionError(f"split placements overlap: {p!r}, {q!r}")
print(f"Native Pattern placement split at tick {tick}; source Pattern data remained unchanged.")
PY
    then
      echo "ERROR: clip split changed unrelated placement or Pattern state"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_PLACEMENT_SPLIT_XML" \
      "Arrangement undo latest sequencer edit"; then
      echo "ERROR: placement split UNDO action unavailable"; exit 1
    fi
    sleep 0.3
    ARRANGE_PLACEMENT_UNDO_PNG="/tmp/mpc-groovebox-arrange-placement-split-undo.png"
    ARRANGE_PLACEMENT_UNDO_XML="/tmp/mpc-groovebox-arrange-placement-split-undo-uiautomator.xml"
    ARRANGE_PLACEMENT_UNDO_REMOTE="/sdcard/mpc-groovebox-arrange-placement-split-undo-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_PLACEMENT_UNDO_REMOTE" "$ARRANGE_PLACEMENT_UNDO_XML"; then
      echo "ERROR: placement split UNDO hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_PLACEMENT_UNDO_PNG"; then
      echo "ERROR: placement split UNDO screenshot failed"; exit 1
    fi
    arrangement_placement_undo_line="$(arrangement_placement_state_line UNDO)"
    if [[ -z "$arrangement_placement_undo_line" ]]; then
      echo "ERROR: placement split UNDO did not log native state"; exit 1
    fi
    if ! python3 - "$arrangement_placement_target_line" "$arrangement_placement_undo_line" \
      "$ARRANGE_PLACEMENT_UNDO_XML" "$arrange_event_track" "$arrange_cursor_tick" <<'PY'
import re, sys, xml.etree.ElementTree as ET
target_line, undo_line, path, track_s, tick_s = sys.argv[1:]
track,tick=map(int,(track_s,tick_s))
pat=re.compile(r"UI_ARRANGE_PLACEMENT_STATE: action=(TARGET|UNDO) track=(\d+) tick=(\d+) placements=(.*?) pattern=(.*)$")
base=pat.search(target_line); undo=pat.search(undo_line)
if not base or not undo or base.group(4)!=undo.group(4) or base.group(5)!=undo.group(5):
    raise AssertionError("UNDO must restore exact placement and MIDI Pattern serialization")
if tuple(map(int,undo.group(2,3)))!=(track,tick): raise AssertionError("UNDO target identity mismatch")
nodes=list(ET.parse(path).getroot().iter("node"))
split=[n for n in nodes if n.attrib.get("content-desc")==f"Arrangement split Pattern placement at target tick {tick}"]
if len(split)!=1 or split[0].attrib.get("enabled")!="true":
    raise AssertionError("UNDO should restore SPLIT on the original unsplit clip")
print("Placement split UNDO restored exact clip/Patterndata and enabled SPLIT again.")
PY
    then
      echo "ERROR: placement split UNDO did not restore baseline and target action"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_PLACEMENT_UNDO_XML" \
      "Arrangement redo latest sequencer edit"; then
      echo "ERROR: placement split REDO action unavailable"; exit 1
    fi
    sleep 0.3
    ARRANGE_PLACEMENT_REDO_PNG="/tmp/mpc-groovebox-arrange-placement-split-redo.png"
    ARRANGE_PLACEMENT_REDO_XML="/tmp/mpc-groovebox-arrange-placement-split-redo-uiautomator.xml"
    ARRANGE_PLACEMENT_REDO_REMOTE="/sdcard/mpc-groovebox-arrange-placement-split-redo-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_PLACEMENT_REDO_REMOTE" "$ARRANGE_PLACEMENT_REDO_XML"; then
      echo "ERROR: placement split REDO hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_PLACEMENT_REDO_PNG"; then
      echo "ERROR: placement split REDO screenshot failed"; exit 1
    fi
    arrangement_placement_redo_line="$(arrangement_placement_state_line REDO)"
    if [[ -z "$arrangement_placement_redo_line" ]]; then
      echo "ERROR: placement split REDO did not log native state"; exit 1
    fi
    if ! python3 - "$arrangement_placement_split_line" "$arrangement_placement_redo_line" \
      "$arrange_event_track" "$arrange_cursor_tick" <<'PY'
import re, sys
split_line,redo_line,track_s,tick_s=sys.argv[1:]
track,tick=map(int,(track_s,tick_s))
pat=re.compile(r"UI_ARRANGE_PLACEMENT_STATE: action=(SPLIT|REDO) track=(\d+) tick=(\d+) placements=(.*?) pattern=(.*)$")
split=pat.search(split_line); redo=pat.search(redo_line)
if not split or not redo or split.group(4)!=redo.group(4) or split.group(5)!=redo.group(5):
    raise AssertionError("REDO must reproduce exact split placement and source Pattern state")
if tuple(map(int,redo.group(2,3)))!=(track,tick): raise AssertionError("REDO target identity mismatch")
print("Placement split REDO reproduced the exact two-clip state.")
PY
    then
      echo "ERROR: placement split REDO was not deterministic"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_PLACEMENT_REDO_XML" \
      "Arrangement undo latest sequencer edit"; then
      echo "ERROR: final placement split UNDO unavailable"; exit 1
    fi
    sleep 0.3
    ARRANGE_PLACEMENT_RESTORED_PNG="/tmp/mpc-groovebox-arrange-placement-split-restored.png"
    ARRANGE_PLACEMENT_RESTORED_XML="/tmp/mpc-groovebox-arrange-placement-split-restored-uiautomator.xml"
    ARRANGE_PLACEMENT_RESTORED_REMOTE="/sdcard/mpc-groovebox-arrange-placement-split-restored-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_PLACEMENT_RESTORED_REMOTE" "$ARRANGE_PLACEMENT_RESTORED_XML"; then
      echo "ERROR: final placement split restoration hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_PLACEMENT_RESTORED_PNG"; then
      echo "ERROR: final placement split restoration screenshot failed"; exit 1
    fi
    arrangement_placement_restored_line="$(arrangement_placement_state_line UNDO)"
    if [[ -z "$arrangement_placement_restored_line" ]]; then
      echo "ERROR: final placement split UNDO did not log native state"; exit 1
    fi
    if ! python3 - "$arrangement_placement_target_line" "$arrangement_placement_restored_line" <<'PY'
import re, sys
target_line,restored_line=sys.argv[1:]
pat=re.compile(r"UI_ARRANGE_PLACEMENT_STATE: action=(TARGET|UNDO) track=(\d+) tick=(\d+) placements=(.*?) pattern=(.*)$")
base=pat.search(target_line); restored=pat.search(restored_line)
if not base or not restored or base.group(4)!=restored.group(4) or base.group(5)!=restored.group(5):
    raise AssertionError("final UNDO did not restore exact original Arrangement placement state")
print("Arrangement clip split → UNDO → REDO → UNDO restored exact native placement state.")
PY
    then
      echo "ERROR: final placement state differs from its original baseline"; exit 1
    fi
    echo "Arrangement clip SPLIT → UNDO → REDO → UNDO passed."

    # Exercise a complete placement lifecycle using the real Function Bar. The split
    # creates two source ranges; delete the right range to make a gap, place a short
    # Pattern clip in that gap, and move it one grid later. Cleanup is native Undo.
    ARRANGE_PLACEMENT_FLOW_SPLIT_PNG="/tmp/mpc-groovebox-arrange-placement-flow-split.png"
    ARRANGE_PLACEMENT_FLOW_SPLIT_XML="/tmp/mpc-groovebox-arrange-placement-flow-split-uiautomator.xml"
    ARRANGE_PLACEMENT_FLOW_SPLIT_REMOTE="/sdcard/mpc-groovebox-arrange-placement-flow-split-uiautomator.xml"
    if ! tap_visible_description_early "$ARRANGE_PLACEMENT_RESTORED_XML" \
      "Arrangement split Pattern placement at target tick $arrange_cursor_tick"; then
      echo "ERROR: Pattern placement lifecycle SPLIT was not actionable"; exit 1
    fi
    sleep 0.3
    if ! dump_device_ui "$ARRANGE_PLACEMENT_FLOW_SPLIT_REMOTE" "$ARRANGE_PLACEMENT_FLOW_SPLIT_XML"; then
      echo "ERROR: lifecycle SPLIT hierarchy capture failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_PLACEMENT_FLOW_SPLIT_PNG"; then
      echo "ERROR: lifecycle SPLIT screenshot capture failed"; exit 1
    fi
    arrangement_placement_flow_split_line="$(arrangement_placement_state_line SPLIT)"
    if [[ -z "$arrangement_placement_flow_split_line" ]]; then
      echo "ERROR: lifecycle SPLIT did not log native placement state"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_PLACEMENT_FLOW_SPLIT_XML" \
      "Arrangement delete Pattern placement at tick $arrange_cursor_tick"; then
      echo "ERROR: Pattern placement CLIP DEL action not actionable at the clip start"; exit 1
    fi
    sleep 0.3
    ARRANGE_PLACEMENT_FLOW_DELETE_PNG="/tmp/mpc-groovebox-arrange-placement-flow-delete.png"
    ARRANGE_PLACEMENT_FLOW_DELETE_XML="/tmp/mpc-groovebox-arrange-placement-flow-delete-uiautomator.xml"
    ARRANGE_PLACEMENT_FLOW_DELETE_REMOTE="/sdcard/mpc-groovebox-arrange-placement-flow-delete-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_PLACEMENT_FLOW_DELETE_REMOTE" "$ARRANGE_PLACEMENT_FLOW_DELETE_XML"; then
      echo "ERROR: lifecycle clip-delete hierarchy capture failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_PLACEMENT_FLOW_DELETE_PNG"; then
      echo "ERROR: lifecycle clip-delete screenshot capture failed"; exit 1
    fi
    arrangement_placement_flow_delete_line="$(arrangement_placement_state_line DELETE)"
    if [[ -z "$arrangement_placement_flow_delete_line" ]]; then
      echo "ERROR: deleting a Pattern placement did not log native state"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_PLACEMENT_FLOW_DELETE_XML" \
      "Arrangement place Pattern at empty target tick $arrange_cursor_tick"; then
      echo "ERROR: Pattern PLACE action not actionable in the new gap"; exit 1
    fi
    sleep 0.3
    ARRANGE_PLACEMENT_FLOW_PLACE_PNG="/tmp/mpc-groovebox-arrange-placement-flow-place.png"
    ARRANGE_PLACEMENT_FLOW_PLACE_XML="/tmp/mpc-groovebox-arrange-placement-flow-place-uiautomator.xml"
    ARRANGE_PLACEMENT_FLOW_PLACE_REMOTE="/sdcard/mpc-groovebox-arrange-placement-flow-place-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_PLACEMENT_FLOW_PLACE_REMOTE" "$ARRANGE_PLACEMENT_FLOW_PLACE_XML"; then
      echo "ERROR: lifecycle PLACE hierarchy capture failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_PLACEMENT_FLOW_PLACE_PNG"; then
      echo "ERROR: lifecycle PLACE screenshot capture failed"; exit 1
    fi
    arrangement_placement_flow_place_line="$(arrangement_placement_state_line PLACE)"
    if [[ -z "$arrangement_placement_flow_place_line" ]]; then
      echo "ERROR: native Pattern placement ADD did not log state"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_PLACEMENT_FLOW_PLACE_XML" \
      "Arrangement move Pattern placement at tick $arrange_cursor_tick one grid later"; then
      echo "ERROR: new Pattern placement CLIP + GRID action not actionable"; exit 1
    fi
    sleep 0.3
    ARRANGE_PLACEMENT_FLOW_MOVE_PNG="/tmp/mpc-groovebox-arrange-placement-flow-move.png"
    ARRANGE_PLACEMENT_FLOW_MOVE_XML="/tmp/mpc-groovebox-arrange-placement-flow-move-uiautomator.xml"
    ARRANGE_PLACEMENT_FLOW_MOVE_REMOTE="/sdcard/mpc-groovebox-arrange-placement-flow-move-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_PLACEMENT_FLOW_MOVE_REMOTE" "$ARRANGE_PLACEMENT_FLOW_MOVE_XML"; then
      echo "ERROR: lifecycle MOVE hierarchy capture failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_PLACEMENT_FLOW_MOVE_PNG"; then
      echo "ERROR: lifecycle MOVE screenshot capture failed"; exit 1
    fi
    arrangement_placement_flow_move_line="$(arrangement_placement_state_line MOVE)"
    if [[ -z "$arrangement_placement_flow_move_line" ]]; then
      echo "ERROR: native Pattern placement MOVE did not log state"; exit 1
    fi

    if ! python3 - "$arrangement_placement_target_line" "$arrangement_placement_flow_split_line" \
      "$arrangement_placement_flow_delete_line" "$arrangement_placement_flow_place_line" \
      "$arrangement_placement_flow_move_line" "$ARRANGE_PLACEMENT_FLOW_SPLIT_XML" \
      "$ARRANGE_PLACEMENT_FLOW_DELETE_XML" "$ARRANGE_PLACEMENT_FLOW_PLACE_XML" \
      "$ARRANGE_PLACEMENT_FLOW_MOVE_XML" "$arrange_event_track" "$arrange_cursor_tick" \
      "$arrange_event_grid" <<'PY'
import re, sys, xml.etree.ElementTree as ET
baseline_line,split_line,delete_line,place_line,move_line,split_xml,delete_xml,place_xml,move_xml,track_s,tick_s,grid_s=sys.argv[1:]
track,tick,grid=map(int,(track_s,tick_s,grid_s))
pat=re.compile(r"UI_ARRANGE_PLACEMENT_STATE: action=(TARGET|SPLIT|DELETE|PLACE|MOVE) track=(\d+) tick=(\d+) placements=(.*?) pattern=(.*)$")
def decode(line,action):
    m=pat.search(line)
    if not m or m.group(1)!=action or int(m.group(2))!=track:
        raise AssertionError(f"missing/invalid {action} state: {line!r}")
    items=[]
    for item in m.group(4).split(";"):
        if not item: continue
        fields=item.split(",")
        if len(fields)!=5: raise AssertionError(f"invalid placement record: {item!r}")
        items.append((fields[0],fields[1],*map(int,fields[2:])))
    return int(m.group(3)),items,m.group(5)
base_tick,base,base_pattern=decode(baseline_line,"TARGET")
split_tick,split,split_pattern=decode(split_line,"SPLIT")
delete_tick,deleted,delete_pattern=decode(delete_line,"DELETE")
place_tick,placed,place_pattern=decode(place_line,"PLACE")
move_tick,moved,move_pattern=decode(move_line,"MOVE")
if base_tick!=tick or any(x!=base_pattern for x in (split_pattern,delete_pattern,place_pattern,move_pattern)):
    raise AssertionError("clip edits changed underlying Pattern notes or the baseline cursor tick")
if len(base)!=1 or len(split)!=2:
    raise AssertionError(f"SPLIT must turn the single baseline placement into two: {base!r} -> {split!r}")
original=base[0]
left=[p for p in split if p[2]==original[2] and p[3]==tick-original[2]]
right=[p for p in split if p[2]==tick and p[3]==original[2]+original[3]-tick]
if len(left)!=1 or len(right)!=1 or left[0][1]!=original[1] or right[0][1]!=original[1]:
    raise AssertionError(f"split ranges do not preserve their source Pattern: {split!r}")
if len(deleted)!=1 or deleted[0]!=left[0]:
    raise AssertionError(f"CLIP DEL must remove only the right split placement: {deleted!r}")
if len(placed)!=2 or not any(p==left[0] for p in placed):
    raise AssertionError(f"PLACE must retain the left placement: {placed!r}")
new_clips=[p for p in placed if p!=left[0]]
if len(new_clips)!=1:
    raise AssertionError(f"PLACE should create exactly one new placement: {placed!r}")
new_clip=new_clips[0]
pattern_length_text, separator, _source_events_text = base_pattern.partition("|")
if not separator:
    raise AssertionError(f"invalid native Pattern source data: {base_pattern!r}")
pattern_length=int(pattern_length_text)
expected_clip_length=min(pattern_length, grid*2)
if new_clip[1]!=original[1] or new_clip[2]!=tick or new_clip[3]!=expected_clip_length or new_clip[4]!=0:
    raise AssertionError(f"new placement should use the source Pattern's leading range: {new_clip!r}")
if len(moved)!=2 or left[0] not in moved:
    raise AssertionError(f"MOVE changed other placements: {moved!r}")
moved_clips=[p for p in moved if p!=left[0]]
if len(moved_clips)!=1 or moved_clips[0]!=(new_clip[0],new_clip[1],tick+grid,expected_clip_length,new_clip[4]):
    raise AssertionError(f"CLIP + GRID must move only the new clip by exactly one grid: {moved_clips!r}")
# After SPLIT, the cursor sits on the new shared boundary. It must remain
# accessible, but SPLIT must disappear there because the target is no longer
# strictly inside a single placement. Assert that state explicitly rather than
# requiring an invalid second split action on the boundary.
split_nodes=list(ET.parse(split_xml).getroot().iter("node"))
cursor=[n for n in split_nodes
        if n.attrib.get("content-desc")==f"Arrangement paste target Track {track+1:02d} tick {tick}"]
if len(cursor)!=1 or cursor[0].attrib.get("text")!=f"PASTE TARGET • TRACK {track+1:02d} • TICK {tick}":
    raise AssertionError("SPLIT state lost its accessible insertion target")
split_actions=[n for n in split_nodes
               if (n.attrib.get("content-desc") or "").startswith("Arrangement split Pattern placement at target tick")]
if any(n.attrib.get("enabled")=="true" for n in split_actions):
    raise AssertionError("SPLIT remained enabled at a shared placement boundary")
undo=[n for n in split_nodes
      if n.attrib.get("content-desc")=="Arrangement undo latest sequencer edit"]
if len(undo)!=1 or undo[0].attrib.get("enabled")!="true":
    raise AssertionError("SPLIT result should expose native Undo")
# DELETE leaves the insertion cursor in an empty gap: the clip-specific
# delete action must be disabled/renamed to "at target", while PLACE is enabled.
delete_nodes=list(ET.parse(delete_xml).getroot().iter("node"))
delete_cursor=[n for n in delete_nodes
               if n.attrib.get("content-desc")==f"Arrangement paste target Track {track+1:02d} tick {tick}"]
if len(delete_cursor)!=1 or delete_cursor[0].attrib.get("text")!=f"PASTE TARGET • TRACK {track+1:02d} • TICK {tick}":
    raise AssertionError("DELETE state lost its accessible insertion target")
delete_action=[n for n in delete_nodes
               if n.attrib.get("content-desc")=="Arrangement delete Pattern placement at target"]
if len(delete_action)!=1 or delete_action[0].attrib.get("enabled")!="false":
    raise AssertionError("DELETE must be disabled while the cursor is in the gap it created")
place_gap=[n for n in delete_nodes
           if n.attrib.get("content-desc")==f"Arrangement place Pattern at empty target tick {tick}"]
if len(place_gap)!=1 or place_gap[0].attrib.get("enabled")!="true":
    raise AssertionError("the empty post-DELETE gap must expose an enabled PLACE action")
for path,description in [
    (place_xml, f"Arrangement duplicate Pattern placement at tick {tick} one clip later"),
    (move_xml, "Arrangement move Pattern placement at tick")]:
    nodes=list(ET.parse(path).getroot().iter("node"))
    matching=[n for n in nodes if (n.attrib.get("content-desc") or "").startswith(description)]
    if not matching: raise AssertionError(f"{description} state lacks its accessible action/target")
print("Arrangement Pattern clip SPLIT → DELETE → PLACE → MOVE changed only placement state; Pattern events remained identical.")
PY
    then
      echo "ERROR: Pattern placement create/move native state or accessibility validation failed"; exit 1
    fi

    # Duplicate the selected Arrangement Pattern clip into its immediately adjacent
    # free range. The source Pattern and source placement must remain untouched.
    arrangement_placement_duplicate_source_tick="$(sed -nE 's/.*UI_ARRANGE_PLACEMENT_STATE: action=MOVE track=[0-9]+ tick=([0-9]+).*/\1/p' <<<"$arrangement_placement_flow_move_line")"
    if [[ -z "$arrangement_placement_duplicate_source_tick" ]]; then
      echo "ERROR: could not derive the live Pattern clip start for CLIP DUP"; exit 1
    fi
    if ! tap_visible_description_early "$ARRANGE_PLACEMENT_FLOW_MOVE_XML" \
      "Arrangement duplicate Pattern placement at tick $arrangement_placement_duplicate_source_tick one clip later"; then
      echo "ERROR: Arrangement CLIP DUP Function Bar action not actionable"; exit 1
    fi
    sleep 0.3
    ARRANGE_PLACEMENT_DUP_PNG="/tmp/mpc-groovebox-arrange-placement-duplicate.png"
    ARRANGE_PLACEMENT_DUP_XML="/tmp/mpc-groovebox-arrange-placement-duplicate-uiautomator.xml"
    ARRANGE_PLACEMENT_DUP_REMOTE="/sdcard/mpc-groovebox-arrange-placement-duplicate-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_PLACEMENT_DUP_REMOTE" "$ARRANGE_PLACEMENT_DUP_XML"; then
      echo "ERROR: Arrangement CLIP DUP hierarchy capture failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_PLACEMENT_DUP_PNG"; then
      echo "ERROR: Arrangement CLIP DUP screenshot capture failed"; exit 1
    fi
    arrangement_placement_duplicate_line="$(arrangement_placement_state_line DUPLICATE)"
    if [[ -z "$arrangement_placement_duplicate_line" ]]; then
      echo "ERROR: native Arrangement CLIP DUP did not log placement state"; exit 1
    fi
    if ! python3 - "$arrangement_placement_flow_move_line" "$arrangement_placement_duplicate_line" \
      "$ARRANGE_PLACEMENT_DUP_XML" "$arrange_event_track" \
      "$arrangement_placement_duplicate_source_tick" <<'PY'
import re, sys, xml.etree.ElementTree as ET
before_line, duplicate_line, hierarchy_path, track_s, cursor_s = sys.argv[1:]
track, cursor = map(int, (track_s, cursor_s))
pattern = re.compile(
    r"UI_ARRANGE_PLACEMENT_STATE: action=(MOVE|DUPLICATE) track=(\d+) "
    r"tick=(\d+) placements=(.*?) pattern=(.*)$")
def decode(line, action):
    match = pattern.search(line)
    if not match or match.group(1) != action:
        raise AssertionError(f"missing {action} native placement state: {line!r}")
    if tuple(map(int, match.group(2,3))) != (track,cursor):
        raise AssertionError(f"{action} did not preserve the source cursor/track")
    placement_list = []
    for item in match.group(4).split(";"):
        if not item: continue
        fields = item.split(",")
        if len(fields) != 5: raise AssertionError(f"invalid clip record: {item!r}")
        placement_list.append((fields[0],fields[1],*map(int,fields[2:])))
    length_s, sep, events = match.group(5).partition("|")
    if not sep: raise AssertionError(f"invalid source Pattern state: {match.group(5)!r}")
    return match.group(3), int(length_s), placement_list, match.group(5)
_, before_length, before_clips, before_pattern = decode(before_line,"MOVE")
_, duplicate_length, clips, duplicate_pattern = decode(duplicate_line,"DUPLICATE")
if before_length != duplicate_length or before_pattern != duplicate_pattern:
    raise AssertionError("CLIP DUP altered underlying Pattern data or sequence length")
sources=[clip for clip in before_clips if clip[2] == cursor]
if len(sources) != 1:
    raise AssertionError(f"cursor should identify one source clip starting at {cursor}: {before_clips!r}")
source=sources[0]
expected_start=source[2]+source[3]
if len(clips) != len(before_clips)+1:
    raise AssertionError(f"CLIP DUP must add exactly one placement: {before_clips!r} -> {clips!r}")
clones=[clip for clip in clips if clip[2] == expected_start and clip[0] != source[0]]
if len(clones) != 1:
    raise AssertionError(f"CLIP DUP must add one adjacent placement at {expected_start}: {clips!r}")
clone=clones[0]
if clone[1] != source[1] or clone[3:] != source[3:]:
    raise AssertionError(f"duplicate clip did not preserve source Pattern/length/source offset: {source!r} -> {clone!r}")
if [clip for clip in clips if clip[0] != clone[0]] != before_clips:
    raise AssertionError("CLIP DUP changed or reordered existing clip identities/ranges")
nodes=list(ET.parse(hierarchy_path).getroot().iter("node"))
dup=[node for node in nodes if node.attrib.get("content-desc")==
     f"Arrangement duplicate Pattern placement at tick {cursor} one clip later"]
if len(dup)!=1 or dup[0].attrib.get("enabled")!="false":
    raise AssertionError("CLIP DUP should disable when its immediately adjacent destination is now occupied")
undo=[node for node in nodes if node.attrib.get("content-desc")=="Arrangement undo latest sequencer edit"]
redo=[node for node in nodes if node.attrib.get("content-desc")=="Arrangement redo latest sequencer edit"]
if len(undo)!=1 or undo[0].attrib.get("enabled")!="true":
    raise AssertionError("CLIP DUP did not expose native Undo")
if len(redo)!=1 or redo[0].attrib.get("enabled")!="false":
    raise AssertionError("CLIP DUP unexpectedly exposes Redo before Undo")
print(f"Arrangement CLIP DUP added one adjacent clip at tick {expected_start}; source clip and MIDI Pattern were unchanged.")
PY
    then
      echo "ERROR: Arrangement CLIP DUP native-state or accessibility validation failed"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_PLACEMENT_DUP_XML" \
      "Arrangement undo latest sequencer edit"; then
      echo "ERROR: Arrangement CLIP DUP UNDO action not available"; exit 1
    fi
    sleep 0.3
    ARRANGE_PLACEMENT_DUP_UNDO_PNG="/tmp/mpc-groovebox-arrange-placement-duplicate-undo.png"
    ARRANGE_PLACEMENT_DUP_UNDO_XML="/tmp/mpc-groovebox-arrange-placement-duplicate-undo-uiautomator.xml"
    ARRANGE_PLACEMENT_DUP_UNDO_REMOTE="/sdcard/mpc-groovebox-arrange-placement-duplicate-undo-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_PLACEMENT_DUP_UNDO_REMOTE" "$ARRANGE_PLACEMENT_DUP_UNDO_XML"; then
      echo "ERROR: Arrangement CLIP DUP UNDO hierarchy capture failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_PLACEMENT_DUP_UNDO_PNG"; then
      echo "ERROR: Arrangement CLIP DUP UNDO screenshot capture failed"; exit 1
    fi
    arrangement_placement_duplicate_undo_line="$(arrangement_placement_state_line UNDO)"
    if [[ -z "$arrangement_placement_duplicate_undo_line" ]]; then
      echo "ERROR: CLIP DUP UNDO did not log native state"; exit 1
    fi
    if ! python3 - "$arrangement_placement_flow_move_line" "$arrangement_placement_duplicate_undo_line" \
      "$ARRANGE_PLACEMENT_DUP_UNDO_XML" "$arrange_event_track" \
      "$arrangement_placement_duplicate_source_tick" <<'PY'
import re, sys, xml.etree.ElementTree as ET
before_line, undo_line, hierarchy_path, track_s, cursor_s = sys.argv[1:]
track, cursor = map(int,(track_s,cursor_s))
pattern=re.compile(r"UI_ARRANGE_PLACEMENT_STATE: action=(MOVE|UNDO) track=(\d+) tick=(\d+) placements=(.*?) pattern=(.*)$")
a=pattern.search(before_line); b=pattern.search(undo_line)
if not a or not b or a.group(1)!="MOVE" or b.group(1)!="UNDO":
    raise AssertionError("missing placement state for CLIP DUP Undo")
if tuple(map(int,a.group(2,3)))!=(track,cursor) or tuple(map(int,b.group(2,3)))!=(track,cursor):
    raise AssertionError("CLIP DUP Undo changed the source cursor")
if a.group(4)!=b.group(4) or a.group(5)!=b.group(5):
    raise AssertionError("CLIP DUP Undo did not restore exact prior placement/Pattern serialization")
nodes=list(ET.parse(hierarchy_path).getroot().iter("node"))
dup=[n for n in nodes if n.attrib.get("content-desc")==f"Arrangement duplicate Pattern placement at tick {cursor} one clip later"]
if len(dup)!=1 or dup[0].attrib.get("enabled")!="true":
    raise AssertionError("CLIP DUP Undo should restore the enabled duplicate action")
redo=[n for n in nodes if n.attrib.get("content-desc")=="Arrangement redo latest sequencer edit"]
if len(redo)!=1 or redo[0].attrib.get("enabled")!="true":
    raise AssertionError("CLIP DUP Undo did not expose native Redo")
print("Arrangement CLIP DUP UNDO restored exact pre-duplicate clip state.")
PY
    then
      echo "ERROR: CLIP DUP UNDO did not restore the pre-duplicate state"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_PLACEMENT_DUP_UNDO_XML" \
      "Arrangement redo latest sequencer edit"; then
      echo "ERROR: Arrangement CLIP DUP REDO action not available"; exit 1
    fi
    sleep 0.3
    ARRANGE_PLACEMENT_DUP_REDO_PNG="/tmp/mpc-groovebox-arrange-placement-duplicate-redo.png"
    ARRANGE_PLACEMENT_DUP_REDO_XML="/tmp/mpc-groovebox-arrange-placement-duplicate-redo-uiautomator.xml"
    ARRANGE_PLACEMENT_DUP_REDO_REMOTE="/sdcard/mpc-groovebox-arrange-placement-duplicate-redo-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_PLACEMENT_DUP_REDO_REMOTE" "$ARRANGE_PLACEMENT_DUP_REDO_XML"; then
      echo "ERROR: Arrangement CLIP DUP REDO hierarchy capture failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_PLACEMENT_DUP_REDO_PNG"; then
      echo "ERROR: Arrangement CLIP DUP REDO screenshot capture failed"; exit 1
    fi
    arrangement_placement_duplicate_redo_line="$(arrangement_placement_state_line REDO)"
    if [[ -z "$arrangement_placement_duplicate_redo_line" ]]; then
      echo "ERROR: CLIP DUP REDO did not log native state"; exit 1
    fi
    if ! python3 - "$arrangement_placement_duplicate_line" "$arrangement_placement_duplicate_redo_line" <<'PY'
import re, sys
first,redo=sys.argv[1:]
pattern=re.compile(r"UI_ARRANGE_PLACEMENT_STATE: action=(DUPLICATE|REDO) track=(\d+) tick=(\d+) placements=(.*?) pattern=(.*)$")
a=pattern.search(first); b=pattern.search(redo)
if not a or not b or a.group(4)!=b.group(4) or a.group(5)!=b.group(5):
    raise AssertionError("CLIP DUP Redo did not reproduce exact duplicate placement state")
print("Arrangement CLIP DUP REDO reproduced the exact duplicate clip state.")
PY
    then
      echo "ERROR: CLIP DUP REDO changed duplicate clip state"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_PLACEMENT_DUP_REDO_XML" \
      "Arrangement undo latest sequencer edit"; then
      echo "ERROR: final CLIP DUP cleanup UNDO action not available"; exit 1
    fi
    sleep 0.3
    ARRANGE_PLACEMENT_DUP_RESTORED_PNG="/tmp/mpc-groovebox-arrange-placement-duplicate-restored.png"
    ARRANGE_PLACEMENT_DUP_RESTORED_XML="/tmp/mpc-groovebox-arrange-placement-duplicate-restored-uiautomator.xml"
    ARRANGE_PLACEMENT_DUP_RESTORED_REMOTE="/sdcard/mpc-groovebox-arrange-placement-duplicate-restored-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_PLACEMENT_DUP_RESTORED_REMOTE" "$ARRANGE_PLACEMENT_DUP_RESTORED_XML"; then
      echo "ERROR: final CLIP DUP cleanup hierarchy capture failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_PLACEMENT_DUP_RESTORED_PNG"; then
      echo "ERROR: final CLIP DUP cleanup screenshot capture failed"; exit 1
    fi
    arrangement_placement_duplicate_restored_line="$(arrangement_placement_state_line UNDO)"
    if [[ -z "$arrangement_placement_duplicate_restored_line" ]]; then
      echo "ERROR: final CLIP DUP cleanup did not log native state"; exit 1
    fi
    if ! python3 - "$arrangement_placement_flow_move_line" "$arrangement_placement_duplicate_restored_line" <<'PY'
import re, sys
first,undo=sys.argv[1:]
pattern=re.compile(r"UI_ARRANGE_PLACEMENT_STATE: action=(MOVE|UNDO) track=(\d+) tick=(\d+) placements=(.*?) pattern=(.*)$")
a=pattern.search(first); b=pattern.search(undo)
if not a or not b or a.group(4)!=b.group(4) or a.group(5)!=b.group(5):
    raise AssertionError("final CLIP DUP Undo did not restore exact pre-duplicate state")
print("Arrangement CLIP DUP → UNDO → REDO → UNDO restored exact source placement state.")
PY
    then
      echo "ERROR: final CLIP DUP cleanup did not restore exact state"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_PLACEMENT_FLOW_MOVE_XML" \
      "Arrangement undo latest sequencer edit"; then
      echo "ERROR: clip MOVE UNDO action not available"; exit 1
    fi
    sleep 0.25
    ARRANGE_PLACEMENT_FLOW_UNDO_PNG="/tmp/mpc-groovebox-arrange-placement-flow-undo.png"
    ARRANGE_PLACEMENT_FLOW_UNDO_XML="/tmp/mpc-groovebox-arrange-placement-flow-undo-uiautomator.xml"
    ARRANGE_PLACEMENT_FLOW_UNDO_REMOTE="/sdcard/mpc-groovebox-arrange-placement-flow-undo-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_PLACEMENT_FLOW_UNDO_REMOTE" "$ARRANGE_PLACEMENT_FLOW_UNDO_XML"; then
      echo "ERROR: clip MOVE UNDO hierarchy capture failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_PLACEMENT_FLOW_UNDO_PNG"; then
      echo "ERROR: clip MOVE UNDO screenshot capture failed"; exit 1
    fi
    arrangement_placement_flow_undo_line="$(arrangement_placement_state_line UNDO)"
    if [[ -z "$arrangement_placement_flow_undo_line" ]]; then
      echo "ERROR: clip MOVE UNDO did not log native placement state"; exit 1
    fi
    if ! python3 - "$arrangement_placement_flow_place_line" "$arrangement_placement_flow_undo_line" \
      "$ARRANGE_PLACEMENT_FLOW_UNDO_XML" "$arrange_event_track" "$arrange_cursor_tick" <<'PY'
import re, sys, xml.etree.ElementTree as ET
place_line,undo_line,path,track_s,tick_s=sys.argv[1:]
pat=re.compile(r"UI_ARRANGE_PLACEMENT_STATE: action=(PLACE|UNDO) track=(\d+) tick=(\d+) placements=(.*?) pattern=(.*)$")
place=pat.search(place_line); undo=pat.search(undo_line)
if not place or not undo or place.group(5)!=undo.group(5) or place.group(4)!=undo.group(4):
    raise AssertionError("Undo of CLIP MOVE did not restore exact PLACE placement state")
if (int(undo.group(2)),int(undo.group(3)))!=(int(track_s),int(tick_s)):
    raise AssertionError("Undo of CLIP MOVE did not return the cursor to the clip's original start")
nodes=list(ET.parse(path).getroot().iter("node"))
move=[n for n in nodes if n.attrib.get("content-desc")==f"Arrangement move Pattern placement at tick {tick_s} one grid later"]
if len(move)!=1 or move[0].attrib.get("enabled")!="true":
    raise AssertionError("Undo should restore the enabled one-grid placement move")
print("Arrangement Pattern placement MOVE UNDO restored the exact placed clip and source cursor.")
PY
    then
      echo "ERROR: clip MOVE UNDO did not restore its exact placement state"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_PLACEMENT_FLOW_UNDO_XML" \
      "Arrangement redo latest sequencer edit"; then
      echo "ERROR: clip MOVE REDO action not available"; exit 1
    fi
    sleep 0.25
    ARRANGE_PLACEMENT_FLOW_REDO_PNG="/tmp/mpc-groovebox-arrange-placement-flow-redo.png"
    ARRANGE_PLACEMENT_FLOW_REDO_XML="/tmp/mpc-groovebox-arrange-placement-flow-redo-uiautomator.xml"
    ARRANGE_PLACEMENT_FLOW_REDO_REMOTE="/sdcard/mpc-groovebox-arrange-placement-flow-redo-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_PLACEMENT_FLOW_REDO_REMOTE" "$ARRANGE_PLACEMENT_FLOW_REDO_XML"; then
      echo "ERROR: clip MOVE REDO hierarchy capture failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_PLACEMENT_FLOW_REDO_PNG"; then
      echo "ERROR: clip MOVE REDO screenshot capture failed"; exit 1
    fi
    arrangement_placement_flow_redo_line="$(arrangement_placement_state_line REDO)"
    if [[ -z "$arrangement_placement_flow_redo_line" ]]; then
      echo "ERROR: clip MOVE REDO did not log native placement state"; exit 1
    fi
    if ! python3 - "$arrangement_placement_flow_move_line" "$arrangement_placement_flow_redo_line" <<'PY'
import re, sys
before,redo=sys.argv[1:]
pat=re.compile(r"UI_ARRANGE_PLACEMENT_STATE: action=(MOVE|REDO) track=(\d+) tick=(\d+) placements=(.*?) pattern=(.*)$")
a=pat.search(before); b=pat.search(redo)
if not a or not b or a.group(4)!=b.group(4) or a.group(5)!=b.group(5):
    raise AssertionError("clip MOVE REDO did not reproduce exact placement and Pattern state")
print("Arrangement Pattern placement MOVE REDO reproduced the exact moved clip state.")
PY
    then
      echo "ERROR: clip MOVE REDO changed placement/Pattern state"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_PLACEMENT_FLOW_REDO_XML" \
      "Arrangement undo latest sequencer edit"; then
      echo "ERROR: final clip MOVE cleanup UNDO unavailable"; exit 1
    fi
    sleep 0.25
    ARRANGE_PLACEMENT_FLOW_MOVE_RESTORED_PNG="/tmp/mpc-groovebox-arrange-placement-flow-move-restored.png"
    ARRANGE_PLACEMENT_FLOW_MOVE_RESTORED_XML="/tmp/mpc-groovebox-arrange-placement-flow-move-restored-uiautomator.xml"
    ARRANGE_PLACEMENT_FLOW_MOVE_RESTORED_REMOTE="/sdcard/mpc-groovebox-arrange-placement-flow-move-restored-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_PLACEMENT_FLOW_MOVE_RESTORED_REMOTE" "$ARRANGE_PLACEMENT_FLOW_MOVE_RESTORED_XML"; then
      echo "ERROR: final clip MOVE restoration hierarchy capture failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_PLACEMENT_FLOW_MOVE_RESTORED_PNG"; then
      echo "ERROR: final clip MOVE restoration screenshot capture failed"; exit 1
    fi
    arrangement_placement_flow_move_restored_line="$(arrangement_placement_state_line UNDO)"
    if [[ -z "$arrangement_placement_flow_move_restored_line" ]]; then
      echo "ERROR: final clip MOVE restoration did not log native state"; exit 1
    fi
    if ! python3 - "$arrangement_placement_flow_place_line" "$arrangement_placement_flow_move_restored_line" <<'PY'
import re, sys
place,undo=sys.argv[1:]
pat=re.compile(r"UI_ARRANGE_PLACEMENT_STATE: action=(PLACE|UNDO) track=(\d+) tick=(\d+) placements=(.*?) pattern=(.*)$")
a=pat.search(place); b=pat.search(undo)
if not a or not b or a.group(4)!=b.group(4) or a.group(5)!=b.group(5):
    raise AssertionError("final MOVE UNDO did not return to exact placed clip state")
print("Arrangement Pattern placement final MOVE UNDO returned to the exact placed state.")
PY
    then
      echo "ERROR: final clip MOVE UNDO state mismatch"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_PLACEMENT_FLOW_MOVE_RESTORED_XML" \
      "Arrangement undo latest sequencer edit"; then
      echo "ERROR: PLACE UNDO action not available"; exit 1
    fi
    sleep 0.25
    ARRANGE_PLACEMENT_FLOW_PLACE_RESTORED_PNG="/tmp/mpc-groovebox-arrange-placement-flow-place-restored.png"
    ARRANGE_PLACEMENT_FLOW_PLACE_RESTORED_XML="/tmp/mpc-groovebox-arrange-placement-flow-place-restored-uiautomator.xml"
    ARRANGE_PLACEMENT_FLOW_PLACE_RESTORED_REMOTE="/sdcard/mpc-groovebox-arrange-placement-flow-place-restored-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_PLACEMENT_FLOW_PLACE_RESTORED_REMOTE" "$ARRANGE_PLACEMENT_FLOW_PLACE_RESTORED_XML"; then
      echo "ERROR: PLACE UNDO hierarchy capture failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_PLACEMENT_FLOW_PLACE_RESTORED_PNG"; then
      echo "ERROR: PLACE UNDO screenshot capture failed"; exit 1
    fi
    arrangement_placement_flow_place_restored_line="$(arrangement_placement_state_line UNDO)"
    if [[ -z "$arrangement_placement_flow_place_restored_line" ]]; then
      echo "ERROR: PLACE UNDO did not log native placement state"; exit 1
    fi
    if ! python3 - "$arrangement_placement_flow_delete_line" "$arrangement_placement_flow_place_restored_line" <<'PY'
import re, sys
deleted,undo=sys.argv[1:]
pat=re.compile(r"UI_ARRANGE_PLACEMENT_STATE: action=(DELETE|UNDO) track=(\d+) tick=(\d+) placements=(.*?) pattern=(.*)$")
a=pat.search(deleted); b=pat.search(undo)
if not a or not b or a.group(4)!=b.group(4) or a.group(5)!=b.group(5):
    raise AssertionError("Undo of PLACE did not restore the pre-PLACE gap state")
print("Arrangement Pattern placement PLACE UNDO removed only the temporary new clip.")
PY
    then
      echo "ERROR: PLACE UNDO did not restore the gap state"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_PLACEMENT_FLOW_PLACE_RESTORED_XML" \
      "Arrangement undo latest sequencer edit"; then
      echo "ERROR: CLIP DEL UNDO action not available"; exit 1
    fi
    sleep 0.25
    ARRANGE_PLACEMENT_FLOW_DELETE_RESTORED_PNG="/tmp/mpc-groovebox-arrange-placement-flow-delete-restored.png"
    ARRANGE_PLACEMENT_FLOW_DELETE_RESTORED_XML="/tmp/mpc-groovebox-arrange-placement-flow-delete-restored-uiautomator.xml"
    ARRANGE_PLACEMENT_FLOW_DELETE_RESTORED_REMOTE="/sdcard/mpc-groovebox-arrange-placement-flow-delete-restored-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_PLACEMENT_FLOW_DELETE_RESTORED_REMOTE" "$ARRANGE_PLACEMENT_FLOW_DELETE_RESTORED_XML"; then
      echo "ERROR: CLIP DEL UNDO hierarchy capture failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_PLACEMENT_FLOW_DELETE_RESTORED_PNG"; then
      echo "ERROR: CLIP DEL UNDO screenshot capture failed"; exit 1
    fi
    arrangement_placement_flow_delete_restored_line="$(arrangement_placement_state_line UNDO)"
    if [[ -z "$arrangement_placement_flow_delete_restored_line" ]]; then
      echo "ERROR: CLIP DEL UNDO did not log native placement state"; exit 1
    fi
    if ! python3 - "$arrangement_placement_flow_split_line" "$arrangement_placement_flow_delete_restored_line" <<'PY'
import re, sys
split,undo=sys.argv[1:]
pat=re.compile(r"UI_ARRANGE_PLACEMENT_STATE: action=(SPLIT|UNDO) track=(\d+) tick=(\d+) placements=(.*?) pattern=(.*)$")
a=pat.search(split); b=pat.search(undo)
if not a or not b or a.group(4)!=b.group(4) or a.group(5)!=b.group(5):
    raise AssertionError("Undo of CLIP DEL did not restore exact split placement state")
print("Arrangement Pattern placement DELETE UNDO restored the exact split clip state.")
PY
    then
      echo "ERROR: CLIP DEL UNDO did not restore the split state"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_PLACEMENT_FLOW_DELETE_RESTORED_XML" \
      "Arrangement undo latest sequencer edit"; then
      echo "ERROR: initial lifecycle SPLIT cleanup UNDO unavailable"; exit 1
    fi
    sleep 0.25
    ARRANGE_PLACEMENT_FLOW_FINAL_PNG="/tmp/mpc-groovebox-arrange-placement-flow-final.png"
    ARRANGE_PLACEMENT_FLOW_FINAL_XML="/tmp/mpc-groovebox-arrange-placement-flow-final-uiautomator.xml"
    ARRANGE_PLACEMENT_FLOW_FINAL_REMOTE="/sdcard/mpc-groovebox-arrange-placement-flow-final-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_PLACEMENT_FLOW_FINAL_REMOTE" "$ARRANGE_PLACEMENT_FLOW_FINAL_XML"; then
      echo "ERROR: final placement lifecycle hierarchy capture failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_PLACEMENT_FLOW_FINAL_PNG"; then
      echo "ERROR: final placement lifecycle screenshot capture failed"; exit 1
    fi
    arrangement_placement_flow_final_line="$(arrangement_placement_state_line UNDO)"
    if [[ -z "$arrangement_placement_flow_final_line" ]]; then
      echo "ERROR: final placement lifecycle restoration did not log native state"; exit 1
    fi
    if ! python3 - "$arrangement_placement_target_line" "$arrangement_placement_flow_final_line" \
      "$ARRANGE_PLACEMENT_FLOW_FINAL_XML" "$arrange_event_track" "$arrange_cursor_tick" <<'PY'
import re, sys, xml.etree.ElementTree as ET
baseline_line,final_line,path,track_s,tick_s=sys.argv[1:]
pat=re.compile(r"UI_ARRANGE_PLACEMENT_STATE: action=(TARGET|UNDO) track=(\d+) tick=(\d+) placements=(.*?) pattern=(.*)$")
base=pat.search(baseline_line); final=pat.search(final_line)
if not base or not final or base.group(4)!=final.group(4) or base.group(5)!=final.group(5):
    raise AssertionError("final Undo did not restore exact original Pattern placement serialization")
if (int(final.group(2)),int(final.group(3)))!=(int(track_s),int(tick_s)):
    raise AssertionError("final Undo did not restore the source insertion cursor")
print("Arrangement Pattern placement SPLIT → DELETE → PLACE → MOVE → UNDO/REDO → cleanup restored exact state.")
PY
    then
      echo "ERROR: Pattern placement lifecycle cleanup did not restore the baseline"; exit 1
    fi
    echo "Arrangement Pattern placement real-touch PLACE → MOVE ± GRID → UNDO → REDO → UNDO plus full fixture cleanup passed."

    if ! tap_visible_description_early "$ARRANGE_PLACEMENT_FLOW_FINAL_XML" \
      "Arrangement paste copied event at target tick $arrange_cursor_tick"; then
      echo "ERROR: Function Bar PASTE did not accept its explicit target cursor"; exit 1
    fi
    sleep 0.35
    ARRANGE_CURSOR_PASTED_PNG="/tmp/mpc-groovebox-arrange-event-cursor-pasted.png"
    ARRANGE_CURSOR_PASTED_XML="/tmp/mpc-groovebox-arrange-event-cursor-pasted-uiautomator.xml"
    ARRANGE_CURSOR_PASTED_REMOTE="/sdcard/mpc-groovebox-arrange-event-cursor-pasted-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_CURSOR_PASTED_REMOTE" "$ARRANGE_CURSOR_PASTED_XML"; then
      echo "ERROR: cursor-pasted hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_CURSOR_PASTED_PNG"; then
      echo "ERROR: cursor-pasted screenshot failed"; exit 1
    fi
    arrangement_cursor_paste_line="$(arrangement_event_state_line PASTE)"
    if [[ -z "$arrangement_cursor_paste_line" ]]; then
      echo "ERROR: cursor PASTE did not log authoritative Pattern state"; exit 1
    fi
    if ! python3 - "$ARRANGE_CURSOR_PASTED_XML" "$arrangement_cursor_paste_line" \
      "$arrange_event_baseline_data" "$arrange_event_track" "$arrange_event_tick" \
      "$arrange_event_note" "$arrange_cursor_tick" <<'PY'
import re, sys, xml.etree.ElementTree as ET
path,line,baseline,track_s,source_s,note_s,target_s=sys.argv[1:]
track,source,note,target=map(int,(track_s,source_s,note_s,target_s))
m=re.search(r"UI_ARRANGE_EVENT_STATE: action=PASTE track=(\d+) tick=(\d+) note=(\d+) data=(.*)$",line)
if not m or tuple(map(int,m.group(1,2,3)))!=(track,target,note):
    raise AssertionError(f"PASTE target event identity mismatch: {line!r}")
def decode(value):
    length_s,sep,rest=value.partition("|")
    if not sep: raise AssertionError(f"invalid Pattern serialization: {value!r}")
    return int(length_s),[tuple(map(int,e.split(","))) for e in rest.split(";") if e]
base_len,base=decode(baseline); actual_len,actual=decode(m.group(4))
source_events=[e for e in base if e[0]==source and e[2]==note]
if len(source_events)!=1: raise AssertionError("source event missing/ambiguous in baseline")
source_event=source_events[0]
expected=[e for e in base if not(e[0]==source and e[2]==note)]
pasted=list(source_event); pasted[0]=target
expected=sorted(expected+[tuple(pasted)],key=lambda e:e[0])
if actual_len!=base_len or actual!=expected:
    raise AssertionError(f"cursor PASTE must add a full-fidelity clone at the cursor: {expected!r} != {actual!r}")
nodes=list(ET.parse(path).getroot().iter("node"))
info=[n for n in nodes if n.attrib.get("content-desc")=="Arrangement editing guidance"]
if len(info)!=1 or info[0].attrib.get("text")!=f"EVENT • TRACK {track+1:02d} • TICK {target} • NOTE {note:03d}":
    raise AssertionError("PASTE should select the inserted event, not leave stale cursor selection")
print("Explicit-target PASTE preserved duration, pitch, velocity, probability, ratchet and nudge.")
PY
    then
      echo "ERROR: explicit-target PASTE changed unrelated Pattern state or event parameters"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_CURSOR_PASTED_XML" \
      "Arrangement undo latest sequencer edit"; then
      echo "ERROR: cursor PASTE UNDO action unavailable"; exit 1
    fi
    sleep 0.3
    ARRANGE_CURSOR_UNDO_PNG="/tmp/mpc-groovebox-arrange-event-cursor-undo.png"
    ARRANGE_CURSOR_UNDO_XML="/tmp/mpc-groovebox-arrange-event-cursor-undo-uiautomator.xml"
    ARRANGE_CURSOR_UNDO_REMOTE="/sdcard/mpc-groovebox-arrange-event-cursor-undo-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_CURSOR_UNDO_REMOTE" "$ARRANGE_CURSOR_UNDO_XML"; then
      echo "ERROR: cursor PASTE UNDO hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_CURSOR_UNDO_PNG"; then
      echo "ERROR: cursor PASTE UNDO screenshot failed"; exit 1
    fi
    arrangement_cursor_undo_line="$(arrangement_event_state_line UNDO)"
    if [[ -z "$arrangement_cursor_undo_line" ]]; then
      echo "ERROR: cursor PASTE UNDO did not log native Pattern state"; exit 1
    fi
    if ! python3 - "$ARRANGE_CURSOR_UNDO_XML" "$arrangement_cursor_undo_line" \
      "$arrange_event_baseline_data" "$arrange_event_track" "$arrange_event_tick" \
      "$arrange_event_note" "$arrange_cursor_tick" <<'PY'
import re, sys, xml.etree.ElementTree as ET
path,line,baseline,track_s,source_s,note_s,target_s=sys.argv[1:]
track,source,note,target=map(int,(track_s,source_s,note_s,target_s))
m=re.search(r"UI_ARRANGE_EVENT_STATE: action=UNDO track=(\d+) tick=(\d+) note=(\d+) data=(.*)$",line)
if not m or tuple(map(int,m.group(1,2,3)))!=(track,target,note):
    raise AssertionError(f"cursor Undo identity mismatch: {line!r}")
def decode(value):
    length_s,sep,rest=value.partition("|")
    if not sep: raise AssertionError(f"invalid Pattern serialization: {value!r}")
    return int(length_s),[tuple(map(int,e.split(","))) for e in rest.split(";") if e]
base_len,base=decode(baseline); actual_len,actual=decode(m.group(4))
expected=[e for e in base if not(e[0]==source and e[2]==note)]
if actual_len!=base_len or actual!=expected:
    raise AssertionError("UNDO should return to the prior CUT Pattern")
nodes=list(ET.parse(path).getroot().iter("node"))
info=[n for n in nodes if n.attrib.get("content-desc")==f"Arrangement paste target Track {track+1:02d} tick {target}"]
if len(info)!=1: raise AssertionError("Undo did not restore the insertion cursor")
paste=[n for n in nodes if n.attrib.get("content-desc")==f"Arrangement paste copied event at target tick {target}"]
if len(paste)!=1 or paste[0].attrib.get("enabled")!="true":
    raise AssertionError("Undo did not restore a usable PASTE cursor")
print("PASTE UNDO restored the empty snapped cursor and exact pre-paste CUT Pattern.")
PY
    then
      echo "ERROR: cursor PASTE UNDO did not restore cursor and cut Pattern"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_CURSOR_UNDO_XML" \
      "Arrangement redo latest sequencer edit"; then
      echo "ERROR: cursor PASTE REDO action unavailable"; exit 1
    fi
    sleep 0.3
    ARRANGE_CURSOR_REDO_PNG="/tmp/mpc-groovebox-arrange-event-cursor-redo.png"
    ARRANGE_CURSOR_REDO_XML="/tmp/mpc-groovebox-arrange-event-cursor-redo-uiautomator.xml"
    ARRANGE_CURSOR_REDO_REMOTE="/sdcard/mpc-groovebox-arrange-event-cursor-redo-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_CURSOR_REDO_REMOTE" "$ARRANGE_CURSOR_REDO_XML"; then
      echo "ERROR: cursor PASTE REDO hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_CURSOR_REDO_PNG"; then
      echo "ERROR: cursor PASTE REDO screenshot failed"; exit 1
    fi
    arrangement_cursor_redo_line="$(arrangement_event_state_line REDO)"
    if [[ -z "$arrangement_cursor_redo_line" ]]; then
      echo "ERROR: cursor PASTE REDO did not log native Pattern state"; exit 1
    fi
    if ! python3 - "$ARRANGE_CURSOR_REDO_XML" "$arrangement_cursor_redo_line" \
      "$arrange_event_baseline_data" "$arrange_event_track" "$arrange_event_tick" \
      "$arrange_event_note" "$arrange_cursor_tick" <<'PY'
import re, sys, xml.etree.ElementTree as ET
path,line,baseline,track_s,source_s,note_s,target_s=sys.argv[1:]
track,source,note,target=map(int,(track_s,source_s,note_s,target_s))
m=re.search(r"UI_ARRANGE_EVENT_STATE: action=REDO track=(\d+) tick=(\d+) note=(\d+) data=(.*)$",line)
if not m or tuple(map(int,m.group(1,2,3)))!=(track,target,note):
    raise AssertionError(f"cursor Redo identity mismatch: {line!r}")
def decode(value):
    length_s,sep,rest=value.partition("|")
    if not sep: raise AssertionError(f"invalid Pattern serialization: {value!r}")
    return int(length_s),[tuple(map(int,e.split(","))) for e in rest.split(";") if e]
base_len,base=decode(baseline); actual_len,actual=decode(m.group(4))
source_events=[e for e in base if e[0]==source and e[2]==note]
if len(source_events)!=1: raise AssertionError("source event missing from baseline")
pasted=list(source_events[0]); pasted[0]=target
expected=sorted([e for e in base if not(e[0]==source and e[2]==note)]+[tuple(pasted)],key=lambda e:e[0])
if actual_len!=base_len or actual!=expected: raise AssertionError("cursor REDO did not reproduce PASTE state")
nodes=list(ET.parse(path).getroot().iter("node"))
info=[n for n in nodes if n.attrib.get("content-desc")=="Arrangement editing guidance"]
if len(info)!=1 or info[0].attrib.get("text")!=f"EVENT • TRACK {track+1:02d} • TICK {target} • NOTE {note:03d}":
    raise AssertionError("Redo did not select the recreated event")
print("Cursor PASTE REDO reproduced the exact event and selected it.")
PY
    then
      echo "ERROR: cursor PASTE REDO failed exact Pattern verification"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_CURSOR_REDO_XML" \
      "Arrangement undo latest sequencer edit"; then
      echo "ERROR: final cursor PASTE UNDO action unavailable"; exit 1
    fi
    sleep 0.3
    ARRANGE_CURSOR_RESTORED_PNG="/tmp/mpc-groovebox-arrange-event-cursor-restored.png"
    ARRANGE_CURSOR_RESTORED_XML="/tmp/mpc-groovebox-arrange-event-cursor-restored-uiautomator.xml"
    ARRANGE_CURSOR_RESTORED_REMOTE="/sdcard/mpc-groovebox-arrange-event-cursor-restored-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_CURSOR_RESTORED_REMOTE" "$ARRANGE_CURSOR_RESTORED_XML"; then
      echo "ERROR: cursor PASTE final-undo hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_CURSOR_RESTORED_PNG"; then
      echo "ERROR: cursor PASTE final-undo screenshot failed"; exit 1
    fi
    arrangement_cursor_final_undo_line="$(arrangement_event_state_line UNDO)"
    if [[ -z "$arrangement_cursor_final_undo_line" ]]; then
      echo "ERROR: cursor PASTE final UNDO did not log native Pattern state"; exit 1
    fi
    if ! python3 - "$ARRANGE_CURSOR_RESTORED_XML" "$arrangement_cursor_final_undo_line" \
      "$arrange_event_baseline_data" "$arrange_event_track" "$arrange_event_tick" \
      "$arrange_event_note" "$arrange_cursor_tick" <<'PY'
import re, sys, xml.etree.ElementTree as ET
path,line,baseline,track_s,source_s,note_s,target_s=sys.argv[1:]
track,source,note,target=map(int,(track_s,source_s,note_s,target_s))
m=re.search(r"UI_ARRANGE_EVENT_STATE: action=UNDO track=(\d+) tick=(\d+) note=(\d+) data=(.*)$",line)
if not m or tuple(map(int,m.group(1,2,3)))!=(track,target,note): raise AssertionError("final cursor Undo identity mismatch")
def decode(value):
    length_s,sep,rest=value.partition("|")
    if not sep: raise AssertionError(f"invalid Pattern serialization: {value!r}")
    return int(length_s),[tuple(map(int,e.split(","))) for e in rest.split(";") if e]
base_len,base=decode(baseline); actual_len,actual=decode(m.group(4))
expected=[e for e in base if not(e[0]==source and e[2]==note)]
if actual_len!=base_len or actual!=expected: raise AssertionError("final paste Undo is not the exact CUT Pattern")
nodes=list(ET.parse(path).getroot().iter("node"))
info=[n for n in nodes if n.attrib.get("content-desc")==f"Arrangement paste target Track {track+1:02d} tick {target}"]
if len(info)!=1: raise AssertionError("final paste Undo lost insertion cursor")
print("Second PASTE undo restored the exact CUT state and retained the insertion cursor.")
PY
    then
      echo "ERROR: second cursor PASTE undo did not restore CUT state"; exit 1
    fi

    if ! tap_visible_description_early "$ARRANGE_CURSOR_RESTORED_XML" \
      "Arrangement undo latest sequencer edit"; then
      echo "ERROR: final CUT restoration UNDO action unavailable after cursor-paste cycle"; exit 1
    fi
    sleep 0.3
    ARRANGE_CURSOR_FINAL_PNG="/tmp/mpc-groovebox-arrange-event-cursor-final.png"
    ARRANGE_CURSOR_FINAL_XML="/tmp/mpc-groovebox-arrange-event-cursor-final-uiautomator.xml"
    ARRANGE_CURSOR_FINAL_REMOTE="/sdcard/mpc-groovebox-arrange-event-cursor-final-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_CURSOR_FINAL_REMOTE" "$ARRANGE_CURSOR_FINAL_XML"; then
      echo "ERROR: final CUT undo hierarchy failed after cursor-paste cycle"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_CURSOR_FINAL_PNG"; then
      echo "ERROR: final CUT undo screenshot failed after cursor-paste cycle"; exit 1
    fi
    arrangement_cursor_final_cut_undo_line="$(arrangement_event_state_line UNDO)"
    if [[ -z "$arrangement_cursor_final_cut_undo_line" ]]; then
      echo "ERROR: final CUT undo did not log the restored source"; exit 1
    fi
    validate_arrangement_duplicate_state UNDO "$ARRANGE_CURSOR_FINAL_XML" \
      "$arrangement_cursor_final_cut_undo_line" "$arrange_event_tick" || exit 1
    echo "Arrangement CUT → empty target → PASTE → UNDO → REDO → UNDO → undo CUT restored exact native Pattern."

    arrange_loop_drag_points="$(python3 - \
      "$ARRANGE_LOOP_BEFORE_XML" "$arrange_density" \
      "$loop_original_end" "$loop_target_end" \
      "$loop_visible_bars" "$loop_viewport_start" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

path, density_text, end_s, target_s, visible_s, viewport_s = sys.argv[1:]
end_bar, target_bar, visible_bars, viewport_start = map(
    int, (end_s, target_s, visible_s, viewport_s))
nodes = list(ET.parse(path).getroot().iter("node"))
view_nodes = [n for n in nodes
              if n.attrib.get("content-desc") == "MPC linear arrangement editor"]
if len(view_nodes) != 1:
    raise AssertionError(f"expected one live Arrangement canvas, got {len(view_nodes)}")
m = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",
                 view_nodes[0].attrib.get("bounds", ""))
if m is None:
    raise AssertionError(f"invalid Arrangement canvas bounds {view_nodes[0].attrib}")
left, top, right, bottom = map(int, m.groups())
if right <= left or bottom <= top:
    raise AssertionError("Arrangement canvas bounds are not positive")
density_values = re.findall(r"Override density:\s*(\d+)", density_text)
if not density_values:
    density_values = re.findall(r"Physical density:\s*(\d+)", density_text)
if not density_values:
    raise AssertionError("could not parse Android wm density: " + repr(density_text))
density = int(density_values[-1]) / 160.0
header = 138.0 * density
ruler = 34.0 * density
width = right-left
height = bottom-top
if visible_bars < 1 or header >= width or ruler >= height:
    raise AssertionError("Arrangement canvas cannot contain its timeline ruler")
bar_width = (width-header)/visible_bars
start_x = left + header + (end_bar + 0.999 - viewport_start)*bar_width
target_x = left + header + (target_bar - viewport_start + 0.5)*bar_width
y = top + 4.0*density
# The loop-end handle is allowed to sit on the view's right boundary for the
# last visible bar. Clamp by at most the hit radius so the injected touch stays
# inside the Android view while still hitting the same real drawn handle.
sx = max(left + 2, min(right - 2, round(start_x)))
tx = max(left + 2, min(right - 2, round(target_x)))
yy = round(y)
if not (left < sx < right and left < tx < right and top < yy < top+ruler):
    raise AssertionError(f"derived end-handle swipe escaped ruler: {(sx,yy,tx,yy)} bounds={(left,top,right,bottom)}")
if abs(sx-start_x) > 22*density:
    raise AssertionError("clamped start coordinate moved outside the loop-handle hit radius")
if abs(tx-sx) < 2*density:
    raise AssertionError("target bar movement is too small to reach a different bar")
print(sx, yy, tx, yy)
PY
    )" || { echo "ERROR: could not derive a valid live loop-brace swipe from the accessibility hierarchy"; exit 1; }
    read -r loop_start_x loop_y loop_changed_x loop_changed_y <<< "$arrange_loop_drag_points"
    if ! adb_bounded 10 shell input swipe "$loop_start_x" "$loop_y" "$loop_changed_x" "$loop_changed_y" 550; then
      echo "ERROR: external touch did not execute Arrange loop-brace drag"; exit 1
    fi

    loop_changed_line=""
    for loop_poll in {1..20}; do
      log_snapshot="$(adb_bounded 5 logcat -d -v brief 2>/dev/null || true)"
      loop_changed_line="$(grep -F -- 'UI_ARRANGE_LOOP_TOUCH_COMMITTED:' <<<"$log_snapshot" | tail -n 1 || true)"
      loop_state="$(sed -nE 's/.*start=([0-9]+) end=([0-9]+).*/\1 \2/p' <<<"$loop_changed_line")"
      read -r loop_changed_start loop_changed_end <<<"$loop_state"
      if [[ "$loop_changed_start" == "$loop_original_start" \
          && "$loop_changed_end" == "$loop_target_end" ]]; then
        break
      fi
      sleep 0.35
    done
    if [[ "$loop_changed_start" != "$loop_original_start" \
        || "$loop_changed_end" != "$loop_target_end" ]]; then
      echo "ERROR: real Arrange loop-handle touch did not commit expected native range $loop_original_start..$loop_target_end"
      echo "Last callback: $loop_changed_line"; exit 1
    fi
    echo "Arrange loop-brace native state changed by real touchscreen drag: $loop_original_start..$loop_target_end"

    ARRANGE_LOOP_CHANGED_PNG="/tmp/mpc-groovebox-arrange-loop-changed.png"
    ARRANGE_LOOP_CHANGED_XML="/tmp/mpc-groovebox-arrange-loop-changed-uiautomator.xml"
    ARRANGE_LOOP_CHANGED_REMOTE="/sdcard/mpc-groovebox-arrange-loop-changed-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_LOOP_CHANGED_REMOTE" "$ARRANGE_LOOP_CHANGED_XML"; then
      echo "ERROR: Arrange loop changed-state hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_LOOP_CHANGED_PNG"; then
      echo "ERROR: Arrange loop changed-state screenshot failed"; exit 1
    fi

    arrange_loop_restore_points="$(python3 - \
      "$ARRANGE_LOOP_CHANGED_XML" "$arrange_density" \
      "$loop_target_end" "$loop_original_end" \
      "$(sed -nE 's/.*visibleBars=([0-9]+).*/\1/p' <<<"$loop_changed_line")" \
      "$(sed -nE 's/.*viewportStart=([0-9]+).*/\1/p' <<<"$loop_changed_line")" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, density_text, end_s, target_s, visible_s, viewport_s = sys.argv[1:]
end_bar, target_bar, visible_bars, viewport_start = map(
    int, (end_s, target_s, visible_s, viewport_s))
nodes = list(ET.parse(path).getroot().iter("node"))
views = [n for n in nodes
         if n.attrib.get("content-desc") == "MPC linear arrangement editor"]
if len(views) != 1:
    raise AssertionError(f"expected one Arrangement view after loop drag, got {len(views)}")
m = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", views[0].attrib.get("bounds", ""))
if m is None: raise AssertionError("invalid live Arrangement bounds")
left, top, right, bottom = map(int, m.groups())
dens = re.findall(r"Override density:\s*(\d+)", density_text)
if not dens: dens = re.findall(r"Physical density:\s*(\d+)", density_text)
if not dens: raise AssertionError("could not parse Android density")
density = int(dens[-1])/160.0
header, ruler = 138.0*density, 34.0*density
bar_width = (right-left-header)/visible_bars
start_x = left + header + (end_bar + 0.999 - viewport_start)*bar_width
target_x = left + header + (target_bar - viewport_start + 0.5)*bar_width
y = top + 4.0*density
sx = max(left + 2, min(right - 2, round(start_x)))
tx = max(left + 2, min(right - 2, round(target_x)))
y = round(y)
if not (left < sx < right and left < tx < right and top < y < top+ruler):
    raise AssertionError("restore swipe would leave the Arrangement ruler")
if abs(sx-start_x) > 22*density:
    raise AssertionError("clamped restore start moved outside loop-handle hit radius")
if abs(tx-sx) < 2*density:
    raise AssertionError("restore drag target is too close to reach the original bar")
print(sx,y,tx,y)
PY
    )" || { echo "ERROR: could not derive live loop-handle restore gesture"; exit 1; }
    read -r loop_restore_start_x loop_restore_y loop_restore_end_x loop_restore_end_y <<< "$arrange_loop_restore_points"
    if ! adb_bounded 10 shell input swipe "$loop_restore_start_x" "$loop_restore_y" "$loop_restore_end_x" "$loop_restore_end_y" 550; then
      echo "ERROR: external touch could not restore original Arrange loop range"; exit 1
    fi

    loop_restored_line=""
    loop_commit_count=0
    for loop_poll in {1..20}; do
      log_snapshot="$(adb_bounded 5 logcat -d -v brief 2>/dev/null || true)"
      loop_restored_line="$(grep -F -- 'UI_ARRANGE_LOOP_TOUCH_COMMITTED:' <<<"$log_snapshot" | tail -n 1 || true)"
      loop_commit_count="$(grep -Fc -- 'UI_ARRANGE_LOOP_TOUCH_COMMITTED:' <<<"$log_snapshot" || true)"
      loop_state="$(sed -nE 's/.*start=([0-9]+) end=([0-9]+).*/\1 \2/p' <<<"$loop_restored_line")"
      read -r loop_restored_start loop_restored_end <<<"$loop_state"
      if (( loop_commit_count >= 2 )) \
          && [[ "$loop_restored_start" == "$loop_original_start" \
             && "$loop_restored_end" == "$loop_original_end" ]]; then
        break
      fi
      sleep 0.35
    done
    if (( loop_commit_count < 2 )) \
        || [[ "$loop_restored_start" != "$loop_original_start" \
           || "$loop_restored_end" != "$loop_original_end" ]]; then
      echo "ERROR: Arrange loop-brace restore did not return to native range $loop_original_start..$loop_original_end"
      echo "Last callback: $loop_restored_line"; exit 1
    fi

    ARRANGE_LOOP_RESTORED_PNG="/tmp/mpc-groovebox-arrange-loop-restored.png"
    ARRANGE_LOOP_RESTORED_XML="/tmp/mpc-groovebox-arrange-loop-restored-uiautomator.xml"
    ARRANGE_LOOP_RESTORED_REMOTE="/sdcard/mpc-groovebox-arrange-loop-restored-uiautomator.xml"
    if ! dump_device_ui "$ARRANGE_LOOP_RESTORED_REMOTE" "$ARRANGE_LOOP_RESTORED_XML"; then
      echo "ERROR: Arrange loop restored-state hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$ARRANGE_LOOP_RESTORED_PNG"; then
      echo "ERROR: Arrange loop restored-state screenshot failed"; exit 1
    fi

    if ! adb_bounded 10 shell "run-as $PACKAGE sh -c 'echo $loop_original_start $loop_original_end $loop_target_end > cache/mpc-arrange-loop-touch-result'"; then
      echo "ERROR: could not write Arrange loop acceptance result"; exit 1
    fi
    if ! adb_bounded 10 shell run-as "$PACKAGE" touch cache/mpc-arrange-loop-touch-capture-release; then
      echo "ERROR: could not release Arrange loop app-side acceptance checkpoint"; exit 1
    fi
    arrangement_loop_touch_capture_done=1
    echo "Arrange loop-brace real-touch → native commit → real-touch restore round-trip passed."
  fi

  if grep -Fq -- "UI_STEP_TOUCH_ACCEPTANCE_COMPLETE:" <<<"$log_snapshot"; then
    step_touch_acceptance_complete=1
  fi

  if [ "$step_touch_capture_done" -eq 0 ] \
      && grep -Fq -- "UI_STEP_TOUCH_CAPTURE_READY:" <<<"$log_snapshot"; then
    echo "Capturing Step Sequencer before-touch screenshot and accessibility hierarchy..."
    STEP_TOUCH_BEFORE_PNG="/tmp/mpc-groovebox-step-touch-before.png"
    STEP_TOUCH_BEFORE_XML="/tmp/mpc-groovebox-step-touch-before-uiautomator.xml"
    STEP_TOUCH_BEFORE_REMOTE="/sdcard/mpc-groovebox-step-touch-before-uiautomator.xml"
    if ! dump_device_ui "$STEP_TOUCH_BEFORE_REMOTE" "$STEP_TOUCH_BEFORE_XML"; then
      echo "ERROR: Step Sequencer initial accessibility hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$STEP_TOUCH_BEFORE_PNG"; then
      echo "ERROR: Step Sequencer initial screenshot failed"; exit 1
    fi
    if ! python3 - "$STEP_TOUCH_BEFORE_XML" <<'PY'
import sys
import xml.etree.ElementTree as ET
nodes = list(ET.parse(sys.argv[1]).getroot().iter("node"))
def bounds(node):
    import re
    m = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",
                     node.attrib.get("bounds", ""))
    if m is None:
        raise AssertionError(f"invalid live accessibility bounds: {node.attrib}")
    return tuple(map(int, m.groups()))
workspace = [n for n in nodes
             if n.attrib.get("content-desc") == "MPC Step Sequencer workspace"]
if len(workspace) != 1:
    raise AssertionError("the actual Step Sequencer workspace is not visible")
track_context = [n for n in nodes
                 if n.attrib.get("content-desc") == "Step Sequencer track context"]
if len(track_context) != 1:
    raise AssertionError("Step Sequencer track context is missing or duplicated")
wx1, wy1, wx2, wy2 = bounds(workspace[0])
tx1, ty1, tx2, ty2 = bounds(track_context[0])
if (tx2 - tx1) < (wx2 - wx1) * 0.15:
    raise AssertionError(
        f"Step track context occupies only {(tx2-tx1)/(wx2-wx1):.3f} of workspace width")
context_text = track_context[0].attrib.get("text", "")
if context_text != "TRACK 01 • DRUM":
    raise AssertionError(f"Step track context should be fully visible as 'TRACK 01 • DRUM': {context_text!r}")
pad_info = [n for n in nodes
            if n.attrib.get("class", "").endswith(".TextView")
            and n.attrib.get("text", "") == "PAD 01"]
pad_minus = [n for n in nodes
             if n.attrib.get("class", "").endswith(".Button")
             and n.attrib.get("text", "") == "PAD −"]
pad_plus = [n for n in nodes
            if n.attrib.get("class", "").endswith(".Button")
            and n.attrib.get("text", "") == "PAD +"]
incorrect_pad_buttons = [n for n in nodes
                         if n.attrib.get("class", "").endswith(".Button")
                         and n.attrib.get("text", "") == "PAD 01"]
if len(pad_info) != 1 or len(pad_minus) != 1 or len(pad_plus) != 1         or incorrect_pad_buttons:
    raise AssertionError(
        "Step header must expose one PAD status label plus distinct PAD − / PAD + buttons")
if len([n for n in nodes if n.attrib.get("content-desc") == "Pad 1 step 1 off"]) != 1:
    raise AssertionError("Step touch fixture requires Pad 01 / Step 01 to begin empty")
print("Step Sequencer header geometry and PAD controls verified from live bounds.")
PY
    then
      echo "ERROR: Step Sequencer initial state did not match its native fixture"; exit 1
    fi

    step_touch_target_number="$(python3 - "$STEP_TOUCH_BEFORE_XML" <<'PY'
import sys
import xml.etree.ElementTree as ET
nodes = list(ET.parse(sys.argv[1]).getroot().iter("node"))
candidates = []
for node in nodes:
    description = node.attrib.get("content-desc", "")
    prefix, suffix = "Pad 1 step ", " off"
    if not description.startswith(prefix) or not description.endswith(suffix):
        continue
    try:
        number = int(description[len(prefix):-len(suffix)])
    except ValueError:
        continue
    if not 2 <= number <= 16:
        continue
    bounds = node.attrib.get("bounds", "").replace("][", ",").strip("[]").split(",")
    if len(bounds) != 4 or not all(value.isdigit() for value in bounds):
        continue
    left, top, right, bottom = map(int, bounds)
    if (right > left and bottom > top
            and node.attrib.get("visible-to-user") != "false"
            and node.attrib.get("clickable") == "true"
            and node.attrib.get("enabled") == "true"):
        candidates.append(number)
if not candidates:
    raise AssertionError("no visible empty Pad 01 step is available for a real touchscreen test")
print(candidates[0])
PY
    )" || { echo "ERROR: could not choose an empty Step Sequencer cell"; exit 1; }
    step_touch_target_description="Pad 1 step $step_touch_target_number off"
    echo "Step Sequencer real-touch target: $step_touch_target_description"

    if ! tap_visible_description_early "$STEP_TOUCH_BEFORE_XML" "$step_touch_target_description"; then
      echo "ERROR: real touchscreen tap could not add the selected Step event"; exit 1
    fi
    sleep 0.35
    STEP_TOUCH_ADDED_PNG="/tmp/mpc-groovebox-step-touch-after-add.png"
    STEP_TOUCH_ADDED_XML="/tmp/mpc-groovebox-step-touch-after-add-uiautomator.xml"
    STEP_TOUCH_ADDED_REMOTE="/sdcard/mpc-groovebox-step-touch-after-add-uiautomator.xml"
    if ! dump_device_ui "$STEP_TOUCH_ADDED_REMOTE" "$STEP_TOUCH_ADDED_XML"; then
      echo "ERROR: Step Sequencer after-add hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$STEP_TOUCH_ADDED_PNG"; then
      echo "ERROR: Step Sequencer after-add screenshot failed"; exit 1
    fi
    step_touch_add_state="$(python3 - "$STEP_TOUCH_ADDED_XML" "$step_touch_target_number" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, number_s = sys.argv[1:]
number = int(number_s)
nodes = list(ET.parse(path).getroot().iter("node"))
if len([n for n in nodes if n.attrib.get("content-desc") == f"Pad 1 step {number} on"]) != 1:
    raise AssertionError("real Step tap did not create the target native event")
if len([n for n in nodes if n.attrib.get("content-desc") == "Pad 1 step 1 off"]) != 1:
    raise AssertionError("temporary note tap changed the empty Step 01 reference")
info = [n for n in nodes if n.attrib.get("content-desc") == "Step Edit event information"]
if len(info) != 1:
    raise AssertionError("Step Edit native parameter projection is missing")
text = info[0].attrib.get("text", "")
if not re.search(rf"\bSTEP\s+0*{number}\b", text):
    raise AssertionError(f"Step Edit cursor did not follow the touched step: {text!r}")
velocity = re.search(r"\bVEL\s+(\d+)\b", text)
probability = re.search(r"\bPROB\s+(\d+)\b", text)
if velocity is None or not 1 <= int(velocity.group(1)) <= 127:
    raise AssertionError(f"new note has no valid native velocity: {text!r}")
if probability is None or not 0 <= int(probability.group(1)) <= 127:
    raise AssertionError(f"new note has no valid native probability: {text!r}")
print(velocity.group(1), probability.group(1))
PY
    )" || { echo "ERROR: real Step tap did not produce native note state"; cat "$STEP_TOUCH_ADDED_XML"; exit 1; }
    read -r step_touch_velocity_before step_touch_probability_before <<< "$step_touch_add_state"
    echo "Step Sequencer DRAW verified: PAD 01 / STEP $step_touch_target_number VEL=$step_touch_velocity_before PROB=$step_touch_probability_before."

    # Verify the default VEL-focused Step Edit control before switching PARAM.
    # Change only velocity through a real +/- touch, restore it, and compare all
    # five native event fields so the next probability check starts from baseline.
    STEP_TOUCH_VELOCITY_CHANGED_XML="/tmp/mpc-groovebox-step-touch-velocity-changed-uiautomator.xml"
    STEP_TOUCH_VELOCITY_CHANGED_REMOTE="/sdcard/mpc-groovebox-step-touch-velocity-changed-uiautomator.xml"
    STEP_TOUCH_VELOCITY_CHANGED_PNG="/tmp/mpc-groovebox-step-touch-velocity-changed.png"
    STEP_TOUCH_VELOCITY_RESTORED_XML="/tmp/mpc-groovebox-step-touch-velocity-restored-uiautomator.xml"
    STEP_TOUCH_VELOCITY_RESTORED_REMOTE="/sdcard/mpc-groovebox-step-touch-velocity-restored-uiautomator.xml"
    STEP_TOUCH_VELOCITY_RESTORED_PNG="/tmp/mpc-groovebox-step-touch-velocity-restored.png"
    step_touch_velocity_fields_before="$(python3 - "$STEP_TOUCH_ADDED_XML" "$step_touch_target_number" "$step_touch_velocity_before" "$step_touch_probability_before" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, target_s, known_velocity_s, known_probability_s = sys.argv[1:]
target = int(target_s)
nodes = list(ET.parse(path).getroot().iter("node"))
if len([n for n in nodes if n.attrib.get("content-desc") == f"Pad 1 step {target} on"]) != 1:
    raise AssertionError("Velocity focus lost selected Step event")
if len([n for n in nodes if n.attrib.get("content-desc") == "Pad 1 step 1 off"]) != 1:
    raise AssertionError("Velocity baseline changed Step 01")
info = [n for n in nodes if n.attrib.get("content-desc") == "Step Edit event information"]
if len(info) != 1:
    raise AssertionError("Step Edit Velocity projection missing")
text = info[0].attrib.get("text", "")
if re.search(rf"\bSTEP\s+0*{target}\b", text) is None:
    raise AssertionError(f"Velocity focus does not match Step {target}: {text!r}")
patterns = [r"\bVEL\s+(\d+)\b", r"\bPROB\s+(\d+)\b", r"\bRAT\s+(\d+)x\b", r"\bNUDGE\s+([+-]?\d+)\b", r"\bDUR\s+(\d+)\b"]
matches = [re.search(p, text) for p in patterns]
if any(m is None for m in matches):
    raise AssertionError(f"Malformed Velocity baseline: {text!r}")
values = [int(m.group(1)) for m in matches]
if values[0] != int(known_velocity_s) or values[1] != int(known_probability_s):
    raise AssertionError(f"Initial DRAW values differ from native projection: {values!r}")
if not (1 <= values[0] <= 127 and 0 <= values[1] <= 127 and 1 <= values[2] <= 8 and -960 <= values[3] <= 960 and values[4] > 0):
    raise AssertionError(f"Velocity baseline out of range: {values!r}")
print(*values)
PY
)" || { echo "ERROR: could not read complete Step Edit Velocity baseline"; cat "$STEP_TOUCH_ADDED_XML"; exit 1; }
    read -r step_touch_velocity_value_before step_touch_velocity_probability_before step_touch_velocity_ratchet_before step_touch_velocity_nudge_before step_touch_velocity_duration_before <<< "$step_touch_velocity_fields_before"
    if (( step_touch_velocity_value_before < 127 )); then
      step_touch_velocity_direction="+"
      step_touch_velocity_inverse_direction="−"
      step_touch_velocity_expected=$((step_touch_velocity_value_before + 1))
    else
      step_touch_velocity_direction="−"
      step_touch_velocity_inverse_direction="+"
      step_touch_velocity_expected=$((step_touch_velocity_value_before - 1))
    fi
    if ! tap_visible_text_early "$STEP_TOUCH_ADDED_XML" "$step_touch_velocity_direction"; then
      echo "ERROR: Step Edit Velocity +/- control was not actionable"; exit 1
    fi
    sleep 0.25
    if ! dump_device_ui "$STEP_TOUCH_VELOCITY_CHANGED_REMOTE" "$STEP_TOUCH_VELOCITY_CHANGED_XML"; then
      echo "ERROR: Step Edit after-Velocity hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$STEP_TOUCH_VELOCITY_CHANGED_PNG"; then
      echo "ERROR: Step Edit after-Velocity screenshot failed"; exit 1
    fi
    if ! python3 - "$STEP_TOUCH_VELOCITY_CHANGED_XML" "$step_touch_target_number" "$step_touch_velocity_fields_before" "$step_touch_velocity_expected" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, target_s, before_s, expected_s = sys.argv[1:]
target = int(target_s)
before = list(map(int, before_s.split()))
expected = int(expected_s)
nodes = list(ET.parse(path).getroot().iter("node"))
if len([n for n in nodes if n.attrib.get("content-desc") == f"Pad 1 step {target} on"]) != 1:
    raise AssertionError("Velocity edit lost selected Step event")
if len([n for n in nodes if n.attrib.get("content-desc") == "Pad 1 step 1 off"]) != 1:
    raise AssertionError("Velocity edit changed Step 01")
info = [n for n in nodes if n.attrib.get("content-desc") == "Step Edit event information"]
if len(info) != 1:
    raise AssertionError("Step Edit projection missing after Velocity")
text = info[0].attrib.get("text", "")
patterns = [r"\bVEL\s+(\d+)\b", r"\bPROB\s+(\d+)\b", r"\bRAT\s+(\d+)x\b", r"\bNUDGE\s+([+-]?\d+)\b", r"\bDUR\s+(\d+)\b"]
matches = [re.search(p, text) for p in patterns]
if any(m is None for m in matches):
    raise AssertionError(f"Malformed Step Edit state after Velocity: {text!r}")
after = [int(m.group(1)) for m in matches]
if after[0] != expected:
    raise AssertionError(f"Velocity must change by one: {before[0]} -> {after[0]}, expected {expected}")
if after[1:] != before[1:]:
    raise AssertionError(f"Velocity edit modified unrelated fields: {before!r} -> {after!r}")
print(f"Step Edit VEL field isolation passed: {before[0]} -> {after[0]}; other four fields preserved.")
PY
    then
      echo "ERROR: Step Edit Velocity did not make the exact native field-only change"; cat "$STEP_TOUCH_VELOCITY_CHANGED_XML"; exit 1
    fi
    if ! tap_visible_text_early "$STEP_TOUCH_VELOCITY_CHANGED_XML" "$step_touch_velocity_inverse_direction"; then
      echo "ERROR: Step Edit Velocity inverse control was not actionable"; exit 1
    fi
    sleep 0.25
    if ! dump_device_ui "$STEP_TOUCH_VELOCITY_RESTORED_REMOTE" "$STEP_TOUCH_VELOCITY_RESTORED_XML"; then
      echo "ERROR: Step Edit after-Velocity-restore hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$STEP_TOUCH_VELOCITY_RESTORED_PNG"; then
      echo "ERROR: Step Edit after-Velocity-restore screenshot failed"; exit 1
    fi
    if ! python3 - "$STEP_TOUCH_VELOCITY_RESTORED_XML" "$step_touch_target_number" "$step_touch_velocity_fields_before" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, target_s, before_s = sys.argv[1:]
target = int(target_s)
before = list(map(int, before_s.split()))
nodes = list(ET.parse(path).getroot().iter("node"))
if len([n for n in nodes if n.attrib.get("content-desc") == f"Pad 1 step {target} on"]) != 1:
    raise AssertionError("Velocity restore lost selected Step event")
if len([n for n in nodes if n.attrib.get("content-desc") == "Pad 1 step 1 off"]) != 1:
    raise AssertionError("Velocity restore changed Step 01")
info = [n for n in nodes if n.attrib.get("content-desc") == "Step Edit event information"]
if len(info) != 1:
    raise AssertionError("Step Edit projection missing after Velocity restore")
text = info[0].attrib.get("text", "")
patterns = [r"\bVEL\s+(\d+)\b", r"\bPROB\s+(\d+)\b", r"\bRAT\s+(\d+)x\b", r"\bNUDGE\s+([+-]?\d+)\b", r"\bDUR\s+(\d+)\b"]
matches = [re.search(p, text) for p in patterns]
if any(m is None for m in matches):
    raise AssertionError(f"Malformed Step Edit state after Velocity restore: {text!r}")
restored = [int(m.group(1)) for m in matches]
if restored != before:
    raise AssertionError(f"Velocity inverse edit failed full-field restore: {before!r} -> {restored!r}")
print(f"Step Edit VEL round trip restored all five fields exactly: {restored!r}.")
PY
    then
      echo "ERROR: Step Edit Velocity inverse edit failed to restore the full native event"; cat "$STEP_TOUCH_VELOCITY_RESTORED_XML"; exit 1
    fi
    echo "Step Edit VELOCITY real-touch edit → exact inverse restore passed."

    # Start probability acceptance from the restored VEL baseline.
    if ! tap_visible_text_early "$STEP_TOUCH_VELOCITY_RESTORED_XML" "PARAM"; then
      echo "ERROR: Step Sequencer PARAM function could not be tapped"; exit 1
    fi
    sleep 0.2
    STEP_TOUCH_PARAM_XML="/tmp/mpc-groovebox-step-touch-param-selected-uiautomator.xml"
    STEP_TOUCH_PARAM_REMOTE="/sdcard/mpc-groovebox-step-touch-param-selected-uiautomator.xml"
    if ! dump_device_ui "$STEP_TOUCH_PARAM_REMOTE" "$STEP_TOUCH_PARAM_XML"; then
      echo "ERROR: Step Sequencer hierarchy after PARAM selection failed"; exit 1
    fi
    step_touch_probability_fields_before="$(python3 - "$STEP_TOUCH_PARAM_XML" "$step_touch_target_number" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, target_s = sys.argv[1:]
target = int(target_s)
nodes = list(ET.parse(path).getroot().iter("node"))
if len([n for n in nodes if n.attrib.get("content-desc") == f"Pad 1 step {target} on"]) != 1:
    raise AssertionError("Probability baseline lost the active Step event")
if len([n for n in nodes if n.attrib.get("content-desc") == "Pad 1 step 1 off"]) != 1:
    raise AssertionError("Probability baseline changed Step 01")
info = [n for n in nodes if n.attrib.get("content-desc") == "Step Edit event information"]
if len(info) != 1:
    raise AssertionError("Step Edit probability baseline is missing")
text = info[0].attrib.get("text", "")
patterns = [r"\bVEL\s+(\d+)\b", r"\bPROB\s+(\d+)\b", r"\bRAT\s+(\d+)x\b", r"\bNUDGE\s+([+-]?\d+)\b", r"\bDUR\s+(\d+)\b"]
matches = [re.search(p, text) for p in patterns]
if any(m is None for m in matches):
    raise AssertionError(f"Malformed Step Edit probability baseline: {text!r}")
values = [int(m.group(1)) for m in matches]
print(*values)
PY
)" || { echo "ERROR: could not read the complete Step Edit probability baseline"; cat "$STEP_TOUCH_PARAM_XML"; exit 1; }
    read -r step_touch_prob_velocity_before step_touch_prob_value_before step_touch_prob_ratchet_before step_touch_prob_nudge_before step_touch_prob_duration_before <<< "$step_touch_probability_fields_before"
    if (( step_touch_prob_value_before != step_touch_probability_before )); then
      echo "ERROR: Step Edit probability baseline differs from the created note"; exit 1
    fi
    if (( step_touch_probability_before > 0 )); then
      step_touch_probability_direction="−"
      step_touch_probability_inverse_direction="+"
      step_touch_probability_expected=$((step_touch_probability_before - 1))
    else
      step_touch_probability_direction="+"
      step_touch_probability_inverse_direction="−"
      step_touch_probability_expected=1
    fi
    if ! tap_visible_text_early "$STEP_TOUCH_PARAM_XML" "$step_touch_probability_direction"; then
      echo "ERROR: Step Sequencer probability +/- action was not actionable"; exit 1
    fi
    sleep 0.35
    STEP_TOUCH_CHANGED_PNG="/tmp/mpc-groovebox-step-touch-after-parameter.png"
    STEP_TOUCH_CHANGED_XML="/tmp/mpc-groovebox-step-touch-after-parameter-uiautomator.xml"
    STEP_TOUCH_CHANGED_REMOTE="/sdcard/mpc-groovebox-step-touch-after-parameter-uiautomator.xml"
    if ! dump_device_ui "$STEP_TOUCH_CHANGED_REMOTE" "$STEP_TOUCH_CHANGED_XML"; then
      echo "ERROR: Step Sequencer after-parameter hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$STEP_TOUCH_CHANGED_PNG"; then
      echo "ERROR: Step Sequencer after-parameter screenshot failed"; exit 1
    fi
    if ! python3 - "$STEP_TOUCH_CHANGED_XML" "$step_touch_target_number" "$step_touch_probability_fields_before" "$step_touch_probability_expected" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, number_s, before_s, expected_s = sys.argv[1:]
number = int(number_s)
before = list(map(int, before_s.split()))
expected = int(expected_s)
nodes = list(ET.parse(path).getroot().iter("node"))
if len([n for n in nodes if n.attrib.get("content-desc") == f"Pad 1 step {number} on"]) != 1:
    raise AssertionError("parameter action unexpectedly removed the Step event")
if len([n for n in nodes if n.attrib.get("content-desc") == "Pad 1 step 1 off"]) != 1:
    raise AssertionError("probability adjustment changed the Step 01 reference")
info = [n for n in nodes if n.attrib.get("content-desc") == "Step Edit event information"]
if len(info) != 1:
    raise AssertionError("Step Edit parameter value is missing")
text = info[0].attrib.get("text", "")
if re.search(rf"\bSTEP\s+0*{number}\b", text) is None:
    raise AssertionError(f"Step Edit focus lost the active Step: {text!r}")
patterns = [r"\bVEL\s+(\d+)\b", r"\bPROB\s+(\d+)\b", r"\bRAT\s+(\d+)x\b", r"\bNUDGE\s+([+-]?\d+)\b", r"\bDUR\s+(\d+)\b"]
matches = [re.search(p, text) for p in patterns]
if any(m is None for m in matches):
    raise AssertionError(f"Malformed Step Edit state after probability change: {text!r}")
after = [int(m.group(1)) for m in matches]
if after[1] != expected:
    raise AssertionError(f"native probability {after[1]} != expected {expected}")
if [after[i] for i in (0, 2, 3, 4)] != [before[i] for i in (0, 2, 3, 4)]:
    raise AssertionError(f"probability edit modified unrelated fields: {before!r} -> {after!r}")
print(f"Step Edit PROB field isolation passed: {before[1]} -> {after[1]}; other fields preserved.")
PY
    then
      echo "ERROR: visible Step Sequencer parameter control did not make a field-isolated probability change"; cat "$STEP_TOUCH_CHANGED_XML"; exit 1
    fi

    # Inverse-touch PROB and require exact restoration of the complete event.
    STEP_TOUCH_PROBABILITY_RESTORED_PNG="/tmp/mpc-groovebox-step-touch-probability-restored.png"
    STEP_TOUCH_PROBABILITY_RESTORED_XML="/tmp/mpc-groovebox-step-touch-probability-restored-uiautomator.xml"
    STEP_TOUCH_PROBABILITY_RESTORED_REMOTE="/sdcard/mpc-groovebox-step-touch-probability-restored-uiautomator.xml"
    if ! tap_visible_text_early "$STEP_TOUCH_CHANGED_XML" "$step_touch_probability_inverse_direction"; then
      echo "ERROR: Step Edit probability inverse control was not actionable"; exit 1
    fi
    sleep 0.25
    if ! dump_device_ui "$STEP_TOUCH_PROBABILITY_RESTORED_REMOTE" "$STEP_TOUCH_PROBABILITY_RESTORED_XML"; then
      echo "ERROR: Step Edit after-probability-restore hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$STEP_TOUCH_PROBABILITY_RESTORED_PNG"; then
      echo "ERROR: Step Edit after-probability-restore screenshot failed"; exit 1
    fi
    if ! python3 - "$STEP_TOUCH_PROBABILITY_RESTORED_XML" "$step_touch_target_number" "$step_touch_probability_fields_before" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, target_s, before_s = sys.argv[1:]
target = int(target_s)
before = list(map(int, before_s.split()))
nodes = list(ET.parse(path).getroot().iter("node"))
if len([n for n in nodes if n.attrib.get("content-desc") == f"Pad 1 step {target} on"]) != 1:
    raise AssertionError("Probability restore lost the active Step event")
if len([n for n in nodes if n.attrib.get("content-desc") == "Pad 1 step 1 off"]) != 1:
    raise AssertionError("Probability restore changed Step 01")
info = [n for n in nodes if n.attrib.get("content-desc") == "Step Edit event information"]
if len(info) != 1:
    raise AssertionError("Step Edit projection missing after probability restore")
text = info[0].attrib.get("text", "")
patterns = [r"\bVEL\s+(\d+)\b", r"\bPROB\s+(\d+)\b", r"\bRAT\s+(\d+)x\b", r"\bNUDGE\s+([+-]?\d+)\b", r"\bDUR\s+(\d+)\b"]
matches = [re.search(p, text) for p in patterns]
if any(m is None for m in matches):
    raise AssertionError(f"Malformed Step Edit state after probability restore: {text!r}")
restored = [int(m.group(1)) for m in matches]
if restored != before:
    raise AssertionError(f"Probability inverse edit failed exact full-field restore: {before!r} -> {restored!r}")
print(f"Step Edit PROB round trip restored all five fields exactly: {restored!r}.")
PY
    then
      echo "ERROR: Step Edit probability inverse edit failed to restore the full native event"; cat "$STEP_TOUCH_PROBABILITY_RESTORED_XML"; exit 1
    fi
    echo "Step Edit PROB real-touch edit → exact inverse restore passed."

    # Continue from PROB through RAT → NUDGE → DUR using the same visible
    # Step PARAM/+ controls. Validate the complete native projection after each edit.
    STEP_TOUCH_RATCHET_XML="/tmp/mpc-groovebox-step-touch-ratchet-focus-uiautomator.xml"
    STEP_TOUCH_RATCHET_REMOTE="/sdcard/mpc-groovebox-step-touch-ratchet-focus-uiautomator.xml"
    if ! tap_visible_text_early "$STEP_TOUCH_PROBABILITY_RESTORED_XML" "PARAM"; then
      echo "ERROR: Step Edit PARAM could not select Ratchet"; exit 1
    fi
    sleep 0.15
    if ! dump_device_ui "$STEP_TOUCH_RATCHET_REMOTE" "$STEP_TOUCH_RATCHET_XML"; then
      echo "ERROR: Step Edit Ratchet-focus hierarchy failed"; exit 1
    fi
    STEP_TOUCH_RATCHET_FOCUS_PNG="/tmp/mpc-groovebox-step-touch-ratchet-focus.png"
    STEP_TOUCH_RATCHET_CHANGED_XML="/tmp/mpc-groovebox-step-touch-ratchet-changed-uiautomator.xml"
    STEP_TOUCH_RATCHET_CHANGED_REMOTE="/sdcard/mpc-groovebox-step-touch-ratchet-changed-uiautomator.xml"
    STEP_TOUCH_RATCHET_CHANGED_PNG="/tmp/mpc-groovebox-step-touch-ratchet-changed.png"
    STEP_TOUCH_RATCHET_RESTORED_XML="/tmp/mpc-groovebox-step-touch-ratchet-restored-uiautomator.xml"
    STEP_TOUCH_RATCHET_RESTORED_REMOTE="/sdcard/mpc-groovebox-step-touch-ratchet-restored-uiautomator.xml"
    STEP_TOUCH_RATCHET_RESTORED_PNG="/tmp/mpc-groovebox-step-touch-ratchet-restored.png"
    if ! adb_bounded 10 exec-out screencap -p > "$STEP_TOUCH_RATCHET_FOCUS_PNG"; then
      echo "ERROR: Step Edit Ratchet-focus screenshot failed"; exit 1
    fi

    step_touch_ratchet_timing_before="$(python3 - "$STEP_TOUCH_RATCHET_XML" "$step_touch_target_number" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, target_s = sys.argv[1:]
target = int(target_s)
nodes = list(ET.parse(path).getroot().iter("node"))
if len([n for n in nodes if n.attrib.get("content-desc") == f"Pad 1 step {target} on"]) != 1:
    raise AssertionError("Ratchet focus lost selected Step event")
if len([n for n in nodes if n.attrib.get("content-desc") == "Pad 1 step 1 off"]) != 1:
    raise AssertionError("Ratchet focus changed Step 01")
info = [n for n in nodes if n.attrib.get("content-desc") == "Step Edit event information"]
if len(info) != 1:
    raise AssertionError("Step Edit Ratchet projection missing")
text = info[0].attrib.get("text", "")
if re.search(rf"\bSTEP\s+0*{target}\b", text) is None:
    raise AssertionError(f"Ratchet focus does not match Step {target}: {text!r}")
patterns = [r"\bVEL\s+(\d+)\b", r"\bPROB\s+(\d+)\b", r"\bRAT\s+(\d+)x\b", r"\bNUDGE\s+([+-]?\d+)\b", r"\bDUR\s+(\d+)\b"]
matches = [re.search(p, text) for p in patterns]
if any(m is None for m in matches):
    raise AssertionError(f"Malformed Ratchet baseline: {text!r}")
values = [int(m.group(1)) for m in matches]
if not (1 <= values[0] <= 127 and 0 <= values[1] <= 127 and 1 <= values[2] <= 8 and -960 <= values[3] <= 960 and values[4] > 0):
    raise AssertionError(f"Ratchet baseline out of range: {values!r}")
print(*values)
PY
)" || { echo "ERROR: could not read Step Edit Ratchet baseline"; cat "$STEP_TOUCH_RATCHET_XML"; exit 1; }
    read -r step_touch_ratchet_velocity_before step_touch_ratchet_probability_before step_touch_ratchet_before step_touch_ratchet_nudge_before step_touch_ratchet_duration_before <<< "$step_touch_ratchet_timing_before"
    if (( step_touch_ratchet_before < 8 )); then
      step_touch_ratchet_direction="+"
      step_touch_ratchet_inverse_direction="−"
      step_touch_ratchet_expected=$((step_touch_ratchet_before + 1))
    else
      step_touch_ratchet_direction="−"
      step_touch_ratchet_inverse_direction="+"
      step_touch_ratchet_expected=$((step_touch_ratchet_before - 1))
    fi
    if ! tap_visible_text_early "$STEP_TOUCH_RATCHET_XML" "$step_touch_ratchet_direction"; then
      echo "ERROR: Step Edit Ratchet +/- control was not actionable"; exit 1
    fi
    sleep 0.25
    if ! dump_device_ui "$STEP_TOUCH_RATCHET_CHANGED_REMOTE" "$STEP_TOUCH_RATCHET_CHANGED_XML"; then
      echo "ERROR: Step Edit after-Ratchet hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$STEP_TOUCH_RATCHET_CHANGED_PNG"; then
      echo "ERROR: Step Edit after-Ratchet screenshot failed"; exit 1
    fi
    if ! python3 - "$STEP_TOUCH_RATCHET_CHANGED_XML" "$step_touch_target_number" "$step_touch_ratchet_timing_before" "$step_touch_ratchet_expected" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, target_s, before_s, expected_s = sys.argv[1:]
target = int(target_s)
before = list(map(int, before_s.split()))
expected = int(expected_s)
nodes = list(ET.parse(path).getroot().iter("node"))
if len([n for n in nodes if n.attrib.get("content-desc") == f"Pad 1 step {target} on"]) != 1:
    raise AssertionError("Ratchet edit lost selected Step event")
if len([n for n in nodes if n.attrib.get("content-desc") == "Pad 1 step 1 off"]) != 1:
    raise AssertionError("Ratchet edit changed Step 01")
info = [n for n in nodes if n.attrib.get("content-desc") == "Step Edit event information"]
if len(info) != 1:
    raise AssertionError("Step Edit projection missing after Ratchet")
text = info[0].attrib.get("text", "")
patterns = [r"\bVEL\s+(\d+)\b", r"\bPROB\s+(\d+)\b", r"\bRAT\s+(\d+)x\b", r"\bNUDGE\s+([+-]?\d+)\b", r"\bDUR\s+(\d+)\b"]
matches = [re.search(p, text) for p in patterns]
if any(m is None for m in matches):
    raise AssertionError(f"Malformed Step Edit state after Ratchet: {text!r}")
after = [int(m.group(1)) for m in matches]
if after[2] != expected:
    raise AssertionError(f"Ratchet must change by one: {before[2]} -> {after[2]}, expected {expected}")
if [after[0], after[1], after[3], after[4]] != [before[0], before[1], before[3], before[4]]:
    raise AssertionError(f"Ratchet modified unrelated fields: {before!r} -> {after!r}")
print(f"Step Edit RAT field isolation passed: {before[2]} -> {after[2]}; other fields preserved.")
PY
    then
      echo "ERROR: Step Edit Ratchet did not make the exact native field-only change"; cat "$STEP_TOUCH_RATCHET_CHANGED_XML"; exit 1
    fi

    if ! tap_visible_text_early "$STEP_TOUCH_RATCHET_CHANGED_XML" "$step_touch_ratchet_inverse_direction"; then
      echo "ERROR: Step Edit Ratchet inverse control was not actionable"; exit 1
    fi
    sleep 0.25
    if ! dump_device_ui "$STEP_TOUCH_RATCHET_RESTORED_REMOTE" "$STEP_TOUCH_RATCHET_RESTORED_XML"; then
      echo "ERROR: Step Edit after-Ratchet-restore hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$STEP_TOUCH_RATCHET_RESTORED_PNG"; then
      echo "ERROR: Step Edit after-Ratchet-restore screenshot failed"; exit 1
    fi
    if ! python3 - "$STEP_TOUCH_RATCHET_RESTORED_XML" "$step_touch_target_number" "$step_touch_ratchet_timing_before" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, target_s, before_s = sys.argv[1:]
target = int(target_s)
before = list(map(int, before_s.split()))
nodes = list(ET.parse(path).getroot().iter("node"))
if len([n for n in nodes if n.attrib.get("content-desc") == f"Pad 1 step {target} on"]) != 1:
    raise AssertionError("Ratchet restore lost selected Step event")
if len([n for n in nodes if n.attrib.get("content-desc") == "Pad 1 step 1 off"]) != 1:
    raise AssertionError("Ratchet restore changed Step 01")
info = [n for n in nodes if n.attrib.get("content-desc") == "Step Edit event information"]
if len(info) != 1:
    raise AssertionError("Step Edit projection missing after Ratchet restore")
text = info[0].attrib.get("text", "")
patterns = [r"\bVEL\s+(\d+)\b", r"\bPROB\s+(\d+)\b", r"\bRAT\s+(\d+)x\b", r"\bNUDGE\s+([+-]?\d+)\b", r"\bDUR\s+(\d+)\b"]
matches = [re.search(p, text) for p in patterns]
if any(m is None for m in matches):
    raise AssertionError(f"Malformed Step Edit state after Ratchet restore: {text!r}")
restored = [int(m.group(1)) for m in matches]
if restored != before:
    raise AssertionError(f"Ratchet inverse edit failed full-field restore: {before!r} -> {restored!r}")
print(f"Step Edit RAT round trip restored all five fields exactly: {restored!r}.")
PY
    then
      echo "ERROR: Step Edit Ratchet inverse edit failed to restore the full native event"; cat "$STEP_TOUCH_RATCHET_RESTORED_XML"; exit 1
    fi

    STEP_TOUCH_NUDGE_FOCUS_XML="/tmp/mpc-groovebox-step-touch-nudge-focus-uiautomator.xml"
    STEP_TOUCH_NUDGE_FOCUS_REMOTE="/sdcard/mpc-groovebox-step-touch-nudge-focus-uiautomator.xml"
    echo "Step Edit RAT real-touch edit → exact inverse restore passed."
    if ! tap_visible_text_early "$STEP_TOUCH_RATCHET_RESTORED_XML" "PARAM"; then
      echo "ERROR: Step Edit PARAM could not select Nudge after Ratchet restore"; exit 1
    fi
    sleep 0.15
    if ! dump_device_ui "$STEP_TOUCH_NUDGE_FOCUS_REMOTE" "$STEP_TOUCH_NUDGE_FOCUS_XML"; then
      echo "ERROR: Step Edit Nudge-focus hierarchy failed"; exit 1
    fi
    step_touch_timing_before="$(python3 - "$STEP_TOUCH_NUDGE_FOCUS_XML" "$step_touch_target_number" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, target_s = sys.argv[1:]
target = int(target_s)
nodes = list(ET.parse(path).getroot().iter("node"))
info = [n for n in nodes if n.attrib.get("content-desc") == "Step Edit event information"]
if len(info) != 1:
    raise AssertionError("Step Edit timing projection is missing")
text = info[0].attrib.get("text", "")
if re.search(rf"\bSTEP\s+0*{target}\b", text) is None:
    raise AssertionError(f"Step Edit focus does not match target {target}: {text!r}")
patterns = [r"\bVEL\s+(\d+)\b", r"\bPROB\s+(\d+)\b",
            r"\bRAT\s+(\d+)x\b", r"\bNUDGE\s+([+-]?\d+)\b",
            r"\bDUR\s+(\d+)\b"]
values = [re.search(p, text) for p in patterns]
if any(value is None for value in values):
    raise AssertionError(f"Malformed native Step Edit timing state: {text!r}")
parsed = [int(value.group(1)) for value in values]
if not 1 <= parsed[0] <= 127 or not 0 <= parsed[1] <= 127 \
        or not 1 <= parsed[2] <= 8 or parsed[4] <= 0:
    raise AssertionError(f"Step Edit timing baseline outside supported range: {parsed!r}")
print(*parsed)
PY
)" || { echo "ERROR: could not read complete native Step Edit fields before Nudge"; exit 1; }
    read -r step_touch_velocity_timing_before step_touch_probability_timing_before step_touch_ratchet_before step_touch_nudge_before step_touch_duration_before <<< "$step_touch_timing_before"

    if ! tap_visible_text_early "$STEP_TOUCH_NUDGE_FOCUS_XML" "+"; then
      echo "ERROR: Step Edit Nudge + control was not actionable"; exit 1
    fi
    sleep 0.25
    STEP_TOUCH_NUDGE_CHANGED_PNG="/tmp/mpc-groovebox-step-touch-nudge-changed.png"
    STEP_TOUCH_NUDGE_CHANGED_XML="/tmp/mpc-groovebox-step-touch-nudge-changed-uiautomator.xml"
    STEP_TOUCH_NUDGE_CHANGED_REMOTE="/sdcard/mpc-groovebox-step-touch-nudge-changed-uiautomator.xml"
    if ! dump_device_ui "$STEP_TOUCH_NUDGE_CHANGED_REMOTE" "$STEP_TOUCH_NUDGE_CHANGED_XML"; then
      echo "ERROR: Step Edit after-Nudge hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$STEP_TOUCH_NUDGE_CHANGED_PNG"; then
      echo "ERROR: Step Edit after-Nudge screenshot failed"; exit 1
    fi
    if ! python3 - "$STEP_TOUCH_NUDGE_CHANGED_XML" "$step_touch_target_number" "$step_touch_timing_before" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, target_s, before_s = sys.argv[1:]
target = int(target_s)
before = list(map(int, before_s.split()))
nodes = list(ET.parse(path).getroot().iter("node"))
if len([n for n in nodes if n.attrib.get("content-desc") == f"Pad 1 step {target} on"]) != 1:
    raise AssertionError("Nudge edit lost the selected native Step event")
info = [n for n in nodes if n.attrib.get("content-desc") == "Step Edit event information"]
if len(info) != 1:
    raise AssertionError("Nudge edit lost the Step Edit native projection")
text = info[0].attrib.get("text", "")
patterns = [r"\bVEL\s+(\d+)\b", r"\bPROB\s+(\d+)\b",
            r"\bRAT\s+(\d+)x\b", r"\bNUDGE\s+([+-]?\d+)\b",
            r"\bDUR\s+(\d+)\b"]
matches = [re.search(p, text) for p in patterns]
if any(m is None for m in matches):
    raise AssertionError(f"Malformed native Step Edit projection after Nudge: {text!r}")
after = [int(m.group(1)) for m in matches]
expected_nudge = min(960, before[3] + 60)
if after[3] != expected_nudge:
    raise AssertionError(f"Nudge should change by one normal 60-tick increment: {before[3]} -> {after[3]}, expected {expected_nudge}")
if [after[0], after[1], after[2], after[4]] != [before[0], before[1], before[2], before[4]]:
    raise AssertionError(f"Nudge edit changed unrelated Step fields: {before!r} -> {after!r}")
if len([n for n in nodes if n.attrib.get("content-desc") == "Pad 1 step 1 off"]) != 1:
    raise AssertionError("Nudge edit changed the Step 01 reference")
print(f"Step Edit NUDGE + changed only nudge ticks: {before[3]} -> {after[3]}.")
PY
    then
      echo "ERROR: Step Edit Nudge + did not make the exact native field-only change"; cat "$STEP_TOUCH_NUDGE_CHANGED_XML"; exit 1
    fi

    # Advance NUDGE → DUR and assert a quantize-relative duration delta.
    STEP_TOUCH_DURATION_FOCUS_XML="/tmp/mpc-groovebox-step-touch-duration-focus-uiautomator.xml"
    STEP_TOUCH_DURATION_FOCUS_REMOTE="/sdcard/mpc-groovebox-step-touch-duration-focus-uiautomator.xml"
    if ! tap_visible_text_early "$STEP_TOUCH_NUDGE_CHANGED_XML" "PARAM"; then
      echo "ERROR: Step Edit PARAM could not select Duration"; exit 1
    fi
    sleep 0.15
    if ! dump_device_ui "$STEP_TOUCH_DURATION_FOCUS_REMOTE" "$STEP_TOUCH_DURATION_FOCUS_XML"; then
      echo "ERROR: Step Edit Duration-focus hierarchy failed"; exit 1
    fi
    if ! tap_visible_text_early "$STEP_TOUCH_DURATION_FOCUS_XML" "+"; then
      echo "ERROR: Step Edit Duration + control was not actionable"; exit 1
    fi
    sleep 0.25
    STEP_TOUCH_DURATION_CHANGED_PNG="/tmp/mpc-groovebox-step-touch-duration-changed.png"
    STEP_TOUCH_DURATION_CHANGED_XML="/tmp/mpc-groovebox-step-touch-duration-changed-uiautomator.xml"
    STEP_TOUCH_DURATION_CHANGED_REMOTE="/sdcard/mpc-groovebox-step-touch-duration-changed-uiautomator.xml"
    if ! dump_device_ui "$STEP_TOUCH_DURATION_CHANGED_REMOTE" "$STEP_TOUCH_DURATION_CHANGED_XML"; then
      echo "ERROR: Step Edit after-Duration hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$STEP_TOUCH_DURATION_CHANGED_PNG"; then
      echo "ERROR: Step Edit after-Duration screenshot failed"; exit 1
    fi
    if ! python3 - "$STEP_TOUCH_NUDGE_CHANGED_XML" "$STEP_TOUCH_DURATION_CHANGED_XML" "$step_touch_target_number" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
before_path, after_path, target_s = sys.argv[1:]
target = int(target_s)
def read_state(path):
    nodes = list(ET.parse(path).getroot().iter("node"))
    if len([n for n in nodes if n.attrib.get("content-desc") == f"Pad 1 step {target} on"]) != 1:
        raise AssertionError("Duration edit lost the selected native Step event")
    info = [n for n in nodes if n.attrib.get("content-desc") == "Step Edit event information"]
    if len(info) != 1:
        raise AssertionError("Duration edit lost the Step Edit native projection")
    text = info[0].attrib.get("text", "")
    patterns = [r"\bVEL\s+(\d+)\b", r"\bPROB\s+(\d+)\b",
                r"\bRAT\s+(\d+)x\b", r"\bNUDGE\s+([+-]?\d+)\b",
                r"\bDUR\s+(\d+)\b"]
    matches = [re.search(p, text) for p in patterns]
    if any(m is None for m in matches):
        raise AssertionError(f"Malformed native Step Edit projection: {text!r}")
    return [int(m.group(1)) for m in matches], nodes
before, _ = read_state(before_path)
after, nodes = read_state(after_path)
increment = max(1, before[4] // 4)
expected_duration = min(before[4] + increment, before[4] * 4)
if after[4] != expected_duration:
    raise AssertionError(f"Duration should increase by one quarter-grid increment: {before[4]} -> {after[4]}, expected {expected_duration}")
if after[:4] != before[:4]:
    raise AssertionError(f"Duration edit changed velocity/probability/ratchet/nudge: {before!r} -> {after!r}")
if len([n for n in nodes if n.attrib.get("content-desc") == "Pad 1 step 1 off"]) != 1:
    raise AssertionError("Duration edit changed the Step 01 reference")
print(f"Step Edit DURATION + changed only duration ticks: {before[4]} -> {after[4]}.")
PY
    then
      echo "ERROR: Step Edit Duration + did not make the exact native field-only change"; cat "$STEP_TOUCH_DURATION_CHANGED_XML"; exit 1
    fi
    echo "Step Edit NUDGE and DURATION real-touch field-isolation round trips passed."

    if ! tap_visible_description_early "$STEP_TOUCH_DURATION_CHANGED_XML" "Pad 1 step $step_touch_target_number on"; then
      echo "ERROR: could not remove the temporary Step event with a real touchscreen tap"; exit 1
    fi
    sleep 0.35
    STEP_TOUCH_ERASED_PNG="/tmp/mpc-groovebox-step-touch-after-erase.png"
    STEP_TOUCH_ERASED_XML="/tmp/mpc-groovebox-step-touch-after-erase-uiautomator.xml"
    STEP_TOUCH_ERASED_REMOTE="/sdcard/mpc-groovebox-step-touch-after-erase-uiautomator.xml"
    if ! dump_device_ui "$STEP_TOUCH_ERASED_REMOTE" "$STEP_TOUCH_ERASED_XML"; then
      echo "ERROR: Step Sequencer after-erase hierarchy failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$STEP_TOUCH_ERASED_PNG"; then
      echo "ERROR: Step Sequencer after-erase screenshot failed"; exit 1
    fi
    if ! python3 - "$STEP_TOUCH_ERASED_XML" "$step_touch_target_number" <<'PY'
import sys
import xml.etree.ElementTree as ET
path, number_s = sys.argv[1:]
number = int(number_s)
nodes = list(ET.parse(path).getroot().iter("node"))
if len([n for n in nodes if n.attrib.get("content-desc") == f"Pad 1 step {number} off"]) != 1:
    raise AssertionError("Step ERASE did not remove the target native event")
if len([n for n in nodes if n.attrib.get("content-desc") == "Pad 1 step 1 off"]) != 1:
    raise AssertionError("Step ERASE changed the empty Step 01 reference")
info = [n for n in nodes if n.attrib.get("content-desc") == "Step Edit event information"]
if len(info) != 1 or f"STEP {number:02d}" not in info[0].attrib.get("text", "") \
        or "EMPTY" not in info[0].attrib.get("text", ""):
    raise AssertionError("Step Edit did not report the removed target as EMPTY")
print(f"Step Sequencer ERASE verified: PAD 01 / STEP {number} empty; Step 01 fixture unchanged.")
PY
    then
      echo "ERROR: Step Sequencer did not restore the native empty-step state"; cat "$STEP_TOUCH_ERASED_XML"; exit 1
    fi

    # Verify page navigation through real touchscreen input after the edit round-trip.
    # The selected relative column must remain empty and map to the matching column on page 2,
    # then return to the same absolute Step without changing Pattern data.
    STEP_TOUCH_PAGE2_PNG="/tmp/mpc-groovebox-step-touch-page2.png"
    STEP_TOUCH_PAGE2_XML="/tmp/mpc-groovebox-step-touch-page2-uiautomator.xml"
    STEP_TOUCH_PAGE2_REMOTE="/sdcard/mpc-groovebox-step-touch-page2-uiautomator.xml"
    if ! tap_visible_description_early "$STEP_TOUCH_ERASED_XML" "MPC Step Sequencer next 16-step page"; then
      echo "ERROR: real touchscreen tap could not open the next Step page"; exit 1
    fi
    sleep 0.25
    if ! dump_device_ui "$STEP_TOUCH_PAGE2_REMOTE" "$STEP_TOUCH_PAGE2_XML"; then
      echo "ERROR: Step page 2 hierarchy capture failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$STEP_TOUCH_PAGE2_PNG"; then
      echo "ERROR: Step page 2 screenshot failed"; exit 1
    fi
    if ! python3 - "$STEP_TOUCH_PAGE2_XML" "$step_touch_target_number" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, relative_s = sys.argv[1:]
relative = int(relative_s)
absolute_step = relative + 16
nodes = list(ET.parse(path).getroot().iter("node"))
info = [n for n in nodes if n.attrib.get("content-desc") == "Step Edit event information"]
if len(info) != 1:
    raise AssertionError("Step Edit event context missing on page 2")
text = info[0].attrib.get("text", "")
if re.search(rf"\bSTEP\s+0*{absolute_step}\b", text) is None:
    raise AssertionError(f"Page 2 selection is not absolute step {absolute_step}: {text!r}")
cell_desc = f"Pad 1 step {relative}"
cells = [n.attrib.get("content-desc", "") for n in nodes
         if n.attrib.get("content-desc", "") in (cell_desc + " on", cell_desc + " off")]
if len(cells) != 1:
    raise AssertionError(f"Page 2 target column is missing or duplicated: {cells!r}")
cell_is_active = cells[0].endswith(" on")
if cell_is_active == ("EMPTY" in text):
    raise AssertionError(f"Step information and visible active-cell state disagree: cell={cells[0]!r}, info={text!r}")
if not cell_is_active:
    if "EMPTY" not in text:
        raise AssertionError(f"Empty page-2 cell should be selected as EMPTY: {text!r}")
else:
    patterns = [r"\bVEL\s+(\d+)\b", r"\bPROB\s+(\d+)\b", r"\bRAT\s+(\d+)x\b", r"\bNUDGE\s+([+-]?\d+)\b", r"\bDUR\s+(\d+)\b"]
    if any(re.search(p, text) is None for p in patterns):
        raise AssertionError(f"Active page-2 event has incomplete native fields: {text!r}")
previous = [n for n in nodes if n.attrib.get("content-desc") == "MPC Step Sequencer previous 16-step page"]
if len(previous) != 1 or previous[0].attrib.get("enabled") != "true" or previous[0].attrib.get("visible-to-user") == "false":
    raise AssertionError("Page 2 does not expose an actionable previous-page control")
print(f"Real-touch Step page 1 → page 2 preserved cursor at absolute step {absolute_step}; targetActive={cell_is_active}.")
PY
    then
      echo "ERROR: real-touch Step page forward navigation failed state assertions"; cat "$STEP_TOUCH_PAGE2_XML"; exit 1
    fi

    STEP_TOUCH_PAGE1_RESTORED_PNG="/tmp/mpc-groovebox-step-touch-page1-restored.png"
    STEP_TOUCH_PAGE1_RESTORED_XML="/tmp/mpc-groovebox-step-touch-page1-restored-uiautomator.xml"
    STEP_TOUCH_PAGE1_RESTORED_REMOTE="/sdcard/mpc-groovebox-step-touch-page1-restored-uiautomator.xml"
    if ! tap_visible_description_early "$STEP_TOUCH_PAGE2_XML" "MPC Step Sequencer previous 16-step page"; then
      echo "ERROR: real touchscreen tap could not return to the original Step page"; exit 1
    fi
    sleep 0.25
    if ! dump_device_ui "$STEP_TOUCH_PAGE1_RESTORED_REMOTE" "$STEP_TOUCH_PAGE1_RESTORED_XML"; then
      echo "ERROR: restored Step page 1 hierarchy capture failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$STEP_TOUCH_PAGE1_RESTORED_PNG"; then
      echo "ERROR: restored Step page 1 screenshot failed"; exit 1
    fi
    if ! python3 - "$STEP_TOUCH_PAGE1_RESTORED_XML" "$step_touch_target_number" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, target_s = sys.argv[1:]
target = int(target_s)
nodes = list(ET.parse(path).getroot().iter("node"))
info = [n for n in nodes if n.attrib.get("content-desc") == "Step Edit event information"]
if len(info) != 1:
    raise AssertionError("Step Edit event context missing after returning to page 1")
text = info[0].attrib.get("text", "")
if re.search(rf"\bSTEP\s+0*{target}\b", text) is None or "EMPTY" not in text:
    raise AssertionError(f"Returning to page 1 did not restore the original empty selection: {text!r}")
for description in (f"Pad 1 step {target} off", "Pad 1 step 1 off"):
    if len([n for n in nodes if n.attrib.get("content-desc") == description]) != 1:
        raise AssertionError(f"Page return changed or lost expected empty cell: {description!r}")
next_page = [n for n in nodes if n.attrib.get("content-desc") == "MPC Step Sequencer next 16-step page"]
if len(next_page) != 1 or next_page[0].attrib.get("enabled") != "true" or next_page[0].attrib.get("visible-to-user") == "false":
    raise AssertionError("Returned page 1 does not expose an actionable next-page control")
print(f"Real-touch Step page 2 → page 1 restored selected empty step {target}; event fixture unchanged.")
PY
    then
      echo "ERROR: real-touch Step page return failed state assertions"; cat "$STEP_TOUCH_PAGE1_RESTORED_XML"; exit 1
    fi

    # Exercise Pad selection through real visible PAD +/- touch controls.
    # The absolute Step cursor stays selected while its displayed event follows the Pad.
    STEP_TOUCH_PAD2_PNG="/tmp/mpc-groovebox-step-touch-pad2.png"
    STEP_TOUCH_PAD2_XML="/tmp/mpc-groovebox-step-touch-pad2-uiautomator.xml"
    STEP_TOUCH_PAD2_REMOTE="/sdcard/mpc-groovebox-step-touch-pad2-uiautomator.xml"
    if ! tap_visible_text_early "$STEP_TOUCH_PAGE1_RESTORED_XML" "PAD +"; then
      echo "ERROR: real touchscreen PAD + control was not actionable"; exit 1
    fi
    sleep 0.25
    if ! dump_device_ui "$STEP_TOUCH_PAD2_REMOTE" "$STEP_TOUCH_PAD2_XML"; then
      echo "ERROR: Pad 2 hierarchy capture failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$STEP_TOUCH_PAD2_PNG"; then
      echo "ERROR: Pad 2 screenshot failed"; exit 1
    fi
    if ! python3 - "$STEP_TOUCH_PAD2_XML" "$step_touch_target_number" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, target_s = sys.argv[1:]
target = int(target_s)
nodes = list(ET.parse(path).getroot().iter("node"))
pad = [n for n in nodes if n.attrib.get("text", "").strip() == "PAD 02"]
if len(pad) != 1:
    raise AssertionError("PAD + did not select Pad 2; visible pad labels: " + repr([(n.attrib.get("text", ""), n.attrib.get("content-desc", "")) for n in nodes if "PAD" in n.attrib.get("text", "")]))
info = [n for n in nodes if n.attrib.get("content-desc") == "Step Edit event information"]
if len(info) != 1:
    raise AssertionError("Step Edit event information missing after Pad selection")
text = info[0].attrib.get("text", "")
if re.search(rf"\bSTEP\s+0*{target}\b", text) is None:
    raise AssertionError(f"Pad selection changed the absolute step cursor: {text!r}")
cell_prefix = f"Pad 2 step {target}"
cells = [n.attrib.get("content-desc", "") for n in nodes
         if n.attrib.get("content-desc", "") in (cell_prefix + " on", cell_prefix + " off")]
if len(cells) != 1:
    raise AssertionError(f"Step target is not projected for the selected Pad 2: {cells!r}")
active = cells[0].endswith(" on")
if active == ("EMPTY" in text):
    raise AssertionError(f"Pad 2 cell and native Step event projection disagree: {cells[0]!r}, {text!r}")
if active:
    patterns = [r"\bVEL\s+(\d+)\b", r"\bPROB\s+(\d+)\b", r"\bRAT\s+(\d+)x\b", r"\bNUDGE\s+([+-]?\d+)\b", r"\bDUR\s+(\d+)\b"]
    if any(re.search(p, text) is None for p in patterns):
        raise AssertionError(f"Pad 2 active event has incomplete native fields: {text!r}")
print(f"Real-touch PAD + selected Pad 2; absolute step {target} preserved and projection matches selected-Pad state (active={active}).")
PY
    then
      echo "ERROR: real-touch Pad 2 selection failed state assertions"; cat "$STEP_TOUCH_PAD2_XML"; exit 1
    fi

    STEP_TOUCH_PAD1_RESTORED_PNG="/tmp/mpc-groovebox-step-touch-pad1-restored.png"
    STEP_TOUCH_PAD1_RESTORED_XML="/tmp/mpc-groovebox-step-touch-pad1-restored-uiautomator.xml"
    STEP_TOUCH_PAD1_RESTORED_REMOTE="/sdcard/mpc-groovebox-step-touch-pad1-restored-uiautomator.xml"
    if ! tap_visible_text_early "$STEP_TOUCH_PAD2_XML" "PAD −"; then
      echo "ERROR: real touchscreen PAD − control was not actionable"; exit 1
    fi
    sleep 0.25
    if ! dump_device_ui "$STEP_TOUCH_PAD1_RESTORED_REMOTE" "$STEP_TOUCH_PAD1_RESTORED_XML"; then
      echo "ERROR: restored Pad 1 hierarchy capture failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$STEP_TOUCH_PAD1_RESTORED_PNG"; then
      echo "ERROR: restored Pad 1 screenshot failed"; exit 1
    fi
    if ! python3 - "$STEP_TOUCH_PAD1_RESTORED_XML" "$step_touch_target_number" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, target_s = sys.argv[1:]
target = int(target_s)
nodes = list(ET.parse(path).getroot().iter("node"))
pad = [n for n in nodes if n.attrib.get("text", "").strip() == "PAD 01"]
if len(pad) != 1:
    raise AssertionError("PAD − did not restore Pad 1")
for description in (f"Pad 1 step {target} off", "Pad 1 step 1 off"):
    if len([n for n in nodes if n.attrib.get("content-desc") == description]) != 1:
        raise AssertionError(f"Pad 1 state not restored or an event was modified: {description!r}")
info = [n for n in nodes if n.attrib.get("content-desc") == "Step Edit event information"]
if len(info) != 1:
    raise AssertionError("Step Edit information missing after restoring Pad 1")
text = info[0].attrib.get("text", "")
if re.search(rf"\bSTEP\s+0*{target}\b", text) is None or "EMPTY" not in text:
    raise AssertionError(f"Pad 1 selected-step state was not restored: {text!r}")
print(f"Real-touch PAD − restored Pad 1, selected empty step {target}, and the Step 01 fixture unchanged.")
PY
    then
      echo "ERROR: real-touch Pad 1 restore failed state assertions"; cat "$STEP_TOUCH_PAD1_RESTORED_XML"; exit 1
    fi

    if ! adb_bounded 10 shell "run-as $PACKAGE sh -c 'echo $step_touch_target_number > cache/mpc-step-touch-target'"; then
      echo "ERROR: could not report the Step target to the native audit"; exit 1
    fi
    if ! adb_bounded 10 shell run-as "$PACKAGE" touch cache/mpc-step-touch-capture-release; then
      echo "ERROR: could not release Step Sequencer touch-acceptance checkpoint"; exit 1
    fi
    step_touch_capture_done=1
    echo "Step Sequencer real-touch → native add → probability edit → native remove round-trip passed."
  fi

  if [ "$main_runtime_capture_done" -eq 0 ] \
      && grep -Fq -- "UI_MAIN_CAPTURE_READY" <<<"$log_snapshot"; then
    echo "Capturing ready Main workspace and Android accessibility hierarchy..."
    if ! adb_bounded 10 exec-out screencap -p > /tmp/mpc-groovebox-main-ready.png; then
      echo "ERROR: ready Main screenshot capture failed"
      exit 1
    fi
    if ! timeout --signal=TERM --kill-after=3s 30s \
        adb shell uiautomator dump /sdcard/mpc-groovebox-main-ready-uiautomator.xml \
        > /tmp/mpc-groovebox-main-ready-uiautomator.log 2>&1; then
      cat /tmp/mpc-groovebox-main-ready-uiautomator.log || true
      echo "ERROR: ready Main accessibility dump failed"
      exit 1
    fi
    if ! adb_bounded 10 exec-out cat /sdcard/mpc-groovebox-main-ready-uiautomator.xml \
        > /tmp/mpc-groovebox-main-ready-uiautomator.xml; then
      echo "ERROR: ready Main hierarchy retrieval failed"
      exit 1
    fi
    if ! test -s /tmp/mpc-groovebox-main-ready.png \
        || ! test -s /tmp/mpc-groovebox-main-ready-uiautomator.xml; then
      echo "ERROR: ready Main screenshot or hierarchy is empty"
      exit 1
    fi
    if ! python3 - /tmp/mpc-groovebox-main-ready-uiautomator.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
nodes = list(root.iter("node"))
required = [
    "MPC One Main Toolbar",
    "MPC Toolbar Menu",
    "MPC shortcut BROWSER",
    "Main Track and Arrangement workspace",
    "MPC Main XL Mixer Strips",
    "MPC Main mixer strips shown",
]
def dimensions(node):
    m = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",
                     node.attrib.get("bounds", ""))
    if not m:
        raise AssertionError("invalid bounds for " + repr(node.attrib))
    x1, y1, x2, y2 = map(int, m.groups())
    return x1, y1, x2, y2

for description in required:
    found = [n for n in nodes if n.attrib.get("content-desc") == description]
    if len(found) != 1:
        raise AssertionError(f"expected exactly one Main control {description!r}; got {len(found)}")
    x1, y1, x2, y2 = dimensions(found[0])
    if x2 <= x1 or y2 <= y1 or found[0].attrib.get("visible-to-user") == "false":
        raise AssertionError(f"Main control is not visible with positive bounds: {description!r}")
track_types = [
    n for n in nodes
    if n.attrib.get("content-desc", "").startswith("Main Track Type DRUM ")
]
if len(track_types) != 1:
    raise AssertionError(f"expected one truthful Main Track Type state; got {len(track_types)}")
if "type conversion unavailable" not in track_types[0].attrib.get("content-desc", ""):
    raise AssertionError("Main Track Type must not advertise a missing conversion operation")
print("Ready Main accessibility controls verified:", len(required), "+ Track Type truthfulness")
PY
    then
      echo "ERROR: ready Main hierarchy failed canonical control checks"
      cat /tmp/mpc-groovebox-main-ready-uiautomator.xml
      exit 1
    fi
    # Assert the live layout relationship, not emulator-specific coordinates:
    # the legacy weighted mixer must be absent and the XL mixer must occupy
    # nearly the full persistent context column with its show/hide target inside it.
    if ! python3 - /tmp/mpc-groovebox-main-ready-uiautomator.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
nodes = list(root.iter("node"))

def rect(node, label):
    match = re.fullmatch(
        r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",
        node.attrib.get("bounds", ""))
    if not match:
        raise AssertionError(f"invalid Android accessibility bounds for {label}")
    return tuple(map(int, match.groups()))

def one(description):
    found = [node for node in nodes
             if node.attrib.get("content-desc", "") == description]
    if len(found) != 1:
        raise AssertionError(
            f"expected exactly one {description!r}, got {len(found)}")
    return found[0]

legacy = [node for node in nodes
          if node.attrib.get("content-desc", "") == "MPC condensed Mixer Strip"]
if legacy:
    raise AssertionError("legacy duplicate condensed mixer panel remains in Main hierarchy")

area = one("MPC shell mixer strips")
mixer = one("MPC Main XL Mixer Strips")
toggle = one("MPC Main mixer strips shown")
ax1, ay1, ax2, ay2 = rect(area, "persistent shell context area")
sx1, sy1, sx2, sy2 = rect(mixer, "Main XL mixer strips")
tx1, ty1, tx2, ty2 = rect(toggle, "Main XL mixer visibility control")

area_width, area_height = ax2 - ax1, ay2 - ay1
mixer_width, mixer_height = sx2 - sx1, sy2 - sy1
if min(area_width, area_height, mixer_width, mixer_height) <= 0:
    raise AssertionError("Main shell or XL mixer has non-positive bounds")

vertical_fill = mixer_height / area_height
horizontal_fill = mixer_width / area_width
if vertical_fill < 0.75:
    raise AssertionError(
        f"Main XL mixer fills only {vertical_fill:.3f} of the context height")
if horizontal_fill < 0.85:
    raise AssertionError(
        f"Main XL mixer fills only {horizontal_fill:.3f} of the context width")
if not (sx1 <= tx1 < tx2 <= sx2 and sy1 <= ty1 < ty2 <= sy2):
    raise AssertionError("Main XL mixer show/hide hit target escaped strip bounds")

print(
    f"Main XL geometry verified from live bounds: vertical={vertical_fill:.3f}, "
    f"horizontal={horizontal_fill:.3f}; legacy panel absent; toggle hit target contained")
PY
    then
      echo "ERROR: Main XL Channel Strip runtime geometry invariant failed"
      cat /tmp/mpc-groovebox-main-ready-uiautomator.xml
      exit 1
    fi
    if ! adb_bounded 10 shell run-as "$PACKAGE" touch cache/mpc-main-capture-release; then
      echo "ERROR: could not release app-side Main capture checkpoint"
      exit 1
    fi
    main_runtime_capture_done=1
  fi

  if [ "$browser_runtime_capture_done" -eq 0 ] \
      && grep -Fq -- "UI_BROWSER_CAPTURE_READY" <<<"$log_snapshot"; then
    echo "Capturing actual rendered Browser screen and Android accessibility hierarchy..."
    if ! adb_bounded 10 exec-out screencap -p > /tmp/mpc-groovebox-browser-device.png; then
      echo "ERROR: real Browser emulator screenshot capture failed"
      exit 1
    fi
    if ! timeout --signal=TERM --kill-after=3s 30s \
        adb shell uiautomator dump /sdcard/mpc-groovebox-browser-uiautomator.xml \
        > /tmp/mpc-groovebox-browser-uiautomator.log 2>&1; then
      cat /tmp/mpc-groovebox-browser-uiautomator.log || true
      echo "ERROR: Browser-context uiautomator dump failed or timed out"
      exit 1
    fi
    if ! adb_bounded 10 exec-out cat /sdcard/mpc-groovebox-browser-uiautomator.xml \
        > /tmp/mpc-groovebox-browser-uiautomator.xml; then
      echo "ERROR: Browser-context UI hierarchy retrieval failed"
      exit 1
    fi
    if ! test -s /tmp/mpc-groovebox-browser-device.png \
        || ! test -s /tmp/mpc-groovebox-browser-uiautomator.xml; then
      echo "ERROR: real Browser screenshot or accessibility hierarchy is empty"
      exit 1
    fi
    for required in 'content-desc="MPC Browser filter SAMPLES"' \
        'content-desc="MPC Browser scan local audio files"' \
        'text="SAMPLE ASSIGN"' 'text="LOAD"' 'text="UP"'; do
      if ! grep -Fq -- "$required" /tmp/mpc-groovebox-browser-uiautomator.xml; then
        echo "ERROR: Browser platform hierarchy is missing expected visible control: $required"
        cat /tmp/mpc-groovebox-browser-uiautomator.xml
        exit 1
      fi
    done

    echo "Checking actual Android accessibility hit bounds for Browser Options and Function Bar..."
    if ! python3 - /tmp/mpc-groovebox-browser-uiautomator.xml <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
nodes = list(root.iter("node"))
def bounds(node):
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",
                         node.attrib.get("bounds", ""))
    if match is None:
        raise AssertionError("malformed accessibility bounds: " + repr(node.attrib))
    return tuple(map(int, match.groups()))

def one(description):
    found = [node for node in nodes if node.attrib.get("content-desc") == description]
    if len(found) != 1:
        raise AssertionError(f"expected one accessibility node for {description!r}; got {len(found)}")
    return found[0]

options = one("MPC Browser Options")
x1, y1, x2, y2 = bounds(options)
if x2 <= x1 or y2 <= y1:
    raise AssertionError("Browser Options has zero/empty Android accessibility bounds: "
                         + options.attrib.get("bounds", ""))
root_bounds = bounds(nodes[0])
if x2 > root_bounds[2] or y2 > root_bounds[3]:
    raise AssertionError("Browser Options is outside the accessible screen bounds: "
                         + options.attrib.get("bounds", ""))
if root_bounds[2] - x2 > 48:
    raise AssertionError("Browser Options is separated from the safe right edge by an unexpected gap: "
                         + options.attrib.get("bounds", ""))

descriptions = [
    "MPC Browser Sample Assign unavailable: no source item selected",
    "MPC Browser audition unavailable: no source item selected",
    "MPC Browser LOAD source file with Android Document Picker",
    "Browser UP from CONTENT",
    "MPC Browser BACK to previous workspace",
]
widths = []
for description in descriptions:
    node = one(description)
    left, top, right, bottom = bounds(node)
    if right <= left or bottom <= top:
        raise AssertionError(f"empty hit bounds for {description!r}: {node.attrib.get('bounds')}")
    widths.append(right - left)
tolerance = max(4, round(max(widths) * 0.08))
if max(widths) - min(widths) > tolerance:
    raise AssertionError(f"Function Bar action hit widths are unequal: {widths} (tolerance {tolerance})")
print("Browser Options bounds:", (x1, y1, x2, y2))
print("Function Bar accessibility widths:", widths)
PY
    then
      echo "ERROR: Browser accessibility bounds are clipped or Function Bar targets are unequal"
      cat /tmp/mpc-groovebox-browser-uiautomator.xml
      exit 1
    fi

    if ! adb_bounded 10 shell run-as "$PACKAGE" \
        touch cache/mpc-browser-capture-release; then
      echo "ERROR: could not release app-side Browser capture checkpoint"
      exit 1
    fi
    browser_runtime_capture_done=1
  fi

  if [ "$track_view_mute_capture_done" -eq 0 ] \
      && grep -Fq -- "UI_TRACK_VIEW_MUTED_CAPTURE_READY:" <<<"$log_snapshot"; then
    echo "Capturing Track View mute state and Android accessibility hierarchy..."
    if ! adb_bounded 10 exec-out screencap -p > /tmp/mpc-groovebox-track-view-muted.png; then
      echo "ERROR: Track View mute-state screenshot capture failed"; exit 1
    fi
    if ! timeout --signal=TERM --kill-after=3s 30s \
        adb shell uiautomator dump /sdcard/mpc-groovebox-track-view-muted-uiautomator.xml \
        > /tmp/mpc-groovebox-track-view-muted-uiautomator.log 2>&1; then
      cat /tmp/mpc-groovebox-track-view-muted-uiautomator.log || true
      echo "ERROR: Track View mute-state accessibility dump failed"; exit 1
    fi
    if ! adb_bounded 10 exec-out cat /sdcard/mpc-groovebox-track-view-muted-uiautomator.xml \
        > /tmp/mpc-groovebox-track-view-muted-uiautomator.xml; then
      echo "ERROR: Track View mute-state hierarchy retrieval failed"; exit 1
    fi
    track_view_mute_state_line=$(grep -F -- "UI_TRACK_VIEW_MUTED_CAPTURE_READY:" <<<"$log_snapshot" | tail -n 1)
    case "$track_view_mute_state_line" in
      *mutedAfterToggle=true*) track_view_mute_target=true ;;
      *mutedAfterToggle=false*) track_view_mute_target=false ;;
      *) echo "ERROR: Track View mute capture marker omitted its target state"; exit 1 ;;
    esac
    if ! python3 - /tmp/mpc-groovebox-track-view-muted-uiautomator.xml "$track_view_mute_target" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

path, expected_muted = sys.argv[1], sys.argv[2]
if expected_muted not in ("true", "false"):
    raise SystemExit(f"Track View mute capture marker lacks target state: {expected_muted!r}")
root = ET.parse(path).getroot()
nodes = [
    node for node in root.iter("node")
    if re.fullmatch(r"Track View track \d+ mute", node.attrib.get("content-desc", ""))
]
if len(nodes) != 1:
    raise SystemExit(f"expected one actionable Track View mute node, found {len(nodes)}")
node = nodes[0]
expected_text = "MUTE" if expected_muted == "true" else "M"
actual_text = node.attrib.get("text", "")
if actual_text != expected_text:
    raise SystemExit(f"Track View native mute target is {expected_muted}, but UI label is {actual_text!r}")
if node.attrib.get("enabled") != "true" or node.attrib.get("clickable") != "true":
    raise SystemExit("Track View mute state is visible but not actionable")
bounds = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
if not bounds:
    raise SystemExit(f"Track View mute control has invalid bounds: {node.attrib.get('bounds')!r}")
x1, y1, x2, y2 = map(int, bounds.groups())
if x2 <= x1 or y2 <= y1:
    raise SystemExit(f"Track View mute control has empty bounds: {node.attrib.get('bounds')!r}")
print(f"Track View mute state captured: label={actual_text} expectedMuted={expected_muted} bounds={(x1, y1, x2, y2)}")
PY
    then
      echo "ERROR: captured Track View mute state does not match the actual native model"
      cat /tmp/mpc-groovebox-track-view-muted-uiautomator.xml
      exit 1
    fi
    if ! test -s /tmp/mpc-groovebox-track-view-muted.png; then
      echo "ERROR: Track View mute-state screenshot is empty"; exit 1
    fi
    if ! adb_bounded 10 shell run-as "$PACKAGE" touch cache/mpc-track-view-muted-capture-release; then
      echo "ERROR: could not release app-side Track View mute capture checkpoint"; exit 1
    fi
    track_view_mute_capture_done=1
  fi

  if [ "$arrangement_grid_focus_capture_done" -eq 0 ] \
      && grep -Fq -- "UI_ARRANGEMENT_GRID_FOCUS_CAPTURE_READY" <<<"$log_snapshot"; then
    echo "Capturing Arrangement-to-Grid focused-step screenshot and accessibility hierarchy..."
    if ! adb_bounded 10 exec-out screencap -p > /tmp/mpc-groovebox-arrange-grid-focus.png; then
      echo "ERROR: Arrangement-to-Grid focus screenshot capture failed"
      exit 1
    fi
    if ! timeout --signal=TERM --kill-after=3s 30s \
        adb shell uiautomator dump /sdcard/mpc-groovebox-arrange-grid-focus-uiautomator.xml \
        > /tmp/mpc-groovebox-arrange-grid-focus-uiautomator.log 2>&1; then
      cat /tmp/mpc-groovebox-arrange-grid-focus-uiautomator.log || true
      echo "ERROR: Arrangement-to-Grid focus accessibility dump failed"
      exit 1
    fi
    if ! adb_bounded 10 exec-out cat /sdcard/mpc-groovebox-arrange-grid-focus-uiautomator.xml \
        > /tmp/mpc-groovebox-arrange-grid-focus-uiautomator.xml; then
      echo "ERROR: Arrangement-to-Grid focus hierarchy retrieval failed"
      exit 1
    fi
    if ! test -s /tmp/mpc-groovebox-arrange-grid-focus.png \
        || ! test -s /tmp/mpc-groovebox-arrange-grid-focus-uiautomator.xml; then
      echo "ERROR: Arrangement-to-Grid focus screenshot or hierarchy is empty"
      exit 1
    fi

    focus_line="$(grep -F -- 'UI_ARRANGEMENT_GRID_FOCUS_COMPLETE:' <<<"$log_snapshot" | tail -n 1 || true)"
    if [[ -z "$focus_line" ]]; then
      echo "ERROR: UI capture checkpoint has no matching focused-step assertion"
      exit 1
    fi
    target_step="$(sed -nE 's/.*targetStep=([0-9]+).*/\1/p' <<<"$focus_line")"
    first_visible_step="$(sed -nE 's/.*firstVisibleStep=([0-9]+).*/\1/p' <<<"$focus_line")"
    last_visible_step="$(sed -nE 's/.*lastVisibleStep=([0-9]+).*/\1/p' <<<"$focus_line")"
    if [[ -z "$target_step" || -z "$first_visible_step" || -z "$last_visible_step" ]]; then
      echo "ERROR: cannot parse focused-step assertion values: $focus_line"
      exit 1
    fi

    if ! python3 - /tmp/mpc-groovebox-arrange-grid-focus-uiautomator.xml \
        "$target_step" "$first_visible_step" "$last_visible_step" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

path, target_s, first_s, last_s = sys.argv[1:]
target, first, last = map(int, (target_s, first_s, last_s))
nodes = list(ET.parse(path).getroot().iter("node"))

def bounds(node):
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",
                         node.attrib.get("bounds", ""))
    if match is None:
        raise AssertionError("invalid Android accessibility bounds: " + repr(node.attrib))
    return tuple(map(int, match.groups()))

def one_description(description):
    found = [node for node in nodes if node.attrib.get("content-desc") == description]
    if len(found) != 1:
        raise AssertionError(f"expected one visible {description!r}, got {len(found)}")
    node = found[0]
    x1, y1, x2, y2 = bounds(node)
    if x2 <= x1 or y2 <= y1 or node.attrib.get("visible-to-user") == "false":
        raise AssertionError(f"not visible/hittable: {description!r}: {node.attrib}")
    return node

one_description("MPC Grid View workspace")
one_description("MPC Grid View drum event grid")

accessible_ranges = [
    node for node in nodes
    if node.attrib.get("content-desc", "").startswith("Grid visible step range: steps ")
]
if len(accessible_ranges) != 1:
    raise AssertionError(f"expected one semantic Grid step-range accessibility node, got {len(accessible_ranges)}")
description = accessible_ranges[0].attrib.get("content-desc", "")
accessible_match = re.fullmatch(
    r"Grid visible step range: steps\s+0*(\d+) through 0*(\d+) of 0*(\d+)",
    description)
if accessible_match is None:
    raise AssertionError(f"Grid step-range accessibility label is malformed: {description!r}")
accessible_first, accessible_last, accessible_total = map(int, accessible_match.groups())
if not accessible_first <= target <= accessible_last <= accessible_total:
    raise AssertionError(f"focused step {target} is outside accessible Grid window {description!r}")

ranges = []
for node in nodes:
    text = node.attrib.get("text", "")
    match = re.fullmatch(r"0*(\d+)[–-]0*(\d+)\s*/\s*0*(\d+)", text)
    if match:
        ranges.append(tuple(map(int, match.groups())))
if len(ranges) != 1:
    raise AssertionError(f"expected one visible Grid step-range label, got {ranges}")
shown_first, shown_last, total = ranges[0]
if (shown_first, shown_last) != (first, last):
    raise AssertionError(f"Grid window disagrees with focused route: UI={(shown_first, shown_last)}, log={(first, last)}")
if not shown_first <= target <= shown_last <= total:
    raise AssertionError(f"focused step {target} is outside visible Grid window {ranges[0]}")
if (accessible_first, accessible_last, accessible_total) != (first, last, total):
    raise AssertionError("Grid text and semantic accessibility range disagree")
print(f"Arrange-to-Grid focus evidence verified: step {target}, visible {first}–{last} of {total}")
PY
    then
      echo "ERROR: Arrangement-to-Grid focus screenshot/accessibility hierarchy failed semantic checks"
      cat /tmp/mpc-groovebox-arrange-grid-focus-uiautomator.xml
      exit 1
    fi
    echo "Running real Grid DRAW → native note state → ERASE → empty-state round-trip..."
    grid_density_info="$(adb_bounded 10 shell wm density)" || {
      echo "ERROR: could not read Android density for Grid cell hit testing"; exit 1;
    }
    grid_edit_coordinates="$(python3 - \
      /tmp/mpc-groovebox-arrange-grid-focus-uiautomator.xml \
      "$grid_density_info" "$target_step" "$first_visible_step" "$last_visible_step" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, density_text, target_s, first_s, last_s = sys.argv[1:]
target, first, last = map(int, (target_s, first_s, last_s))
root = ET.parse(path).getroot()
nodes = list(root.iter("node"))
def bounds(node):
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",
                         node.attrib.get("bounds", ""))
    if match is None: raise AssertionError("invalid bounds " + repr(node.attrib))
    return tuple(map(int, match.groups()))
grid = [n for n in nodes if n.attrib.get("content-desc") == "MPC Grid View drum event grid"]
if len(grid) != 1: raise AssertionError(f"expected one Grid canvas, got {len(grid)}")
left, top, right, bottom = bounds(grid[0])
density_matches = re.findall(r"Override density:\s*(\d+)", density_text)
if not density_matches: density_matches = re.findall(r"Physical density:\s*(\d+)", density_text)
if not density_matches: raise AssertionError("unparseable Android density: " + repr(density_text))
density = int(density_matches[-1]) / 160.0
range_nodes = [n for n in nodes if n.attrib.get("content-desc", "").startswith("Grid visible step range: steps ")]
if len(range_nodes) != 1: raise AssertionError(f"expected one Grid range, got {len(range_nodes)}")
m = re.fullmatch(r"Grid visible step range: steps\s+0*(\d+) through 0*(\d+) of 0*(\d+)",
                 range_nodes[0].attrib.get("content-desc", ""))
if m is None: raise AssertionError("malformed Grid range")
shown_first, shown_last, total = map(int, m.groups())
if (shown_first, shown_last) != (first, last): raise AssertionError("Grid range disagrees with route")
visible_steps = shown_last - shown_first + 1
if visible_steps != 16: raise AssertionError(f"expected 16 visible step columns, got {visible_steps}")
event_counts = [int(m.group(1)) for n in nodes
                for m in [re.search(r"\bevents=(\d+)\b", n.attrib.get("text", ""))] if m]
if not event_counts or any(count != 1 for count in event_counts):
    raise AssertionError(f"fixture must have exactly one seeded note, got {event_counts}")
edit_step = target + 1 if target < shown_last else target - 1
if edit_step == target or not shown_first <= edit_step <= shown_last:
    raise AssertionError("no adjacent empty step is available")
# Grid launches at its default 16-pad view; Pad 01 is the bottom row.
visible_pads = 16
column, row = edit_step - shown_first, visible_pads - 1
left_inset, top_inset, end_inset = 42*density, 24*density, 6*density
plot_width = max(1.0, (right-left)-left_inset-end_inset)
plot_height = max(1.0, (bottom-top)-top_inset-end_inset)
x = round(left + left_inset + (column+0.5)*plot_width/visible_steps)
y = round(top + top_inset + (row+0.5)*plot_height/visible_pads)
if not (left < x < right and top < y < bottom): raise AssertionError(f"Grid cell tap {(x,y)} escaped bounds")
print(x, y, edit_step)
PY
    )" || {
      echo "ERROR: could not derive a valid adjacent empty Grid cell from live bounds"; exit 1;
    }
    read -r grid_edit_x grid_edit_y grid_edit_step <<< "$grid_edit_coordinates"
    echo "Grid round-trip target: PAD 01 STEP $grid_edit_step at $grid_edit_x,$grid_edit_y"
    if ! adb_bounded 10 shell input tap "$grid_edit_x" "$grid_edit_y"; then
      echo "ERROR: real Grid DRAW cell tap failed"; exit 1
    fi
    sleep 0.35
    GRID_EDIT_DRAW_XML="/tmp/mpc-groovebox-grid-edit-after-draw-uiautomator.xml"
    GRID_EDIT_DRAW_REMOTE="/sdcard/mpc-groovebox-grid-edit-after-draw-uiautomator.xml"
    GRID_EDIT_DRAW_PNG="/tmp/mpc-groovebox-grid-edit-after-draw.png"
    if ! dump_device_ui "$GRID_EDIT_DRAW_REMOTE" "$GRID_EDIT_DRAW_XML"; then
      echo "ERROR: could not read Grid state after drawing an empty cell"; exit 1
    fi
    if ! python3 - "$GRID_EDIT_DRAW_XML" "$grid_edit_step" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, step_s = sys.argv[1:]
step = int(step_s)
nodes = list(ET.parse(path).getroot().iter("node"))
prefix = f"Grid selection: PAD 01, STEP {step:02d}, note present, velocity "
found = [n for n in nodes if n.attrib.get("content-desc", "").startswith(prefix)]
if len(found) != 1:
    raise AssertionError(f"DRAW did not expose native note state {prefix!r}; selected labels={[n.attrib.get('content-desc') for n in nodes if 'Grid selection:' in n.attrib.get('content-desc','')]}")
m = re.fullmatch(re.escape(prefix) + r"(\d+)", found[0].attrib.get("content-desc", ""))
if m is None or not 1 <= int(m.group(1)) <= 127:
    raise AssertionError("Grid DRAW reported invalid native MIDI velocity")
print(f"Grid DRAW verified: PAD 01 STEP {step:02d}, velocity {m.group(1)}")
PY
    then
      echo "ERROR: Grid DRAW did not produce a native note"; cat "$GRID_EDIT_DRAW_XML"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$GRID_EDIT_DRAW_PNG"; then
      echo "ERROR: Grid DRAW screenshot capture failed"; exit 1
    fi


    # Real Grid event parameter acceptance. Cycle through all five native fields;
    # change one field at a time, prove isolation, restore the full event, then erase.
    if ! tap_visible_description_early "$GRID_EDIT_DRAW_XML" "Grid Select tool"; then
      echo "ERROR: Grid SELECT tool was not actionable after DRAW"; exit 1
    fi
    sleep 0.2
    GRID_EDIT_SELECT_TOOL_XML="/tmp/mpc-groovebox-grid-event-select-tool-uiautomator.xml"
    GRID_EDIT_SELECT_TOOL_REMOTE="/sdcard/mpc-groovebox-grid-event-select-tool-uiautomator.xml"
    if ! dump_device_ui "$GRID_EDIT_SELECT_TOOL_REMOTE" "$GRID_EDIT_SELECT_TOOL_XML"; then
      echo "ERROR: Grid SELECT tool hierarchy capture failed"; exit 1
    fi
    if ! adb_bounded 10 shell input tap "$grid_edit_x" "$grid_edit_y"; then
      echo "ERROR: real Grid note selection touch failed"; exit 1
    fi
    sleep 0.3

    grid_event_current_xml=""
    grid_event_read_state() {
      python3 - "$1" "$grid_edit_step" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, step_s = sys.argv[1:]
step = int(step_s)
nodes = list(ET.parse(path).getroot().iter("node"))
prefix = f"Grid event parameters: PAD 01, STEP {step:02d}, VEL "
states = [n.attrib.get("content-desc", "") for n in nodes
          if n.attrib.get("content-desc", "").startswith(prefix)]
if len(states) != 1:
    raise AssertionError(f"expected one full selected Grid event state, got {states!r}")
match = re.fullmatch(
    r"Grid event parameters: PAD 01, STEP 0*(\d+), VEL (\d+), PROB (\d+), "
    r"RAT (\d+)x, NUDGE ([+-]?\d+), DUR (\d+)", states[0])
if match is None:
    raise AssertionError(f"malformed Grid event parameter state: {states[0]!r}")
values = list(map(int, match.groups()))
if values[0] != step or not 1 <= values[1] <= 127 or not 0 <= values[2] <= 127 \
        or not 1 <= values[3] <= 8 or not -960 <= values[4] <= 960 or values[5] <= 0:
    raise AssertionError(f"Grid event values outside supported native bounds: {values!r}")
resolution = [n.attrib.get("content-desc", "") for n in nodes
              if n.attrib.get("content-desc", "").startswith("Grid quantize resolution: ")]
if len(resolution) != 1:
    raise AssertionError(f"Grid tick resolution is not exposed accessibly: {resolution!r}")
tick_match = re.search(r"\((\d+) ticks\)$", resolution[0])
if tick_match is None or int(tick_match.group(1)) < 1:
    raise AssertionError(f"malformed accessible Grid resolution: {resolution[0]!r}")
print(*values, int(tick_match.group(1)))
PY
    }

    grid_event_test_parameter() {
      local parameter_name="$1"
      local parameter_token="$2"
      local parameter_index="$3"
      local parameter_minimum="$4"
      local parameter_maximum="$5"
      local selected_xml="/tmp/mpc-groovebox-grid-event-$parameter_name-selected-uiautomator.xml"
      local selected_remote="/sdcard/mpc-groovebox-grid-event-$parameter_name-selected-uiautomator.xml"
      local selected_png="/tmp/mpc-groovebox-grid-event-$parameter_name-selected.png"
      local changed_xml="/tmp/mpc-groovebox-grid-event-$parameter_name-changed-uiautomator.xml"
      local changed_remote="/sdcard/mpc-groovebox-grid-event-$parameter_name-changed-uiautomator.xml"
      local changed_png="/tmp/mpc-groovebox-grid-event-$parameter_name-changed.png"
      local restored_xml="/tmp/mpc-groovebox-grid-event-$parameter_name-restored-uiautomator.xml"
      local restored_remote="/sdcard/mpc-groovebox-grid-event-$parameter_name-restored-uiautomator.xml"
      local restored_png="/tmp/mpc-groovebox-grid-event-$parameter_name-restored.png"
      local current_state step_before velocity_before probability_before ratchet_before nudge_before duration_before grid_ticks
      local current_value increment direction action_description restore_description next_value

      if [ "$parameter_name" != "velocity" ]; then
        if ! tap_visible_description_early "$grid_event_current_xml" "Grid event parameter cycle"; then
          echo "ERROR: Grid parameter cycle action failed before $parameter_token"; exit 1
        fi
        sleep 0.2
      fi
      if ! dump_device_ui "$selected_remote" "$selected_xml"; then
        echo "ERROR: Grid $parameter_token selected-state hierarchy failed"; exit 1
      fi
      if ! adb_bounded 10 exec-out screencap -p > "$selected_png"; then
        echo "ERROR: Grid $parameter_token selected-state screenshot failed"; exit 1
      fi
      current_state="$(grid_event_read_state "$selected_xml")" || {
        echo "ERROR: Grid $parameter_token state unavailable before edit"; cat "$selected_xml"; exit 1;
      }
      read -r step_before velocity_before probability_before ratchet_before nudge_before duration_before grid_ticks <<< "$current_state"
      if [ "$parameter_name" = "duration" ]; then
        parameter_minimum=$((grid_ticks / 4))
        if [ "$parameter_minimum" -lt 1 ]; then parameter_minimum=1; fi
        parameter_maximum=$((grid_ticks * 4))
      fi

      if ! python3 - "$selected_xml" "$parameter_token" "$parameter_name" <<'PY'
import sys
import xml.etree.ElementTree as ET
path, token, name = sys.argv[1:]
nodes = list(ET.parse(path).getroot().iter("node"))
cycle = [n for n in nodes if n.attrib.get("content-desc") == "Grid event parameter cycle"]
if len(cycle) != 1 or cycle[0].attrib.get("text") != "PARAM " + token:
    raise AssertionError(f"Grid field cycle control mismatch for {name}: {[n.attrib for n in cycle]!r}")
plus = [n for n in nodes if n.attrib.get("content-desc") == "Grid event " + name + " plus"]
minus = [n for n in nodes if n.attrib.get("content-desc") == "Grid event " + name + " minus"]
if len(plus) != 1 or len(minus) != 1:
    raise AssertionError(f"Grid {name} must expose its own native parameter +/- controls")
print(f"Grid selected {name.upper()} field with dedicated +/- controls.")
PY
      then
        echo "ERROR: Grid parameter controls did not follow PARAM cycle"; cat "$selected_xml"; exit 1
      fi

      case "$parameter_name" in
        velocity) current_value="$velocity_before"; increment=1 ;;
        probability) current_value="$probability_before"; increment=1 ;;
        ratchet) current_value="$ratchet_before"; increment=1 ;;
        nudge) current_value="$nudge_before"; increment=60 ;;
        duration)
          current_value="$duration_before"
          increment=$((grid_ticks / 4))
          if [ "$increment" -lt 1 ]; then increment=1; fi
          ;;
        *) echo "ERROR: unknown Grid field $parameter_name"; exit 1 ;;
      esac
      direction=1
      if [ "$current_value" -ge "$parameter_maximum" ]; then direction=-1; fi
      if [ "$current_value" -le "$parameter_minimum" ]; then direction=1; fi
      if [ "$direction" -gt 0 ]; then
        action_description="Grid event $parameter_name plus"
        restore_description="Grid event $parameter_name minus"
      else
        action_description="Grid event $parameter_name minus"
        restore_description="Grid event $parameter_name plus"
      fi

      if ! tap_visible_description_early "$selected_xml" "$action_description"; then
        echo "ERROR: real Grid $parameter_token adjustment touch failed"; exit 1
      fi
      sleep 0.25
      if ! dump_device_ui "$changed_remote" "$changed_xml"; then
        echo "ERROR: Grid $parameter_token changed-state hierarchy failed"; exit 1
      fi
      if ! adb_bounded 10 exec-out screencap -p > "$changed_png"; then
        echo "ERROR: Grid $parameter_token changed-state screenshot failed"; exit 1
      fi
      if ! python3 - "$selected_xml" "$changed_xml" "$grid_edit_step" "$parameter_name" "$direction" "$grid_ticks" "$parameter_minimum" "$parameter_maximum" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
before_path, after_path, step_s, name, direction_s, ticks_s, minimum_s, maximum_s = sys.argv[1:]
step, direction, ticks, minimum, maximum = map(int, (step_s, direction_s, ticks_s, minimum_s, maximum_s))
indices = {"velocity":1, "probability":2, "ratchet":3, "nudge":4, "duration":5}
increments = {"velocity":1, "probability":1, "ratchet":1, "nudge":60,
              "duration":max(1, ticks // 4)}
def state(path):
    nodes = list(ET.parse(path).getroot().iter("node"))
    matches = [n.attrib.get("content-desc", "") for n in nodes
               if n.attrib.get("content-desc", "").startswith(
                   f"Grid event parameters: PAD 01, STEP {step:02d}, VEL ")]
    if len(matches) != 1:
        raise AssertionError(f"event projection missing in {path}: {matches!r}")
    pattern = re.compile(
        r"Grid event parameters: PAD 01, STEP 0*(\d+), VEL (\d+), PROB (\d+), "
        r"RAT (\d+)x, NUDGE ([+-]?\d+), DUR (\d+)")
    match = pattern.fullmatch(matches[0])
    if match is None:
        raise AssertionError(f"malformed event projection: {matches[0]!r}")
    values = list(map(int, match.groups()))
    if values[0] != step:
        raise AssertionError(f"selected step changed: {values!r}")
    return values, nodes
before, _ = state(before_path)
after, nodes = state(after_path)
i = indices[name]
target = max(minimum, min(maximum, before[i] + direction * increments[name]))
if name == "duration":
    target = max(increments[name], min(ticks * 4, before[i] + direction * increments[name]))
expected = before[:]
expected[i] = target
if after != expected:
    raise AssertionError(
        f"Grid {name} must change only its own field by {direction * increments[name]}: "
        f"before={before!r}, expected={expected!r}, after={after!r}")
restore = [n for n in nodes if n.attrib.get("content-desc") == "Grid event " + name + (" minus" if direction > 0 else " plus")]
if len(restore) != 1 or restore[0].attrib.get("enabled") != "true":
    raise AssertionError(f"opposite {name} control must be enabled to restore the fixture")
print(f"Grid {name} edit changed only its field: {before[i]} -> {after[i]}.")
PY
      then
        echo "ERROR: Grid $parameter_token changed unrelated event fields or used wrong delta"; cat "$changed_xml"; exit 1
      fi

      if ! tap_visible_description_early "$changed_xml" "$restore_description"; then
        echo "ERROR: Grid $parameter_token restore touch failed"; exit 1
      fi
      sleep 0.25
      if ! dump_device_ui "$restored_remote" "$restored_xml"; then
        echo "ERROR: Grid $parameter_token restored-state hierarchy failed"; exit 1
      fi
      if ! adb_bounded 10 exec-out screencap -p > "$restored_png"; then
        echo "ERROR: Grid $parameter_token restored-state screenshot failed"; exit 1
      fi
      if ! python3 - "$selected_xml" "$restored_xml" "$grid_edit_step" "$parameter_name" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
before_path, after_path, step_s, name = sys.argv[1:]
step = int(step_s)
def state(path):
    nodes = list(ET.parse(path).getroot().iter("node"))
    found = [n.attrib.get("content-desc", "") for n in nodes
             if n.attrib.get("content-desc", "").startswith(
                 f"Grid event parameters: PAD 01, STEP {step:02d}, VEL ")]
    if len(found) != 1:
        raise AssertionError(f"Grid event projection missing: {found!r}")
    match = re.fullmatch(
        r"Grid event parameters: PAD 01, STEP 0*(\d+), VEL (\d+), PROB (\d+), "
        r"RAT (\d+)x, NUDGE ([+-]?\d+), DUR (\d+)", found[0])
    if match is None:
        raise AssertionError(f"malformed Grid state: {found[0]!r}")
    return list(map(int, match.groups())), nodes
before, _ = state(before_path)
after, nodes = state(after_path)
if after != before:
    raise AssertionError(f"Grid {name} +/- did not restore the entire native event: {before!r} -> {after!r}")
if len([n for n in nodes if n.attrib.get("content-desc") == "Grid event parameter cycle"]) != 1:
    raise AssertionError("Grid parameter selector disappeared after restoration")
print(f"Grid {name.upper()} changed and restored with all five musical fields exact.")
PY
      then
        echo "ERROR: Grid $parameter_token did not restore exact full event state"; cat "$restored_xml"; exit 1
      fi
      grid_event_current_xml="$restored_xml"
    }

    grid_event_test_parameter "velocity" "VEL" 1 1 127
    grid_event_test_parameter "probability" "PROB" 2 0 127
    grid_event_test_parameter "ratchet" "RAT" 3 1 8
    grid_event_test_parameter "nudge" "NUDGE" 4 -960 960
    grid_event_test_parameter "duration" "DUR" 5 1 1

    # Return to VEL before ERASE so the empty-state contract remains explicit
    # and the standard VEL controls are the disabled controls being asserted.
    if ! tap_visible_description_early "$grid_event_current_xml" "Grid event parameter cycle"; then
      echo "ERROR: Grid PARAM could not return to VEL before ERASE"; exit 1
    fi
    sleep 0.2
    GRID_EDIT_EMPTY_VEL_FOCUS_XML="/tmp/mpc-groovebox-grid-empty-vel-focus-uiautomator.xml"
    GRID_EDIT_EMPTY_VEL_FOCUS_REMOTE="/sdcard/mpc-groovebox-grid-empty-vel-focus-uiautomator.xml"
    if ! dump_device_ui "$GRID_EDIT_EMPTY_VEL_FOCUS_REMOTE" "$GRID_EDIT_EMPTY_VEL_FOCUS_XML"; then
      echo "ERROR: Grid VEL focus hierarchy before ERASE failed"; exit 1
    fi
    grid_event_current_xml="$GRID_EDIT_EMPTY_VEL_FOCUS_XML"

    echo "Grid PARAM cycling and real-touch +/- acceptance passed for VEL, PROB, RAT, NUDGE and DUR."
    if ! tap_visible_description_early "$grid_event_current_xml" "Grid Erase tool"; then
      echo "ERROR: Grid ERASE tool was not actionable after DRAW"; exit 1
    fi
    sleep 0.2
    if ! adb_bounded 10 shell input tap "$grid_edit_x" "$grid_edit_y"; then
      echo "ERROR: real Grid ERASE cell tap failed"; exit 1
    fi
    sleep 0.35
    GRID_EDIT_ERASE_XML="/tmp/mpc-groovebox-grid-edit-after-erase-uiautomator.xml"
    GRID_EDIT_ERASE_REMOTE="/sdcard/mpc-groovebox-grid-edit-after-erase-uiautomator.xml"
    GRID_EDIT_ERASE_PNG="/tmp/mpc-groovebox-grid-edit-after-erase.png"
    if ! dump_device_ui "$GRID_EDIT_ERASE_REMOTE" "$GRID_EDIT_ERASE_XML"; then
      echo "ERROR: could not read Grid state after erasing the drawn cell"; exit 1
    fi
    if ! python3 - "$GRID_EDIT_ERASE_XML" "$grid_edit_step" <<'PY'
import sys
import xml.etree.ElementTree as ET
path, step_s = sys.argv[1:]
step = int(step_s)
nodes = list(ET.parse(path).getroot().iter("node"))
expected = f"Grid selection: PAD 01, STEP {step:02d}, no note"
found = [n for n in nodes if n.attrib.get("content-desc") == expected]
if len(found) != 1:
    raise AssertionError(f"ERASE did not restore empty state {expected!r}; selected labels={[n.attrib.get('content-desc') for n in nodes if 'Grid selection:' in n.attrib.get('content-desc','')]}")
parameter = [n for n in nodes if n.attrib.get("content-desc") == f"Grid event parameters: PAD 01, STEP {step:02d}, empty"]
if len(parameter) != 1:
    raise AssertionError("Grid parameter strip did not report EMPTY after erasing the selected note")
plus = [n for n in nodes if n.attrib.get("content-desc") == "Grid event velocity plus"]
minus = [n for n in nodes if n.attrib.get("content-desc") == "Grid event velocity minus"]
if len(plus) != 1 or plus[0].attrib.get("enabled") != "false" \
        or len(minus) != 1 or minus[0].attrib.get("enabled") != "false":
    raise AssertionError("Grid velocity controls must disable when the selected note is empty")
print(f"Grid ERASE verified: {expected}; parameter controls disabled for empty cell")
PY
    then
      echo "ERROR: Grid ERASE failed to restore empty native state"; cat "$GRID_EDIT_ERASE_XML"; exit 1
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$GRID_EDIT_ERASE_PNG"; then
      echo "ERROR: Grid ERASE screenshot capture failed"; exit 1
    fi
    if ! tap_visible_description_early "$GRID_EDIT_ERASE_XML" "Grid Draw tool"; then
      echo "ERROR: Grid DRAW tool state could not be restored after round-trip"; exit 1
    fi
    echo "Grid DRAW → native note state → ERASE → empty-state round-trip passed."

    if ! adb_bounded 10 shell run-as "$PACKAGE" \
        touch cache/mpc-arrange-grid-focus-capture-release; then
      echo "ERROR: could not release app-side Arrangement-to-Grid capture checkpoint"
      exit 1
    fi
    arrangement_grid_focus_capture_done=1
  fi


  if [ "$arrangement_back_capture_done" -eq 0 ] \
      && grep -Fq -- "UI_ARRANGEMENT_BACK_CAPTURE_READY" <<<"$log_snapshot"; then
    echo "Capturing actual Arrangement after Grid BACK..."
    if ! adb_bounded 10 exec-out screencap -p > /tmp/mpc-groovebox-arrange-back.png; then echo "ERROR: Arrange-after-BACK screenshot failed"; exit 1; fi
    if ! timeout --signal=TERM --kill-after=3s 30s adb shell uiautomator dump /sdcard/mpc-groovebox-arrange-back-uiautomator.xml > /tmp/mpc-groovebox-arrange-back-uiautomator.log 2>&1; then cat /tmp/mpc-groovebox-arrange-back-uiautomator.log || true; echo "ERROR: Arrange-after-BACK hierarchy dump failed"; exit 1; fi
    if ! adb_bounded 10 exec-out cat /sdcard/mpc-groovebox-arrange-back-uiautomator.xml > /tmp/mpc-groovebox-arrange-back-uiautomator.xml; then echo "ERROR: Arrange-after-BACK hierarchy retrieval failed"; exit 1; fi
    if ! test -s /tmp/mpc-groovebox-arrange-back.png || ! test -s /tmp/mpc-groovebox-arrange-back-uiautomator.xml; then echo "ERROR: Arrange-after-BACK evidence is empty"; exit 1; fi
    back_line="$(grep -F -- 'UI_ARRANGEMENT_BACK_CONTEXT_COMPLETE:' <<<"$log_snapshot" | tail -n 1 || true)"
    if [[ -z "$back_line" || "$back_line" != *"eventPresent=true"* || "$back_line" != *"patternDataUnchanged=true"* ]]; then echo "ERROR: Arrange-after-BACK lacks event/pattern assertion"; printf '%s\n' "$back_line"; exit 1; fi
    if ! python3 - /tmp/mpc-groovebox-arrange-back-uiautomator.xml <<'PY'
import re,sys,xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter("node"))
def bounds(n):
 m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",n.attrib.get("bounds",""))
 if m is None: raise AssertionError("invalid bounds "+repr(n.attrib))
 return tuple(map(int,m.groups()))
def desc(value):
 found=[n for n in nodes if n.attrib.get("content-desc")==value]
 if len(found)!=1: raise AssertionError(f"expected one {value!r}, got {len(found)}")
 n=found[0]; x1,y1,x2,y2=bounds(n)
 if x2<=x1 or y2<=y1 or n.attrib.get("visible-to-user")=="false": raise AssertionError(f"not visible: {value!r}")
desc("MPC linear arrangement editor"); desc("Arrangement editing guidance")
if any(n.attrib.get("content-desc")=="MPC Grid View workspace" for n in nodes): raise AssertionError("Grid remains visible after BACK")
buttons=[n for n in nodes if n.attrib.get("text","").strip()=="GRID" and n.attrib.get("clickable")=="true" and n.attrib.get("enabled")=="true"]
if len(buttons)!=1: raise AssertionError(f"expected enabled Arrangement GRID action, got {len(buttons)}")
x1,y1,x2,y2=bounds(buttons[0])
if x2<=x1 or y2<=y1: raise AssertionError("GRID action has empty bounds")
print("Arrange-after-BACK hierarchy confirms the Arrangement workspace and real GRID action.")
PY
    then echo "ERROR: Arrangement was not confirmed by hierarchy"; cat /tmp/mpc-groovebox-arrange-back-uiautomator.xml; exit 1; fi
    if ! adb_bounded 10 shell run-as "$PACKAGE" touch cache/mpc-arrange-back-capture-release; then echo "ERROR: release Arrange capture failed"; exit 1; fi
    arrangement_back_capture_done=1
  fi

  if [ "$arrangement_grid_reentry_capture_done" -eq 0 ] \
      && grep -Fq -- "UI_ARRANGEMENT_GRID_BACK_REENTRY_CAPTURE_READY" <<<"$log_snapshot"; then
    echo "Capturing Grid after reopening same Arrangement event..."
    if ! adb_bounded 10 exec-out screencap -p > /tmp/mpc-groovebox-arrange-grid-reentry.png; then echo "ERROR: Grid re-entry screenshot failed"; exit 1; fi
    if ! timeout --signal=TERM --kill-after=3s 30s adb shell uiautomator dump /sdcard/mpc-groovebox-arrange-grid-reentry-uiautomator.xml > /tmp/mpc-groovebox-arrange-grid-reentry-uiautomator.log 2>&1; then cat /tmp/mpc-groovebox-arrange-grid-reentry-uiautomator.log || true; echo "ERROR: Grid re-entry hierarchy dump failed"; exit 1; fi
    if ! adb_bounded 10 exec-out cat /sdcard/mpc-groovebox-arrange-grid-reentry-uiautomator.xml > /tmp/mpc-groovebox-arrange-grid-reentry-uiautomator.xml; then echo "ERROR: Grid re-entry hierarchy retrieval failed"; exit 1; fi
    if ! test -s /tmp/mpc-groovebox-arrange-grid-reentry.png || ! test -s /tmp/mpc-groovebox-arrange-grid-reentry-uiautomator.xml; then echo "ERROR: Grid re-entry evidence is empty"; exit 1; fi
    reentry_line="$(grep -F -- 'UI_ARRANGEMENT_GRID_BACK_REENTRY_COMPLETE:' <<<"$log_snapshot" | tail -n 1 || true)"
    if [[ -z "$reentry_line" || "$reentry_line" != *"eventPresent=true"* || "$reentry_line" != *"patternDataUnchanged=true"* ]]; then echo "ERROR: re-entry lacks event/pattern assertion"; printf '%s\n' "$reentry_line"; exit 1; fi
    reentry_track="$(sed -nE 's/.*trackIndex=([0-9]+).*/\1/p' <<<"$reentry_line")"
    reentry_step="$(sed -nE 's/.*targetStep=([0-9]+).*/\1/p' <<<"$reentry_line")"
    reentry_first="$(sed -nE 's/.*firstVisibleStep=([0-9]+).*/\1/p' <<<"$reentry_line")"
    reentry_last="$(sed -nE 's/.*lastVisibleStep=([0-9]+).*/\1/p' <<<"$reentry_line")"
    reentry_total="$(sed -nE 's/.*totalSteps=([0-9]+).*/\1/p' <<<"$reentry_line")"
    if [[ -z "$reentry_track" || -z "$reentry_step" || -z "$reentry_first" || -z "$reentry_last" || -z "$reentry_total" ]]; then echo "ERROR: invalid re-entry checkpoint: $reentry_line"; exit 1; fi
    if ! python3 - /tmp/mpc-groovebox-arrange-grid-reentry-uiautomator.xml "$reentry_track" "$reentry_step" "$reentry_first" "$reentry_last" "$reentry_total" <<'PY'
import re,sys,xml.etree.ElementTree as ET
path,track_s,target_s,first_s,last_s,total_s=sys.argv[1:]
track,target,first,last,total=map(int,(track_s,target_s,first_s,last_s,total_s))
nodes=list(ET.parse(path).getroot().iter("node"))
def bounds(n):
 m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",n.attrib.get("bounds",""))
 if m is None: raise AssertionError("invalid bounds "+repr(n.attrib))
 return tuple(map(int,m.groups()))
def desc(value):
 found=[n for n in nodes if n.attrib.get("content-desc")==value]
 if len(found)!=1: raise AssertionError(f"expected one {value!r}, got {len(found)}")
 n=found[0]; x1,y1,x2,y2=bounds(n)
 if x2<=x1 or y2<=y1 or n.attrib.get("visible-to-user")=="false": raise AssertionError(f"not visible: {value!r}")
desc("MPC Grid View workspace"); desc("MPC Grid View drum event grid")
if any(n.attrib.get("content-desc")=="MPC linear arrangement editor" for n in nodes): raise AssertionError("Arrangement remains visible")
prefix=f"TRACK {track+1:02d} •"
if not any(n.attrib.get("text","").startswith(prefix) for n in nodes): raise AssertionError(f"Track context not restored: {prefix}")
ranges=[]
for n in nodes:
 m=re.fullmatch(r"(?:STEPS\s*)?0*(\d+)[–-]0*(\d+)\s*/\s*0*(\d+)",n.attrib.get("text","").strip())
 if m: ranges.append(tuple(map(int,m.groups())))
if len(ranges)!=1: raise AssertionError(f"expected one Grid range, got {ranges}")
shown=ranges[0]
if shown!=(first,last,total): raise AssertionError(f"range {shown} disagrees with log {(first,last,total)}")
if not first<=target<=last<=total: raise AssertionError(f"target step {target} outside {shown}")
semantic=[n for n in nodes if n.attrib.get("content-desc","").startswith("Grid visible step range: steps ")]
if len(semantic)!=1: raise AssertionError(f"expected one semantic Grid range, got {len(semantic)}")
m=re.fullmatch(r"Grid visible step range: steps\s+0*(\d+) through 0*(\d+) of 0*(\d+)",semantic[0].attrib.get("content-desc",""))
if m is None or tuple(map(int,m.groups()))!=shown: raise AssertionError("Grid range text and description disagree")
print(f"Re-entered Grid evidence verified: Track {track+1}, step {target}, visible {first}–{last} of {total}.")
PY
    then echo "ERROR: Grid re-entry hierarchy failed"; cat /tmp/mpc-groovebox-arrange-grid-reentry-uiautomator.xml; exit 1; fi
    if ! adb_bounded 10 shell run-as "$PACKAGE" touch cache/mpc-arrange-grid-reentry-capture-release; then echo "ERROR: release Grid re-entry capture failed"; exit 1; fi
    arrangement_grid_reentry_capture_done=1
  fi

  if [ "$storage_picker_capture_done" -eq 0 ] \
      && grep -Fq -- "UI_STORAGE_PICKER_LAUNCH_REQUESTED" <<<"$log_snapshot"; then
    echo "Verifying actual Android Document Picker launch and capturing evidence..."
    picker_activity=""
    for picker_attempt in 1 2 3 4 5 6 7 8 9 10; do
      picker_activity="$(adb_bounded 5 shell dumpsys activity activities 2>/dev/null \
        | grep -E 'mResumedActivity|topResumedActivity' | tail -n 1 || true)"
      if [[ -n "$picker_activity" ]] && ! grep -Fq -- "$PACKAGE" <<<"$picker_activity"; then
        break
      fi
      sleep 1
    done
    if [[ -z "$picker_activity" ]] || grep -Fq -- "$PACKAGE" <<<"$picker_activity"; then
      echo "ERROR: Android Document Picker did not become the resumed activity"
      printf '%s\n' "$picker_activity"
      exit 1
    fi
    printf '%s\n' "$picker_activity" > /tmp/mpc-groovebox-storage-picker-activity.txt
    if ! timeout --signal=TERM --kill-after=3s 30s \
        adb shell uiautomator dump /sdcard/mpc-groovebox-storage-picker-uiautomator.xml \
        > /tmp/mpc-groovebox-storage-picker-uiautomator.log 2>&1; then
      cat /tmp/mpc-groovebox-storage-picker-uiautomator.log || true
      echo "ERROR: Android Document Picker accessibility dump failed"
      exit 1
    fi
    if ! adb_bounded 10 exec-out cat /sdcard/mpc-groovebox-storage-picker-uiautomator.xml \
        > /tmp/mpc-groovebox-storage-picker-uiautomator.xml; then
      echo "ERROR: Android Document Picker hierarchy retrieval failed"
      exit 1
    fi
    if ! test -s /tmp/mpc-groovebox-storage-picker-uiautomator.xml; then
      echo "ERROR: Android Document Picker hierarchy is empty"
      exit 1
    fi

    # The resumed-activity record can precede the first visible frame. Capture
    # after the accessibility tree is ready, then retry briefly if the bitmap
    # is still the nearly-black launch transition rather than rendered UI.
    sleep 1
    picker_screenshot_bytes=0
    for screenshot_attempt in 1 2 3 4 5; do
      if ! adb_bounded 10 exec-out screencap -p > /tmp/mpc-groovebox-storage-picker.png; then
        echo "ERROR: Android Document Picker screenshot capture failed"
        exit 1
      fi
      picker_screenshot_bytes=$(wc -c < /tmp/mpc-groovebox-storage-picker.png)
      if [ "$picker_screenshot_bytes" -ge 25000 ]; then
        break
      fi
      sleep 1
    done
    if [ "$picker_screenshot_bytes" -lt 25000 ]; then
      echo "ERROR: Android Document Picker screenshot remained blank/transition-sized"
      echo "Screenshot bytes: $picker_screenshot_bytes"
      exit 1
    fi
    if ! python3 - /tmp/mpc-groovebox-storage-picker-uiautomator.xml <<'PY'
import sys
import xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
nodes = list(root.iter("node"))
if not nodes:
    raise AssertionError("Android Document Picker hierarchy contains no accessibility nodes")
print("Android Document Picker accessibility nodes:", len(nodes))
PY
    then
      echo "ERROR: Android Document Picker hierarchy is not valid accessibility XML"
      exit 1
    fi
    if ! adb_bounded 5 shell input keyevent KEYCODE_BACK; then
      echo "ERROR: could not dismiss Android Document Picker with Back"
      exit 1
    fi
    storage_picker_capture_done=1
  fi

  if grep -Fq -- "UI_STORAGE_PICKER_RETURNED" <<<"$log_snapshot" \
      && [ "$storage_picker_capture_done" -ne 1 ]; then
    echo "ERROR: app reported return from picker without captured picker evidence"
    exit 1
  fi
  if grep -Fq -- "UI_INTERACTION_COMPLETE" <<<"$log_snapshot"; then
    if [ "$arrangement_loop_touch_capture_done" -ne 1 ] \
        || [ "$arrangement_loop_touch_acceptance_complete" -ne 1 ]; then
      echo "ERROR: UI audit completed without verified real Arrangement loop-brace touch/restore acceptance"
      exit 1
    fi
    if [ "$step_touch_capture_done" -ne 1 ] \
        || [ "$step_touch_acceptance_complete" -ne 1 ]; then
      echo "ERROR: UI audit completed without verified Step Sequencer real-touch acceptance"
      exit 1
    fi
    if [ "$arrangement_back_capture_done" -ne 1 ] \
        || [ "$arrangement_grid_reentry_capture_done" -ne 1 ] \
        || [ "$track_view_mute_capture_done" -ne 1 ]; then
      echo "ERROR: UI audit completed without Track View mute and Arrange navigation evidence"
      exit 1
    fi
    ui_audit_complete=1
    break
  fi
  if grep -Eq 'UI_HIERARCHY_FAILED:|UI_INTERACTION_FAILED:|UI_STARTUP_FINALIZATION_FAILED|STARTUP_NATIVE_FAILED' <<<"$log_snapshot"; then
    echo "ERROR: application-side UI audit reported failure before external UI dump"
    # Earlier audit stages may already have rendered the Browser screenshot and
    # accessibility tree. Preserve them even when a later check fails.
    adb_bounded 10 exec-out run-as "$PACKAGE" cat cache/mpc-groovebox-browser-render.png > /tmp/mpc-groovebox-browser.png 2>/dev/null || true
    adb_bounded 10 exec-out run-as "$PACKAGE" cat cache/mpc-groovebox-browser-hierarchy.txt > /tmp/mpc-groovebox-browser-hierarchy.txt 2>/dev/null || true
    grep -E -A 8 -B 3 'UI_HIERARCHY_FAILED:|UI_INTERACTION_FAILED:|UI_STARTUP_FINALIZATION_FAILED|STARTUP_NATIVE_FAILED' <<<"$log_snapshot" | tail -n 120 || true
    exit 1
  fi

  # Poll process liveness every ten iterations so one slow adb call cannot
  # stretch the audit timeout into minutes or hours.
  if (( audit_poll % 10 == 0 )); then
    pid_output="$(adb_bounded 5 shell pidof "$PACKAGE" 2>/dev/null || true)"
    if ! grep -Eq '[0-9]' <<<"$(tr -d '\r' <<<"$pid_output")"; then
      echo "ERROR: MPC Groovebox process exited or ADB became unresponsive during UI audit"
      exit 1
    fi
  fi
  audit_poll=$((audit_poll + 1))
  sleep 1
done

if [ "$ui_audit_complete" -ne 1 ]; then
  echo "ERROR: application-side UI audit did not reach UI_INTERACTION_COMPLETE within ${UI_AUDIT_TIMEOUT_SECONDS}s wall-clock deadline"
  adb_bounded 10 logcat -d -v threadtime > /tmp/mpc-groovebox-logcat.txt || true
  tail -n 350 /tmp/mpc-groovebox-logcat.txt || true
  exit 1
fi

if [ "$track_view_mute_capture_done" -ne 1 ] \
    || ! test -s /tmp/mpc-groovebox-track-view-muted.png \
    || ! test -s /tmp/mpc-groovebox-track-view-muted-uiautomator.xml; then
  echo "ERROR: Track View mute acceptance lacks screenshot/hierarchy evidence"; exit 1
fi

if [ "$arrangement_back_capture_done" -ne 1 ] \
    || ! test -s /tmp/mpc-groovebox-arrange-back.png \
    || ! test -s /tmp/mpc-groovebox-arrange-back-uiautomator.xml; then
  echo "ERROR: Arrange-after-BACK acceptance lacks screenshot/hierarchy evidence"; exit 1
fi
if [ "$arrangement_grid_reentry_capture_done" -ne 1 ] \
    || ! test -s /tmp/mpc-groovebox-arrange-grid-reentry.png \
    || ! test -s /tmp/mpc-groovebox-arrange-grid-reentry-uiautomator.xml; then
  echo "ERROR: same-event Grid re-entry lacks screenshot/hierarchy evidence"; exit 1
fi

echo "Application-side UI audit completed; collecting app-side Browser layout evidence."

if [ "$main_runtime_capture_done" -ne 1 ] \
    || ! test -s /tmp/mpc-groovebox-main-ready.png \
    || ! test -s /tmp/mpc-groovebox-main-ready-uiautomator.xml; then
  echo "ERROR: ready Main workspace lacks real screenshot/hierarchy evidence"
  exit 1
fi

if [ "$storage_picker_capture_done" -ne 1 ] \
    || ! test -s /tmp/mpc-groovebox-storage-picker.png \
    || ! test -s /tmp/mpc-groovebox-storage-picker-uiautomator.xml \
    || ! test -s /tmp/mpc-groovebox-storage-picker-activity.txt; then
  echo "ERROR: storage-picker round trip lacks captured UI/activity evidence"
  exit 1
fi
if ! grep -Fq -- "UI_STORAGE_PICKER_RETURNED" <<<"$log_snapshot"; then
  echo "ERROR: app audit completed without confirming the Browser return after picker cancellation"
  exit 1
fi
if [ "$browser_runtime_capture_done" -ne 1 ] \
    || ! test -s /tmp/mpc-groovebox-browser-device.png \
    || ! test -s /tmp/mpc-groovebox-browser-uiautomator.xml; then
  echo "ERROR: application audit completed without real Browser screen and UI hierarchy evidence"
  exit 1
fi
cp /tmp/mpc-groovebox-browser-device.png /tmp/mpc-groovebox-browser.png
if ! adb_bounded 10 exec-out run-as "$PACKAGE" cat cache/mpc-groovebox-browser-hierarchy.txt > /tmp/mpc-groovebox-browser-hierarchy.txt; then
  echo "ERROR: Browser runtime accessibility hierarchy could not be retrieved from the debug app"
  exit 1
fi
if ! test -s /tmp/mpc-groovebox-browser.png || ! test -s /tmp/mpc-groovebox-browser-hierarchy.txt; then
  echo "ERROR: Browser runtime evidence files are empty"
  exit 1
fi
for required in 'contentDescription="MPC Browser results list"' 'text="OPEN STORAGE…"' 'contentDescription="MPC Browser current sample"'; do
  if ! grep -Fq -- "$required" /tmp/mpc-groovebox-browser-hierarchy.txt; then
    echo "ERROR: Browser runtime hierarchy is missing visible results contract: $required"
    cat /tmp/mpc-groovebox-browser-hierarchy.txt
    exit 1
  fi
done

echo "Dumping post-audit UI hierarchy..."
if ! timeout --signal=TERM --kill-after=3s 30s adb shell uiautomator dump > /tmp/mpc-groovebox-uiautomator.txt 2>&1; then
  cat /tmp/mpc-groovebox-uiautomator.txt || true
  echo "ERROR: post-audit uiautomator dump failed or timed out after 30s"
  exit 1
fi
if ! adb_bounded 10 shell test -s "$DEVICE_DUMP"; then
  echo "ERROR: post-audit UI dump file was not created: $DEVICE_DUMP"
  cat /tmp/mpc-groovebox-uiautomator.txt || true
  exit 1
fi
if ! adb_bounded 10 exec-out cat "$DEVICE_DUMP" >"$DUMP"; then
  echo "ERROR: post-audit UI dump read failed or timed out: $DEVICE_DUMP"
  cat /tmp/mpc-groovebox-uiautomator.txt || true
  exit 1
fi


echo "Running Browser local MediaStore → audition → Pad/Layer assignment runtime test..."
LOCAL_SAMPLE_SOURCE="android/app/src/main/assets/samples/pad01.wav.b64"
LOCAL_SAMPLE_NAME="Browser-Smoke-Local.wav"
LOCAL_SAMPLE_DEVICE="/sdcard/Music/$LOCAL_SAMPLE_NAME"
LOCAL_SAMPLE_HOST="/tmp/$LOCAL_SAMPLE_NAME"
BROWSER_LOCAL_XML="/tmp/mpc-groovebox-browser-local-results-uiautomator.xml"
BROWSER_LOCAL_REMOTE="/sdcard/mpc-groovebox-browser-local-results-uiautomator.xml"

if [[ ! -s "$LOCAL_SAMPLE_SOURCE" ]]; then
  echo "ERROR: bundled WAV fixture is missing: $LOCAL_SAMPLE_SOURCE"
  exit 1
fi
if ! base64 --decode "$LOCAL_SAMPLE_SOURCE" > "$LOCAL_SAMPLE_HOST"; then
  echo "ERROR: could not decode Browser WAV fixture"
  exit 1
fi
if ! file "$LOCAL_SAMPLE_HOST" | grep -Fq -- "WAVE"; then
  echo "ERROR: Browser WAV fixture is not a decoded WAV file"
  file "$LOCAL_SAMPLE_HOST" || true
  exit 1
fi
if ! adb_bounded 10 shell mkdir -p /sdcard/Music; then
  echo "ERROR: could not prepare emulator Music folder"
  exit 1
fi
if ! adb_bounded 30 push "$LOCAL_SAMPLE_HOST" "$LOCAL_SAMPLE_DEVICE"; then
  echo "ERROR: could not copy Browser WAV fixture to emulator storage"
  exit 1
fi
emulator_sdk="$(adb_bounded 10 shell getprop ro.build.version.sdk | tr -d '\r')"
if [[ "$emulator_sdk" =~ ^[0-9]+$ ]] && (( emulator_sdk >= 33 )); then
  media_permission="android.permission.READ_MEDIA_AUDIO"
else
  media_permission="android.permission.READ_EXTERNAL_STORAGE"
fi
if ! adb_bounded 10 shell pm grant "$PACKAGE" "$media_permission"; then
  echo "ERROR: could not grant emulator local-audio permission: $media_permission"
  exit 1
fi
# ACTION_MEDIA_SCANNER_SCAN_FILE was deprecated in Android 10 and does
# not reliably seed MediaStore on current emulator images. Use the provider's
# scan_file call as shell, then verify the exact indexed row before exercising
# the app UI. This keeps the runtime test about Browser behavior, not scanner lag.
LOCAL_SAMPLE_CANONICAL="/storage/emulated/0/Music/$LOCAL_SAMPLE_NAME"
if ! adb_bounded 15 shell content call \
    --uri content://media \
    --method scan_file \
    --arg "$LOCAL_SAMPLE_CANONICAL" > /tmp/mpc-browser-media-scan.txt 2>&1; then
  echo "ERROR: MediaStore scan_file call failed for local WAV fixture"
  cat /tmp/mpc-browser-media-scan.txt || true
  exit 1
fi

# scan_file returns the exact indexed content URI. Query that returned item
# by numeric _id; quoting the fixture's hyphenated display name in the legacy
# shell content-query --where parser is not portable across emulator API levels.
MEDIASTORE_FIXTURE_QUERY="/tmp/mpc-browser-mediastore-fixture-query.txt"
MEDIA_ITEM_URI="$(grep -oE 'content://media/[^[:space:]]+/audio/media/[0-9]+' \
    /tmp/mpc-browser-media-scan.txt | head -n 1 || true)"
MEDIA_ITEM_ID="${MEDIA_ITEM_URI##*/}"
MEDIA_COLLECTION_URI="${MEDIA_ITEM_URI%/*}"
if [[ ! "$MEDIA_ITEM_ID" =~ ^[0-9]+$ || -z "$MEDIA_COLLECTION_URI" ]]; then
  echo "ERROR: MediaStore scan_file did not return a usable audio content URI"
  cat /tmp/mpc-browser-media-scan.txt || true
  exit 1
fi
if ! adb_bounded 10 shell content query \
    --uri "$MEDIA_COLLECTION_URI" \
    --projection _id:_display_name:mime_type \
    --where "_id=$MEDIA_ITEM_ID" > "$MEDIASTORE_FIXTURE_QUERY" 2>&1; then
  echo "ERROR: could not query scan_file's returned MediaStore item by ID"
  cat /tmp/mpc-browser-media-scan.txt || true
  cat "$MEDIASTORE_FIXTURE_QUERY" || true
  exit 1
fi
if ! grep -Fq -- "$LOCAL_SAMPLE_NAME" "$MEDIASTORE_FIXTURE_QUERY"; then
  echo "ERROR: scan_file URI did not resolve to the expected local WAV"
  echo "--- scan_file response ---"
  cat /tmp/mpc-browser-media-scan.txt || true
  echo "--- MediaStore query ---"
  cat "$MEDIASTORE_FIXTURE_QUERY" || true
  exit 1
fi
echo "MediaStore fixture indexed and verified: $LOCAL_SAMPLE_NAME (ID $MEDIA_ITEM_ID)"

ui_target_center() {
  local hierarchy_path="$1"
  local expected_description="$2"
  python3 - "$hierarchy_path" "$expected_description" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

path, expected = sys.argv[1], sys.argv[2]
root = ET.parse(path).getroot()

def bounds(value):
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", value or "")
    return tuple(map(int, match.groups())) if match else None

def intersect(rect, clip):
    if clip is None:
        return rect
    x1, y1, x2, y2 = rect
    a1, b1, a2, b2 = clip
    result = (max(x1, a1), max(y1, b1), min(x2, a2), min(y2, b2))
    return result if result[0] < result[2] and result[1] < result[3] else None

def visit(node, clips):
    rect = bounds(node.attrib.get("bounds"))
    cls = node.attrib.get("class", "")
    child_clips = clips
    if rect and ("ScrollView" in cls or node.attrib.get("scrollable") == "true"):
        child_clips = clips + [rect]

    desc = node.attrib.get("content-desc", "")
    if (desc == expected or desc.startswith(expected)) and rect \
            and node.attrib.get("clickable") == "true" \
            and node.attrib.get("enabled") == "true":
        visible = rect
        for clip in reversed(clips):
            visible = intersect(visible, clip)
            if visible is None:
                break
        if visible:
            x1, y1, x2, y2 = visible
            print((x1+x2)//2, (y1+y2)//2)
            return True

    for child in list(node):
        if visit(child, child_clips):
            return True
    return False

if not visit(root, []):
    sys.exit(2)
PY
}

tap_ui_description() {
  local hierarchy_path="$1"
  local expected_description="$2"
  local coordinates tap_x tap_y
  # Always resolve touch coordinates from a just-captured hierarchy. Several
  # workflows restart the Activity or transition out of an external picker;
  # a previously valid dump can point at a different control after that change.
  if ! dump_device_ui "$DEVICE_DUMP" "$hierarchy_path"; then
    echo "ERROR: could not refresh UI hierarchy before tapping $expected_description"
    return 1
  fi
  if ! coordinates="$(ui_target_center "$hierarchy_path" "$expected_description")"; then
    echo "ERROR: visible clickable UI target not found: $expected_description"
    return 1
  fi
  read -r tap_x tap_y <<< "$coordinates"
  if ! adb_bounded 10 shell input tap "$tap_x" "$tap_y"; then
    echo "ERROR: could not tap UI target $expected_description at $tap_x,$tap_y"
    return 1
  fi
}

# DocumentsUI can expose the same filename in both its editable Search
# field and the actual result row. Generic tap_ui_text deliberately chooses the
# first exact label, so it is unsafe for this duplicate-label picker screen.
# This helper ignores editable-text matches and clicks the nearest real
# enabled/clickable ancestor of the matching result-row text node.
tap_ui_picker_result_row() {
  local hierarchy_path="$1"
  local expected_text="$2"
  local tap_mode="${3:-auto}"
  local tap_kind="${4:-label}"
  local coordinates tap_x tap_y
  if ! coordinates="$(python3 - "$hierarchy_path" "$expected_text" "$tap_kind" <<'PY'
import sys
import xml.etree.ElementTree as ET

path, expected, tap_kind = sys.argv[1], sys.argv[2], sys.argv[3]
root = ET.parse(path).getroot()
nodes = list(root.iter("node"))
parents = {child: parent for parent in nodes for child in list(parent)}
for node in nodes:
    if node.attrib.get("text", "").strip() != expected:
        continue
    cls = node.attrib.get("class", "").lower()
    if any(token in cls for token in ("edittext", "searchauto", "autocomplete")):
        continue
    candidate = node
    tile_bounds = None
    row_bounds = None
    icon_bounds = None
    row_node = None
    while candidate is not None:
        bounds = candidate.attrib.get("bounds", "")
        cls_candidate = candidate.attrib.get("class", "").lower()
        resource_id = candidate.attrib.get("resource-id", "")
        if resource_id == "com.android.documentsui:id/item_root" and bounds.startswith("[") and bounds.endswith("]"):
            row_bounds = bounds
            row_node = candidate
        if "materialcardview" in cls_candidate and bounds.startswith("[") and bounds.endswith("]"):
            tile_bounds = bounds
        if (candidate.attrib.get("clickable") == "true"
                and candidate.attrib.get("enabled") == "true"
                and bounds.startswith("[") and bounds.endswith("]")):
            try:
                first, second = bounds[1:-1].split("][")
                left, top = map(int, first.split(","))
                right, bottom = map(int, second.split(","))
            except ValueError:
                candidate = parents.get(candidate)
                continue
            if right > left and bottom > top:
                print((left + right) // 2, (top + bottom) // 2)
                raise SystemExit(0)
        candidate = parents.get(candidate)
    # A row-centre tap is not independent of the title hit area on this
    # DocumentsUI layout: Build #2728's row centre (540,527) still landed
    # inside the title bounds [189,499][697,556]. Choose the measured trailing
    # gutter of item_root instead, outside the title TextView, and fail closed
    # if this row has no such target rather than silently reusing label bounds.
    if tap_kind == "row" and row_bounds:
        try:
            first, second = row_bounds[1:-1].split("][")
            left, top = map(int, first.split(","))
            right, bottom = map(int, second.split(","))
            label_first, label_second = node.attrib.get("bounds", "")[1:-1].split("][")
            label_right = int(label_second.split(",")[0])
            target_x = left + ((right - left) * 7 // 8)
            if target_x <= label_right:
                target_x = right - max(8, (right - left) // 32)
            if right > left and bottom > top and target_x > label_right and target_x < right:
                print(target_x, (top + bottom) // 2)
                raise SystemExit(0)
        except ValueError:
            pass
        print("ERROR: no trailing item_root hit area exists outside the folder title", file=sys.stderr)
        raise SystemExit(3)
    # Folder activation and file selection use different hit targets on some
    # DocumentsUI grids. Keep each target explicit rather than guessing.
    if tap_kind == "icon" and row_node is not None:
        for child in row_node.iter("node"):
            if child.attrib.get("resource-id") == "com.android.documentsui:id/icon":
                try:
                    first, second = child.attrib.get("bounds", "")[1:-1].split("][")
                    left, top = map(int, first.split(","))
                    right, bottom = map(int, second.split(","))
                except ValueError:
                    continue
                if right > left and bottom > top:
                    print((left + right) // 2, (top + bottom) // 2)
                    raise SystemExit(0)
    if tap_kind == "card" and tile_bounds:
        try:
            first, second = tile_bounds[1:-1].split("][")
            left, top = map(int, first.split(","))
            right, bottom = map(int, second.split(","))
            if right > left and bottom > top:
                print((left + right) // 2, (top + bottom) // 2)
                raise SystemExit(0)
        except ValueError:
            pass
    # Otherwise use the actual label bounds for normal file selections.
    if row_bounds:
        try:
            first, second = node.attrib.get("bounds", "")[1:-1].split("][")
            left, top = map(int, first.split(","))
            right, bottom = map(int, second.split(","))
            if right > left and bottom > top:
                print((left + right) // 2, (top + bottom) // 2)
                raise SystemExit(0)
        except ValueError:
            pass
    if tile_bounds:
        try:
            first, second = tile_bounds[1:-1].split("][")
            left, top = map(int, first.split(","))
            right, bottom = map(int, second.split(","))
            if right > left and bottom > top:
                print((left + right) // 2, (top + bottom) // 2)
                raise SystemExit(0)
        except ValueError:
            pass
    bounds = node.attrib.get("bounds", "")
    if bounds.startswith("[") and bounds.endswith("]"):
        try:
            first, second = bounds[1:-1].split("][")
            left, top = map(int, first.split(","))
            right, bottom = map(int, second.split(","))
            if right > left and bottom > top:
                print((left + right) // 2, (top + bottom) // 2)
                raise SystemExit(0)
        except ValueError:
            pass
sys.exit(2)
PY
)"; then
    echo "ERROR: visible DocumentsUI result text not found for: $expected_text"
    return 1
  fi
  read -r tap_x tap_y <<< "$coordinates"
  # Explicit "single" is used for directory activation: a second rapid touch
  # can be interpreted as a selection gesture by DocumentsUI's touch handler.
  if [[ "$tap_mode" == "single" ]]; then
    echo "DocumentsUI tap: single $tap_kind '$expected_text' at $tap_x,$tap_y" >&2
    if [[ "$tap_kind" == "row" ]]; then
      # On API 30, `input tap` emits a MotionEvent with TOOL_TYPE_UNKNOWN.
      # DocumentsUI directory activation can ignore that synthetic gesture
      # even when its row bounds and the screenshot agree. API 31 exposes the
      # system-server input command; name the touchscreen source explicitly
      # so the event carries TOOL_TYPE_FINGER, like a real finger touch.
      if ! adb_bounded 10 shell cmd input touchscreen tap "$tap_x" "$tap_y"; then
        echo "ERROR: could not inject finger-type DocumentsUI row tap for $expected_text at $tap_x,$tap_y"
        return 1
      fi
    elif ! adb_bounded 10 shell input tap "$tap_x" "$tap_y"; then
      echo "ERROR: could not tap DocumentsUI result row $expected_text at $tap_x,$tap_y"
      return 1
    fi
    return 0
  elif [[ "$tap_mode" == "double" ]]; then
    # Send both taps in one remote-shell command so ADB startup latency does
    # not split them into two ordinary focus-only taps. Avoid nested "sh -c"
    # quoting; the Android shell receives the semicolon sequence directly.
    echo "DocumentsUI tap: $tap_mode $tap_kind '$expected_text' at $tap_x,$tap_y" >&2
    if ! adb_bounded 10 shell "input tap $tap_x $tap_y; sleep 0.35; input tap $tap_x $tap_y"; then
      echo "ERROR: could not double-tap DocumentsUI result row $expected_text"
      return 1
    fi
    return 0
  fi
  if ! adb_bounded 10 shell input tap "$tap_x" "$tap_y"; then
    echo "ERROR: could not tap DocumentsUI result row $expected_text at $tap_x,$tap_y"
    return 1
  fi
  # Preserve the guarded existing behavior for normal file-selection rows.
  sleep 0.05
  local picker_activity
  picker_activity="$(adb_bounded 5 shell dumpsys activity activities 2>/dev/null \
    | grep -E 'mResumedActivity|topResumedActivity' | tail -n 1 || true)"
  if [[ -n "$picker_activity" ]] && ! grep -Fq -- "$PACKAGE" <<<"$picker_activity"; then
    if ! adb_bounded 10 shell input tap "$tap_x" "$tap_y"; then
      echo "ERROR: could not activate focused DocumentsUI result $expected_text"
      return 1
    fi
  fi
}

tap_ui_text() {
  local hierarchy_path="$1"
  local expected_text="$2"
  local coordinates tap_x tap_y
  if ! coordinates="$(python3 - "$hierarchy_path" "$expected_text" <<'PY'
import sys
import xml.etree.ElementTree as ET

path, expected = sys.argv[1], sys.argv[2]
root = ET.parse(path).getroot()
nodes = list(root.iter("node"))
parents = {child: parent for parent in nodes for child in list(parent)}
for node in nodes:
    if node.attrib.get("text", "").strip() != expected:
        continue
    cls = node.attrib.get("class", "").lower()
    if any(token in cls for token in ("edittext", "searchauto", "autocomplete")):
        continue
    candidate = node
    while candidate is not None:
        bounds = candidate.attrib.get("bounds", "")
        if candidate.attrib.get("clickable") == "true" and candidate.attrib.get("enabled") == "true" and bounds.startswith("[") and bounds.endswith("]"):
            try:
                left_top, right_bottom = bounds[1:-1].split("][")
                left, top = map(int, left_top.split(","))
                right, bottom = map(int, right_bottom.split(","))
                if right > left and bottom > top:
                    print((left + right) // 2, (top + bottom) // 2)
                    raise SystemExit(0)
            except ValueError:
                pass
        candidate = parents.get(candidate)
    # Some DocumentsUI list items delegate touch handling while exposing only
    # a non-clickable TextView in the accessibility tree. Tap its visible center
    # as a bounded fallback; never fall back to the editable Search field.
    bounds = node.attrib.get("bounds", "")
    if bounds.startswith("[") and bounds.endswith("]"):
        try:
            left_top, right_bottom = bounds[1:-1].split("][")
            left, top = map(int, left_top.split(","))
            right, bottom = map(int, right_bottom.split(","))
            if right > left and bottom > top:
                print((left + right) // 2, (top + bottom) // 2)
                raise SystemExit(0)
        except ValueError:
            pass
sys.exit(2)
PY
)"; then
    echo "ERROR: visible non-editable picker text not found: $expected_text"
    return 1
  fi
  read -r tap_x tap_y <<< "$coordinates"
  if ! adb_bounded 10 shell input tap "$tap_x" "$tap_y"; then
    echo "ERROR: could not select visible picker text $expected_text at $tap_x,$tap_y"
    return 1
  fi
}

wait_for_ui_description() {
  local hierarchy_path="$1" remote_path="$2" expected_description="$3" max_attempts="$4"
  local hierarchy_failures=0
  for ((attempt=1; attempt<=max_attempts; attempt++)); do
    if ! dump_device_ui "$remote_path" "$hierarchy_path"; then
      hierarchy_failures=$((hierarchy_failures + 1))
      sleep 1
      continue
    fi
    if python3 - "$hierarchy_path" "$expected_description" <<'PY'
import sys
import xml.etree.ElementTree as ET
path, expected = sys.argv[1], sys.argv[2]
root = ET.parse(path).getroot()
raise SystemExit(0 if any(n.attrib.get("content-desc", "").startswith(expected)
                          for n in root.iter("node")) else 1)
PY
    then return 0; fi
    sleep 1
  done
  if (( hierarchy_failures == max_attempts )); then
    echo "ERROR: could not retrieve emulator UI hierarchy in $max_attempts bounded attempts while waiting for: $expected_description"
  else
    echo "ERROR: timed out waiting for UI target: $expected_description (hierarchy capture failures: $hierarchy_failures/$max_attempts)"
  fi
  cat "$hierarchy_path" || true
  return 1
}


# Scroll a real, currently visible editor container until a named actionable
# accessibility target is fully present. The swipe starts on a visible static
# TextView inside that ScrollView (never on the editable waveform), and its
# coordinates are derived from the live UI hierarchy rather than guessed.
scroll_ui_until_description() {
  local hierarchy_path="$1" remote_path="$2" expected_description="$3" max_swipes="${4:-8}"
  local coordinates swipe_x swipe_from swipe_to
  for ((attempt=1; attempt<=max_swipes; attempt++)); do
    if ! dump_device_ui "$remote_path" "$hierarchy_path"; then return 1; fi
    if python3 - "$hierarchy_path" "$expected_description" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

path, expected = sys.argv[1], sys.argv[2]
nodes = list(ET.parse(path).getroot().iter("node"))
parents = {child: parent for parent in nodes for child in list(parent)}

def rect(node):
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",
                         node.attrib.get("bounds", ""))
    return tuple(map(int, match.groups())) if match else None

def intersect(a, b):
    if a is None or b is None:
        return None
    x1, y1, x2, y2 = a
    bx1, by1, bx2, by2 = b
    result = (max(x1, bx1), max(y1, by1),
              min(x2, bx2), min(y2, by2))
    return result if result[0] < result[2] and result[1] < result[3] else None

for node in nodes:
    if (node.attrib.get("content-desc") != expected
            or node.attrib.get("clickable") != "true"
            or node.attrib.get("enabled") != "true"
            or node.attrib.get("visible-to-user") == "false"):
        continue
    visible = rect(node)
    if visible is None:
        continue
    ancestor = parents.get(node)
    while ancestor is not None and visible is not None:
        if (ancestor.attrib.get("class") == "android.widget.ScrollView"
                or ancestor.attrib.get("scrollable") == "true"):
            visible = intersect(visible, rect(ancestor))
        ancestor = parents.get(ancestor)
    if visible is not None:
        raise SystemExit(0)
raise SystemExit(1)
PY
    then
      echo "Track Edit control is exposed after $((attempt - 1)) scroll gesture(s): $expected_description"
      return 0
    fi

    if ! coordinates="$(python3 - "$hierarchy_path" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

path = sys.argv[1]
root = ET.parse(path).getroot()
nodes = list(root.iter("node"))
parents = {child: parent for parent in nodes for child in list(parent)}

def rect(node):
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",
                         node.attrib.get("bounds", ""))
    return tuple(map(int, match.groups())) if match else None

def intersection(a, b):
    if a is None or b is None:
        return None
    result = (max(a[0], b[0]), max(a[1], b[1]),
              min(a[2], b[2]), min(a[3], b[3]))
    return result if result[0] < result[2] and result[1] < result[3] else None

waveform = next((n for n in nodes
                 if n.attrib.get("content-desc") == "Track Edit Samples waveform"), None)
scroll = None
if waveform is not None:
    scroll = parents.get(waveform)
    while scroll is not None and not (
            scroll.attrib.get("class") == "android.widget.ScrollView"
            and scroll.attrib.get("scrollable") == "true"):
        scroll = parents.get(scroll)

if scroll is None:
    # UiAutomator can omit the waveform after the Track Edit ScrollView has
    # already been scrolled below it. In that state, resolve the same editor
    # viewport from stable semantic children rather than failing or guessing.
    semantic_markers = {
        "Track Edit layer gain value",
        "Track Edit layer tuning value",
        "Track Edit layer pan value",
        "Track Edit region",
        "Track Edit velocity",
        "Track Edit attack value",
        "Track Edit decay value",
        "Track Edit sustain value",
        "Track Edit release value",
        "Track Edit cutoff value value",
    }
    scroll_candidates = []
    for candidate in nodes:
        if (candidate.attrib.get("class") != "android.widget.ScrollView"
                or candidate.attrib.get("scrollable") != "true"
                or candidate.attrib.get("visible-to-user") == "false"):
            continue
        candidate_bounds = rect(candidate)
        if candidate_bounds is None:
            continue
        descriptions = [n.attrib.get("content-desc", "")
                        for n in candidate.iter("node")]
        marker_count = sum(1 for marker in semantic_markers
                           if marker in descriptions)
        if marker_count:
            width = candidate_bounds[2] - candidate_bounds[0]
            height = candidate_bounds[3] - candidate_bounds[1]
            scroll_candidates.append((marker_count, width * height, candidate))
    if not scroll_candidates:
        raise SystemExit(
            "Track Edit ScrollView is unavailable and no visible scrollable "
            "container exposes the current layer parameters")
    scroll = max(scroll_candidates, key=lambda entry: (entry[0], entry[1]))[2]

viewport = rect(scroll)
if viewport is None:
    raise SystemExit("Track Edit ScrollView has no valid bounds")
left, top, right, bottom = viewport
height = bottom - top
destination_y = top + max(40, min(100, height // 6))

# Choose a visible, non-clickable TextView in this ScrollView whose center is
# far enough below the destination to produce a meaningful upward drag.
candidates = []
for node in scroll.iter("node"):
    if node.attrib.get("class") != "android.widget.TextView":
        continue
    if node.attrib.get("clickable") == "true":
        continue
    if not node.attrib.get("text", "").strip():
        continue
    visible = intersection(rect(node), viewport)
    if visible is None:
        continue
    x1, y1, x2, y2 = visible
    cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
    if cx <= left or cx >= right or cy < destination_y + 85 or cy >= bottom - 35:
        continue
    candidates.append((cy, cx))

if not candidates:
    raise SystemExit("No safe visible static TextView is available as a scroll gesture origin")
start_y, swipe_x = max(candidates)
if start_y - destination_y < 85:
    raise SystemExit("Available scroll gesture is too short to move the Track Edit content")
print(swipe_x, start_y, destination_y)
PY
)"; then
      echo "ERROR: could not derive a safe Track Edit scroll gesture from live UI bounds"
      return 1
    fi
    read -r swipe_x swipe_from swipe_to <<< "$coordinates"
    echo "Scrolling Track Edit content using live bounds: x=$swipe_x y=$swipe_from->$swipe_to"
    if ! adb_bounded 10 shell input swipe "$swipe_x" "$swipe_from" "$swipe_x" "$swipe_to" 350; then
      echo "ERROR: Track Edit ScrollView gesture failed"
      return 1
    fi
    sleep 0.35
  done

  if ! dump_device_ui "$remote_path" "$hierarchy_path"; then return 1; fi
  if python3 - "$hierarchy_path" "$expected_description" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

path, expected = sys.argv[1], sys.argv[2]
nodes = list(ET.parse(path).getroot().iter("node"))
parents = {child: parent for parent in nodes for child in list(parent)}

def rect(node):
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",
                         node.attrib.get("bounds", ""))
    return tuple(map(int, match.groups())) if match else None

def intersect(a, b):
    if a is None or b is None:
        return None
    x1, y1, x2, y2 = a
    bx1, by1, bx2, by2 = b
    result = (max(x1, bx1), max(y1, by1),
              min(x2, bx2), min(y2, by2))
    return result if result[0] < result[2] and result[1] < result[3] else None

for node in nodes:
    if (node.attrib.get("content-desc") != expected
            or node.attrib.get("clickable") != "true"
            or node.attrib.get("enabled") != "true"
            or node.attrib.get("visible-to-user") == "false"):
        continue
    visible = rect(node)
    if visible is None:
        continue
    ancestor = parents.get(node)
    while ancestor is not None and visible is not None:
        if (ancestor.attrib.get("class") == "android.widget.ScrollView"
                or ancestor.attrib.get("scrollable") == "true"):
            visible = intersect(visible, rect(ancestor))
        ancestor = parents.get(ancestor)
    if visible is not None:
        raise SystemExit(0)
raise SystemExit(1)
PY
  then
    echo "Track Edit control is exposed after $max_swipes scroll gesture(s): $expected_description"
    return 0
  fi
  echo "ERROR: actionable Track Edit control remained outside the visible ScrollView after $max_swipes scroll gesture(s): $expected_description"
  cat "$hierarchy_path" || true
  return 1
}

# Scroll further down using a visible pan-value descendant when the real
# CENTER LAYER PAN action sits just below the current Track Edit viewport.
# This uses current ScrollView bounds, not guessed full-screen coordinates.
scroll_track_edit_to_layer_pan_center() {
  local hierarchy_path="$1" remote_path="$2" max_swipes="${3:-4}"
  local coordinates swipe_x swipe_from swipe_to
  for ((attempt=1; attempt<=max_swipes; attempt++)); do
    if ! dump_device_ui "$remote_path" "$hierarchy_path"; then return 1; fi
    if python3 - "$hierarchy_path" <<'PY'
import sys
import xml.etree.ElementTree as ET
nodes = list(ET.parse(sys.argv[1]).getroot().iter("node"))
raise SystemExit(0 if any(
    n.attrib.get("content-desc", "") == "Track Edit layer pan center"
    and n.attrib.get("clickable") == "true"
    and n.attrib.get("enabled") == "true"
    and n.attrib.get("visible-to-user") != "false"
    for n in nodes) else 1)
PY
    then
      echo "Track Edit CENTER LAYER PAN action is visible after $((attempt - 1)) bounded scroll gesture(s)."
      return 0
    fi

    if ! coordinates="$(python3 - "$hierarchy_path" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

nodes = list(ET.parse(sys.argv[1]).getroot().iter("node"))
parents = {child: parent for parent in nodes for child in list(parent)}
target = next((n for n in nodes
               if n.attrib.get("content-desc", "") == "Track Edit layer pan value"), None)
if target is None:
    raise SystemExit("Track Edit PAN value is not visible for deriving a safe scroll gesture")
scroll = parents.get(target)
while scroll is not None and not (
        scroll.attrib.get("class") == "android.widget.ScrollView"
        and scroll.attrib.get("scrollable") == "true"):
    scroll = parents.get(scroll)
if scroll is None:
    raise SystemExit("Track Edit PAN value has no scrollable editor ancestor")
match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",
                     scroll.attrib.get("bounds", ""))
if match is None:
    raise SystemExit("Track Edit ScrollView bounds are invalid")
left, top, right, bottom = map(int, match.groups())
height = bottom - top
if right - left < 100 or height < 220:
    raise SystemExit("Track Edit ScrollView is too small for a safe pan-center reveal")
margin = max(45, min(90, height // 5))
start_y = bottom - margin
distance = max(95, min(180, height // 4))
end_y = max(top + margin, start_y - distance)
if start_y - end_y < 85:
    raise SystemExit("Track Edit pan-center reveal gesture is too short")
print((left + right) // 2, start_y, end_y)
PY
)"; then
      echo "ERROR: could not derive a safe live-bounds scroll to Track Edit CENTER LAYER PAN"
      cat "$hierarchy_path" || true
      return 1
    fi
    read -r swipe_x swipe_from swipe_to <<< "$coordinates"
    echo "Revealing Track Edit CENTER LAYER PAN from the visible PAN row: x=$swipe_x y=$swipe_from->$swipe_to"
    if ! adb_bounded 10 shell input swipe "$swipe_x" "$swipe_from" "$swipe_x" "$swipe_to" 350; then
      echo "ERROR: Track Edit pan-center scroll gesture failed"
      return 1
    fi
    sleep 0.35
  done

  if ! dump_device_ui "$remote_path" "$hierarchy_path"; then return 1; fi
  echo "ERROR: Track Edit CENTER LAYER PAN stayed outside the viewport after $max_swipes bounded scroll gesture(s)"
  cat "$hierarchy_path" || true
  return 1
}

# Reverse-scroll the Track Edit editor using live UiAutomator bounds so the
# upper sample-layer GAIN row is actually present before post-velocity checks.
# scroll_ui_until_description() deliberately advances toward lower controls;
# it cannot reveal a row that has been scrolled above the viewport.
scroll_track_edit_to_layer_gain() {
  local hierarchy_path="$1" remote_path="$2" max_swipes="${3:-8}"
  local coordinates swipe_x swipe_from swipe_to
  for ((attempt=1; attempt<=max_swipes; attempt++)); do
    if ! dump_device_ui "$remote_path" "$hierarchy_path"; then return 1; fi
    if python3 - "$hierarchy_path" <<'PY'
import sys
import xml.etree.ElementTree as ET
nodes = list(ET.parse(sys.argv[1]).getroot().iter("node"))
descriptions = {n.attrib.get("content-desc", "") for n in nodes}
raise SystemExit(0 if {
    "Track Edit region",
    "Track Edit layer gain value",
    "Track Edit layer gain decrease",
    "Track Edit layer tuning value",
    "Track Edit layer pan value",
}.issubset(descriptions) else 1)
PY
    then
      echo "Track Edit REGION/GAIN/TUNE/PAN fields are visible after $((attempt - 1)) reverse scroll gesture(s)."
      return 0
    fi

    if ! coordinates="$(python3 - "$hierarchy_path" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

nodes = list(ET.parse(sys.argv[1]).getroot().iter("node"))
parents = {child: parent for parent in nodes for child in list(parent)}
target = next((n for n in nodes
               if n.attrib.get("content-desc", "") == "Track Edit layer tuning value"), None)
if target is None:
    raise SystemExit("no visible semantic layer parameter is available to locate the Track Edit ScrollView")

scroll = parents.get(target)
while scroll is not None and not (
        scroll.attrib.get("class") == "android.widget.ScrollView"
        and scroll.attrib.get("scrollable") == "true"):
    scroll = parents.get(scroll)
if scroll is None:
    raise SystemExit("visible layer parameter has no scrollable Track Edit ancestor")

match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",
                     scroll.attrib.get("bounds", ""))
if match is None:
    raise SystemExit("Track Edit ScrollView has no valid bounds")
left, top, right, bottom = map(int, match.groups())
height = bottom - top
if right - left < 100 or height < 220:
    raise SystemExit(f"Track Edit ScrollView viewport is too small for a safe reverse scroll: {(left, top, right, bottom)!r}")
margin = max(45, min(90, height // 5))
start_y = top + margin
end_y = bottom - margin
if end_y - start_y < 100:
    raise SystemExit("Track Edit reverse-scroll gesture is too short")
print((left + right) // 2, start_y, end_y)
PY
)"; then
      echo "ERROR: could not derive a safe reverse-scroll gesture from the live Track Edit hierarchy"
      cat "$hierarchy_path" || true
      return 1
    fi
    read -r swipe_x swipe_from swipe_to <<< "$coordinates"
    echo "Restoring upper Track Edit layer viewport using live bounds: x=$swipe_x y=$swipe_from->$swipe_to"
    if ! adb_bounded 10 shell input swipe "$swipe_x" "$swipe_from" "$swipe_x" "$swipe_to" 350; then
      echo "ERROR: Track Edit reverse-scroll gesture failed"
      return 1
    fi
    sleep 0.35
  done

  if ! dump_device_ui "$remote_path" "$hierarchy_path"; then return 1; fi
  echo "ERROR: Track Edit GAIN/TUNE/PAN fields did not return to the hierarchy after $max_swipes reverse scroll gesture(s)"
  cat "$hierarchy_path" || true
  return 1
}

wait_for_successful_audition() {
  local hierarchy_path="$1" remote_path="$2" expected_name="$3" max_attempts="$4"
  for ((attempt=1; attempt<=max_attempts; attempt++)); do
    if ! dump_device_ui "$remote_path" "$hierarchy_path"; then return 1; fi
    if python3 - "$hierarchy_path" "$expected_name" <<'PY'
import sys
import xml.etree.ElementTree as ET
path, expected_name = sys.argv[1], sys.argv[2]
root = ET.parse(path).getroot()
texts = [n.attrib.get("text", "").strip() for n in root.iter("node")]
success = any(value == "AUDITION • " + expected_name
              or value == "AUDITION COMPLETE • " + expected_name
              for value in texts)
raise SystemExit(0 if success else 1)
PY
    then return 0; fi
    sleep 1
  done
  echo "ERROR: selected WAV did not reach a successful audition state: $expected_name"
  cat "$hierarchy_path" || true
  return 1
}

wait_for_ui_text() {
  local hierarchy_path="$1" remote_path="$2" expected_fragment="$3" max_attempts="$4"
  for ((attempt=1; attempt<=max_attempts; attempt++)); do
    if ! dump_device_ui "$remote_path" "$hierarchy_path"; then return 1; fi
    if python3 - "$hierarchy_path" "$expected_fragment" <<'PY'
import sys
import xml.etree.ElementTree as ET
path, expected = sys.argv[1], sys.argv[2]
root = ET.parse(path).getroot()
texts = [node.attrib.get("text", "") for node in root.iter("node")]
raise SystemExit(0 if any(expected in value for value in texts) else 1)
PY
    then return 0; fi
    sleep 1
  done
  echo "ERROR: timed out waiting for UI text: $expected_fragment"
  cat "$hierarchy_path" || true
  return 1
}

# Open a real DocumentsUI directory and verify the expected child appears.
# Folder navigation opts into the fast double-tap path because Android 16 can
# focus non-clickable SAF folder cards on the first tap and dumpsys may not
# reliably distinguish the foreground picker from the paused app.
# Test the physical hit target, not merely the presence of an accessibility
# label. DocumentsUI includes partially clipped/preloaded rows in UIAutomator XML;
# such rows may sit under the fixed "Use this folder" footer and cannot be tapped.
picker_folder_row_fully_visible() {
  local hierarchy_path="$1" folder_label="$2"
  python3 - "$hierarchy_path" "$folder_label" <<'PY'
import sys
import xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
folder_label = sys.argv[2]
nodes = list(root.iter("node"))
parents = {child: parent for parent in nodes for child in list(parent)}
def rect(raw):
    try:
        a, b = raw[1:-1].split("][")
        x1, y1 = map(int, a.split(","))
        x2, y2 = map(int, b.split(","))
        return x1, y1, x2, y2
    except (ValueError, IndexError):
        return None
footer = next((rect(n.attrib.get("bounds", "")) for n in nodes
               if n.attrib.get("resource-id") == "com.android.documentsui:id/container_save"), None)
directory = next((rect(n.attrib.get("bounds", "")) for n in nodes
                  if n.attrib.get("resource-id") == "com.android.documentsui:id/dir_list"), None)
if not footer or not directory:
    raise SystemExit(1)
for n in nodes:
    if n.attrib.get("text", "").strip() != folder_label:
        continue
    current, row = n, None
    while current is not None:
        if current.attrib.get("resource-id") == "com.android.documentsui:id/item_root":
            row = rect(current.attrib.get("bounds", ""))
            break
        current = parents.get(current)
    if not row:
        continue
    left, top, right, bottom = row
    if left >= 0 and right <= directory[2] and top >= directory[1] and bottom <= min(directory[3], footer[1] - 8):
        raise SystemExit(0)
raise SystemExit(1)
PY
}

picker_hierarchy_signature() {
  local hierarchy_path="$1"
  python3 - "$hierarchy_path" <<'PY'
import json
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
fields = (
    "text", "content-desc", "resource-id", "class", "bounds",
    "clickable", "enabled", "focused", "focusable", "scrollable", "selected"
)
signature = [
    [node.attrib.get(field, "") for field in fields]
    for node in root.iter("node")
]
print(json.dumps(signature, ensure_ascii=False, separators=(",", ":")))
PY
}

# DocumentsUI can change its scroll/focus layout between a screencap and an
# accessibility dump. Require two identical hierarchy snapshots, capture the
# screenshot, then require a third matching snapshot before using any bounds.
capture_stable_picker_state() {
  local remote_path="$1" hierarchy_path="$2" screenshot_path="$3"
  local max_attempts="${4:-6}"
  local check_path="${hierarchy_path%.xml}-stability-check.xml"
  local before_signature check_signature final_signature
  local attempt
  for ((attempt=1; attempt<=max_attempts; attempt++)); do
    if ! dump_device_ui "$remote_path" "$hierarchy_path"; then return 1; fi
    before_signature="$(picker_hierarchy_signature "$hierarchy_path")" || return 1
    sleep 0.25
    if ! dump_device_ui "$remote_path" "$check_path"; then return 1; fi
    check_signature="$(picker_hierarchy_signature "$check_path")" || return 1
    if [[ "$before_signature" != "$check_signature" ]]; then
      sleep 0.25
      continue
    fi
    if ! adb_bounded 10 exec-out screencap -p > "$screenshot_path"; then
      echo "ERROR: could not capture stable DocumentsUI screenshot"
      return 1
    fi
    if ! dump_device_ui "$remote_path" "$hierarchy_path"; then return 1; fi
    final_signature="$(picker_hierarchy_signature "$hierarchy_path")" || return 1
    if [[ "$final_signature" == "$check_signature" ]]; then
      echo "DocumentsUI state stabilized after $attempt attempt(s): $hierarchy_path"
      return 0
    fi
    sleep 0.25
  done
  echo "ERROR: DocumentsUI screenshot and accessibility hierarchy did not stabilize"
  echo "Hierarchy: $hierarchy_path"
  cat "$hierarchy_path" || true
  return 1
}

tap_ui_picker_folder_until_child() {
  local hierarchy_path="$1" remote_path="$2" folder_label="$3" child_label="$4"
  local evidence_prefix="${5:-document-tree}"
  local root_png="/tmp/mpc-groovebox-browser-${evidence_prefix}-root.png"
  local root_xml="/tmp/mpc-groovebox-browser-${evidence_prefix}-root-uiautomator.xml"
  local after_activation_png="/tmp/mpc-groovebox-browser-${evidence_prefix}-root-after-activation.png"
  local after_activation_xml="/tmp/mpc-groovebox-browser-${evidence_prefix}-root-after-activation-uiautomator.xml"
  local after_activation_remote="${remote_path%.xml}-after-activation.xml"

  # Builds #2723–#2726 produced incompatible viewport positions between a
  # screenshot and the next UI dump. Do not guess another tap or keyboard path:
  # first prove the hierarchy remains unchanged around a fresh screenshot.
  if ! capture_stable_picker_state "$remote_path" "$root_xml" "$root_png" 6; then
    echo "ERROR: could not establish a stable DocumentsUI provider-root state"
    return 1
  fi
  if ! picker_folder_row_fully_visible "$root_xml" "$folder_label"; then
    echo "ERROR: target SAF folder is not fully visible above the picker footer: $folder_label"
    return 1
  fi

  if ! tap_ui_picker_result_row "$root_xml" "$folder_label" single row; then
    echo "ERROR: DocumentsUI folder title is not actionable: $folder_label"
    return 1
  fi
  if wait_for_ui_text "$hierarchy_path" "$remote_path" "$child_label" 12; then
    return 0
  fi

  # Preserve a stable post-action screenshot/XML pair. If navigation fails,
  # diagnose from matched evidence instead of layering on speculative gestures.
  if ! capture_stable_picker_state "$after_activation_remote" "$after_activation_xml" "$after_activation_png" 6; then
    echo "ERROR: could not capture stable DocumentsUI state after folder activation"
  fi
  echo "ERROR: single folder-title tap did not expose $child_label"
  return 1
}

wait_for_ui_text_pair() {
  local hierarchy_path="$1" remote_path="$2" first="$3" second="$4" max_attempts="$5"
  for ((attempt=1; attempt<=max_attempts; attempt++)); do
    if ! dump_device_ui "$remote_path" "$hierarchy_path"; then return 1; fi
    if python3 - "$hierarchy_path" "$first" "$second" <<'PY'
import sys
import xml.etree.ElementTree as ET
path, first, second = sys.argv[1], sys.argv[2], sys.argv[3]
root = ET.parse(path).getroot()
texts = [n.attrib.get("text", "") for n in root.iter("node")]
raise SystemExit(0 if any(first in value and second in value for value in texts) else 1)
PY
    then return 0; fi
    sleep 1
  done
  echo "ERROR: timed out waiting for UI text pair: $first / $second"
  cat "$hierarchy_path" || true
  return 1
}

wait_for_ui_node_text_desc() {
  local hierarchy_path="$1" remote_path="$2" expected_text="$3" expected_description="$4" max_attempts="$5"
  for ((attempt=1; attempt<=max_attempts; attempt++)); do
    if ! dump_device_ui "$remote_path" "$hierarchy_path"; then return 1; fi
    if python3 - "$hierarchy_path" "$expected_text" "$expected_description" <<'PY'
import sys
import xml.etree.ElementTree as ET
path, expected_text, expected_desc = sys.argv[1], sys.argv[2], sys.argv[3]
root = ET.parse(path).getroot()
ok = any(expected_text in n.attrib.get("text", "")
         and expected_desc in n.attrib.get("content-desc", "")
         for n in root.iter("node"))
raise SystemExit(0 if ok else 1)
PY
    then return 0; fi
    sleep 1
  done
  echo "ERROR: timed out waiting for node text/description: $expected_text / $expected_description"
  cat "$hierarchy_path" || true
  return 1
}

echo "Running Track Edit sample-layer tuning → leave/re-enter → restore runtime test..."
TRACK_EDIT_XML="/tmp/mpc-groovebox-track-edit-tuning-uiautomator.xml"
TRACK_EDIT_REMOTE="/sdcard/mpc-groovebox-track-edit-tuning-uiautomator.xml"
TRACK_EDIT_BEFORE_PNG="/tmp/mpc-groovebox-track-edit-tuning-before.png"
TRACK_EDIT_BEFORE_XML="/tmp/mpc-groovebox-track-edit-tuning-before-uiautomator.xml"
TRACK_EDIT_CONTEXT_BEFORE_XML="/tmp/mpc-groovebox-track-edit-tuning-context-before-uiautomator.xml"
TRACK_EDIT_CONTEXT_BEFORE_REMOTE="/sdcard/mpc-groovebox-track-edit-tuning-context-before-uiautomator.xml"
TRACK_EDIT_CONTEXT_REOPENED_XML="/tmp/mpc-groovebox-track-edit-tuning-context-reopened-uiautomator.xml"
TRACK_EDIT_CONTEXT_REOPENED_REMOTE="/sdcard/mpc-groovebox-track-edit-tuning-context-reopened-uiautomator.xml"
TRACK_EDIT_CHANGED_PNG="/tmp/mpc-groovebox-track-edit-tuning-changed.png"
TRACK_EDIT_CHANGED_XML="/tmp/mpc-groovebox-track-edit-tuning-changed-uiautomator.xml"
TRACK_EDIT_REOPENED_PNG="/tmp/mpc-groovebox-track-edit-tuning-reopened.png"
TRACK_EDIT_REOPENED_XML="/tmp/mpc-groovebox-track-edit-tuning-reopened-uiautomator.xml"
TRACK_EDIT_RESTORED_PNG="/tmp/mpc-groovebox-track-edit-tuning-restored.png"
TRACK_EDIT_RESTORED_XML="/tmp/mpc-groovebox-track-edit-tuning-restored-uiautomator.xml"

# The Pad/Layer/Sample context sits above the waveform, while tuning is
# below it. Read them from separate real hierarchy snapshots instead of
# assuming off-screen children remain accessible after scrolling.
read_track_edit_context_values() {
  python3 - "$1" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
nodes = list(root.iter("node"))

def one(description):
    found = [n for n in nodes if n.attrib.get("content-desc", "") == description]
    if len(found) != 1:
        raise SystemExit(f"expected one node {description!r}, found {len(found)}")
    return found[0]

pad_text = one("Track Edit Pad context").attrib.get("text", "")
layer_text = one("Track Edit selected layer").attrib.get("text", "")
pad = re.sub(r"\s+", " ", pad_text).strip()
layer = re.sub(r"\s+", " ", layer_text).strip()
sample_text = one("Track Edit sample").attrib.get("text", "")
sample = sample_text.splitlines()[-1].strip() if sample_text.strip() else ""
if not sample or "NO SAMPLE" in sample.upper():
    raise SystemExit(f"Track Edit test requires a real sample on the selected Pad/Layer: {sample_text!r}")
print("\t".join((pad, layer, sample.replace("\t", " "))))
PY
}

read_track_edit_tuning_value() {
  python3 - "$1" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
nodes = list(root.iter("node"))
found = [n for n in nodes
         if n.attrib.get("content-desc", "") == "Track Edit layer tuning value"]
if len(found) != 1:
    raise SystemExit(
        f"expected one visible Track Edit tuning value, found {len(found)}")
tuning_text = found[0].attrib.get("text", "")
match = re.search(r"([+-]?\d+(?:\.\d+)?)\s*st\b", tuning_text)
if not match:
    raise SystemExit(f"could not parse layer tuning value from {tuning_text!r}")
print(f"{float(match.group(1)):.2f}")
PY
}

# The app-side audit may leave the navigation shell on Main, Menu, Browser or
# Sampler. Resolve the actual entry target from accessibility, return through the
# foreground workspace's real BACK action if required, then preserve that exact
# context for the Track Edit exit assertion. No coordinate guesses or state hacks.
wait_for_track_edit_entry() {
  local remote_path="$1" host_path="$2" attempts="$3"
  for ((entry_attempt=1; entry_attempt<=attempts; entry_attempt++)); do
    if ! dump_device_ui "$remote_path" "$host_path"; then return 1; fi
    if grep -Fq -- 'content-desc="Main Track Edit"' "$host_path"; then
      printf '%s\n' "Main Track Edit"
      return 0
    fi
    if grep -Fq -- 'content-desc="MPC Menu TRACK EDIT"' "$host_path"; then
      printf '%s\n' "MPC Menu TRACK EDIT"
      return 0
    fi
    sleep 1
  done
  echo "ERROR: neither Main Track Edit nor MPC Menu TRACK EDIT appeared"
  cat "$host_path" || true
  return 1
}

if ! dump_device_ui "$DEVICE_DUMP" "$DUMP"; then
  echo "ERROR: could not inspect the workspace before Track Edit tuning acceptance"; exit 1
fi
track_edit_entry_description=""
if grep -Fq -- 'content-desc="Main Track Edit"' "$DUMP"; then
  track_edit_entry_description="Main Track Edit"
elif grep -Fq -- 'content-desc="MPC Menu TRACK EDIT"' "$DUMP"; then
  track_edit_entry_description="MPC Menu TRACK EDIT"
elif grep -Fq -- 'content-desc="MPC Browser BACK to previous workspace"' "$DUMP"; then
  echo "Post-audit workspace is Browser; using its real BACK action before Track Edit acceptance..."
  if ! tap_ui_description "$DUMP" "MPC Browser BACK to previous workspace"; then
    echo "ERROR: Browser BACK failed before Track Edit acceptance"; exit 1
  fi
elif grep -Fq -- 'content-desc="MPC Sampler recording workspace"' "$DUMP" \
    && grep -Fq -- 'text="BACK"' "$DUMP"; then
  echo "Post-audit workspace is Sampler; using its real BACK action before Track Edit acceptance..."
  if ! tap_ui_text "$DUMP" "BACK"; then
    echo "ERROR: Sampler BACK failed before Track Edit acceptance"; exit 1
  fi
else
  echo "ERROR: post-audit hierarchy exposes no known route to Track Edit"
  cat "$DUMP" || true
  exit 1
fi
if [[ -z "$track_edit_entry_description" ]]; then
  if ! track_edit_entry_description="$(wait_for_track_edit_entry "$DEVICE_DUMP" "$DUMP" 25)"; then
    echo "ERROR: could not resolve Track Edit entry after returning from the post-audit workspace"; exit 1
  fi
fi
track_edit_return_description="$track_edit_entry_description"
echo "Track Edit entry route selected from live hierarchy: $track_edit_entry_description"
if ! tap_ui_description "$DUMP" "$track_edit_entry_description"; then
  echo "ERROR: selected Track Edit entry control was not actionable: $track_edit_entry_description"; exit 1
fi
if ! wait_for_ui_description "$TRACK_EDIT_XML" "$TRACK_EDIT_REMOTE" "Track Edit Samples tab" 25; then
  echo "ERROR: Track Edit workspace did not open"; exit 1
fi
if ! tap_ui_description "$TRACK_EDIT_XML" "Track Edit Samples tab"; then
  echo "ERROR: Track Edit Samples tab was not actionable"; exit 1
fi
if ! wait_for_ui_description "$TRACK_EDIT_XML" "$TRACK_EDIT_REMOTE" "Track Edit Samples waveform" 20; then
  echo "ERROR: Track Edit Samples editor body did not render"; exit 1
fi
if ! dump_device_ui "$TRACK_EDIT_CONTEXT_BEFORE_REMOTE" "$TRACK_EDIT_CONTEXT_BEFORE_XML"; then
  echo "ERROR: Track Edit initial Pad/Layer/sample context hierarchy failed"; exit 1
fi
IFS=$'\t' read -r track_edit_pad_before track_edit_layer_before track_edit_sample_before \
  < <(read_track_edit_context_values "$TRACK_EDIT_CONTEXT_BEFORE_XML") || {
    echo "ERROR: Track Edit initial Pad/Layer/sample context snapshot was invalid"; exit 1;
  }
if [[ "$track_edit_pad_before" != "PAD 01" || "$track_edit_layer_before" != "LAYER 1/8" ]]; then
  echo "ERROR: Track Edit fixture must begin on PAD 01 / LAYER 1/8"
  echo "Pad=$track_edit_pad_before Layer=$track_edit_layer_before"; exit 1
fi
if ! scroll_ui_until_description "$TRACK_EDIT_XML" "$TRACK_EDIT_REMOTE" "Track Edit layer tuning increase" 8; then
  echo "ERROR: Track Edit layer tuning control did not become actionable after scrolling"; exit 1
fi
if ! dump_device_ui "$TRACK_EDIT_REMOTE" "$TRACK_EDIT_BEFORE_XML"; then
  echo "ERROR: Track Edit initial tuning hierarchy failed"; exit 1
fi
track_edit_tuning_before="$(read_track_edit_tuning_value "$TRACK_EDIT_BEFORE_XML")" || {
  echo "ERROR: Track Edit initial tuning value could not be read from the visible control"; exit 1;
}
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_BEFORE_PNG"; then
  echo "ERROR: Track Edit initial tuning screenshot failed"; exit 1
fi
if [[ "$track_edit_pad_before" != "PAD 01" || "$track_edit_layer_before" != "LAYER 1/8" ]]; then
  echo "ERROR: Track Edit fixture must begin on PAD 01 / LAYER 1/8"
  echo "Pad=$track_edit_pad_before Layer=$track_edit_layer_before"; exit 1
fi
echo "Track Edit baseline: Pad=$track_edit_pad_before Layer=$track_edit_layer_before sample=$track_edit_sample_before tuning=$track_edit_tuning_before st"

if ! tap_ui_description "$TRACK_EDIT_BEFORE_XML" "Track Edit layer tuning increase"; then
  echo "ERROR: Track Edit layer tuning increase was not actionable"; exit 1
fi
track_edit_tuning_changed=0
for attempt in {1..15}; do
  if ! dump_device_ui "$TRACK_EDIT_REMOTE" "$TRACK_EDIT_XML"; then
    echo "ERROR: could not observe Track Edit after tuning increment"; exit 1
  fi
  track_edit_tuning_current="$(read_track_edit_tuning_value "$TRACK_EDIT_XML" 2>/dev/null || true)"
  if [[ -n "$track_edit_tuning_current" && "$track_edit_tuning_current" != "$track_edit_tuning_before" ]]; then
    track_edit_tuning_changed=1; break
  fi
  sleep 1
done
if [[ "$track_edit_tuning_changed" != "1" ]]; then
  echo "ERROR: Track Edit tuning increment did not change the projected sampler value"; exit 1
fi
if ! python3 - "$track_edit_tuning_before" "$track_edit_tuning_current" <<'PY'
import sys
before, after = map(float, sys.argv[1:])
if abs((after - before) - 1.0) > 0.06:
    raise SystemExit(f"one TUNE+ tap should add one semitone: before={before}, after={after}")
print(f"Track Edit layer tuning increment verified: {before:+.2f} st → {after:+.2f} st")
PY
then
  echo "ERROR: Track Edit layer tuning increment was not one semitone"; exit 1
fi
if ! dump_device_ui "$TRACK_EDIT_REMOTE" "$TRACK_EDIT_CHANGED_XML"; then
  echo "ERROR: Track Edit changed tuning hierarchy failed"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_CHANGED_PNG"; then
  echo "ERROR: Track Edit changed tuning screenshot failed"; exit 1
fi

if ! tap_ui_description "$TRACK_EDIT_CHANGED_XML" "Track Edit back"; then
  echo "ERROR: Track Edit BACK did not return to Main"; exit 1
fi
if ! wait_for_ui_description "$DUMP" "$DEVICE_DUMP" "$track_edit_return_description" 25; then
  echo "ERROR: original Track Edit entry route was not restored after BACK: $track_edit_return_description"; exit 1
fi
if ! tap_ui_description "$DUMP" "$track_edit_return_description"; then
  echo "ERROR: could not re-enter Track Edit through its original route: $track_edit_return_description"; exit 1
fi
if ! wait_for_ui_description "$TRACK_EDIT_XML" "$TRACK_EDIT_REMOTE" "Track Edit Samples tab" 25; then
  echo "ERROR: Track Edit did not reopen"; exit 1
fi
if ! tap_ui_description "$TRACK_EDIT_XML" "Track Edit Samples tab"; then
  echo "ERROR: Track Edit Samples tab was not actionable after re-entry"; exit 1
fi
if ! wait_for_ui_description "$TRACK_EDIT_XML" "$TRACK_EDIT_REMOTE" "Track Edit Samples waveform" 20; then
  echo "ERROR: Track Edit Samples editor body did not render after re-entry"; exit 1
fi
if ! dump_device_ui "$TRACK_EDIT_CONTEXT_REOPENED_REMOTE" "$TRACK_EDIT_CONTEXT_REOPENED_XML"; then
  echo "ERROR: Track Edit re-entry Pad/Layer/sample context hierarchy failed"; exit 1
fi
IFS=$'\t' read -r track_edit_pad_reopened track_edit_layer_reopened track_edit_sample_reopened \
  < <(read_track_edit_context_values "$TRACK_EDIT_CONTEXT_REOPENED_XML") || {
    echo "ERROR: Track Edit re-entry Pad/Layer/sample context snapshot was invalid"; exit 1;
  }
if ! scroll_ui_until_description "$TRACK_EDIT_XML" "$TRACK_EDIT_REMOTE" "Track Edit layer tuning increase" 8; then
  echo "ERROR: Track Edit tuning control did not become actionable after re-entry scrolling"; exit 1
fi
if ! dump_device_ui "$TRACK_EDIT_REMOTE" "$TRACK_EDIT_REOPENED_XML"; then
  echo "ERROR: Track Edit re-entry tuning hierarchy failed"; exit 1
fi
track_edit_tuning_reopened="$(read_track_edit_tuning_value "$TRACK_EDIT_REOPENED_XML")" || {
  echo "ERROR: Track Edit re-entry tuning value could not be read from the visible control"; exit 1;
}
if [[ "$track_edit_pad_reopened" != "$track_edit_pad_before" \
    || "$track_edit_layer_reopened" != "$track_edit_layer_before" \
    || "$track_edit_sample_reopened" != "$track_edit_sample_before" \
    || "$track_edit_tuning_reopened" != "$track_edit_tuning_current" ]]; then
  echo "ERROR: Track Edit parameter/Pad/Layer/sample did not survive leaving and re-entering"
  echo "Before: Pad=$track_edit_pad_before Layer=$track_edit_layer_before sample=$track_edit_sample_before tuning=$track_edit_tuning_before"
  echo "Reopened: Pad=$track_edit_pad_reopened Layer=$track_edit_layer_reopened sample=$track_edit_sample_reopened tuning=$track_edit_tuning_reopened"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_REOPENED_PNG"; then
  echo "ERROR: Track Edit re-entry screenshot failed"; exit 1
fi

if ! tap_ui_description "$TRACK_EDIT_REOPENED_XML" "Track Edit layer tuning decrease"; then
  echo "ERROR: Track Edit tuning reset control was not actionable"; exit 1
fi
track_edit_tuning_restored=0
for attempt in {1..15}; do
  if ! dump_device_ui "$TRACK_EDIT_REMOTE" "$TRACK_EDIT_XML"; then
    echo "ERROR: could not verify restored Track Edit tuning"; exit 1
  fi
  track_edit_tuning_current="$(read_track_edit_tuning_value "$TRACK_EDIT_XML" 2>/dev/null || true)"
  if [[ "$track_edit_tuning_current" == "$track_edit_tuning_before" ]]; then
    track_edit_tuning_restored=1; break
  fi
  sleep 1
done
if [[ "$track_edit_tuning_restored" != "1" ]]; then
  echo "ERROR: Track Edit tuning test could not restore its initial sampler value"; exit 1
fi
if ! dump_device_ui "$TRACK_EDIT_REMOTE" "$TRACK_EDIT_RESTORED_XML"; then
  echo "ERROR: Track Edit restored tuning hierarchy failed"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_RESTORED_PNG"; then
  echo "ERROR: Track Edit restored tuning screenshot failed"; exit 1
fi
# EDIT-004: mutate only sample-layer gain through the real Track Edit touch controls,
# confirm TUNE/PAN remain unchanged, then inverse-touch and restore the original value.
echo "Running Track Edit sample-layer gain field-isolation → inverse restore runtime test..."
TRACK_EDIT_GAIN_CHANGED_PNG="/tmp/mpc-groovebox-track-edit-gain-changed.png"
TRACK_EDIT_GAIN_CHANGED_XML="/tmp/mpc-groovebox-track-edit-gain-changed-uiautomator.xml"
TRACK_EDIT_GAIN_REMOTE="/sdcard/mpc-groovebox-track-edit-gain-changed-uiautomator.xml"
TRACK_EDIT_GAIN_RESTORED_PNG="/tmp/mpc-groovebox-track-edit-gain-restored.png"
TRACK_EDIT_GAIN_RESTORED_XML="/tmp/mpc-groovebox-track-edit-gain-restored-uiautomator.xml"
TRACK_EDIT_GAIN_RESTORED_REMOTE="/sdcard/mpc-groovebox-track-edit-gain-restored-uiautomator.xml"
TRACK_EDIT_GAIN_RESTORE_CHECK_XML="/tmp/mpc-groovebox-track-edit-gain-restore-check-uiautomator.xml"
TRACK_EDIT_GAIN_RESTORE_CHECK_REMOTE="/sdcard/mpc-groovebox-track-edit-gain-restore-check-uiautomator.xml"

read_track_edit_gain_value() {
  python3 - "$1" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
found = [n for n in root.iter("node")
         if n.attrib.get("content-desc", "") == "Track Edit layer gain value"]
if len(found) != 1:
    raise SystemExit(f"expected one Track Edit layer gain value, found {len(found)}")
text = found[0].attrib.get("text", "")
match = re.search(r"(\d+)\s*%", text)
if match is None:
    raise SystemExit(f"could not parse layer gain percentage from {text!r}")
print(int(match.group(1)))
PY
}

read_track_edit_pan_value() {
  python3 - "$1" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
found = [n for n in root.iter("node")
         if n.attrib.get("content-desc", "") == "Track Edit layer pan value"]
if len(found) != 1:
    raise SystemExit(f"expected one Track Edit layer pan value, found {len(found)}")
print(re.sub(r"\s+", " ", found[0].attrib.get("text", "")).strip())
PY
}

read_track_edit_region_bounds() {
  python3 - "$1" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
nodes = list(ET.parse(sys.argv[1]).getroot().iter("node"))
found = [n for n in nodes
         if n.attrib.get("content-desc", "") == "Track Edit region"]
if len(found) != 1:
    raise SystemExit(f"expected one Track Edit region field, found {len(found)}")
text = re.sub(r"\s+", " ", found[0].attrib.get("text", "")).strip()
match = re.search(r"\bS\s+(\d+)\s*•\s*E\s+(\d+)\s*•\s*(\d+)\s+frames\b", text)
if match is None:
    raise SystemExit(f"could not parse sample region start/end/total from {text!r}")
start, end, total = map(int, match.groups())
if not 0 <= start < end <= total:
    raise SystemExit(f"invalid sample region: start={start}, end={end}, total={total}")
print(start, end, total)
PY
}


track_edit_gain_before="$(read_track_edit_gain_value "$TRACK_EDIT_RESTORED_XML")" || {
  echo "ERROR: initial Track Edit layer gain could not be read"; exit 1;
}
track_edit_gain_tuning_before="$(read_track_edit_tuning_value "$TRACK_EDIT_RESTORED_XML")" || {
  echo "ERROR: TUNE baseline missing before gain acceptance"; exit 1;
}
track_edit_gain_pan_before="$(read_track_edit_pan_value "$TRACK_EDIT_RESTORED_XML")" || {
  echo "ERROR: PAN baseline missing before gain acceptance"; exit 1;
}
if (( track_edit_gain_before >= 10 )); then
  track_edit_gain_direction="decrease"
  track_edit_gain_inverse_direction="increase"
  track_edit_gain_expected=$((track_edit_gain_before - 10))
else
  track_edit_gain_direction="increase"
  track_edit_gain_inverse_direction="decrease"
  track_edit_gain_expected=$((track_edit_gain_before + 10))
fi
if ! tap_ui_description "$TRACK_EDIT_RESTORED_XML" "Track Edit layer gain $track_edit_gain_direction"; then
  echo "ERROR: Track Edit layer gain $track_edit_gain_direction was not actionable"; exit 1
fi
track_edit_gain_changed=0
for attempt in {1..15}; do
  if ! dump_device_ui "$TRACK_EDIT_GAIN_REMOTE" "$TRACK_EDIT_GAIN_CHANGED_XML"; then
    echo "ERROR: Track Edit gain changed hierarchy capture failed"; exit 1
  fi
  track_edit_gain_current="$(read_track_edit_gain_value "$TRACK_EDIT_GAIN_CHANGED_XML" 2>/dev/null || true)"
  if [[ "$track_edit_gain_current" == "$track_edit_gain_expected" ]]; then
    track_edit_gain_changed=1; break
  fi
  sleep 1
done
if [[ "$track_edit_gain_changed" != "1" ]]; then
  echo "ERROR: Track Edit gain touch did not change native projection by exactly 10 percentage points"
  echo "Before=$track_edit_gain_before expected=$track_edit_gain_expected actual=$track_edit_gain_current"; exit 1
fi
track_edit_gain_tuning_changed="$(read_track_edit_tuning_value "$TRACK_EDIT_GAIN_CHANGED_XML")" || {
  echo "ERROR: TUNE projection missing after gain edit"; exit 1;
}
track_edit_gain_pan_changed="$(read_track_edit_pan_value "$TRACK_EDIT_GAIN_CHANGED_XML")" || {
  echo "ERROR: PAN projection missing after gain edit"; exit 1;
}
if [[ "$track_edit_gain_tuning_changed" != "$track_edit_gain_tuning_before" \
    || "$track_edit_gain_pan_changed" != "$track_edit_gain_pan_before" ]]; then
  echo "ERROR: changing sample-layer gain modified unrelated TUNE/PAN fields"
  echo "Tune: $track_edit_gain_tuning_before -> $track_edit_gain_tuning_changed"
  echo "Pan: $track_edit_gain_pan_before -> $track_edit_gain_pan_changed"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_GAIN_CHANGED_PNG"; then
  echo "ERROR: Track Edit gain changed screenshot capture failed"; exit 1
fi
if ! tap_ui_description "$TRACK_EDIT_GAIN_CHANGED_XML" "Track Edit layer gain $track_edit_gain_inverse_direction"; then
  echo "ERROR: Track Edit gain inverse control was not actionable"; exit 1
fi
track_edit_gain_restored=0
for attempt in {1..15}; do
  if ! dump_device_ui "$TRACK_EDIT_GAIN_RESTORE_CHECK_REMOTE" "$TRACK_EDIT_GAIN_RESTORE_CHECK_XML"; then
    echo "ERROR: Track Edit gain restoration hierarchy capture failed"; exit 1
  fi
  track_edit_gain_current="$(read_track_edit_gain_value "$TRACK_EDIT_GAIN_RESTORE_CHECK_XML" 2>/dev/null || true)"
  if [[ "$track_edit_gain_current" == "$track_edit_gain_before" ]]; then
    track_edit_gain_restored=1; break
  fi
  sleep 1
done
if [[ "$track_edit_gain_restored" != "1" ]]; then
  echo "ERROR: Track Edit gain inverse touch did not restore the original layer gain"; exit 1
fi
if ! dump_device_ui "$TRACK_EDIT_GAIN_RESTORED_REMOTE" "$TRACK_EDIT_GAIN_RESTORED_XML"; then
  echo "ERROR: Track Edit restored gain hierarchy capture failed"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_GAIN_RESTORED_PNG"; then
  echo "ERROR: Track Edit restored gain screenshot capture failed"; exit 1
fi
track_edit_gain_final="$(read_track_edit_gain_value "$TRACK_EDIT_GAIN_RESTORED_XML")" || {
  echo "ERROR: restored Track Edit gain value could not be read"; exit 1;
}
track_edit_gain_tuning_final="$(read_track_edit_tuning_value "$TRACK_EDIT_GAIN_RESTORED_XML")" || {
  echo "ERROR: restored TUNE projection missing"; exit 1;
}
track_edit_gain_pan_final="$(read_track_edit_pan_value "$TRACK_EDIT_GAIN_RESTORED_XML")" || {
  echo "ERROR: restored PAN projection missing"; exit 1;
}
if [[ "$track_edit_gain_final" != "$track_edit_gain_before" \
    || "$track_edit_gain_tuning_final" != "$track_edit_gain_tuning_before" \
    || "$track_edit_gain_pan_final" != "$track_edit_gain_pan_before" ]]; then
  echo "ERROR: gain inverse edit did not restore original gain/TUNE/PAN state"
  echo "Gain: $track_edit_gain_before -> $track_edit_gain_final; TUNE: $track_edit_gain_tuning_before -> $track_edit_gain_tuning_final; PAN: $track_edit_gain_pan_before -> $track_edit_gain_pan_final"; exit 1
fi
echo "Track Edit layer GAIN real-touch field isolation passed: $track_edit_gain_before% -> $track_edit_gain_expected% -> $track_edit_gain_final%; TUNE/PAN preserved."

# EDIT-005: set a real sample-layer pan value, then restore the fixture's original
# centered pan while proving that GAIN and TUNE remain untouched.
echo "Running Track Edit sample-layer PAN field-isolation → center restore runtime test..."
TRACK_EDIT_PAN_CHANGED_PNG="/tmp/mpc-groovebox-track-edit-pan-changed.png"
TRACK_EDIT_PAN_CHANGED_XML="/tmp/mpc-groovebox-track-edit-pan-changed-uiautomator.xml"
TRACK_EDIT_PAN_REMOTE="/sdcard/mpc-groovebox-track-edit-pan-changed-uiautomator.xml"
TRACK_EDIT_PAN_RESTORED_PNG="/tmp/mpc-groovebox-track-edit-pan-restored.png"
TRACK_EDIT_PAN_RESTORED_XML="/tmp/mpc-groovebox-track-edit-pan-restored-uiautomator.xml"
TRACK_EDIT_PAN_RESTORED_REMOTE="/sdcard/mpc-groovebox-track-edit-pan-restored-uiautomator.xml"
TRACK_EDIT_PAN_RESTORE_CHECK_XML="/tmp/mpc-groovebox-track-edit-pan-restore-check-uiautomator.xml"
TRACK_EDIT_PAN_RESTORE_CHECK_REMOTE="/sdcard/mpc-groovebox-track-edit-pan-restore-check-uiautomator.xml"

track_edit_pan_before="$(read_track_edit_pan_value "$TRACK_EDIT_GAIN_RESTORED_XML")" || {
  echo "ERROR: initial Track Edit layer pan could not be read"; exit 1;
}
track_edit_pan_gain_before="$(read_track_edit_gain_value "$TRACK_EDIT_GAIN_RESTORED_XML")" || {
  echo "ERROR: GAIN baseline missing before PAN acceptance"; exit 1;
}
track_edit_pan_tuning_before="$(read_track_edit_tuning_value "$TRACK_EDIT_GAIN_RESTORED_XML")" || {
  echo "ERROR: TUNE baseline missing before PAN acceptance"; exit 1;
}
if [[ "$track_edit_pan_before" != "PAN C" ]]; then
  echo "ERROR: PAN acceptance fixture must begin centered so its inverse touch can restore exact state"
  echo "Initial layer pan=$track_edit_pan_before"; exit 1
fi
if ! tap_ui_description "$TRACK_EDIT_GAIN_RESTORED_XML" "Track Edit layer pan decrease"; then
  echo "ERROR: Track Edit layer PAN-left control was not actionable"; exit 1
fi
track_edit_pan_changed=0
for attempt in {1..15}; do
  if ! dump_device_ui "$TRACK_EDIT_PAN_REMOTE" "$TRACK_EDIT_PAN_CHANGED_XML"; then
    echo "ERROR: Track Edit changed PAN hierarchy capture failed"; exit 1
  fi
  track_edit_pan_current="$(read_track_edit_pan_value "$TRACK_EDIT_PAN_CHANGED_XML" 2>/dev/null || true)"
  if [[ "$track_edit_pan_current" == "PAN L100" ]]; then
    track_edit_pan_changed=1; break
  fi
  sleep 1
done
if [[ "$track_edit_pan_changed" != "1" ]]; then
  echo "ERROR: Track Edit PAN-left touch did not set full-left pan"
  echo "Before=$track_edit_pan_before expected=PAN L100 actual=$track_edit_pan_current"; exit 1
fi
track_edit_pan_gain_changed="$(read_track_edit_gain_value "$TRACK_EDIT_PAN_CHANGED_XML")" || {
  echo "ERROR: GAIN projection missing after PAN edit"; exit 1;
}
track_edit_pan_tuning_changed="$(read_track_edit_tuning_value "$TRACK_EDIT_PAN_CHANGED_XML")" || {
  echo "ERROR: TUNE projection missing after PAN edit"; exit 1;
}
if [[ "$track_edit_pan_gain_changed" != "$track_edit_pan_gain_before" \
    || "$track_edit_pan_tuning_changed" != "$track_edit_pan_tuning_before" ]]; then
  echo "ERROR: sample-layer PAN edit modified unrelated GAIN/TUNE fields"
  echo "Gain: $track_edit_pan_gain_before -> $track_edit_pan_gain_changed"
  echo "Tune: $track_edit_pan_tuning_before -> $track_edit_pan_tuning_changed"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_PAN_CHANGED_PNG"; then
  echo "ERROR: Track Edit changed PAN screenshot capture failed"; exit 1
fi
if ! scroll_track_edit_to_layer_pan_center "$TRACK_EDIT_PAN_CHANGED_XML" "$TRACK_EDIT_PAN_REMOTE" 4; then
  echo "ERROR: Track Edit CENTER LAYER PAN did not become visible after bounded scrolling"; exit 1
fi
if ! tap_ui_description "$TRACK_EDIT_PAN_CHANGED_XML" "Track Edit layer pan center"; then
  echo "ERROR: Track Edit CENTER LAYER PAN control was not actionable"; exit 1
fi
track_edit_pan_restored=0
for attempt in {1..15}; do
  if ! dump_device_ui "$TRACK_EDIT_PAN_RESTORE_CHECK_REMOTE" "$TRACK_EDIT_PAN_RESTORE_CHECK_XML"; then
    echo "ERROR: Track Edit PAN restoration hierarchy capture failed"; exit 1
  fi
  track_edit_pan_current="$(read_track_edit_pan_value "$TRACK_EDIT_PAN_RESTORE_CHECK_XML" 2>/dev/null || true)"
  if [[ "$track_edit_pan_current" == "$track_edit_pan_before" ]]; then
    track_edit_pan_restored=1; break
  fi
  sleep 1
done
if [[ "$track_edit_pan_restored" != "1" ]]; then
  echo "ERROR: Track Edit center-pan touch did not restore the original pan"; exit 1
fi
if ! dump_device_ui "$TRACK_EDIT_PAN_RESTORED_REMOTE" "$TRACK_EDIT_PAN_RESTORED_XML"; then
  echo "ERROR: Track Edit restored PAN hierarchy capture failed"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_PAN_RESTORED_PNG"; then
  echo "ERROR: Track Edit restored PAN screenshot capture failed"; exit 1
fi
track_edit_pan_final="$(read_track_edit_pan_value "$TRACK_EDIT_PAN_RESTORED_XML")" || {
  echo "ERROR: restored PAN value could not be read"; exit 1;
}
track_edit_pan_gain_final="$(read_track_edit_gain_value "$TRACK_EDIT_PAN_RESTORED_XML")" || {
  echo "ERROR: GAIN projection missing after PAN restore"; exit 1;
}
track_edit_pan_tuning_final="$(read_track_edit_tuning_value "$TRACK_EDIT_PAN_RESTORED_XML")" || {
  echo "ERROR: TUNE projection missing after PAN restore"; exit 1;
}
if [[ "$track_edit_pan_final" != "$track_edit_pan_before" \
    || "$track_edit_pan_gain_final" != "$track_edit_pan_gain_before" \
    || "$track_edit_pan_tuning_final" != "$track_edit_pan_tuning_before" ]]; then
  echo "ERROR: PAN inverse edit failed to restore original PAN/GAIN/TUNE state"
  echo "PAN: $track_edit_pan_before -> $track_edit_pan_final; GAIN: $track_edit_pan_gain_before -> $track_edit_pan_gain_final; TUNE: $track_edit_pan_tuning_before -> $track_edit_pan_tuning_final"; exit 1
fi
echo "Track Edit layer PAN real-touch field isolation passed: PAN C → PAN L100 → PAN C; GAIN/TUNE preserved."

# EDIT-008: mutate sample-layer region start/end through their real 1K controls,
# prove one boundary changes at a time and restore the exact native region.
echo "Running Track Edit sample-region Start/End field-isolation → exact restore runtime test..."
TRACK_EDIT_REGION_BEFORE_PNG="/tmp/mpc-groovebox-track-edit-region-before.png"
TRACK_EDIT_REGION_BEFORE_XML="/tmp/mpc-groovebox-track-edit-region-before-uiautomator.xml"
TRACK_EDIT_REGION_BEFORE_REMOTE="/sdcard/mpc-groovebox-track-edit-region-before-uiautomator.xml"
TRACK_EDIT_REGION_START_CHANGED_PNG="/tmp/mpc-groovebox-track-edit-region-start-changed.png"
TRACK_EDIT_REGION_START_CHANGED_XML="/tmp/mpc-groovebox-track-edit-region-start-changed-uiautomator.xml"
TRACK_EDIT_REGION_START_CHANGED_REMOTE="/sdcard/mpc-groovebox-track-edit-region-start-changed-uiautomator.xml"
TRACK_EDIT_REGION_START_RESTORED_PNG="/tmp/mpc-groovebox-track-edit-region-start-restored.png"
TRACK_EDIT_REGION_START_RESTORED_XML="/tmp/mpc-groovebox-track-edit-region-start-restored-uiautomator.xml"
TRACK_EDIT_REGION_START_RESTORED_REMOTE="/sdcard/mpc-groovebox-track-edit-region-start-restored-uiautomator.xml"
TRACK_EDIT_REGION_END_CHANGED_PNG="/tmp/mpc-groovebox-track-edit-region-end-changed.png"
TRACK_EDIT_REGION_END_CHANGED_XML="/tmp/mpc-groovebox-track-edit-region-end-changed-uiautomator.xml"
TRACK_EDIT_REGION_END_CHANGED_REMOTE="/sdcard/mpc-groovebox-track-edit-region-end-changed-uiautomator.xml"
TRACK_EDIT_REGION_END_RESTORED_PNG="/tmp/mpc-groovebox-track-edit-region-end-restored.png"
TRACK_EDIT_REGION_END_RESTORED_XML="/tmp/mpc-groovebox-track-edit-region-end-restored-uiautomator.xml"
TRACK_EDIT_REGION_END_RESTORED_REMOTE="/sdcard/mpc-groovebox-track-edit-region-end-restored-uiautomator.xml"
TRACK_EDIT_REGION_CHECK_XML="/tmp/mpc-groovebox-track-edit-region-check-uiautomator.xml"
TRACK_EDIT_REGION_CHECK_REMOTE="/sdcard/mpc-groovebox-track-edit-region-check-uiautomator.xml"

if ! scroll_track_edit_to_layer_gain "$TRACK_EDIT_REGION_BEFORE_XML" "$TRACK_EDIT_REGION_BEFORE_REMOTE" 8; then
  echo "ERROR: Track Edit REGION/GAIN/TUNE/PAN fields did not return to the visible upper-layer viewport"; exit 1
fi
if ! dump_device_ui "$TRACK_EDIT_REGION_BEFORE_REMOTE" "$TRACK_EDIT_REGION_BEFORE_XML"; then
  echo "ERROR: Track Edit sample-region baseline hierarchy failed"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_REGION_BEFORE_PNG"; then
  echo "ERROR: Track Edit sample-region baseline screenshot failed"; exit 1
fi
track_edit_region_before="$(read_track_edit_region_bounds "$TRACK_EDIT_REGION_BEFORE_XML")" || {
  echo "ERROR: initial Track Edit sample-region values could not be read"; exit 1;
}
read -r track_edit_region_start_before track_edit_region_end_before track_edit_region_total_before <<< "$track_edit_region_before"
track_edit_region_gain_before="$(read_track_edit_gain_value "$TRACK_EDIT_REGION_BEFORE_XML")" || {
  echo "ERROR: GAIN baseline missing before sample-region acceptance"; exit 1;
}
track_edit_region_tuning_before="$(read_track_edit_tuning_value "$TRACK_EDIT_REGION_BEFORE_XML")" || {
  echo "ERROR: TUNE baseline missing before sample-region acceptance"; exit 1;
}
track_edit_region_pan_before="$(read_track_edit_pan_value "$TRACK_EDIT_REGION_BEFORE_XML")" || {
  echo "ERROR: PAN baseline missing before sample-region acceptance"; exit 1;
}

# Choose a safe ±1000-frame move for Start without changing End.
if (( track_edit_region_end_before - track_edit_region_start_before > 1000 )); then
  track_edit_region_start_direction="increase"
  track_edit_region_start_inverse="decrease"
  track_edit_region_start_expected=$((track_edit_region_start_before + 1000))
elif (( track_edit_region_start_before >= 1000 )); then
  track_edit_region_start_direction="decrease"
  track_edit_region_start_inverse="increase"
  track_edit_region_start_expected=$((track_edit_region_start_before - 1000))
else
  echo "ERROR: sample-region fixture has no safe 1000-frame Start move"; exit 1
fi
if ! tap_ui_description "$TRACK_EDIT_REGION_BEFORE_XML" "Track Edit layer region start $track_edit_region_start_direction"; then
  echo "ERROR: Track Edit region Start control was not actionable"; exit 1
fi
track_edit_region_start_changed=0
for attempt in {1..15}; do
  if ! dump_device_ui "$TRACK_EDIT_REGION_CHECK_REMOTE" "$TRACK_EDIT_REGION_CHECK_XML"; then
    echo "ERROR: could not observe sample-region Start edit"; exit 1
  fi
  track_edit_region_current="$(read_track_edit_region_bounds "$TRACK_EDIT_REGION_CHECK_XML" 2>/dev/null || true)"
  if [[ -n "$track_edit_region_current" ]]; then
    read -r track_edit_region_start_current track_edit_region_end_current track_edit_region_total_current <<< "$track_edit_region_current"
    if (( track_edit_region_start_current == track_edit_region_start_expected && track_edit_region_end_current == track_edit_region_end_before && track_edit_region_total_current == track_edit_region_total_before )); then
      track_edit_region_start_changed=1; break
    fi
  fi
  sleep 1
done
if [[ "$track_edit_region_start_changed" != "1" ]]; then
  echo "ERROR: Start touch did not change only the start frame by exactly 1000"
  echo "Before=$track_edit_region_before expected=$track_edit_region_start_expected — $track_edit_region_end_before actual=$track_edit_region_current"; exit 1
fi
if ! dump_device_ui "$TRACK_EDIT_REGION_START_CHANGED_REMOTE" "$TRACK_EDIT_REGION_START_CHANGED_XML"; then
  echo "ERROR: changed region Start hierarchy capture failed"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_REGION_START_CHANGED_PNG"; then
  echo "ERROR: changed region Start screenshot capture failed"; exit 1
fi
track_edit_region_gain_changed="$(read_track_edit_gain_value "$TRACK_EDIT_REGION_START_CHANGED_XML")" || { echo "ERROR: GAIN missing after Start edit"; exit 1; }
track_edit_region_tuning_changed="$(read_track_edit_tuning_value "$TRACK_EDIT_REGION_START_CHANGED_XML")" || { echo "ERROR: TUNE missing after Start edit"; exit 1; }
track_edit_region_pan_changed="$(read_track_edit_pan_value "$TRACK_EDIT_REGION_START_CHANGED_XML")" || { echo "ERROR: PAN missing after Start edit"; exit 1; }
if [[ "$track_edit_region_gain_changed" != "$track_edit_region_gain_before" || "$track_edit_region_tuning_changed" != "$track_edit_region_tuning_before" || "$track_edit_region_pan_changed" != "$track_edit_region_pan_before" ]]; then
  echo "ERROR: region Start edit modified unrelated GAIN/TUNE/PAN state"; exit 1
fi
if ! tap_ui_description "$TRACK_EDIT_REGION_START_CHANGED_XML" "Track Edit layer region start $track_edit_region_start_inverse"; then
  echo "ERROR: inverse region Start control was not actionable"; exit 1
fi
track_edit_region_start_restored=0
for attempt in {1..15}; do
  if ! dump_device_ui "$TRACK_EDIT_REGION_CHECK_REMOTE" "$TRACK_EDIT_REGION_CHECK_XML"; then
    echo "ERROR: could not observe region Start restoration"; exit 1
  fi
  track_edit_region_current="$(read_track_edit_region_bounds "$TRACK_EDIT_REGION_CHECK_XML" 2>/dev/null || true)"
  if [[ "$track_edit_region_current" == "$track_edit_region_before" ]]; then
    track_edit_region_start_restored=1; break
  fi
  sleep 1
done
if [[ "$track_edit_region_start_restored" != "1" ]]; then
  echo "ERROR: inverse Start touch did not restore the exact region"; exit 1
fi
if ! dump_device_ui "$TRACK_EDIT_REGION_START_RESTORED_REMOTE" "$TRACK_EDIT_REGION_START_RESTORED_XML"; then
  echo "ERROR: restored region Start hierarchy capture failed"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_REGION_START_RESTORED_PNG"; then
  echo "ERROR: restored region Start screenshot capture failed"; exit 1
fi

# Choose a safe ±1000-frame End move without changing Start.
if (( track_edit_region_end_before - track_edit_region_start_before > 1000 )); then
  track_edit_region_end_direction="decrease"
  track_edit_region_end_inverse="increase"
  track_edit_region_end_expected=$((track_edit_region_end_before - 1000))
elif (( track_edit_region_total_before - track_edit_region_end_before >= 1000 )); then
  track_edit_region_end_direction="increase"
  track_edit_region_end_inverse="decrease"
  track_edit_region_end_expected=$((track_edit_region_end_before + 1000))
else
  echo "ERROR: sample-region fixture has no safe 1000-frame End move"; exit 1
fi
if ! tap_ui_description "$TRACK_EDIT_REGION_START_RESTORED_XML" "Track Edit layer region end $track_edit_region_end_direction"; then
  echo "ERROR: Track Edit region End control was not actionable"; exit 1
fi
track_edit_region_end_changed=0
for attempt in {1..15}; do
  if ! dump_device_ui "$TRACK_EDIT_REGION_CHECK_REMOTE" "$TRACK_EDIT_REGION_CHECK_XML"; then
    echo "ERROR: could not observe sample-region End edit"; exit 1
  fi
  track_edit_region_current="$(read_track_edit_region_bounds "$TRACK_EDIT_REGION_CHECK_XML" 2>/dev/null || true)"
  if [[ -n "$track_edit_region_current" ]]; then
    read -r track_edit_region_start_current track_edit_region_end_current track_edit_region_total_current <<< "$track_edit_region_current"
    if (( track_edit_region_start_current == track_edit_region_start_before && track_edit_region_end_current == track_edit_region_end_expected && track_edit_region_total_current == track_edit_region_total_before )); then
      track_edit_region_end_changed=1; break
    fi
  fi
  sleep 1
done
if [[ "$track_edit_region_end_changed" != "1" ]]; then
  echo "ERROR: End touch did not change only the end frame by exactly 1000"
  echo "Before=$track_edit_region_before expected=$track_edit_region_start_before — $track_edit_region_end_expected actual=$track_edit_region_current"; exit 1
fi
if ! dump_device_ui "$TRACK_EDIT_REGION_END_CHANGED_REMOTE" "$TRACK_EDIT_REGION_END_CHANGED_XML"; then
  echo "ERROR: changed region End hierarchy capture failed"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_REGION_END_CHANGED_PNG"; then
  echo "ERROR: changed region End screenshot capture failed"; exit 1
fi
track_edit_region_gain_changed="$(read_track_edit_gain_value "$TRACK_EDIT_REGION_END_CHANGED_XML")" || { echo "ERROR: GAIN missing after End edit"; exit 1; }
track_edit_region_tuning_changed="$(read_track_edit_tuning_value "$TRACK_EDIT_REGION_END_CHANGED_XML")" || { echo "ERROR: TUNE missing after End edit"; exit 1; }
track_edit_region_pan_changed="$(read_track_edit_pan_value "$TRACK_EDIT_REGION_END_CHANGED_XML")" || { echo "ERROR: PAN missing after End edit"; exit 1; }
if [[ "$track_edit_region_gain_changed" != "$track_edit_region_gain_before" || "$track_edit_region_tuning_changed" != "$track_edit_region_tuning_before" || "$track_edit_region_pan_changed" != "$track_edit_region_pan_before" ]]; then
  echo "ERROR: region End edit modified unrelated GAIN/TUNE/PAN state"; exit 1
fi
if ! tap_ui_description "$TRACK_EDIT_REGION_END_CHANGED_XML" "Track Edit layer region end $track_edit_region_end_inverse"; then
  echo "ERROR: inverse region End control was not actionable"; exit 1
fi
track_edit_region_end_restored=0
for attempt in {1..15}; do
  if ! dump_device_ui "$TRACK_EDIT_REGION_CHECK_REMOTE" "$TRACK_EDIT_REGION_CHECK_XML"; then
    echo "ERROR: could not observe region End restoration"; exit 1
  fi
  track_edit_region_current="$(read_track_edit_region_bounds "$TRACK_EDIT_REGION_CHECK_XML" 2>/dev/null || true)"
  if [[ "$track_edit_region_current" == "$track_edit_region_before" ]]; then
    track_edit_region_end_restored=1; break
  fi
  sleep 1
done
if [[ "$track_edit_region_end_restored" != "1" ]]; then
  echo "ERROR: inverse End touch did not restore the exact region"; exit 1
fi
if ! dump_device_ui "$TRACK_EDIT_REGION_END_RESTORED_REMOTE" "$TRACK_EDIT_REGION_END_RESTORED_XML"; then
  echo "ERROR: restored region End hierarchy capture failed"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_REGION_END_RESTORED_PNG"; then
  echo "ERROR: restored region End screenshot capture failed"; exit 1
fi
track_edit_region_gain_restored="$(read_track_edit_gain_value "$TRACK_EDIT_REGION_END_RESTORED_XML")" || { echo "ERROR: restored GAIN missing"; exit 1; }
track_edit_region_tuning_restored="$(read_track_edit_tuning_value "$TRACK_EDIT_REGION_END_RESTORED_XML")" || { echo "ERROR: restored TUNE missing"; exit 1; }
track_edit_region_pan_restored="$(read_track_edit_pan_value "$TRACK_EDIT_REGION_END_RESTORED_XML")" || { echo "ERROR: restored PAN missing"; exit 1; }
if [[ "$track_edit_region_gain_restored" != "$track_edit_region_gain_before" || "$track_edit_region_tuning_restored" != "$track_edit_region_tuning_before" || "$track_edit_region_pan_restored" != "$track_edit_region_pan_before" ]]; then
  echo "ERROR: sample-region round-trip did not preserve GAIN/TUNE/PAN"; exit 1
fi
echo "Track Edit sample region Start/End real-touch isolation and exact restoration passed: $track_edit_region_before."

# EDIT-007: verify both sample-layer velocity-range bounds are native-backed,
# field-isolated, and inverse-restorable through real on-screen touch controls.
echo "Running Track Edit sample-layer velocity MIN/MAX field-isolation → restore runtime test..."
TRACK_EDIT_VELOCITY_BASELINE_PNG="/tmp/mpc-groovebox-track-edit-velocity-range-before.png"
TRACK_EDIT_VELOCITY_BASELINE_XML="/tmp/mpc-groovebox-track-edit-velocity-range-before-uiautomator.xml"
TRACK_EDIT_VELOCITY_REMOTE="/sdcard/mpc-groovebox-track-edit-velocity-range-uiautomator.xml"
TRACK_EDIT_VELOCITY_MAX_CHANGED_PNG="/tmp/mpc-groovebox-track-edit-velocity-max-changed.png"
TRACK_EDIT_VELOCITY_MAX_CHANGED_XML="/tmp/mpc-groovebox-track-edit-velocity-max-changed-uiautomator.xml"
TRACK_EDIT_VELOCITY_MAX_CHANGED_REMOTE="/sdcard/mpc-groovebox-track-edit-velocity-max-changed-uiautomator.xml"
TRACK_EDIT_VELOCITY_MAX_RESTORE_CHECK_XML="/tmp/mpc-groovebox-track-edit-velocity-max-restore-check-uiautomator.xml"
TRACK_EDIT_VELOCITY_MAX_RESTORE_CHECK_REMOTE="/sdcard/mpc-groovebox-track-edit-velocity-max-restore-check-uiautomator.xml"
TRACK_EDIT_VELOCITY_MAX_RESTORED_PNG="/tmp/mpc-groovebox-track-edit-velocity-max-restored.png"
TRACK_EDIT_VELOCITY_MAX_RESTORED_XML="/tmp/mpc-groovebox-track-edit-velocity-max-restored-uiautomator.xml"
TRACK_EDIT_VELOCITY_MAX_RESTORED_REMOTE="/sdcard/mpc-groovebox-track-edit-velocity-max-restored-uiautomator.xml"
TRACK_EDIT_VELOCITY_MIN_CHANGED_PNG="/tmp/mpc-groovebox-track-edit-velocity-min-changed.png"
TRACK_EDIT_VELOCITY_MIN_CHANGED_XML="/tmp/mpc-groovebox-track-edit-velocity-min-changed-uiautomator.xml"
TRACK_EDIT_VELOCITY_MIN_CHANGED_REMOTE="/sdcard/mpc-groovebox-track-edit-velocity-min-changed-uiautomator.xml"
TRACK_EDIT_VELOCITY_MIN_RESTORE_CHECK_XML="/tmp/mpc-groovebox-track-edit-velocity-min-restore-check-uiautomator.xml"
TRACK_EDIT_VELOCITY_MIN_RESTORE_CHECK_REMOTE="/sdcard/mpc-groovebox-track-edit-velocity-min-restore-check-uiautomator.xml"
TRACK_EDIT_VELOCITY_MIN_RESTORED_PNG="/tmp/mpc-groovebox-track-edit-velocity-min-restored.png"
TRACK_EDIT_VELOCITY_MIN_RESTORED_XML="/tmp/mpc-groovebox-track-edit-velocity-min-restored-uiautomator.xml"
TRACK_EDIT_VELOCITY_MIN_RESTORED_REMOTE="/sdcard/mpc-groovebox-track-edit-velocity-min-restored-uiautomator.xml"
TRACK_EDIT_VELOCITY_REENTRY_XML="/tmp/mpc-groovebox-track-edit-velocity-range-reentry-uiautomator.xml"
TRACK_EDIT_VELOCITY_REENTRY_REMOTE="/sdcard/mpc-groovebox-track-edit-velocity-range-reentry-uiautomator.xml"

read_track_edit_velocity_range() {
  python3 - "$1" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
found = [n for n in root.iter("node")
         if n.attrib.get("content-desc", "") == "Track Edit velocity"]
if len(found) != 1:
    raise SystemExit(f"expected one Track Edit velocity range field, found {len(found)}")
text = found[0].attrib.get("text", "")
match = re.search(r"VELOCITY\s+(\d+)\s*[—–-]\s*(\d+)", text)
if match is None:
    raise SystemExit(f"could not parse layer velocity range from {text!r}")
minimum, maximum = map(int, match.groups())
if not 0 <= minimum <= maximum <= 127:
    raise SystemExit(f"layer velocity range outside contract: {minimum} — {maximum}")
print(minimum, maximum)
PY
}

if ! scroll_ui_until_description "$TRACK_EDIT_XML" "$TRACK_EDIT_REMOTE" "Track Edit layer velocity minimum increase" 8; then
  echo "ERROR: Track Edit velocity-range field did not become visible after scrolling"; exit 1
fi
if ! dump_device_ui "$TRACK_EDIT_VELOCITY_REMOTE" "$TRACK_EDIT_VELOCITY_BASELINE_XML"; then
  echo "ERROR: Track Edit velocity-range baseline hierarchy capture failed"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_VELOCITY_BASELINE_PNG"; then
  echo "ERROR: Track Edit velocity-range baseline screenshot capture failed"; exit 1
fi
track_edit_velocity_range_before="$(read_track_edit_velocity_range "$TRACK_EDIT_VELOCITY_BASELINE_XML")" || {
  echo "ERROR: initial Track Edit velocity range could not be read"; exit 1;
}
read -r track_edit_velocity_min_before track_edit_velocity_max_before <<< "$track_edit_velocity_range_before"
if (( track_edit_velocity_max_before > track_edit_velocity_min_before )); then
  track_edit_velocity_max_direction="decrease"
  track_edit_velocity_max_inverse="MAX +"
  track_edit_velocity_max_expected=$((track_edit_velocity_max_before - 1))
elif (( track_edit_velocity_max_before < 127 )); then
  track_edit_velocity_max_direction="increase"
  track_edit_velocity_max_inverse="MAX −"
  track_edit_velocity_max_expected=$((track_edit_velocity_max_before + 1))
else
  echo "ERROR: velocity MAX fixture has no safe adjacent value to exercise"; exit 1
fi
if ! tap_ui_text "$TRACK_EDIT_VELOCITY_BASELINE_XML" "MAX $([[ "$track_edit_velocity_max_direction" == "decrease" ]] && printf '−' || printf '+')"; then
  echo "ERROR: Track Edit velocity MAX control was not actionable"; exit 1
fi
track_edit_velocity_max_changed=0
for attempt in {1..15}; do
  if ! dump_device_ui "$TRACK_EDIT_VELOCITY_MAX_CHANGED_REMOTE" "$TRACK_EDIT_VELOCITY_MAX_CHANGED_XML"; then
    echo "ERROR: Track Edit velocity MAX changed hierarchy capture failed"; exit 1
  fi
  track_edit_velocity_max_current="$(read_track_edit_velocity_range "$TRACK_EDIT_VELOCITY_MAX_CHANGED_XML" 2>/dev/null || true)"
  if [[ -n "$track_edit_velocity_max_current" ]]; then
    read -r track_edit_velocity_min_current track_edit_velocity_max_current_value <<< "$track_edit_velocity_max_current"
    if (( track_edit_velocity_min_current == track_edit_velocity_min_before         && track_edit_velocity_max_current_value == track_edit_velocity_max_expected )); then
      track_edit_velocity_max_changed=1; break
    fi
  fi
  sleep 1
done
if [[ "$track_edit_velocity_max_changed" != "1" ]]; then
  echo "ERROR: velocity MAX touch did not change only the upper bound by one"
  echo "Before=$track_edit_velocity_range_before expected=$track_edit_velocity_min_before — $track_edit_velocity_max_expected actual=$track_edit_velocity_max_current"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_VELOCITY_MAX_CHANGED_PNG"; then
  echo "ERROR: Track Edit velocity MAX changed screenshot capture failed"; exit 1
fi
if ! tap_ui_text "$TRACK_EDIT_VELOCITY_MAX_CHANGED_XML" "$track_edit_velocity_max_inverse"; then
  echo "ERROR: Track Edit velocity MAX inverse control was not actionable"; exit 1
fi
track_edit_velocity_max_restored=0
for attempt in {1..15}; do
  if ! dump_device_ui "$TRACK_EDIT_VELOCITY_MAX_RESTORE_CHECK_REMOTE" "$TRACK_EDIT_VELOCITY_MAX_RESTORE_CHECK_XML"; then
    echo "ERROR: Track Edit velocity MAX restoration hierarchy capture failed"; exit 1
  fi
  track_edit_velocity_range_current="$(read_track_edit_velocity_range "$TRACK_EDIT_VELOCITY_MAX_RESTORE_CHECK_XML" 2>/dev/null || true)"
  if [[ "$track_edit_velocity_range_current" == "$track_edit_velocity_range_before" ]]; then
    track_edit_velocity_max_restored=1; break
  fi
  sleep 1
done
if [[ "$track_edit_velocity_max_restored" != "1" ]]; then
  echo "ERROR: Track Edit velocity MAX inverse touch did not restore the original range"; exit 1
fi
if ! dump_device_ui "$TRACK_EDIT_VELOCITY_REMOTE" "$TRACK_EDIT_VELOCITY_MAX_RESTORED_XML"; then
  echo "ERROR: Track Edit restored velocity MAX hierarchy capture failed"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_VELOCITY_MAX_RESTORED_PNG"; then
  echo "ERROR: Track Edit restored velocity MAX screenshot capture failed"; exit 1
fi
echo "Track Edit velocity MAX changed only the upper bound and inverse-restored the exact range: $track_edit_velocity_range_before."

# The MIN control must leave MAX untouched and restore its original lower bound.
track_edit_velocity_range_before_min="$(read_track_edit_velocity_range "$TRACK_EDIT_VELOCITY_MAX_RESTORED_XML")" || {
  echo "ERROR: velocity MIN baseline could not be read after MAX restore"; exit 1;
}
read -r track_edit_velocity_min_before_min track_edit_velocity_max_before_min <<< "$track_edit_velocity_range_before_min"
if (( track_edit_velocity_min_before_min < track_edit_velocity_max_before_min )); then
  track_edit_velocity_min_direction="increase"
  track_edit_velocity_min_action="MIN +"
  track_edit_velocity_min_inverse="MIN −"
  track_edit_velocity_min_expected=$((track_edit_velocity_min_before_min + 1))
elif (( track_edit_velocity_min_before_min > 0 )); then
  track_edit_velocity_min_direction="decrease"
  track_edit_velocity_min_action="MIN −"
  track_edit_velocity_min_inverse="MIN +"
  track_edit_velocity_min_expected=$((track_edit_velocity_min_before_min - 1))
else
  echo "ERROR: velocity MIN fixture has no safe adjacent value to exercise"; exit 1
fi
if ! tap_ui_text "$TRACK_EDIT_VELOCITY_MAX_RESTORED_XML" "$track_edit_velocity_min_action"; then
  echo "ERROR: Track Edit velocity MIN control was not actionable"; exit 1
fi
track_edit_velocity_min_changed=0
for attempt in {1..15}; do
  if ! dump_device_ui "$TRACK_EDIT_VELOCITY_MIN_CHANGED_REMOTE" "$TRACK_EDIT_VELOCITY_MIN_CHANGED_XML"; then
    echo "ERROR: Track Edit velocity MIN changed hierarchy capture failed"; exit 1
  fi
  track_edit_velocity_min_current="$(read_track_edit_velocity_range "$TRACK_EDIT_VELOCITY_MIN_CHANGED_XML" 2>/dev/null || true)"
  if [[ -n "$track_edit_velocity_min_current" ]]; then
    read -r track_edit_velocity_min_current_value track_edit_velocity_max_current <<< "$track_edit_velocity_min_current"
    if (( track_edit_velocity_min_current_value == track_edit_velocity_min_expected         && track_edit_velocity_max_current == track_edit_velocity_max_before_min )); then
      track_edit_velocity_min_changed=1; break
    fi
  fi
  sleep 1
done
if [[ "$track_edit_velocity_min_changed" != "1" ]]; then
  echo "ERROR: velocity MIN touch did not change only the lower bound by one"
  echo "Before=$track_edit_velocity_range_before_min expected=$track_edit_velocity_min_expected — $track_edit_velocity_max_before_min actual=$track_edit_velocity_min_current"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_VELOCITY_MIN_CHANGED_PNG"; then
  echo "ERROR: Track Edit velocity MIN changed screenshot capture failed"; exit 1
fi
if ! tap_ui_text "$TRACK_EDIT_VELOCITY_MIN_CHANGED_XML" "$track_edit_velocity_min_inverse"; then
  echo "ERROR: Track Edit velocity MIN inverse control was not actionable"; exit 1
fi
track_edit_velocity_min_restored=0
for attempt in {1..15}; do
  if ! dump_device_ui "$TRACK_EDIT_VELOCITY_MIN_RESTORE_CHECK_REMOTE" "$TRACK_EDIT_VELOCITY_MIN_RESTORE_CHECK_XML"; then
    echo "ERROR: Track Edit velocity MIN restoration hierarchy capture failed"; exit 1
  fi
  track_edit_velocity_range_current="$(read_track_edit_velocity_range "$TRACK_EDIT_VELOCITY_MIN_RESTORE_CHECK_XML" 2>/dev/null || true)"
  if [[ "$track_edit_velocity_range_current" == "$track_edit_velocity_range_before_min" ]]; then
    track_edit_velocity_min_restored=1; break
  fi
  sleep 1
done
if [[ "$track_edit_velocity_min_restored" != "1" ]]; then
  echo "ERROR: Track Edit velocity MIN inverse touch did not restore the original range"; exit 1
fi
if ! dump_device_ui "$TRACK_EDIT_VELOCITY_REMOTE" "$TRACK_EDIT_VELOCITY_MIN_RESTORED_XML"; then
  echo "ERROR: Track Edit restored velocity MIN hierarchy capture failed"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_VELOCITY_MIN_RESTORED_PNG"; then
  echo "ERROR: Track Edit restored velocity MIN screenshot capture failed"; exit 1
fi
echo "Track Edit velocity MIN changed only the lower bound and inverse-restored the exact range: $track_edit_velocity_range_before_min."

# Re-enter the upper layer controls using a direction-aware reverse scroll.
# The velocity-row smoke leaves GAIN above the viewport; merely waiting for the
# TUNE+ button can return early while GAIN is still absent from UiAutomator.
if ! scroll_track_edit_to_layer_gain "$TRACK_EDIT_VELOCITY_REENTRY_XML" "$TRACK_EDIT_VELOCITY_REMOTE" 8; then
  echo "ERROR: Track Edit upper layer parameters did not become visible after velocity-range acceptance"; exit 1
fi
track_edit_velocity_reentry_gain="$(read_track_edit_gain_value "$TRACK_EDIT_VELOCITY_REENTRY_XML")" || {
  echo "ERROR: GAIN value missing after velocity-range edits"; exit 1;
}
track_edit_velocity_reentry_tuning="$(read_track_edit_tuning_value "$TRACK_EDIT_VELOCITY_REENTRY_XML")" || {
  echo "ERROR: TUNE value missing after velocity-range edits"; exit 1;
}
track_edit_velocity_reentry_pan="$(read_track_edit_pan_value "$TRACK_EDIT_VELOCITY_REENTRY_XML")" || {
  echo "ERROR: PAN value missing after velocity-range edits"; exit 1;
}
if [[ "$track_edit_velocity_reentry_gain" != "$track_edit_pan_gain_before" \
    || "$track_edit_velocity_reentry_tuning" != "$track_edit_pan_tuning_before" \
    || "$track_edit_velocity_reentry_pan" != "$track_edit_pan_before" ]]; then
  echo "ERROR: velocity-range edits changed GAIN/TUNE/PAN outside their own fields"
  echo "GAIN $track_edit_pan_gain_before -> $track_edit_velocity_reentry_gain; TUNE $track_edit_pan_tuning_before -> $track_edit_velocity_reentry_tuning; PAN $track_edit_pan_before -> $track_edit_velocity_reentry_pan"; exit 1
fi
echo "Track Edit velocity MIN/MAX round trips restored exact bounds and preserved GAIN/TUNE/PAN."

# EDIT-009: exercise one amp-envelope parameter and the filter cutoff through
# semantic UI controls; verify field isolation and inverse restoration from native-backed values.
echo "Opening Track Edit AMP ENV for envelope/filter real-touch acceptance..."
TRACK_EDIT_ENV_BASELINE_PNG="/tmp/mpc-groovebox-track-edit-envelope-filter-before.png"
TRACK_EDIT_ENV_BASELINE_XML="/tmp/mpc-groovebox-track-edit-envelope-filter-before-uiautomator.xml"
TRACK_EDIT_ENV_REMOTE="/sdcard/mpc-groovebox-track-edit-envelope-filter-uiautomator.xml"
TRACK_EDIT_ENV_ATTACK_CHANGED_PNG="/tmp/mpc-groovebox-track-edit-envelope-attack-changed.png"
TRACK_EDIT_ENV_ATTACK_CHANGED_XML="/tmp/mpc-groovebox-track-edit-envelope-attack-changed-uiautomator.xml"
TRACK_EDIT_ENV_ATTACK_RESTORED_PNG="/tmp/mpc-groovebox-track-edit-envelope-attack-restored.png"
TRACK_EDIT_ENV_ATTACK_RESTORED_XML="/tmp/mpc-groovebox-track-edit-envelope-attack-restored-uiautomator.xml"
TRACK_EDIT_ENV_ATTACK_CHECK_XML="/tmp/mpc-groovebox-track-edit-envelope-attack-check-uiautomator.xml"
TRACK_EDIT_FILTER_CHANGED_PNG="/tmp/mpc-groovebox-track-edit-filter-cutoff-changed.png"
TRACK_EDIT_FILTER_CHANGED_XML="/tmp/mpc-groovebox-track-edit-filter-cutoff-changed-uiautomator.xml"
TRACK_EDIT_FILTER_RESTORED_PNG="/tmp/mpc-groovebox-track-edit-filter-cutoff-restored.png"
TRACK_EDIT_FILTER_RESTORED_XML="/tmp/mpc-groovebox-track-edit-filter-cutoff-restored-uiautomator.xml"
TRACK_EDIT_FILTER_CHECK_XML="/tmp/mpc-groovebox-track-edit-filter-cutoff-check-uiautomator.xml"

if ! tap_ui_description "$TRACK_EDIT_VELOCITY_REENTRY_XML" "Track Edit AMP ENV tab"; then
  echo "ERROR: Track Edit AMP ENV tab could not be opened"; exit 1
fi
if ! wait_for_ui_description "$TRACK_EDIT_ENV_REMOTE" "$TRACK_EDIT_ENV_BASELINE_XML" "Track Edit attack value" 15; then
  echo "ERROR: Track Edit envelope Attack value is not exposed"; exit 1
fi
if ! wait_for_ui_description "$TRACK_EDIT_ENV_REMOTE" "$TRACK_EDIT_ENV_BASELINE_XML" "Track Edit cutoff value value" 15; then
  echo "ERROR: Track Edit filter cutoff value is not exposed"; exit 1
fi
if ! dump_device_ui "$TRACK_EDIT_ENV_REMOTE" "$TRACK_EDIT_ENV_BASELINE_XML"; then
  echo "ERROR: Track Edit AMP ENV baseline hierarchy capture failed"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_ENV_BASELINE_PNG"; then
  echo "ERROR: Track Edit AMP ENV baseline screenshot capture failed"; exit 1
fi

read_track_edit_envelope_state() {
  python3 - "$1" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
specs = (
    ("attack", re.compile(r"([0-9]+(?:\.[0-9]+)?)\s*ms\b")),
    ("decay", re.compile(r"([0-9]+(?:\.[0-9]+)?)\s*ms\b")),
    ("sustain", re.compile(r"([0-9]+(?:\.[0-9]+)?)\s*%")),
    ("release", re.compile(r"([0-9]+(?:\.[0-9]+)?)\s*ms\b")),
)
values = []
for name, pattern in specs:
    description = "Track Edit " + name + " value"
    found = [n for n in root.iter("node")
             if n.attrib.get("content-desc", "") == description]
    if len(found) != 1:
        raise SystemExit(f"expected one {description}, found {len(found)}")
    value_text = found[0].attrib.get("text", "")
    match = pattern.search(value_text)
    if match is None:
        raise SystemExit(f"could not parse {description} from {value_text!r}")
    values.append(int(round(float(match.group(1)))))
if not (0 <= values[0] <= 2000 and 0 <= values[1] <= 2000
        and 0 <= values[2] <= 100 and 0 <= values[3] <= 2000):
    raise SystemExit(f"envelope value outside supported bounds: {values!r}")
print(" ".join(map(str, values)))
PY
}

read_track_edit_filter_cutoff_hz() {
  python3 - "$1" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
found = [n for n in root.iter("node")
         if n.attrib.get("content-desc", "") == "Track Edit cutoff value value"]
if len(found) != 1:
    raise SystemExit(f"expected one Track Edit cutoff value value field, found {len(found)}")
value_text = found[0].attrib.get("text", "")
khz = re.search(r"([0-9]+(?:\.[0-9]+)?)\s*kHz\b", value_text, re.I)
if khz:
    value = int(round(float(khz.group(1)) * 1000.0))
else:
    hz = re.search(r"([0-9]+(?:\.[0-9]+)?)\s*Hz\b", value_text, re.I)
    if hz is None:
        raise SystemExit(f"could not parse filter cutoff from {value_text!r}")
    value = int(round(float(hz.group(1))))
if not 20 <= value <= 20000:
    raise SystemExit(f"filter cutoff outside supported bounds: {value}")
print(value)
PY
}

track_edit_env_before="$(read_track_edit_envelope_state "$TRACK_EDIT_ENV_BASELINE_XML")" || {
  echo "ERROR: initial Track Edit envelope state could not be read"; exit 1;
}
read -r track_edit_env_attack_before track_edit_env_decay_before track_edit_env_sustain_before track_edit_env_release_before <<< "$track_edit_env_before"
if (( track_edit_env_attack_before < 1900 )); then
  track_edit_env_attack_action="Track Edit attack increase"
  track_edit_env_attack_inverse="Track Edit attack decrease"
  track_edit_env_attack_expected=$((track_edit_env_attack_before + 100))
elif (( track_edit_env_attack_before >= 100 )); then
  track_edit_env_attack_action="Track Edit attack decrease"
  track_edit_env_attack_inverse="Track Edit attack increase"
  track_edit_env_attack_expected=$((track_edit_env_attack_before - 100))
else
  echo "ERROR: AMP ENV Attack has no safe adjacent 100 ms value to exercise"; exit 1
fi
if ! tap_ui_description "$TRACK_EDIT_ENV_BASELINE_XML" "$track_edit_env_attack_action"; then
  echo "ERROR: Track Edit AMP ENV Attack control was not actionable"; exit 1
fi
track_edit_env_attack_changed=0
for attempt in {1..15}; do
  if ! dump_device_ui "$TRACK_EDIT_ENV_REMOTE" "$TRACK_EDIT_ENV_ATTACK_CHANGED_XML"; then
    echo "ERROR: Track Edit changed Attack hierarchy capture failed"; exit 1
  fi
  track_edit_env_current="$(read_track_edit_envelope_state "$TRACK_EDIT_ENV_ATTACK_CHANGED_XML" 2>/dev/null || true)"
  if [[ -n "$track_edit_env_current" ]]; then
    read -r track_edit_env_attack_current track_edit_env_decay_current track_edit_env_sustain_current track_edit_env_release_current <<< "$track_edit_env_current"
    if (( track_edit_env_attack_current == track_edit_env_attack_expected \
        && track_edit_env_decay_current == track_edit_env_decay_before \
        && track_edit_env_sustain_current == track_edit_env_sustain_before \
        && track_edit_env_release_current == track_edit_env_release_before )); then
      track_edit_env_attack_changed=1; break
    fi
  fi
  sleep 1
done
if [[ "$track_edit_env_attack_changed" != "1" ]]; then
  echo "ERROR: Attack edit changed an unexpected envelope field or did not reach its adjacent value"
  echo "Before=$track_edit_env_before expected Attack=$track_edit_env_attack_expected actual=$track_edit_env_current"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_ENV_ATTACK_CHANGED_PNG"; then
  echo "ERROR: Track Edit changed Attack screenshot capture failed"; exit 1
fi
if ! tap_ui_description "$TRACK_EDIT_ENV_ATTACK_CHANGED_XML" "$track_edit_env_attack_inverse"; then
  echo "ERROR: Track Edit AMP ENV Attack inverse control was not actionable"; exit 1
fi
track_edit_env_attack_restored=0
for attempt in {1..15}; do
  if ! dump_device_ui "$TRACK_EDIT_ENV_REMOTE" "$TRACK_EDIT_ENV_ATTACK_CHECK_XML"; then
    echo "ERROR: Track Edit Attack restoration hierarchy capture failed"; exit 1
  fi
  track_edit_env_current="$(read_track_edit_envelope_state "$TRACK_EDIT_ENV_ATTACK_CHECK_XML" 2>/dev/null || true)"
  if [[ "$track_edit_env_current" == "$track_edit_env_before" ]]; then
    track_edit_env_attack_restored=1; break
  fi
  sleep 1
done
if [[ "$track_edit_env_attack_restored" != "1" ]]; then
  echo "ERROR: Attack inverse touch did not restore the exact original envelope"
  echo "Before=$track_edit_env_before actual=$track_edit_env_current"; exit 1
fi
if ! dump_device_ui "$TRACK_EDIT_ENV_REMOTE" "$TRACK_EDIT_ENV_ATTACK_RESTORED_XML"; then
  echo "ERROR: restored Attack hierarchy capture failed"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_ENV_ATTACK_RESTORED_PNG"; then
  echo "ERROR: restored Attack screenshot capture failed"; exit 1
fi
echo "Track Edit AMP ENV Attack real-touch field isolation and exact restore passed: $track_edit_env_before."

# Verify DECAY, SUSTAIN and RELEASE with the same real-touch, independent-field
# and inverse-restoration contract as Attack. Each row has its own archived evidence.
TRACK_EDIT_ENV_WORK_XML="/tmp/mpc-groovebox-track-edit-envelope-work-uiautomator.xml"
TRACK_EDIT_ENV_CHECK_XML="/tmp/mpc-groovebox-track-edit-envelope-check-uiautomator.xml"
TRACK_EDIT_ENV_DECAY_CHANGED_PNG="/tmp/mpc-groovebox-track-edit-envelope-decay-changed.png"
TRACK_EDIT_ENV_DECAY_CHANGED_XML="/tmp/mpc-groovebox-track-edit-envelope-decay-changed-uiautomator.xml"
TRACK_EDIT_ENV_DECAY_RESTORED_PNG="/tmp/mpc-groovebox-track-edit-envelope-decay-restored.png"
TRACK_EDIT_ENV_DECAY_RESTORED_XML="/tmp/mpc-groovebox-track-edit-envelope-decay-restored-uiautomator.xml"
TRACK_EDIT_ENV_SUSTAIN_CHANGED_PNG="/tmp/mpc-groovebox-track-edit-envelope-sustain-changed.png"
TRACK_EDIT_ENV_SUSTAIN_CHANGED_XML="/tmp/mpc-groovebox-track-edit-envelope-sustain-changed-uiautomator.xml"
TRACK_EDIT_ENV_SUSTAIN_RESTORED_PNG="/tmp/mpc-groovebox-track-edit-envelope-sustain-restored.png"
TRACK_EDIT_ENV_SUSTAIN_RESTORED_XML="/tmp/mpc-groovebox-track-edit-envelope-sustain-restored-uiautomator.xml"
TRACK_EDIT_ENV_RELEASE_CHANGED_PNG="/tmp/mpc-groovebox-track-edit-envelope-release-changed.png"
TRACK_EDIT_ENV_RELEASE_CHANGED_XML="/tmp/mpc-groovebox-track-edit-envelope-release-changed-uiautomator.xml"
TRACK_EDIT_ENV_RELEASE_RESTORED_PNG="/tmp/mpc-groovebox-track-edit-envelope-release-restored.png"
TRACK_EDIT_ENV_RELEASE_RESTORED_XML="/tmp/mpc-groovebox-track-edit-envelope-release-restored-uiautomator.xml"

exercise_track_edit_envelope_field() {
  local field="$1" step="$2" baseline_xml="$3"
  local changed_png="$4" changed_xml="$5" restored_png="$6" restored_xml="$7"
  local before expected action inverse index maximum
  local current changed=0 restored=0
  local before_attack before_decay before_sustain before_release
  local now_attack now_decay now_sustain now_release

  before="$(read_track_edit_envelope_state "$baseline_xml")" || {
    echo "ERROR: could not read $field baseline ADSR state"; return 1;
  }
  read -r before_attack before_decay before_sustain before_release <<< "$before"
  case "$field" in
    decay) index=1; maximum=2000; before_value="$before_decay" ;;
    sustain) index=2; maximum=100; before_value="$before_sustain" ;;
    release) index=3; maximum=2000; before_value="$before_release" ;;
    *) echo "ERROR: unsupported envelope field in acceptance helper: $field"; return 1 ;;
  esac
  if (( before_value + step <= maximum )); then
    action="Track Edit $field increase"
    inverse="Track Edit $field decrease"
    expected=$((before_value + step))
  elif (( before_value >= step )); then
    action="Track Edit $field decrease"
    inverse="Track Edit $field increase"
    expected=$((before_value - step))
  else
    echo "ERROR: $field has no safe adjacent value for the acceptance step $step"; return 1
  fi

  if ! scroll_ui_until_description "$TRACK_EDIT_ENV_WORK_XML" "$TRACK_EDIT_ENV_REMOTE" "$action" 8; then
    echo "ERROR: Track Edit $field action did not become visible after bounded scrolling"; return 1
  fi
  if ! tap_ui_description "$TRACK_EDIT_ENV_WORK_XML" "$action"; then
    echo "ERROR: Track Edit $field real-touch action failed"; return 1
  fi
  for attempt in {1..15}; do
    if ! dump_device_ui "$TRACK_EDIT_ENV_REMOTE" "$TRACK_EDIT_ENV_CHECK_XML"; then
      echo "ERROR: Track Edit $field changed-state hierarchy capture failed"; return 1
    fi
    current="$(read_track_edit_envelope_state "$TRACK_EDIT_ENV_CHECK_XML" 2>/dev/null || true)"
    if [[ -n "$current" ]]; then
      read -r now_attack now_decay now_sustain now_release <<< "$current"
      case "$field" in
        decay) changed_value="$now_decay";;
        sustain) changed_value="$now_sustain";;
        release) changed_value="$now_release";;
      esac
      if (( changed_value == expected )) \
          && [[ "$now_attack" == "$before_attack" ]] \
          && [[ "$now_decay" == "$before_decay" || "$field" == "decay" ]] \
          && [[ "$now_sustain" == "$before_sustain" || "$field" == "sustain" ]] \
          && [[ "$now_release" == "$before_release" || "$field" == "release" ]]; then
        changed=1; break
      fi
    fi
    sleep 1
  done
  if [[ "$changed" != "1" ]]; then
    echo "ERROR: $field real-touch did not change only its expected envelope field"
    echo "Before=$before expected=$expected actual=$current"; return 1
  fi
  if ! dump_device_ui "$TRACK_EDIT_ENV_REMOTE" "$changed_xml"; then
    echo "ERROR: Track Edit $field changed-state evidence capture failed"; return 1
  fi
  if ! adb_bounded 10 exec-out screencap -p > "$changed_png"; then
    echo "ERROR: Track Edit $field changed-state screenshot capture failed"; return 1
  fi

  if ! scroll_ui_until_description "$TRACK_EDIT_ENV_WORK_XML" "$TRACK_EDIT_ENV_REMOTE" "$inverse" 8; then
    echo "ERROR: Track Edit $field inverse action did not become visible"; return 1
  fi
  if ! tap_ui_description "$TRACK_EDIT_ENV_WORK_XML" "$inverse"; then
    echo "ERROR: Track Edit $field inverse touch failed"; return 1
  fi
  for attempt in {1..15}; do
    if ! dump_device_ui "$TRACK_EDIT_ENV_REMOTE" "$TRACK_EDIT_ENV_CHECK_XML"; then
      echo "ERROR: Track Edit $field restoration hierarchy capture failed"; return 1
    fi
    current="$(read_track_edit_envelope_state "$TRACK_EDIT_ENV_CHECK_XML" 2>/dev/null || true)"
    if [[ "$current" == "$before" ]]; then restored=1; break; fi
    sleep 1
  done
  if [[ "$restored" != "1" ]]; then
    echo "ERROR: Track Edit $field inverse touch did not restore exact ADSR state"
    echo "Before=$before actual=$current"; return 1
  fi
  if ! dump_device_ui "$TRACK_EDIT_ENV_REMOTE" "$restored_xml"; then
    echo "ERROR: Track Edit $field restored-state evidence capture failed"; return 1
  fi
  if ! adb_bounded 10 exec-out screencap -p > "$restored_png"; then
    echo "ERROR: Track Edit $field restored-state screenshot capture failed"; return 1
  fi
  echo "Track Edit AMP ENV $field real-touch field isolation and exact restore passed: $before."
}

if ! exercise_track_edit_envelope_field "decay" 100 "$TRACK_EDIT_ENV_ATTACK_RESTORED_XML" \
    "$TRACK_EDIT_ENV_DECAY_CHANGED_PNG" "$TRACK_EDIT_ENV_DECAY_CHANGED_XML" \
    "$TRACK_EDIT_ENV_DECAY_RESTORED_PNG" "$TRACK_EDIT_ENV_DECAY_RESTORED_XML"; then exit 1; fi
if ! exercise_track_edit_envelope_field "sustain" 10 "$TRACK_EDIT_ENV_DECAY_RESTORED_XML" \
    "$TRACK_EDIT_ENV_SUSTAIN_CHANGED_PNG" "$TRACK_EDIT_ENV_SUSTAIN_CHANGED_XML" \
    "$TRACK_EDIT_ENV_SUSTAIN_RESTORED_PNG" "$TRACK_EDIT_ENV_SUSTAIN_RESTORED_XML"; then exit 1; fi
if ! exercise_track_edit_envelope_field "release" 100 "$TRACK_EDIT_ENV_SUSTAIN_RESTORED_XML" \
    "$TRACK_EDIT_ENV_RELEASE_CHANGED_PNG" "$TRACK_EDIT_ENV_RELEASE_CHANGED_XML" \
    "$TRACK_EDIT_ENV_RELEASE_RESTORED_PNG" "$TRACK_EDIT_ENV_RELEASE_RESTORED_XML"; then exit 1; fi

track_edit_filter_before="$(read_track_edit_filter_cutoff_hz "$TRACK_EDIT_ENV_ATTACK_RESTORED_XML")" || {
  echo "ERROR: initial Track Edit filter cutoff could not be read"; exit 1;
}
track_edit_filter_envelope_before="$(read_track_edit_envelope_state "$TRACK_EDIT_ENV_ATTACK_RESTORED_XML")" || {
  echo "ERROR: envelope state after Attack restore could not be read"; exit 1;
}
if [[ "$track_edit_filter_envelope_before" != "$track_edit_env_before" ]]; then
  echo "ERROR: Attack inverse touch left a non-baseline envelope state"; exit 1
fi
if (( track_edit_filter_before <= 19500 )); then
  track_edit_filter_action="Track Edit filter cutoff increase"
  track_edit_filter_inverse="Track Edit filter cutoff decrease"
  track_edit_filter_expected=$((track_edit_filter_before + 500))
elif (( track_edit_filter_before >= 520 )); then
  track_edit_filter_action="Track Edit filter cutoff decrease"
  track_edit_filter_inverse="Track Edit filter cutoff increase"
  track_edit_filter_expected=$((track_edit_filter_before - 500))
else
  echo "ERROR: filter cutoff has no safe adjacent 500 Hz value to exercise"; exit 1
fi
if ! scroll_ui_until_description "$TRACK_EDIT_FILTER_CHECK_XML" "$TRACK_EDIT_ENV_REMOTE" "$track_edit_filter_action" 8; then
  echo "ERROR: Track Edit filter cutoff action did not become visible after bounded scrolling"; exit 1
fi
if ! tap_ui_description "$TRACK_EDIT_FILTER_CHECK_XML" "$track_edit_filter_action"; then
  echo "ERROR: Track Edit filter cutoff control was not actionable"; exit 1
fi
track_edit_filter_changed=0
for attempt in {1..15}; do
  if ! dump_device_ui "$TRACK_EDIT_ENV_REMOTE" "$TRACK_EDIT_FILTER_CHANGED_XML"; then
    echo "ERROR: Track Edit changed filter hierarchy capture failed"; exit 1
  fi
  track_edit_filter_current="$(read_track_edit_filter_cutoff_hz "$TRACK_EDIT_FILTER_CHANGED_XML" 2>/dev/null || true)"
  track_edit_filter_envelope_current="$(read_track_edit_envelope_state "$TRACK_EDIT_FILTER_CHANGED_XML" 2>/dev/null || true)"
  if [[ -n "$track_edit_filter_current" && -n "$track_edit_filter_envelope_current" ]]; then
    if (( track_edit_filter_current == track_edit_filter_expected )) \
        && [[ "$track_edit_filter_envelope_current" == "$track_edit_env_before" ]]; then
      track_edit_filter_changed=1; break
    fi
  fi
  sleep 1
done
if [[ "$track_edit_filter_changed" != "1" ]]; then
  echo "ERROR: filter cutoff edit failed to change only cutoff by 500 Hz"
  echo "Before=$track_edit_filter_before expected=$track_edit_filter_expected actual=$track_edit_filter_current; envelope=$track_edit_filter_envelope_current"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_FILTER_CHANGED_PNG"; then
  echo "ERROR: Track Edit changed filter screenshot capture failed"; exit 1
fi
if ! tap_ui_description "$TRACK_EDIT_FILTER_CHANGED_XML" "$track_edit_filter_inverse"; then
  echo "ERROR: Track Edit filter cutoff inverse control was not actionable"; exit 1
fi
track_edit_filter_restored=0
for attempt in {1..15}; do
  if ! dump_device_ui "$TRACK_EDIT_ENV_REMOTE" "$TRACK_EDIT_FILTER_CHECK_XML"; then
    echo "ERROR: Track Edit filter restoration hierarchy capture failed"; exit 1
  fi
  track_edit_filter_current="$(read_track_edit_filter_cutoff_hz "$TRACK_EDIT_FILTER_CHECK_XML" 2>/dev/null || true)"
  track_edit_filter_envelope_current="$(read_track_edit_envelope_state "$TRACK_EDIT_FILTER_CHECK_XML" 2>/dev/null || true)"
  if [[ "$track_edit_filter_current" == "$track_edit_filter_before" \
      && "$track_edit_filter_envelope_current" == "$track_edit_env_before" ]]; then
    track_edit_filter_restored=1; break
  fi
  sleep 1
done
if [[ "$track_edit_filter_restored" != "1" ]]; then
  echo "ERROR: filter inverse touch did not restore the exact cutoff/envelope state"
  echo "Before=$track_edit_filter_before actual=$track_edit_filter_current; envelope=$track_edit_filter_envelope_current"; exit 1
fi
if ! dump_device_ui "$TRACK_EDIT_ENV_REMOTE" "$TRACK_EDIT_FILTER_RESTORED_XML"; then
  echo "ERROR: restored filter hierarchy capture failed"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$TRACK_EDIT_FILTER_RESTORED_PNG"; then
  echo "ERROR: restored filter screenshot capture failed"; exit 1
fi
echo "Track Edit filter cutoff real-touch field isolation and exact restore passed: $track_edit_filter_before Hz; AMP ENV preserved."
echo "Track Edit EDIT-009 AMP ENV/filter acceptance passed with inverse-restored state."

for evidence in \
  "$TRACK_EDIT_CONTEXT_BEFORE_XML" "$TRACK_EDIT_CONTEXT_REOPENED_XML" \
  "$TRACK_EDIT_BEFORE_PNG" "$TRACK_EDIT_BEFORE_XML" \
  "$TRACK_EDIT_CHANGED_PNG" "$TRACK_EDIT_CHANGED_XML" \
  "$TRACK_EDIT_REOPENED_PNG" "$TRACK_EDIT_REOPENED_XML" \
  "$TRACK_EDIT_RESTORED_PNG" "$TRACK_EDIT_RESTORED_XML" \
  "$TRACK_EDIT_GAIN_CHANGED_PNG" "$TRACK_EDIT_GAIN_CHANGED_XML" \
  "$TRACK_EDIT_GAIN_RESTORED_PNG" "$TRACK_EDIT_GAIN_RESTORED_XML" \
  "$TRACK_EDIT_PAN_CHANGED_PNG" "$TRACK_EDIT_PAN_CHANGED_XML" \
  "$TRACK_EDIT_PAN_RESTORED_PNG" "$TRACK_EDIT_PAN_RESTORED_XML" \
  "$TRACK_EDIT_VELOCITY_BASELINE_PNG" "$TRACK_EDIT_VELOCITY_BASELINE_XML" \
  "$TRACK_EDIT_VELOCITY_MAX_CHANGED_PNG" "$TRACK_EDIT_VELOCITY_MAX_CHANGED_XML" \
  "$TRACK_EDIT_VELOCITY_MAX_RESTORED_PNG" "$TRACK_EDIT_VELOCITY_MAX_RESTORED_XML" \
  "$TRACK_EDIT_VELOCITY_MIN_CHANGED_PNG" "$TRACK_EDIT_VELOCITY_MIN_CHANGED_XML" \
  "$TRACK_EDIT_VELOCITY_MIN_RESTORED_PNG" "$TRACK_EDIT_VELOCITY_MIN_RESTORED_XML" \
  "$TRACK_EDIT_ENV_BASELINE_PNG" "$TRACK_EDIT_ENV_BASELINE_XML" \
  "$TRACK_EDIT_ENV_ATTACK_CHANGED_PNG" "$TRACK_EDIT_ENV_ATTACK_CHANGED_XML" \
  "$TRACK_EDIT_ENV_ATTACK_RESTORED_PNG" "$TRACK_EDIT_ENV_ATTACK_RESTORED_XML" \
  "$TRACK_EDIT_FILTER_CHANGED_PNG" "$TRACK_EDIT_FILTER_CHANGED_XML" \
  "$TRACK_EDIT_FILTER_RESTORED_PNG" "$TRACK_EDIT_FILTER_RESTORED_XML" \
  "$TRACK_EDIT_ENV_DECAY_CHANGED_PNG" "$TRACK_EDIT_ENV_DECAY_CHANGED_XML" \
  "$TRACK_EDIT_ENV_DECAY_RESTORED_PNG" "$TRACK_EDIT_ENV_DECAY_RESTORED_XML" \
  "$TRACK_EDIT_ENV_SUSTAIN_CHANGED_PNG" "$TRACK_EDIT_ENV_SUSTAIN_CHANGED_XML" \
  "$TRACK_EDIT_ENV_SUSTAIN_RESTORED_PNG" "$TRACK_EDIT_ENV_SUSTAIN_RESTORED_XML" \
  "$TRACK_EDIT_ENV_RELEASE_CHANGED_PNG" "$TRACK_EDIT_ENV_RELEASE_CHANGED_XML" \
  "$TRACK_EDIT_ENV_RELEASE_RESTORED_PNG" "$TRACK_EDIT_ENV_RELEASE_RESTORED_XML"; do
  if ! test -s "$evidence"; then
    echo "ERROR: Track Edit runtime evidence missing/empty: $evidence"; exit 1
  fi
done
if ! tap_ui_description "$TRACK_EDIT_VELOCITY_REENTRY_XML" "Track Edit back"; then
  echo "ERROR: Track Edit BACK could not return to Main after restoring tuning"; exit 1
fi
if ! wait_for_ui_description "$DUMP" "$DEVICE_DUMP" "$track_edit_return_description" 25; then
  echo "ERROR: original navigation context was not restored after Track Edit acceptance: $track_edit_return_description"
  cat "$DUMP" || true
  exit 1
fi
echo "Track Edit layer tuning persistence/restore plus GAIN/PAN field-isolation and inverse restoration passed."


# The previous smoke ends with the Browser rail highlighted while the workspace
# has returned to Main. Open Browser through the toolbar's explicit route instead
# of assuming the highlighted shortcut mirrors the current workspace.
if ! tap_ui_description "$DUMP" "MPC Project Browser"; then exit 1; fi
if ! wait_for_ui_description "$BROWSER_LOCAL_XML" "$BROWSER_LOCAL_REMOTE" \
    "MPC Browser scan local audio files" 20; then exit 1; fi
if ! tap_ui_description "$BROWSER_LOCAL_XML" "MPC Browser scan local audio files"; then exit 1; fi
# The prior app-side UI audit can leave Browser search/filter state behind.
# Reset both controls explicitly so the fixture visibility check starts from a
# deterministic catalog view rather than the audit's last navigation context.
if ! tap_ui_description "$BROWSER_LOCAL_XML" "MPC Browser clear search"; then exit 1; fi
if ! tap_ui_description "$BROWSER_LOCAL_XML" "MPC Browser filter ALL"; then exit 1; fi
if ! wait_for_ui_description "$BROWSER_LOCAL_XML" "$BROWSER_LOCAL_REMOTE" \
    "MPC Browser file $LOCAL_SAMPLE_NAME" 20; then
  echo "ERROR: clearing inherited Browser state did not reveal the indexed local WAV"
  exit 1
fi

# Exercise the real on-screen Search field against the indexed MediaStore rows.
# Positive and negative queries must change the visible result set, not only the
# query label or accessibility description.
if ! tap_ui_description "$BROWSER_LOCAL_XML" "MPC Browser search files"; then exit 1; fi
if ! adb_bounded 10 shell input text "Browser-Smoke-Local"; then
  echo "ERROR: could not enter positive Browser search query"
  exit 1
fi
# Dismiss the landscape IME before tapping Browser chrome; otherwise the
# keyboard can intercept the Clear button's coordinates and append the next query.
if ! adb_bounded 10 shell input keyevent KEYCODE_BACK; then
  echo "ERROR: could not dismiss the keyboard after positive Browser search"
  exit 1
fi
if ! wait_for_ui_description "$BROWSER_LOCAL_XML" "$BROWSER_LOCAL_REMOTE" \
    "MPC Browser file $LOCAL_SAMPLE_NAME" 10; then
  echo "ERROR: Search did not retain the matching indexed WAV"
  exit 1
fi
if ! tap_ui_description "$BROWSER_LOCAL_XML" "MPC Browser clear search"; then exit 1; fi
if ! tap_ui_description "$BROWSER_LOCAL_XML" "MPC Browser search files"; then exit 1; fi
if ! adb_bounded 10 shell input text "NoSuchWavResult"; then
  echo "ERROR: could not enter negative Browser search query"
  exit 1
fi
if ! adb_bounded 10 shell input keyevent KEYCODE_BACK; then
  echo "ERROR: could not dismiss the keyboard after negative Browser search"
  exit 1
fi
if ! wait_for_ui_description "$BROWSER_LOCAL_XML" "$BROWSER_LOCAL_REMOTE" \
    "MPC Browser status NO MATCHING WAV FILES" 10; then
  echo "ERROR: a non-matching search query did not empty the real result set"
  exit 1
fi
if ! tap_ui_description "$BROWSER_LOCAL_XML" "MPC Browser clear search"; then exit 1; fi
if ! wait_for_ui_description "$BROWSER_LOCAL_XML" "$BROWSER_LOCAL_REMOTE" \
    "MPC Browser file $LOCAL_SAMPLE_NAME" 10; then
  echo "ERROR: clearing Search did not restore indexed results"
  exit 1
fi

# File-type filters must act on actual results too: PROJECTS yields none for
# the WAV-only loader, while ALL and SAMPLES restore the real WAV row.
if ! tap_ui_description "$BROWSER_LOCAL_XML" "MPC Browser filter PROJECTS"; then exit 1; fi
if ! wait_for_ui_description "$BROWSER_LOCAL_XML" "$BROWSER_LOCAL_REMOTE" \
    "MPC Browser status NO MATCHING WAV FILES" 10; then
  echo "ERROR: PROJECTS filter incorrectly retained WAV sample results"
  exit 1
fi
if ! tap_ui_description "$BROWSER_LOCAL_XML" "MPC Browser filter ALL"; then exit 1; fi
if ! wait_for_ui_description "$BROWSER_LOCAL_XML" "$BROWSER_LOCAL_REMOTE" \
    "MPC Browser file $LOCAL_SAMPLE_NAME" 10; then
  echo "ERROR: ALL filter failed to restore indexed WAV results"
  exit 1
fi
if ! tap_ui_description "$BROWSER_LOCAL_XML" "MPC Browser filter SAMPLES"; then exit 1; fi
if ! wait_for_ui_description "$BROWSER_LOCAL_XML" "$BROWSER_LOCAL_REMOTE" \
    "MPC Browser file $LOCAL_SAMPLE_NAME" 10; then
  echo "ERROR: SAMPLES filter failed to restore indexed WAV results"
  exit 1
fi

# The sample follows two pinned Browser actions in a short ScrollView. Scroll it
# fully into the hit area instead of tapping a clipped row at the viewport edge.
if ! adb_bounded 10 shell input swipe 1400 930 1400 760 300; then
  echo "ERROR: could not scroll the Browser result list"
  exit 1
fi
sleep 1
if ! dump_device_ui "$BROWSER_LOCAL_REMOTE" "$BROWSER_LOCAL_XML"; then exit 1; fi
if ! tap_ui_description "$BROWSER_LOCAL_XML" "MPC Browser file $LOCAL_SAMPLE_NAME"; then exit 1; fi
if ! wait_for_ui_description "$BROWSER_LOCAL_XML" "$BROWSER_LOCAL_REMOTE" \
    "MPC Browser audition selected local WAV file" 10; then exit 1; fi
if ! tap_ui_description "$BROWSER_LOCAL_XML" "MPC Browser audition selected local WAV file"; then exit 1; fi
if ! wait_for_successful_audition "$BROWSER_LOCAL_XML" "$BROWSER_LOCAL_REMOTE" \
    "$LOCAL_SAMPLE_NAME" 8; then
  exit 1
fi
if ! tap_ui_description "$BROWSER_LOCAL_XML" \
    "MPC Browser assign selected local WAV to target Pad/Layer"; then exit 1; fi
if ! wait_for_ui_node_text_desc "$BROWSER_LOCAL_XML" "$BROWSER_LOCAL_REMOTE" \
    "CURRENT SAMPLE • $LOCAL_SAMPLE_NAME" "MPC Browser current sample" 20; then
  echo "ERROR: WAV assignment did not update the Browser current Pad/Layer sample"
  exit 1
fi
if ! wait_for_ui_text_pair "$BROWSER_LOCAL_XML" "$BROWSER_LOCAL_REMOTE" \
    "ASSIGNED" "PAD 01 / LAYER 01" 20; then
  echo "ERROR: native sampler assignment did not report the correct target"
  exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > /tmp/mpc-groovebox-browser-local-results.png; then
  echo "ERROR: could not capture local Browser result/assignment screenshot"
  exit 1
fi
echo "Browser local WAV scan, selection, audition and Pad/Layer assignment passed."

# End-to-end SAF selection → Activity result → sampler assignment.
# Use a distinct WAV in Downloads so the test proves that the picker-selected
# document, not the previous MediaStore selection, becomes the Pad/Layer sample.
echo "Running Android Document Picker selection-return-target-preservation test..."
PICKER_SAMPLE_NAME="Browser-Picker-Return.wav"
PICKER_SAMPLE_HOST="/tmp/$PICKER_SAMPLE_NAME"
PICKER_SAMPLE_DEVICE="/sdcard/Download/$PICKER_SAMPLE_NAME"
PICKER_SAMPLE_CANONICAL="/storage/emulated/0/Download/$PICKER_SAMPLE_NAME"
PICKER_MEDIA_SCAN="/tmp/mpc-browser-picker-media-scan.txt"
PICKER_MEDIA_QUERY="/tmp/mpc-browser-picker-media-query.txt"
PICKER_UI_XML="/tmp/mpc-groovebox-picker-select-source-uiautomator.xml"
PICKER_UI_REMOTE="/sdcard/mpc-groovebox-picker-select-source-uiautomator.xml"
PICKER_RETURN_XML="/tmp/mpc-groovebox-browser-picker-assignment-uiautomator.xml"
PICKER_RETURN_REMOTE="/sdcard/mpc-groovebox-browser-picker-assignment-uiautomator.xml"

if ! cp "$LOCAL_SAMPLE_HOST" "$PICKER_SAMPLE_HOST"; then
  echo "ERROR: could not prepare distinct SAF picker WAV fixture"
  exit 1
fi
if ! adb_bounded 10 shell mkdir -p /sdcard/Download; then
  echo "ERROR: could not prepare emulator Downloads folder"
  exit 1
fi
if ! adb_bounded 30 push "$PICKER_SAMPLE_HOST" "$PICKER_SAMPLE_DEVICE"; then
  echo "ERROR: could not copy SAF picker WAV fixture to Downloads"
  exit 1
fi
if ! adb_bounded 15 shell content call \
    --uri content://media \
    --method scan_file \
    --arg "$PICKER_SAMPLE_CANONICAL" > "$PICKER_MEDIA_SCAN" 2>&1; then
  echo "ERROR: MediaStore scan_file failed for picker-return WAV fixture"
  cat "$PICKER_MEDIA_SCAN" || true
  exit 1
fi
PICKER_MEDIA_ITEM_URI="$(grep -oE 'content://media/[^[:space:]]+/audio/media/[0-9]+' \
    "$PICKER_MEDIA_SCAN" | head -n 1 || true)"
PICKER_MEDIA_ITEM_ID="${PICKER_MEDIA_ITEM_URI##*/}"
PICKER_MEDIA_COLLECTION_URI="${PICKER_MEDIA_ITEM_URI%/*}"
if [[ ! "$PICKER_MEDIA_ITEM_ID" =~ ^[0-9]+$ || -z "$PICKER_MEDIA_COLLECTION_URI" ]]; then
  echo "ERROR: MediaStore did not return an audio URI for the picker-return fixture"
  cat "$PICKER_MEDIA_SCAN" || true
  exit 1
fi
if ! adb_bounded 10 shell content query \
    --uri "$PICKER_MEDIA_COLLECTION_URI" \
    --projection _id:_display_name:mime_type \
    --where "_id=$PICKER_MEDIA_ITEM_ID" > "$PICKER_MEDIA_QUERY" 2>&1; then
  echo "ERROR: could not verify the picker-return WAV in MediaStore"
  cat "$PICKER_MEDIA_QUERY" || true
  exit 1
fi
if ! grep -Fq -- "$PICKER_SAMPLE_NAME" "$PICKER_MEDIA_QUERY"; then
  echo "ERROR: indexed picker-return fixture name did not match"
  cat "$PICKER_MEDIA_QUERY" || true
  exit 1
fi

# Open the actual Android SAF provider from Browser's LOAD function.
if ! tap_ui_description "$BROWSER_LOCAL_XML" \
    "MPC Browser LOAD source file with Android Document Picker"; then exit 1; fi
picker_activity=""
for picker_attempt in {1..15}; do
  picker_activity="$(adb_bounded 5 shell dumpsys activity activities 2>/dev/null \
    | grep -E 'mResumedActivity|topResumedActivity' | tail -n 1 || true)"
  if [[ -n "$picker_activity" ]] && ! grep -Fq -- "$PACKAGE" <<<"$picker_activity"; then
    break
  fi
  sleep 1
done
if [[ -z "$picker_activity" ]] || grep -Fq -- "$PACKAGE" <<<"$picker_activity"; then
  echo "ERROR: Android Document Picker did not become the resumed activity for selection test"
  printf '%s\n' "$picker_activity"
  exit 1
fi
printf '%s\n' "$picker_activity" > /tmp/mpc-groovebox-picker-select-source-activity.txt

# Always capture the first DocumentsUI frame before reading or tapping its
# controls. The activity can be resumed before its first accessibility tree is ready.
if ! dump_device_ui "$PICKER_UI_REMOTE" "$PICKER_UI_XML"; then
  echo "ERROR: could not capture initial Android Document Picker hierarchy"
  exit 1
fi

# Recent/search results are scoped to the current DocumentsUI location on
# Android 16; the filename in the EditText is not proof of a result. Navigate
# through the real provider root to Downloads, where the fixture was created.
if ! tap_ui_description "$PICKER_UI_XML" "Show roots"; then
  if ! tap_ui_description "$PICKER_UI_XML" "Show navigation drawer"; then
    echo "ERROR: Android Document Picker root navigation is unavailable"
    exit 1
  fi
fi
if ! wait_for_ui_text "$PICKER_UI_XML" "$PICKER_UI_REMOTE" "Downloads" 8; then
  echo "ERROR: DocumentsUI root drawer did not expose Downloads"
  exit 1
fi
if ! tap_ui_text "$PICKER_UI_XML" "Downloads"; then
  echo "ERROR: could not open Downloads in Android Document Picker"
  exit 1
fi
if ! wait_for_ui_text "$PICKER_UI_XML" "$PICKER_UI_REMOTE" "$PICKER_SAMPLE_NAME" 15; then
  echo "ERROR: picker did not show the prepared WAV in Downloads"
  exit 1
fi

# Android 16 DocumentsUI grid cards expose a separate Preview control while
# the file tile itself is not accessibility-clickable. Switch to the real
# list row before selecting, and verify the mode transition rather than
# repeatedly tapping the decorative thumbnail/card whitespace.
if grep -Fq -- 'content-desc="List view"' "$PICKER_UI_XML"; then
  if ! tap_ui_description "$PICKER_UI_XML" "List view"; then
    echo "ERROR: could not switch DocumentsUI to its selectable list-row mode"
    exit 1
  fi
  picker_list_mode=0
  for list_attempt in {1..10}; do
    if dump_device_ui "$PICKER_UI_REMOTE" "$PICKER_UI_XML" \
        && grep -Fq -- 'content-desc="Grid view"' "$PICKER_UI_XML"; then
      picker_list_mode=1
      break
    fi
    sleep 1
  done
  if [[ "$picker_list_mode" != "1" ]]; then
    echo "ERROR: DocumentsUI did not enter list mode after activating List view"
    exit 1
  fi
elif ! grep -Fq -- 'content-desc="Grid view"' "$PICKER_UI_XML"; then
  echo "ERROR: DocumentsUI exposes neither List view nor Grid view; cannot prove row selection"
  exit 1
fi
if ! grep -Fq -- "$PICKER_SAMPLE_NAME" "$PICKER_UI_XML"; then
  echo "ERROR: picker WAV disappeared while switching to list mode"
  exit 1
fi

# Keep an artifact of the actual picker state with the distinct WAV visible.
if ! adb_bounded 10 exec-out screencap -p > /tmp/mpc-groovebox-picker-select-source.png; then
  echo "ERROR: could not capture picker source selection screen"
  exit 1
fi
# PICKER_UI_XML is already the canonical evidence path; assert its
# contents instead of copying the file onto itself.
if ! test -s "$PICKER_UI_XML"; then
  echo "ERROR: Android Document Picker source hierarchy is missing or empty"
  exit 1
fi
if ! tap_ui_picker_result_row "$PICKER_UI_XML" "$PICKER_SAMPLE_NAME"; then
  echo "ERROR: visible SAF picker WAV result row was not selectable"
  exit 1
fi

# The result must close DocumentsUI and return to the same Browser workspace.
app_resumed=0
for return_attempt in {1..6}; do
  returned_activity="$(adb_bounded 5 shell dumpsys activity activities 2>/dev/null \
    | grep -E 'mResumedActivity|topResumedActivity' | tail -n 1 || true)"
  if grep -Fq -- "$PACKAGE" <<<"$returned_activity"; then
    app_resumed=1
    break
  fi
  sleep 1
done
if [[ "$app_resumed" != "1" ]]; then
  # Some DocumentsUI builds select the row first and expose an explicit Open
  # action instead of returning on the first tap. Inspect the live tree and
  # complete that genuine picker action if present; otherwise retry the row once.
  if ! dump_device_ui "$PICKER_UI_REMOTE" "$PICKER_UI_XML"; then
    echo "ERROR: could not inspect DocumentsUI after selecting the WAV"
    exit 1
  fi
  picker_confirm_label="$(python3 - "$PICKER_UI_XML" <<'PY'
import sys
import xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
nodes = list(root.iter("node"))
parents = {child: parent for parent in nodes for child in list(parent)}
wanted = {"open", "select", "use this file"}
for node in nodes:
    label = node.attrib.get("text", "").strip()
    if label.lower() not in wanted:
        continue
    current = node
    while current is not None:
        if current.attrib.get("clickable") == "true" and current.attrib.get("enabled") == "true":
            print(label)
            raise SystemExit(0)
        current = parents.get(current)
raise SystemExit(1)
PY
)" || picker_confirm_label=""
  if [[ -n "$picker_confirm_label" ]]; then
    if ! tap_ui_text "$PICKER_UI_XML" "$picker_confirm_label"; then
      echo "ERROR: DocumentsUI exposed $picker_confirm_label but it could not be activated"
      exit 1
    fi
  else
    if ! tap_ui_picker_result_row "$PICKER_UI_XML" "$PICKER_SAMPLE_NAME"; then
      echo "ERROR: selected WAV result row disappeared before DocumentsUI could return it"
      exit 1
    fi
    # item_root is focusable on this Android 16 provider build but does not
    # advertise an accessibility click action. Try both standard keyboard
    # activation keys on the focused real file row; the activity-result/URI
    # assertions below still prove that a genuine pick actually completed.
    if ! adb_bounded 5 shell input keyevent KEYCODE_ENTER; then
      echo "ERROR: could not send Enter to focused DocumentsUI WAV row"
      exit 1
    fi
    if ! adb_bounded 5 shell input keyevent KEYCODE_DPAD_CENTER; then
      echo "ERROR: could not activate focused DocumentsUI WAV row"
      exit 1
    fi
  fi
  for return_attempt in {1..20}; do
    returned_activity="$(adb_bounded 5 shell dumpsys activity activities 2>/dev/null \
      | grep -E 'mResumedActivity|topResumedActivity' | tail -n 1 || true)"
    if grep -Fq -- "$PACKAGE" <<<"$returned_activity"; then
      app_resumed=1
      break
    fi
    sleep 1
  done
fi
if [[ "$app_resumed" != "1" ]]; then
  echo "ERROR: selecting a WAV in DocumentsUI did not return to the groovebox Activity"
  printf '%s\n' "$returned_activity"
  if dump_device_ui "$PICKER_UI_REMOTE" "$PICKER_UI_XML"; then
    python3 - "$PICKER_UI_XML" <<'PY'
import sys
import xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
for node in root.iter("node"):
    label = node.attrib.get("text", "").strip()
    desc = node.attrib.get("content-desc", "").strip()
    if label or desc:
        print(f"text={label!r} desc={desc!r}")
PY
  fi
  exit 1
fi
if ! wait_for_ui_node_text_desc "$PICKER_RETURN_XML" "$PICKER_RETURN_REMOTE" \
    "CURRENT SAMPLE • $PICKER_SAMPLE_NAME" "MPC Browser current sample" 30; then
  echo "ERROR: picker-return sample name did not reach Browser's current Pad/Layer state"
  exit 1
fi
if ! wait_for_ui_text_pair "$PICKER_RETURN_XML" "$PICKER_RETURN_REMOTE" \
    "ASSIGNED • $PICKER_SAMPLE_NAME" "PAD 01 / LAYER 01" 30; then
  echo "ERROR: picker-return assignment status did not identify the selected WAV and preserved Pad 01 / Layer 01 target"
  exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > /tmp/mpc-groovebox-browser-picker-assignment.png; then
  echo "ERROR: could not capture post-picker assignment screen"
  exit 1
fi
if ! dump_device_ui "$PICKER_RETURN_REMOTE" "$PICKER_RETURN_XML"; then
  echo "ERROR: could not capture post-picker accessibility hierarchy"
  exit 1
fi
echo "Android Document Picker select → return → Pad/Layer assignment passed."

echo "Running Android user document-tree → Browser results → persisted grant restoration..."
if ! adb_bounded 10 shell mkdir -p "$TREE_FOLDER_DEVICE"; then
  echo "ERROR: could not create user-folder test fixture"; exit 1
fi
if ! adb_bounded 10 shell touch "$TREE_FOLDER_DEVICE/.nomedia"; then
  echo "ERROR: could not protect the tree fixture from MediaStore indexing"; exit 1
fi
if ! adb_bounded 30 push "$LOCAL_SAMPLE_HOST" "$TREE_SAMPLE_DEVICE"; then
  echo "ERROR: could not copy USER FOLDER-only WAV fixture"; exit 1
fi
if ! adb_bounded 10 shell mkdir -p "$FAVORITE_FOLDER_DEVICE"; then
  echo "ERROR: could not create separate FAVORITE 1 fixture folder"; exit 1
fi
if ! adb_bounded 10 shell touch "$FAVORITE_FOLDER_DEVICE/.nomedia"; then
  echo "ERROR: could not protect FAVORITE 1 fixture from MediaStore indexing"; exit 1
fi
if ! adb_bounded 30 push "$LOCAL_SAMPLE_HOST" "$FAVORITE_SAMPLE_DEVICE"; then
  echo "ERROR: could not copy FAVORITE 1-only WAV fixture"; exit 1
fi
if ! adb_bounded 10 shell content query \
    --uri content://media/external/audio/media \
    --projection _display_name \
    --where "_display_name='$TREE_SAMPLE_NAME'" > "$TREE_MEDIA_QUERY" 2>&1; then
  echo "ERROR: could not verify MediaStore exclusion for USER FOLDER fixture"
  cat "$TREE_MEDIA_QUERY" || true
  exit 1
fi
if grep -Fq -- "$TREE_SAMPLE_NAME" "$TREE_MEDIA_QUERY"; then
  echo "ERROR: USER FOLDER-only WAV appeared in MediaStore, so provider isolation cannot be proven"
  cat "$TREE_MEDIA_QUERY" || true
  exit 1
fi
if ! adb_bounded 10 shell content query \
    --uri content://media/external/audio/media \
    --projection _display_name \
    --where "_display_name='$FAVORITE_SAMPLE_NAME'" > "$FAVORITE_MEDIA_QUERY" 2>&1; then
  echo "ERROR: could not verify MediaStore exclusion for FAVORITE 1 fixture"
  cat "$FAVORITE_MEDIA_QUERY" || true
  exit 1
fi
if grep -Fq -- "$FAVORITE_SAMPLE_NAME" "$FAVORITE_MEDIA_QUERY"; then
  echo "ERROR: FAVORITE 1-only WAV appeared in MediaStore, so provider isolation cannot be proven"
  cat "$FAVORITE_MEDIA_QUERY" || true
  exit 1
fi

# The earlier UI audit deliberately exercised a non-matching Search query. Clear it
# again after returning from the real Android file picker, then inspect the actual
# EditText state before navigating to USER FOLDER. Picker return can recreate the
# Activity and restore an older navigation snapshot, so the earlier Browser reset
# is not sufficient proof that this later folder scan starts unfiltered.
if ! tap_ui_description "$PICKER_RETURN_XML" "MPC Browser clear search"; then
  echo "ERROR: could not clear Browser Search after Android Document Picker return"; exit 1
fi
if ! dump_device_ui "$PICKER_RETURN_REMOTE" "$PICKER_RETURN_XML"; then
  echo "ERROR: could not inspect Browser Search after picker return"; exit 1
fi
if ! python3 - "$PICKER_RETURN_XML" <<'PY'
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
fields = [
    node for node in root.iter("node")
    if node.attrib.get("content-desc", "") == "MPC Browser search files"
]
if len(fields) != 1:
    raise SystemExit(f"expected one Browser Search EditText, found {len(fields)}")
query = fields[0].attrib.get("text", "").strip()
# Android UIAutomator reports EditText's hint as its "text" attribute when
# the actual text is empty. MpcBrowserView explicitly uses "Search files" as
# that hint; accept it, but reject any other non-empty query.
if query not in ("", "Search files"):
    raise SystemExit(f"Browser Search retained stale query after CLEAR: {query!r}")
print("Browser Search reset verified after Android Document Picker return.")
PY
then
  echo "ERROR: Browser Search was not empty before USER FOLDER scan"; exit 1
fi

# Enter Places > User Folder. The folder action should only appear in this location.
if ! tap_ui_description "$PICKER_RETURN_XML" "Browser PLACES tab"; then
  echo "ERROR: Browser Places tab is not actionable"; exit 1
fi
if ! wait_for_ui_description "$TREE_BROWSER_XML" "$TREE_BROWSER_REMOTE" \
    "MPC Browser navigation PLACES USER FOLDER" 15; then
  echo "ERROR: Places did not expose the truthful USER FOLDER destination"; exit 1
fi
if ! tap_ui_description "$TREE_BROWSER_XML" "MPC Browser navigation PLACES USER FOLDER"; then
  echo "ERROR: USER FOLDER navigation row was not actionable"; exit 1
fi
if ! wait_for_ui_description "$TREE_BROWSER_XML" "$TREE_BROWSER_REMOTE" \
    "MPC Browser choose a local folder for USER FOLDER" 15; then
  echo "ERROR: USER FOLDER did not expose its Choose Folder action"; exit 1
fi
if ! tap_ui_description "$TREE_BROWSER_XML" "MPC Browser choose a local folder for USER FOLDER"; then
  echo "ERROR: Choose Folder action could not launch Android DocumentsUI"; exit 1
fi

tree_picker_activity=""
for tree_picker_attempt in {1..15}; do
  tree_picker_activity="$(adb_bounded 5 shell dumpsys activity activities 2>/dev/null \
    | grep -E 'mResumedActivity|topResumedActivity' | tail -n 1 || true)"
  if [[ -n "$tree_picker_activity" ]] && ! grep -Fq -- "$PACKAGE" <<<"$tree_picker_activity"; then break; fi
  sleep 1
done
if [[ -z "$tree_picker_activity" ]] || grep -Fq -- "$PACKAGE" <<<"$tree_picker_activity"; then
  echo "ERROR: ACTION_OPEN_DOCUMENT_TREE did not launch an external DocumentsUI activity"
  printf '%s\n' "$tree_picker_activity"
  exit 1
fi
if ! dump_device_ui "$TREE_PICKER_REMOTE" "$TREE_PICKER_XML"; then
  echo "ERROR: could not inspect the initial document-tree picker"; exit 1
fi
# Capture actual DocumentsUI evidence before interacting; this path is included
# in the always-uploaded evidence package, including when the smoke test fails.
if ! adb_bounded 10 exec-out screencap -p > /tmp/mpc-groovebox-browser-tree-picker.png; then
  echo "ERROR: could not capture initial document-tree picker screenshot"; exit 1
fi
# First accept the direct initial-document URI when this provider honors it.
# If DocumentsUI falls back to device root, navigate the real visible folder row.
if ! wait_for_ui_text "$TREE_PICKER_XML" "$TREE_PICKER_REMOTE" "$TREE_SAMPLE_NAME" 4; then
  if ! grep -Fq -- 'package="com.android.documentsui"' "$TREE_PICKER_XML"; then
    echo "ERROR: DocumentsUI closed before exposing the selected-folder child"; exit 1
  fi
  # Current grid/list choice is persisted by DocumentsUI. Normalize to list mode
  # so the whole row is a stable, non-clipped target; then inspect before tapping.
  if grep -Fq -- 'content-desc="List view"' "$TREE_PICKER_XML"; then
    if ! tap_ui_description "$TREE_PICKER_XML" "List view"; then
      echo "ERROR: could not switch DocumentsUI to list mode"; exit 1
    fi
    if ! dump_device_ui "$TREE_PICKER_REMOTE" "$TREE_PICKER_XML"; then
      echo "ERROR: could not inspect DocumentsUI list mode"; exit 1
    fi
  fi
  if ! picker_folder_row_fully_visible "$TREE_PICKER_XML" "$TREE_FOLDER_NAME"; then
    echo "ERROR: target test folder is not fully visible in DocumentsUI"; exit 1
  fi
  if ! tap_ui_picker_folder_until_child "$TREE_PICKER_XML" "$TREE_PICKER_REMOTE" "$TREE_FOLDER_NAME" "$TREE_SAMPLE_NAME" "tree"; then
    echo "ERROR: selected folder did not expose the actual tree-only WAV"; exit 1
  fi
fi
TREE_USE_FOLDER_LABEL="USE THIS FOLDER"
if ! wait_for_ui_text "$TREE_PICKER_XML" "$TREE_PICKER_REMOTE" "$TREE_USE_FOLDER_LABEL" 12; then
  TREE_USE_FOLDER_LABEL="Use this folder"
  if ! wait_for_ui_text "$TREE_PICKER_XML" "$TREE_PICKER_REMOTE" "$TREE_USE_FOLDER_LABEL" 3; then
    echo "ERROR: selected directory did not expose Use this folder"; exit 1
  fi
fi
if ! adb_bounded 10 exec-out screencap -p > /tmp/mpc-groovebox-browser-tree-picker.png; then
  echo "ERROR: could not capture document-tree picker screenshot"; exit 1
fi
if ! dump_device_ui "$TREE_PICKER_REMOTE" "$TREE_PICKER_XML"; then
  echo "ERROR: could not capture document-tree picker hierarchy"; exit 1
fi
if ! tap_ui_text "$TREE_PICKER_XML" "$TREE_USE_FOLDER_LABEL"; then
  echo "ERROR: could not confirm folder selection"; exit 1
fi

tree_app_resumed=0
for tree_return_attempt in {1..25}; do
  tree_return_activity="$(adb_bounded 5 shell dumpsys activity activities 2>/dev/null \
    | grep -E 'mResumedActivity|topResumedActivity' | tail -n 1 || true)"
  if grep -Fq -- "$PACKAGE" <<<"$tree_return_activity"; then tree_app_resumed=1; break; fi
  if dump_device_ui "$TREE_PICKER_REMOTE" "$TREE_PICKER_XML"; then
    tree_allow_label="$(python3 - "$TREE_PICKER_XML" <<'PY'
import sys
import xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
for node in root.iter("node"):
    label = node.attrib.get("text", "").strip()
    if label.casefold() == "allow":
        print(label)
        break
PY
)"
    if [[ -n "$tree_allow_label" ]]; then
      if ! tap_ui_text "$TREE_PICKER_XML" "$tree_allow_label"; then
        echo "ERROR: could not accept DocumentsUI storage permission confirmation"; exit 1
      fi
    fi
  fi
  sleep 1
done
if [[ "$tree_app_resumed" != "1" ]]; then
  echo "ERROR: document-tree selection did not return to the groovebox Activity"
  printf '%s\n' "$tree_return_activity"
  if dump_device_ui "$TREE_PICKER_REMOTE" "$TREE_PICKER_XML"; then
    python3 - "$TREE_PICKER_XML" <<'PY'
import sys
import xml.etree.ElementTree as ET
for node in ET.parse(sys.argv[1]).getroot().iter("node"):
    label = node.attrib.get("text", "").strip()
    desc = node.attrib.get("content-desc", "").strip()
    if label or desc:
        print(f"text={label!r} desc={desc!r}")
PY
  fi
  exit 1
fi
if ! wait_for_ui_text "$TREE_BROWSER_XML" "$TREE_BROWSER_REMOTE" "$TREE_SAMPLE_NAME" 30; then
  echo "ERROR: selected document tree did not return its real WAV file as a Browser row"; exit 1
fi
if ! wait_for_ui_node_text_desc "$TREE_BROWSER_XML" "$TREE_BROWSER_REMOTE" \
    "USER FOLDER • 1 WAV FILE INDEXED" "MPC Browser provider status" 20; then
  echo "ERROR: Browser did not report the actual tree-backed WAV count"; exit 1
fi

# The filters must operate on document-tree results rather than MediaStore's independent list.
if ! tap_ui_description "$TREE_BROWSER_XML" "MPC Browser filter PROJECTS"; then exit 1; fi
if ! dump_device_ui "$TREE_BROWSER_REMOTE" "$TREE_BROWSER_XML"; then exit 1; fi
if grep -Fq -- "$TREE_SAMPLE_NAME" "$TREE_BROWSER_XML"; then
  echo "ERROR: PROJECTS filter did not remove the WAV from the selected folder"; exit 1
fi
if ! tap_ui_description "$TREE_BROWSER_XML" "MPC Browser filter SAMPLES"; then exit 1; fi
if ! wait_for_ui_text "$TREE_BROWSER_XML" "$TREE_BROWSER_REMOTE" "$TREE_SAMPLE_NAME" 12; then
  echo "ERROR: SAMPLES filter did not restore the tree-backed WAV"; exit 1
fi
if ! tap_ui_description "$TREE_BROWSER_XML" "MPC Browser filter ALL"; then exit 1; fi
if ! wait_for_ui_text "$TREE_BROWSER_XML" "$TREE_BROWSER_REMOTE" "$TREE_SAMPLE_NAME" 12; then
  echo "ERROR: ALL filter did not restore the tree-backed WAV"; exit 1
fi

# Verify grant/root restoration after process death, not only after Activity return.
if ! adb_bounded 10 shell am force-stop "$PACKAGE"; then
  echo "ERROR: could not stop the app for tree permission restoration test"; exit 1
fi
# Start a fresh default-intent task, not the earlier ui-audit task used by the smoke harness.
# The saved SAF grant/URI live in app/system storage and survive clearing the Activity task.
if ! adb_bounded 30 shell am start -W --activity-clear-task -n "$ACTIVITY" \
    --es "$SMOKE_MODE_EXTRA" "ui-only"; then
  echo "ERROR: could not restart app for tree permission restoration test"; exit 1
fi
if ! wait_for_ui_description "$DUMP" "$DEVICE_DUMP" "MPC Project Browser" 40; then
  echo "ERROR: Main did not return after app process restart"; exit 1
fi
if ! dump_device_ui "$DEVICE_DUMP" "$DUMP"; then
  echo "ERROR: could not refresh Main hierarchy after process restart"; exit 1
fi
if ! tap_ui_description "$DUMP" "MPC Project Browser"; then
  echo "ERROR: could not reopen Browser after process restart"; exit 1
fi
if ! wait_for_ui_description "$TREE_RESTORED_XML" "$TREE_RESTORED_REMOTE" \
    "MPC Browser navigation PLACES USER FOLDER" 30; then
  echo "ERROR: saved USER FOLDER destination did not survive process restart"; exit 1
fi
if ! tap_ui_description "$TREE_RESTORED_XML" "MPC Browser navigation PLACES USER FOLDER"; then
  echo "ERROR: restored USER FOLDER destination was not actionable"; exit 1
fi
if ! wait_for_ui_text "$TREE_RESTORED_XML" "$TREE_RESTORED_REMOTE" "$TREE_SAMPLE_NAME" 30; then
  echo "ERROR: persisted document-tree grant did not restore the actual WAV result"; exit 1
fi
if ! wait_for_ui_node_text_desc "$TREE_RESTORED_XML" "$TREE_RESTORED_REMOTE" \
    "USER FOLDER • 1 WAV FILE INDEXED" "MPC Browser provider status" 20; then
  echo "ERROR: restored folder provider status did not match the real scan result"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > /tmp/mpc-groovebox-browser-tree-restored.png; then
  echo "ERROR: could not capture restored document-tree Browser screenshot"; exit 1
fi
if ! dump_device_ui "$TREE_RESTORED_REMOTE" "$TREE_RESTORED_XML"; then
  echo "ERROR: could not capture restored document-tree Browser hierarchy"; exit 1
fi
echo "Android document-tree selection, real WAV results, filters and USER FOLDER restoration passed."

# Assign FAVORITE 1 through the real Browser action and Android DocumentsUI tree picker.
# Use a separate Favorite-only WAV so isolation from USER FOLDER and MediaStore is proven.
if ! tap_ui_description "$TREE_RESTORED_XML" "MPC Browser navigation PLACES FAVORITE 1"; then
  echo "ERROR: FAVORITE 1 navigation entry was not actionable"; exit 1
fi
if ! wait_for_ui_description "$FAVORITE_TREE_RESULTS_XML" "$FAVORITE_TREE_RESULTS_REMOTE" \
    "MPC Browser choose a local folder for FAVORITE 1" 15; then
  echo "ERROR: unconfigured FAVORITE 1 did not expose its Choose Folder action"; exit 1
fi
if ! tap_ui_description "$FAVORITE_TREE_RESULTS_XML" "MPC Browser choose a local folder for FAVORITE 1"; then
  echo "ERROR: FAVORITE 1 Choose Folder did not launch Android DocumentsUI"; exit 1
fi

favorite_picker_activity=""
for favorite_picker_attempt in {1..15}; do
  favorite_picker_activity="$(adb_bounded 5 shell dumpsys activity activities 2>/dev/null \
    | grep -E 'mResumedActivity|topResumedActivity' | tail -n 1 || true)"
  if [[ -n "$favorite_picker_activity" ]] && ! grep -Fq -- "$PACKAGE" <<<"$favorite_picker_activity"; then break; fi
  sleep 1
done
if [[ -z "$favorite_picker_activity" ]] || grep -Fq -- "$PACKAGE" <<<"$favorite_picker_activity"; then
  echo "ERROR: FAVORITE 1 did not launch an external document-tree picker"
  printf '%s\n' "$favorite_picker_activity"
  exit 1
fi
if ! dump_device_ui "$FAVORITE_TREE_PICKER_REMOTE" "$FAVORITE_TREE_PICKER_XML"; then
  echo "ERROR: could not inspect the FAVORITE 1 document-tree picker"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > /tmp/mpc-groovebox-browser-favorite-tree-picker.png; then
  echo "ERROR: could not capture FAVORITE 1 picker screenshot"; exit 1
fi
if ! wait_for_ui_text "$FAVORITE_TREE_PICKER_XML" "$FAVORITE_TREE_PICKER_REMOTE" "$FAVORITE_SAMPLE_NAME" 4; then
  if ! grep -Fq -- 'package="com.android.documentsui"' "$FAVORITE_TREE_PICKER_XML"; then
    echo "ERROR: DocumentsUI closed before FAVORITE 1 could select a folder"; exit 1
  fi

  # DocumentsUI remembers the last browsed directory across picker launches.
  # In this flow that is USER FOLDER (AAA...), so explicitly navigate to the
  # storage-provider root before looking for the distinct FAVORITE folder.
  favorite_breadcrumbs="$(python3 - "$FAVORITE_TREE_PICKER_XML" <<'PY'
import sys
import xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
breadcrumb = next((n for n in root.iter("node")
                   if n.attrib.get("resource-id") == "com.android.documentsui:id/horizontal_breadcrumb"), None)
if breadcrumb is None:
    raise SystemExit("DocumentsUI hierarchy has no directory breadcrumb")
labels = []
for item in list(breadcrumb):
    for node in item.iter("node"):
        if node.attrib.get("resource-id") == "com.android.documentsui:id/breadcrumb_text":
            label = node.attrib.get("text", "").strip()
            if label:
                labels.append(label)
                break
if not labels:
    raise SystemExit("DocumentsUI breadcrumb has no usable root label")
print("\n".join(labels))
PY
)"
  favorite_root_label="$(head -n 1 <<< "$favorite_breadcrumbs")"
  favorite_breadcrumb_count="$(printf '%s\n' "$favorite_breadcrumbs" | wc -l | tr -d '[:space:]')"
  if [[ -z "$favorite_root_label" || -z "$favorite_breadcrumb_count" ]]; then
    echo "ERROR: could not identify the DocumentsUI storage-provider root for FAVORITE 1"; exit 1
  fi
  if (( favorite_breadcrumb_count > 1 )); then
    echo "DocumentsUI reopened at a previous folder; returning to provider root '$favorite_root_label'..."
    if ! tap_ui_text "$FAVORITE_TREE_PICKER_XML" "$favorite_root_label"; then
      echo "ERROR: could not navigate DocumentsUI from USER FOLDER back to its provider root"; exit 1
    fi
    if ! dump_device_ui "$FAVORITE_TREE_PICKER_REMOTE" "$FAVORITE_TREE_PICKER_XML"; then
      echo "ERROR: could not inspect DocumentsUI at the FAVORITE 1 provider root"; exit 1
    fi
  fi

  # This must be the folder-grant picker, not the direct WAV-open picker.
  if ! wait_for_ui_text "$FAVORITE_TREE_PICKER_XML" "$FAVORITE_TREE_PICKER_REMOTE" "USE THIS FOLDER" 6; then
    if ! wait_for_ui_text "$FAVORITE_TREE_PICKER_XML" "$FAVORITE_TREE_PICKER_REMOTE" "Use this folder" 3; then
      echo "ERROR: FAVORITE 1 picker never exposed the Android Use This Folder tree-grant action"; exit 1
    fi
  fi

  # Prefer list mode when DocumentsUI offers it; allow the view transition to settle.
  if grep -Fq -- 'content-desc="List view"' "$FAVORITE_TREE_PICKER_XML"; then
    if ! tap_ui_description "$FAVORITE_TREE_PICKER_XML" "List view"; then
      echo "ERROR: could not switch FAVORITE 1 DocumentsUI to list mode"; exit 1
    fi
    for favorite_view_attempt in {1..5}; do
      if ! dump_device_ui "$FAVORITE_TREE_PICKER_REMOTE" "$FAVORITE_TREE_PICKER_XML"; then
        echo "ERROR: could not inspect FAVORITE 1 DocumentsUI after view switch"; exit 1
      fi
      if grep -Fq -- 'content-desc="Grid view"' "$FAVORITE_TREE_PICKER_XML"; then break; fi
      sleep 1
    done
  fi

  # The fixture may not be in the initial viewport; use bounded real swipes.
  favorite_folder_visible=0
  for favorite_folder_scroll in {1..10}; do
    if picker_folder_row_fully_visible "$FAVORITE_TREE_PICKER_XML" "$FAVORITE_FOLDER_NAME"; then
      favorite_folder_visible=1
      break
    fi
    if (( favorite_folder_scroll == 10 )); then break; fi
    if ! adb_bounded 10 shell input swipe 540 1450 540 650 350; then
      echo "ERROR: could not scroll DocumentsUI to the FAVORITE 1 folder fixture"; exit 1
    fi
    sleep 1
    if ! dump_device_ui "$FAVORITE_TREE_PICKER_REMOTE" "$FAVORITE_TREE_PICKER_XML"; then
      echo "ERROR: could not inspect DocumentsUI while locating FAVORITE 1 folder"; exit 1
    fi
  done
  if [[ "$favorite_folder_visible" != "1" ]]; then
    echo "ERROR: Favorite-only fixture folder did not become fully visible at the DocumentsUI provider root"
    cat "$FAVORITE_TREE_PICKER_XML"
    exit 1
  fi
  if ! tap_ui_picker_folder_until_child "$FAVORITE_TREE_PICKER_XML" "$FAVORITE_TREE_PICKER_REMOTE" "$FAVORITE_FOLDER_NAME" "$FAVORITE_SAMPLE_NAME" "favorite-tree"; then
    echo "ERROR: FAVORITE 1 picker did not navigate into the actual Favorite-only WAV folder"; exit 1
  fi
fi

FAVORITE_USE_FOLDER_LABEL="USE THIS FOLDER"
if ! wait_for_ui_text "$FAVORITE_TREE_PICKER_XML" "$FAVORITE_TREE_PICKER_REMOTE" "$FAVORITE_USE_FOLDER_LABEL" 12; then
  FAVORITE_USE_FOLDER_LABEL="Use this folder"
  if ! wait_for_ui_text "$FAVORITE_TREE_PICKER_XML" "$FAVORITE_TREE_PICKER_REMOTE" "$FAVORITE_USE_FOLDER_LABEL" 3; then
    echo "ERROR: FAVORITE 1 selected directory did not expose Use this folder"; exit 1
  fi
fi
if ! adb_bounded 10 exec-out screencap -p > /tmp/mpc-groovebox-browser-favorite-tree-picker.png; then
  echo "ERROR: could not capture selected FAVORITE 1 folder"; exit 1
fi
if ! dump_device_ui "$FAVORITE_TREE_PICKER_REMOTE" "$FAVORITE_TREE_PICKER_XML"; then
  echo "ERROR: could not capture selected FAVORITE 1 folder hierarchy"; exit 1
fi
if ! tap_ui_text "$FAVORITE_TREE_PICKER_XML" "$FAVORITE_USE_FOLDER_LABEL"; then
  echo "ERROR: could not confirm the FAVORITE 1 folder selection"; exit 1
fi

favorite_app_resumed=0
for favorite_return_attempt in {1..25}; do
  favorite_return_activity="$(adb_bounded 5 shell dumpsys activity activities 2>/dev/null \
    | grep -E 'mResumedActivity|topResumedActivity' | tail -n 1 || true)"
  if grep -Fq -- "$PACKAGE" <<<"$favorite_return_activity"; then favorite_app_resumed=1; break; fi
  if dump_device_ui "$FAVORITE_TREE_PICKER_REMOTE" "$FAVORITE_TREE_PICKER_XML"; then
    favorite_allow_label="$(python3 - "$FAVORITE_TREE_PICKER_XML" <<'PY'
import sys
import xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
for node in root.iter("node"):
    label = node.attrib.get("text", "").strip()
    if label.casefold() == "allow":
        print(label)
        break
PY
)"
    if [[ -n "$favorite_allow_label" ]]; then
      if ! tap_ui_text "$FAVORITE_TREE_PICKER_XML" "$favorite_allow_label"; then
        echo "ERROR: could not accept DocumentsUI permission confirmation for FAVORITE 1"; exit 1
      fi
    fi
  fi
  sleep 1
done
if [[ "$favorite_app_resumed" != "1" ]]; then
  echo "ERROR: FAVORITE 1 folder selection did not return to the groovebox Activity"
  printf '%s\n' "$favorite_return_activity"
  exit 1
fi
if ! wait_for_ui_text "$FAVORITE_TREE_RESULTS_XML" "$FAVORITE_TREE_RESULTS_REMOTE" "$FAVORITE_SAMPLE_NAME" 30; then
  echo "ERROR: FAVORITE 1 did not return its real WAV as a Browser row"; exit 1
fi
if ! wait_for_ui_node_text_desc "$FAVORITE_TREE_RESULTS_XML" "$FAVORITE_TREE_RESULTS_REMOTE" \
    "FAVORITE 1 • 1 WAV FILE INDEXED" "MPC Browser provider status" 20; then
  echo "ERROR: FAVORITE 1 provider status did not match its real tree scan"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > /tmp/mpc-groovebox-browser-favorite-tree-results.png; then
  echo "ERROR: could not capture FAVORITE 1 real WAV results"; exit 1
fi
if ! dump_device_ui "$FAVORITE_TREE_RESULTS_REMOTE" "$FAVORITE_TREE_RESULTS_XML"; then
  echo "ERROR: could not capture FAVORITE 1 real WAV hierarchy"; exit 1
fi

# Prove this slot writes its own keys and does not replace USER FOLDER's saved target.
if ! adb_bounded 10 shell "run-as $PACKAGE cat shared_prefs/mpc_browser_storage.xml" > "$FAVORITE_PREFS_HOST"; then
  echo "ERROR: could not inspect the saved Browser favorite preferences"; exit 1
fi
for required_key in 'name="user_tree_uri"' 'name="favorite_tree_uri_1"' 'name="favorite_tree_name_1"'; do
  if ! grep -Fq -- "$required_key" "$FAVORITE_PREFS_HOST"; then
    echo "ERROR: Browser storage preferences lack independent key $required_key"; exit 1
  fi
done
if ! python3 - "$FAVORITE_PREFS_HOST" "$TREE_FOLDER_NAME" "$FAVORITE_FOLDER_NAME" <<'PY'
import sys
import xml.etree.ElementTree as ET
path, user_folder, favorite_folder = sys.argv[1:]
root = ET.parse(path).getroot()
values = {node.attrib.get("name", ""): node.text or ""
          for node in root.findall("string")}
user_uri = values.get("user_tree_uri", "")
favorite_uri = values.get("favorite_tree_uri_1", "")
if not user_uri or not favorite_uri or user_uri == favorite_uri:
    raise SystemExit("USER FOLDER and FAVORITE 1 must persist different tree URIs")
if user_folder not in user_uri or favorite_folder not in favorite_uri:
    raise SystemExit("Saved tree URIs do not point to their respective folder fixtures")
print("Independent USER FOLDER / FAVORITE 1 SAF URI preferences verified.")
PY
then
  echo "ERROR: USER FOLDER and FAVORITE 1 do not retain different folder assignments"; exit 1
fi

# Switch away and back before process death, proving each slot reads its own WAV list.
if ! tap_ui_description "$FAVORITE_TREE_RESULTS_XML" "MPC Browser navigation PLACES USER FOLDER"; then
  echo "ERROR: USER FOLDER navigation was not available after Favorite assignment"; exit 1
fi
if ! wait_for_ui_text "$FAVORITE_TREE_RESULTS_XML" "$FAVORITE_TREE_RESULTS_REMOTE" "$TREE_SAMPLE_NAME" 20; then
  echo "ERROR: assigning FAVORITE 1 replaced the USER FOLDER results"; exit 1
fi
if ! wait_for_ui_node_text_desc "$FAVORITE_TREE_RESULTS_XML" "$FAVORITE_TREE_RESULTS_REMOTE" \
    "USER FOLDER • 1 WAV FILE INDEXED" "MPC Browser provider status" 15; then
  echo "ERROR: USER FOLDER was not independently restored after switching from Favorite"; exit 1
fi
if ! tap_ui_description "$FAVORITE_TREE_RESULTS_XML" "MPC Browser navigation PLACES FAVORITE 1"; then
  echo "ERROR: FAVORITE 1 navigation was not available after returning to USER FOLDER"; exit 1
fi
if ! wait_for_ui_text "$FAVORITE_TREE_RESULTS_XML" "$FAVORITE_TREE_RESULTS_REMOTE" "$FAVORITE_SAMPLE_NAME" 20; then
  echo "ERROR: returning to FAVORITE 1 did not restore its distinct WAV result"; exit 1
fi
if ! wait_for_ui_node_text_desc "$FAVORITE_TREE_RESULTS_XML" "$FAVORITE_TREE_RESULTS_REMOTE" \
    "FAVORITE 1 • 1 WAV FILE INDEXED" "MPC Browser provider status" 15; then
  echo "ERROR: Favorite result/status was not restored after switching back"; exit 1
fi

# Force-stop and recreate the process again; Favorite 1 must restore its own folder grant.
if ! adb_bounded 10 shell am force-stop "$PACKAGE"; then
  echo "ERROR: could not stop the app for FAVORITE 1 persistence verification"; exit 1
fi
if ! adb_bounded 30 shell am start -W --activity-clear-task -n "$ACTIVITY" \
    --es "$SMOKE_MODE_EXTRA" "ui-only"; then
  echo "ERROR: could not restart app for FAVORITE 1 persistence verification"; exit 1
fi
if ! wait_for_ui_description "$DUMP" "$DEVICE_DUMP" "MPC Project Browser" 40; then
  echo "ERROR: Main did not return before Favorite 1 restoration verification"; exit 1
fi
if ! dump_device_ui "$DEVICE_DUMP" "$DUMP"; then
  echo "ERROR: could not inspect Main before Favorite 1 restoration"; exit 1
fi
if ! tap_ui_description "$DUMP" "MPC Project Browser"; then
  echo "ERROR: could not reopen Browser for Favorite 1 restoration"; exit 1
fi
if ! wait_for_ui_description "$FAVORITE_TREE_RESTORED_XML" "$FAVORITE_TREE_RESTORED_REMOTE" \
    "MPC Browser navigation PLACES FAVORITE 1" 30; then
  echo "ERROR: FAVORITE 1 destination was not restored after process restart"; exit 1
fi
if ! tap_ui_description "$FAVORITE_TREE_RESTORED_XML" "MPC Browser navigation PLACES FAVORITE 1"; then
  echo "ERROR: restored FAVORITE 1 destination was not actionable"; exit 1
fi
if ! wait_for_ui_text "$FAVORITE_TREE_RESTORED_XML" "$FAVORITE_TREE_RESTORED_REMOTE" "$FAVORITE_SAMPLE_NAME" 30; then
  echo "ERROR: Favorite 1's saved SAF grant did not restore its actual WAV row"; exit 1
fi
if ! wait_for_ui_node_text_desc "$FAVORITE_TREE_RESTORED_XML" "$FAVORITE_TREE_RESTORED_REMOTE" \
    "FAVORITE 1 • 1 WAV FILE INDEXED" "MPC Browser provider status" 20; then
  echo "ERROR: restored FAVORITE 1 provider status did not match the real scan"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > /tmp/mpc-groovebox-browser-favorite-tree-restored.png; then
  echo "ERROR: could not capture restored FAVORITE 1 Browser screenshot"; exit 1
fi
if ! dump_device_ui "$FAVORITE_TREE_RESTORED_REMOTE" "$FAVORITE_TREE_RESTORED_XML"; then
  echo "ERROR: could not capture restored FAVORITE 1 Browser hierarchy"; exit 1
fi
echo "Browser Favorite 1 assignment, isolated preferences and process-restart restoration passed."


echo "Running Project Save/Open acceptance with measured touches and native-state assertions..."
# Project route touch helpers. Android UIAutomator occasionally returns a cached
# previous-workspace tree after the persistent MPC shell swaps children. Keep the
# screenshots/hierarchies as evidence, use measured shell bounds for real taps,
# and assert the newly rendered page with Activity/domain logs.
PROJECT_MAIN_READY_XML="/tmp/mpc-groovebox-main-ready-uiautomator.xml"

project_marker_count() {
  local marker="$1" logs
  logs="$(adb_bounded 5 shell logcat -d -v brief -s MpcGroovebox:I 2>/dev/null || true)"
  grep -F -c -- "$marker" <<<"$logs" || true
}

wait_for_new_project_marker() {
  local marker="$1" before="$2" max_attempts="${3:-45}" attempt count
  for ((attempt=1; attempt<=max_attempts; attempt++)); do
    count="$(project_marker_count "$marker")"
    if [[ "$count" =~ ^[0-9]+$ ]] && (( count > before )); then return 0; fi
    sleep 1
  done
  echo "ERROR: timed out waiting for new native UI marker: $marker"
  adb_bounded 10 shell logcat -d -v brief -s MpcGroovebox:I 2>/dev/null \
    | grep -E 'MPC_MENU_|MPC_PROJECT_|MPC_SHELL_STATE_|PROJECT_(SAVE|OPEN|TEST_)|STARTUP_COMPLETE' \
    | tail -n 100 || true
  return 1
}

project_shell_geometry() {
  # Derive touch geometry from actual shell nodes in the previously captured
  # Main hierarchy. "MPC Browser" is a Toolbar button, not the workspace; using
  # its bounds made the previous grid calculation produce negative touch points.
  python3 - "$PROJECT_MAIN_READY_XML" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()

def rect(node):
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",
                         node.attrib.get("bounds", ""))
    return tuple(map(int, match.groups())) if match else None

nodes = list(root.iter("node"))
root_rect = rect(nodes[0]) if nodes else None
strips = next((n for n in nodes
               if n.attrib.get("content-desc") == "MPC shell mixer strips"), None)
playhead = next((n for n in nodes
                 if n.attrib.get("content-desc") == "MPC global sequence playhead"), None)
strips_rect = rect(strips) if strips is not None else None
playhead_rect = rect(playhead) if playhead is not None else None
if not root_rect or not strips_rect or not playhead_rect:
    raise SystemExit("Main-ready hierarchy lacks screen, XL-strip or playhead bounds")

_, _, root_right, root_bottom = root_rect
_, _, strips_right, _ = strips_rect
_, _, _, playhead_bottom = playhead_rect

# Map the shared MPC shell regions, not a workspace-child accessibility node:
# Menu tiles occupy the right-hand 4x4 region; the function bar spans that same
# right-hand column. Insets are relative to the measured shell boundaries.
workspace_left = strips_right + 8
workspace_right = root_right - 18
workspace_top = playhead_bottom + 12
workspace_bottom = root_bottom - 108
if (workspace_left >= workspace_right or workspace_top >= workspace_bottom
        or workspace_right > root_right or workspace_bottom > root_bottom):
    raise SystemExit("Calculated MPC shell touch bounds are invalid")
print(root_right, root_bottom, workspace_left, workspace_top,
      workspace_right, workspace_bottom)
PY
}

tap_mpc_menu_tile() {
  local row="$1" col="$2" root_right root_bottom workspace_left workspace_top workspace_right workspace_bottom
  local grid_left grid_right grid_top grid_bottom tap_x tap_y before_project before_main
  read -r root_right root_bottom workspace_left workspace_top workspace_right workspace_bottom <<< "$(project_shell_geometry)"
  grid_left=$((workspace_left + 22))
  grid_right=$((workspace_right - 22))
  grid_top=$((workspace_top + 80))
  grid_bottom=$((workspace_bottom - 10))
  tap_x=$((grid_left + (2 * col + 1) * (grid_right - grid_left) / 8))
  tap_y=$((grid_top + (2 * row + 1) * (grid_bottom - grid_top) / 8))
  if (( row == 3 && col == 3 )); then
    before_project="$(project_marker_count "MPC_PROJECT_RENDER_COMPLETE")"
  elif (( row == 0 && col == 0 )); then
    before_main="$(project_marker_count "MPC_SHELL_STATE_END mode=MAIN")"
  else
    echo "ERROR: unsupported target in Project acceptance Menu helper"; return 1
  fi
  echo "Real Menu tile tap row=$row col=$col at $tap_x,$tap_y"
  if ! adb_bounded 10 shell input tap "$tap_x" "$tap_y"; then return 1; fi
  if (( row == 3 && col == 3 )); then
    wait_for_new_project_marker "MPC_PROJECT_RENDER_COMPLETE" "$before_project" 45
  else
    wait_for_new_project_marker "MPC_SHELL_STATE_END mode=MAIN" "$before_main" 45
  fi
}

tap_project_toolbar_menu() {
  local before
  before="$(project_marker_count "MPC_MENU_RENDER_COMPLETE")"
  if ! tap_visible_description_early "$PROJECT_MAIN_READY_XML" "MPC Toolbar Menu"; then
    echo "ERROR: could not tap the visible MPC Toolbar Menu"; return 1
  fi
  wait_for_new_project_marker "MPC_MENU_RENDER_COMPLETE" "$before" 45
}

tap_project_function_bar() {
  local index="$1" root_right root_bottom workspace_left workspace_top workspace_right workspace_bottom left tap_x tap_y
  read -r root_right root_bottom workspace_left workspace_top workspace_right workspace_bottom <<< "$(project_shell_geometry)"
  left=$((workspace_left - 13))
  tap_x=$((left + (2 * index + 1) * (root_right - left) / 8))
  tap_y=$((root_bottom - 45))
  echo "Real Project Function Bar tap index=$index at $tap_x,$tap_y"
  adb_bounded 10 shell input tap "$tap_x" "$tap_y"
}

tap_main_new_track() {
  local root_right root_bottom workspace_left workspace_top workspace_right workspace_bottom left tap_x tap_y
  read -r root_right root_bottom workspace_left workspace_top workspace_right workspace_bottom <<< "$(project_shell_geometry)"
  left=$((workspace_left - 13))
  tap_x=$((left + (root_right - left) / 12))
  tap_y=$((root_bottom - 45))
  echo "Real Main NEW TRACK Function Bar tap at $tap_x,$tap_y"
  adb_bounded 10 shell input tap "$tap_x" "$tap_y"
}

assert_latest_project_summary() {
  local expected_tracks="$1" expected_placements="$2" logs line
  logs="$(adb_bounded 5 shell logcat -d -v brief -s MpcGroovebox:I 2>/dev/null || true)"
  line="$(grep -F -- "MPC_PROJECT_RENDER_COMPLETE summary=" <<<"$logs" | tail -n 1 || true)"
  if [[ -z "$line" ]] || ! grep -Fq -- "$expected_tracks" <<<"$line" || ! grep -Fq -- "$expected_placements" <<<"$line"; then
    echo "ERROR: rendered Project summary differs from expected Tracks/placements"
    echo "Expected: $expected_tracks ; $expected_placements"; echo "$line"; return 1
  fi
  echo "Verified rendered Project summary: $line"
}

show_project_route_extended() {
  if ! tap_project_toolbar_menu; then exit 1; fi
  if ! tap_mpc_menu_tile 3 3; then exit 1; fi
  if ! assert_latest_project_summary "TRACKS=1" "PLACEMENTS=1"; then exit 1; fi
}


PROJECT_SAVE_XML="/tmp/mpc-groovebox-project-save-picker-uiautomator.xml"
PROJECT_SAVE_REMOTE="/sdcard/mpc-groovebox-project-save-picker-uiautomator.xml"
PROJECT_SAVE_PNG="/tmp/mpc-groovebox-project-save-picker.png"
PROJECT_SAVED_XML="/tmp/mpc-groovebox-project-saved-uiautomator.xml"
PROJECT_SAVED_PNG="/tmp/mpc-groovebox-project-saved.png"
PROJECT_MUTATED_XML="/tmp/mpc-groovebox-project-mutated-uiautomator.xml"
PROJECT_MUTATED_PNG="/tmp/mpc-groovebox-project-mutated.png"
PROJECT_OPEN_XML="/tmp/mpc-groovebox-project-open-picker-uiautomator.xml"
PROJECT_OPEN_REMOTE="/sdcard/mpc-groovebox-project-open-picker-uiautomator.xml"
PROJECT_OPEN_PNG="/tmp/mpc-groovebox-project-open-picker.png"
PROJECT_REOPENED_XML="/tmp/mpc-groovebox-project-reopened-uiautomator.xml"
PROJECT_REOPENED_PNG="/tmp/mpc-groovebox-project-reopened.png"

# Previous Browser tests deliberately use ui-only startup for permission
# restoration. Start a real native session before testing Project persistence.
if ! adb_bounded 10 shell am force-stop "$PACKAGE"; then
  echo "ERROR: could not stop UI-only session before Project persistence test"; exit 1
fi
if ! adb_bounded 10 shell logcat -c; then
  echo "ERROR: could not clear logcat before Project persistence test"; exit 1
fi
if ! adb_bounded 30 shell am start -W --activity-clear-task -n "$ACTIVITY" \
    --es "$SMOKE_MODE_EXTRA" "project-persistence"; then
  echo "ERROR: could not launch native Project persistence acceptance session"; exit 1
fi
# Do not interrogate the UI hierarchy until native startup has completed.
# uiautomator dump can occupy the main thread during cold startup; waiting for
# its first visible toolbar label before the native fixture ran caused the old
# smoke to stall and only dispatch the queued Menu click at timeout.
project_engine_ready=0
for ((attempt=1; attempt<=120; attempt++)); do
  project_logs="$(adb_bounded 5 shell logcat -d -v brief -s MpcGroovebox:I 2>/dev/null || true)"
  if grep -Fq -- "AUDIO_START_RESULT=" <<<"$project_logs" \
      && grep -Fq -- "PROJECT_TEST_FIXTURE_READY" <<<"$project_logs" \
      && grep -Fq -- "STARTUP_COMPLETE" <<<"$project_logs"; then
    project_engine_ready=1
    break
  fi
  sleep 1
done
if [ "$project_engine_ready" -ne 1 ]; then
  echo "ERROR: native Project/Audio startup or acceptance fixture did not finish"
  adb_bounded 10 shell logcat -d -v brief -s MpcGroovebox:I 2>/dev/null \
    | grep -E 'PROJECT_TEST_FIXTURE|AUDIO_START_RESULT=|STARTUP_COMPLETE|EARLY_STARTUP|FAILED' \
    | tail -n 100 || true
  exit 1
fi
project_fixture_line="$(grep -F -- "PROJECT_TEST_FIXTURE_READY" <<<"$project_logs" | tail -n 1)"
for expected_fixture_field in \
    "notes=2" \
    "note1=120/36/180/97/88/3/-11" \
    "note2=600/42/90/111/123/2/7" \
    "placement=project-persistence-clip-01/sequence-1-track-1-pattern-1/480/720/120"; do
  if ! grep -Fq -- "$expected_fixture_field" <<<"$project_fixture_line"; then
    echo "ERROR: native persistence fixture omitted $expected_fixture_field"
    echo "$project_fixture_line"
    exit 1
  fi
done
echo "$project_fixture_line"
echo "PROJECT_NATIVE_STARTUP_READY • awaiting actual Main shell interaction"

# The earlier Main hierarchy is from this same emulator run and uses the same
# canonical persistent Toolbar geometry. Re-use that captured enabled target
# instead of starting another hierarchy dump while cold native startup settles.
if ! tap_visible_description_early \
    "/tmp/mpc-groovebox-main-ready-uiautomator.xml" "MPC Toolbar Menu"; then
  echo "ERROR: could not activate the real MPC Toolbar Menu from captured Main evidence"; exit 1
fi
project_menu_rendered=0
for ((attempt=1; attempt<=60; attempt++)); do
  project_logs="$(adb_bounded 5 shell logcat -d -v brief -s MpcGroovebox:I 2>/dev/null || true)"
  if grep -Fq -- "MPC_MENU_RENDER_COMPLETE" <<<"$project_logs" \
      && grep -Fq -- "MPC_MENU_CLICK_END currentPage=MENU" <<<"$project_logs"; then
    project_menu_rendered=1
    break
  fi
  sleep 1
done
if [ "$project_menu_rendered" -ne 1 ]; then
  echo "ERROR: real Toolbar Menu tap did not complete its Activity render"
  adb_bounded 10 shell logcat -d -v brief -s MpcGroovebox:I 2>/dev/null \
    | grep -E 'MPC_MENU_|MPC_SHELL_STATE_|STARTUP_COMPLETE|AUDIO_START_RESULT=' \
    | tail -n 100 || true
  exit 1
fi
sleep 1
PROJECT_MENU_XML="/tmp/mpc-groovebox-project-menu-uiautomator.xml"
PROJECT_MENU_PNG="/tmp/mpc-groovebox-project-menu.png"
if ! adb_bounded 10 exec-out screencap -p > "$PROJECT_MENU_PNG"; then
  echo "ERROR: could not capture rendered MPC Menu screenshot"; exit 1
fi
if ! dump_device_ui "$DEVICE_DUMP" "$PROJECT_MENU_XML"; then
  echo "ERROR: could not capture rendered MPC Menu hierarchy"; exit 1
fi
# The captured screenshot proves Menu is visible. The UIAutomator snapshot can
# still expose the previous Main subtree, so use the real 4×4 Menu tile geometry
# and then require a fresh Project render containing the authoritative domain summary.
if ! tap_mpc_menu_tile 3 3; then exit 1; fi
if ! assert_latest_project_summary "TRACKS=1" "PLACEMENTS=1"; then exit 1; fi
PROJECT_ROUTE_XML="/tmp/mpc-groovebox-project-route-uiautomator.xml"
PROJECT_ROUTE_PNG="/tmp/mpc-groovebox-project-route.png"
if ! adb_bounded 10 exec-out screencap -p > "$PROJECT_ROUTE_PNG"; then
  echo "ERROR: could not capture Project route screenshot"; exit 1
fi
if ! dump_device_ui "$DEVICE_DUMP" "$PROJECT_ROUTE_XML"; then
  echo "ERROR: could not capture Project route hierarchy"; exit 1
fi
echo "Real Main → Menu → Project touch path and native summary verified; screenshots/hierarchies captured."

# Save the real native Project model through ACTION_CREATE_DOCUMENT.
project_save_marker_before="$(project_marker_count "PROJECT_SAVE_COMPLETE")"
if ! tap_project_function_bar 1; then exit 1; fi
project_save_picker_ready=0
for ((attempt=1; attempt<=25; attempt++)); do
  if dump_device_ui "$PROJECT_SAVE_REMOTE" "$PROJECT_SAVE_XML" \
      && grep -Fq -- 'package="com.android.documentsui"' "$PROJECT_SAVE_XML"; then
    project_save_picker_ready=1
    break
  fi
  sleep 1
done
if [ "$project_save_picker_ready" -ne 1 ]; then
  echo "ERROR: Android DocumentsUI did not appear for Project Save As"; exit 1
fi
if ! capture_stable_picker_state "$PROJECT_SAVE_REMOTE" "$PROJECT_SAVE_XML" "$PROJECT_SAVE_PNG" 8; then
  echo "ERROR: could not capture stable DocumentsUI state for Project Save As"; exit 1
fi
if ! wait_for_ui_text "$PROJECT_SAVE_XML" "$PROJECT_SAVE_REMOTE" "Untitled.mpcproj" 15; then
  echo "ERROR: Save As did not prefill the project filename"; exit 1
fi
if ! tap_ui_text "$PROJECT_SAVE_XML" "SAVE"; then
  echo "ERROR: DocumentsUI Save action was not available for Project Save As"; exit 1
fi
if ! wait_for_new_project_marker "PROJECT_SAVE_COMPLETE" "$project_save_marker_before" 45; then
  echo "ERROR: Project Save As did not complete through the real document provider"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$PROJECT_SAVED_PNG"; then
  echo "ERROR: could not capture saved Project screen"; exit 1
fi
if ! dump_device_ui "$DEVICE_DUMP" "$PROJECT_SAVED_XML"; then
  echo "ERROR: could not capture saved Project hierarchy"; exit 1
fi
if ! grep -Fq -- "PROJECT_SAVE_COMPLETE" <<<"$(adb_bounded 5 shell logcat -d -v brief -s MpcGroovebox:I 2>/dev/null || true)"; then
  echo "ERROR: Project Save As screen has no native-snapshot completion log"; exit 1
fi

# Change the in-memory model after saving; the disk copy must still load the
# earlier single-Track state rather than the currently edited model.
if ! tap_project_toolbar_menu; then exit 1; fi
if ! tap_mpc_menu_tile 0 0; then exit 1; fi
if ! adb_bounded 10 exec-out screencap -p > "$PROJECT_MUTATED_PNG"; then
  echo "ERROR: could not capture Main before Track mutation"; exit 1
fi
if ! dump_device_ui "$DEVICE_DUMP" "$PROJECT_MUTATED_XML"; then
  echo "ERROR: could not capture Main hierarchy before Track mutation"; exit 1
fi
if ! tap_main_new_track; then exit 1; fi
sleep 1
if ! tap_project_toolbar_menu; then exit 1; fi
if ! tap_mpc_menu_tile 3 3; then exit 1; fi
if ! assert_latest_project_summary "TRACKS=2" "PLACEMENTS=2"; then exit 1; fi
if ! adb_bounded 10 exec-out screencap -p > "$PROJECT_MUTATED_PNG"; then
  echo "ERROR: could not capture mutated Project state"; exit 1
fi
if ! dump_device_ui "$DEVICE_DUMP" "$PROJECT_MUTATED_XML"; then
  echo "ERROR: could not capture mutated Project hierarchy"; exit 1
fi

# Open the saved file through ACTION_OPEN_DOCUMENT. This parses the file into
# a new domain Project and replaces the already-mutated in-memory model.
project_open_marker_before="$(project_marker_count "PROJECT_OPEN_COMPLETE")"
if ! tap_project_function_bar 0; then exit 1; fi
project_open_picker_ready=0
for ((attempt=1; attempt<=25; attempt++)); do
  if dump_device_ui "$PROJECT_OPEN_REMOTE" "$PROJECT_OPEN_XML" \
      && grep -Fq -- 'package="com.android.documentsui"' "$PROJECT_OPEN_XML"; then
    project_open_picker_ready=1
    break
  fi
  sleep 1
done
if [ "$project_open_picker_ready" -ne 1 ]; then
  echo "ERROR: Android DocumentsUI did not appear for Project Open"; exit 1
fi
if ! capture_stable_picker_state "$PROJECT_OPEN_REMOTE" "$PROJECT_OPEN_XML" "$PROJECT_OPEN_PNG" 8; then
  echo "ERROR: could not capture stable DocumentsUI state for Project Open"; exit 1
fi
if ! wait_for_ui_text "$PROJECT_OPEN_XML" "$PROJECT_OPEN_REMOTE" "Untitled.mpcproj" 30; then
  echo "ERROR: saved Project file is not discoverable in Android DocumentsUI"; exit 1
fi
if ! tap_ui_picker_result_row "$PROJECT_OPEN_XML" "Untitled.mpcproj" auto label; then
  echo "ERROR: could not select the previously saved Project file"; exit 1
fi
if ! wait_for_new_project_marker "PROJECT_OPEN_COMPLETE" "$project_open_marker_before" 45; then
  echo "ERROR: app did not report a successful load of the selected Project file"; exit 1
fi
project_open_logs="$(adb_bounded 5 shell logcat -d -v brief -s MpcGroovebox:I 2>/dev/null || true)"
project_open_line="$(grep -F -- "PROJECT_OPEN_COMPLETE" <<<"$project_open_logs" | tail -n 1 || true)"
if [[ -z "$project_open_line" ]] \
    || ! grep -Fq -- "TRACKS=1" <<<"$project_open_line" \
    || ! grep -Fq -- "PLACEMENTS=1" <<<"$project_open_line"; then
  echo "ERROR: Project file open did not restore its saved Track/Pattern-placement counts"
  echo "$project_open_line"
  exit 1
fi
show_project_route_extended
if ! assert_latest_project_summary "TRACKS=1" "PLACEMENTS=1"; then exit 1; fi
if ! adb_bounded 10 exec-out screencap -p > "$PROJECT_REOPENED_PNG"; then
  echo "ERROR: could not capture the reopened Project screen"; exit 1
fi
if ! dump_device_ui "$DEVICE_DUMP" "$PROJECT_REOPENED_XML"; then
  echo "ERROR: could not capture reopened Project hierarchy"; exit 1
fi
echo "Real Project Save As → add Track in memory → Open saved file restored exact persisted Track/Pattern-placement model."
echo "Android runtime startup, local Browser, and Project persistence smoke passed."


echo "Running extended Project persistence acceptance: full state digest, restart, legacy v1, cancel and malformed input..."
PROJECT_V2_HOST="/tmp/mpc-groovebox-project-v2.mpcproj"
PROJECT_V1_HOST="/tmp/mpc-groovebox-project-v1-no-placements.mpcproj"
PROJECT_V1_CANONICAL_HOST="/tmp/mpc-groovebox-project-v1-canonical-v2.mpcproj"
PROJECT_MALFORMED_HOST="/tmp/mpc-groovebox-project-malformed.mpcproj"
PROJECT_EXT_OPEN_XML="/tmp/mpc-groovebox-project-extended-open-uiautomator.xml"
PROJECT_EXT_OPEN_REMOTE="/sdcard/mpc-groovebox-project-extended-open-uiautomator.xml"
PROJECT_EXT_OPEN_PNG="/tmp/mpc-groovebox-project-extended-open.png"
PROJECT_EXT_RESTART_MAIN_XML="/tmp/mpc-groovebox-project-restart-main-uiautomator.xml"
PROJECT_EXT_RESTART_MAIN_PNG="/tmp/mpc-groovebox-project-restart-main.png"
PROJECT_EXT_RESTART_PROJECT_XML="/tmp/mpc-groovebox-project-restart-loaded-uiautomator.xml"
PROJECT_EXT_RESTART_PROJECT_PNG="/tmp/mpc-groovebox-project-restart-loaded.png"
PROJECT_EXT_CANCEL_XML="/tmp/mpc-groovebox-project-open-cancelled-uiautomator.xml"
PROJECT_EXT_CANCEL_PNG="/tmp/mpc-groovebox-project-open-cancelled.png"
PROJECT_EXT_OPEN_CANCEL_PICKER_XML="/tmp/mpc-groovebox-project-open-cancel-picker-uiautomator.xml"
PROJECT_EXT_OPEN_CANCEL_PICKER_REMOTE="/sdcard/mpc-groovebox-project-open-cancel-picker-uiautomator.xml"
PROJECT_EXT_OPEN_CANCEL_PICKER_PNG="/tmp/mpc-groovebox-project-open-cancel-picker.png"
PROJECT_EXT_REJECT_XML="/tmp/mpc-groovebox-project-malformed-rejected-uiautomator.xml"
PROJECT_EXT_REJECT_PNG="/tmp/mpc-groovebox-project-malformed-rejected.png"
PROJECT_EXT_V1_XML="/tmp/mpc-groovebox-project-legacy-v1-loaded-uiautomator.xml"
PROJECT_EXT_V1_PNG="/tmp/mpc-groovebox-project-legacy-v1-loaded.png"

wait_for_project_picker_item() {
  local target_xml="$1" target_remote="$2" target_png="$3" item="$4"
  local ready=0 attempt
  for ((attempt=1; attempt<=25; attempt++)); do
    if dump_device_ui "$target_remote" "$target_xml" \
        && grep -Fq -- 'package="com.android.documentsui"' "$target_xml"; then
      ready=1; break
    fi
    sleep 1
  done
  if [ "$ready" -ne 1 ]; then
    echo "ERROR: Android DocumentsUI did not appear for expected item $item"; exit 1
  fi
  if ! capture_stable_picker_state "$target_remote" "$target_xml" "$target_png" 8; then
    echo "ERROR: could not capture stable DocumentsUI state for $item"; exit 1
  fi
  if ! wait_for_ui_text "$target_xml" "$target_remote" "$item" 30; then
    echo "ERROR: Android document provider did not expose $item"
    cat "$target_xml" || true; exit 1
  fi
}

# Android Back may navigate up or dismiss an input method without cancelling
# DocumentsUI. Observe the resumed Activity after each Back and stop as soon as
# our app resumes; never send an additional Back into the app. On failure,
# preserve the picker screenshot and accessibility hierarchy for diagnosis.
cancel_android_document_picker() {
  local remote_path="$1" hierarchy_path="$2" screenshot_path="$3" label="$4"
  local resumed_activity="" attempt back_count=0 app_resumed=0

  for ((attempt=1; attempt<=5; attempt++)); do
    resumed_activity="$(adb_bounded 5 shell dumpsys activity activities 2>/dev/null \
      | grep -E 'mResumedActivity|topResumedActivity' | tail -n 1 || true)"
    if grep -Fq -- "$PACKAGE" <<<"$resumed_activity"; then
      app_resumed=1
      break
    fi
    if [[ -z "$resumed_activity" ]]; then
      sleep 0.5
      continue
    fi
    if ! grep -Fq -- "com.android.documentsui" <<<"$resumed_activity"; then
      echo "ERROR: unexpected foreground Activity while cancelling Android $label picker"
      printf '%s\\n' "$resumed_activity"
      break
    fi
    if ! adb_bounded 10 shell input keyevent KEYCODE_BACK; then
      echo "ERROR: could not send Android Back while cancelling $label picker"
      return 1
    fi
    back_count=$((back_count + 1))
    sleep 0.5
  done

  if [[ "$app_resumed" != "1" ]]; then
    resumed_activity="$(adb_bounded 5 shell dumpsys activity activities 2>/dev/null \
      | grep -E 'mResumedActivity|topResumedActivity' | tail -n 1 || true)"
    if grep -Fq -- "$PACKAGE" <<<"$resumed_activity"; then app_resumed=1; fi
  fi

  if [[ "$app_resumed" != "1" ]]; then
    echo "ERROR: Android DocumentsUI did not return to the app after $back_count Back action(s) while cancelling $label"
    printf '%s\\n' "$resumed_activity"
    adb_bounded 10 exec-out screencap -p > "$screenshot_path" || true
    dump_device_ui "$remote_path" "$hierarchy_path" || true
    return 1
  fi

  echo "Android DocumentsUI returned to the app while cancelling $label after $back_count Back action(s)."
  return 0
}
open_project_document() {
  local item="$1"
  OPEN_PROJECT_COMPLETE_BEFORE="$(project_marker_count "PROJECT_OPEN_COMPLETE")"
  OPEN_PROJECT_REJECTED_BEFORE="$(project_marker_count "PROJECT_OPEN_REJECTED")"
  if ! tap_project_function_bar 0; then exit 1; fi
  wait_for_project_picker_item "$PROJECT_EXT_OPEN_XML" "$PROJECT_EXT_OPEN_REMOTE" "$PROJECT_EXT_OPEN_PNG" "$item"
  if ! tap_ui_picker_result_row "$PROJECT_EXT_OPEN_XML" "$item" auto label; then
    echo "ERROR: could not select $item from the actual Android document picker"; exit 1
  fi
}

# Verify the dedicated debug-only native fixture populated actual non-default
# MIDI events and a clipped placement before the real Save As → mutate → Open.
project_fixture_line_ext="$(adb_bounded 5 shell logcat -d -v brief -s MpcGroovebox:I 2>/dev/null | grep -F -- "PROJECT_TEST_FIXTURE_READY" | tail -n 1 || true)"
for expected_fixture_field in "note1=120/36/180/97/88/3/-11" "note2=600/42/90/111/123/2/7" "placement=project-persistence-clip-01/sequence-1-track-1-pattern-1/480/720/120"; do
  if ! grep -Fq -- "$expected_fixture_field" <<<"$project_fixture_line_ext"; then
    echo "ERROR: native persistence fixture missing field $expected_fixture_field"
    echo "$project_fixture_line_ext"; exit 1
  fi
done

# Verify that the initial real UI Save As → mutate → Open restored the complete
# serialized domain, not merely the visible counts. The native fixture above is
# non-empty and includes non-default tick/pitch/duration/velocity/probability/
# ratchet/nudge values as well as a clipped Pattern placement with source offset.
project_save_line_ext="$(adb_bounded 5 shell logcat -d -v brief -s MpcGroovebox:I 2>/dev/null | grep -F -- "PROJECT_SAVE_COMPLETE" | tail -n 1 || true)"
project_open_line_ext="$(adb_bounded 5 shell logcat -d -v brief -s MpcGroovebox:I 2>/dev/null | grep -F -- "PROJECT_OPEN_COMPLETE" | tail -n 1 || true)"
project_saved_sha_ext="$(sed -nE 's/.*stateSha256=([0-9a-f]{64}).*/\1/p' <<<"$project_save_line_ext")"
project_file_sha_ext="$(sed -nE 's/.*fileSha256=([0-9a-f]{64}).*/\1/p' <<<"$project_open_line_ext")"
project_domain_sha_ext="$(sed -nE 's/.*domainSha256=([0-9a-f]{64}).*/\1/p' <<<"$project_open_line_ext")"
if [[ ! "$project_saved_sha_ext" =~ ^[0-9a-f]{64}$ ]] \
    || [[ "$project_saved_sha_ext" != "$project_file_sha_ext" ]] \
    || [[ "$project_saved_sha_ext" != "$project_domain_sha_ext" ]] \
    || ! grep -Fq -- "formatVersion=2" <<<"$project_save_line_ext" \
    || ! grep -Fq -- "formatVersion=2" <<<"$project_open_line_ext" \
    || ! grep -Fq -- "persistedDomainExact=true" <<<"$project_open_line_ext"; then
  echo "ERROR: Project Save → mutated model → Open did not preserve exact native-domain bytes"
  echo "Save: $project_save_line_ext"; echo "Open: $project_open_line_ext"; exit 1
fi
echo "Project v2 Save/Open exact domain SHA passed: $project_saved_sha_ext"

# A genuine force-stop kills the old application process and native model.
# Launch without the fixture intent, then Open again through Android DocumentsUI.
if ! adb_bounded 10 shell am force-stop "$PACKAGE"; then
  echo "ERROR: could not force-stop app for Project process restart"; exit 1
fi
if ! adb_bounded 10 shell logcat -c; then
  echo "ERROR: could not clear logs before process-restart acceptance"; exit 1
fi
if ! adb_bounded 30 shell am start -W --activity-clear-task -n "$ACTIVITY"; then
  echo "ERROR: could not relaunch app after process death"; exit 1
fi
restart_ready=0
for ((attempt=1; attempt<=45; attempt++)); do
  restart_logs="$(adb_bounded 5 shell logcat -d -v brief -s MpcGroovebox:I 2>/dev/null || true)"
  if grep -Fq -- "STARTUP_COMPLETE" <<<"$restart_logs"; then restart_ready=1; break; fi
  sleep 1
done
if [ "$restart_ready" -ne 1 ]; then
  echo "ERROR: native startup did not complete after process restart"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$PROJECT_EXT_RESTART_MAIN_PNG"; then
  echo "ERROR: could not capture post-restart Main screenshot"; exit 1
fi
if ! dump_device_ui "$DEVICE_DUMP" "$PROJECT_EXT_RESTART_MAIN_XML"; then
  echo "ERROR: could not capture post-restart Main hierarchy"; exit 1
fi
show_project_route_extended
open_project_document "Untitled.mpcproj"
if ! wait_for_new_project_marker "PROJECT_OPEN_COMPLETE" "$OPEN_PROJECT_COMPLETE_BEFORE" 45; then
  echo "ERROR: Open after process restart failed through DocumentsUI"; exit 1
fi
restart_open_line="$(adb_bounded 5 shell logcat -d -v brief -s MpcGroovebox:I 2>/dev/null | grep -F -- "PROJECT_OPEN_COMPLETE" | tail -n 1 || true)"
restart_open_sha="$(sed -nE 's/.*domainSha256=([0-9a-f]{64}).*/\1/p' <<<"$restart_open_line")"
if ! grep -Fq -- "formatVersion=2" <<<"$restart_open_line" \
    || ! grep -Fq -- "persistedDomainExact=true" <<<"$restart_open_line" \
    || ! grep -Fq -- "TRACKS=1" <<<"$restart_open_line" \
    || ! grep -Fq -- "PLACEMENTS=1" <<<"$restart_open_line" \
    || [[ "$restart_open_sha" != "$project_saved_sha_ext" ]]; then
  echo "ERROR: force-stop/relaunch → Open did not restore exact saved native state"
  echo "$restart_open_line"; exit 1
fi
show_project_route_extended
if ! adb_bounded 10 exec-out screencap -p > "$PROJECT_EXT_RESTART_PROJECT_PNG"; then
  echo "ERROR: could not capture reopened Project after process restart"; exit 1
fi
if ! dump_device_ui "$DEVICE_DUMP" "$PROJECT_EXT_RESTART_PROJECT_XML"; then
  echo "ERROR: could not capture reopened Project hierarchy after restart"; exit 1
fi

# Cancel Save As, then Open, and ensure the same real Project remains in memory.
save_cancel_marker_before="$(project_marker_count "PROJECT_SAVE_AS_CANCELLED")"
if ! tap_project_function_bar 1; then exit 1; fi
wait_for_project_picker_item "$PROJECT_SAVE_XML" "$PROJECT_SAVE_REMOTE" "$PROJECT_SAVE_PNG" "Untitled.mpcproj"
if ! cancel_android_document_picker "$PROJECT_SAVE_REMOTE" "$PROJECT_SAVE_XML" "$PROJECT_SAVE_PNG" "Save As"; then exit 1; fi
if ! wait_for_new_project_marker "PROJECT_SAVE_AS_CANCELLED" "$save_cancel_marker_before" 20; then
  echo "ERROR: Save As cancellation was not reported"; exit 1
fi
save_cancel_line_ext="$(adb_bounded 5 shell logcat -d -v brief -s MpcGroovebox:I 2>/dev/null | grep -F -- "PROJECT_SAVE_AS_CANCELLED" | tail -n 1 || true)"
save_cancel_sha_ext="$(sed -nE 's/.*stateSha256=([0-9a-f]{64}).*/\1/p' <<<"$save_cancel_line_ext")"
if [[ "$save_cancel_sha_ext" != "$project_saved_sha_ext" ]]; then
  echo "ERROR: canceled Save As changed the native Project state"; echo "$save_cancel_line_ext"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > /tmp/mpc-groovebox-project-save-cancelled.png; then
  echo "ERROR: could not capture Save As cancel path"; exit 1
fi
if ! dump_device_ui "$DEVICE_DUMP" /tmp/mpc-groovebox-project-save-cancelled-uiautomator.xml; then
  echo "ERROR: could not capture Save As cancellation hierarchy"; exit 1
fi

open_cancel_marker_before="$(project_marker_count "PROJECT_OPEN_CANCELLED")"
if ! tap_project_function_bar 0; then exit 1; fi
wait_for_project_picker_item "$PROJECT_EXT_OPEN_CANCEL_PICKER_XML" "$PROJECT_EXT_OPEN_CANCEL_PICKER_REMOTE" "$PROJECT_EXT_OPEN_CANCEL_PICKER_PNG" "Untitled.mpcproj"
if ! cancel_android_document_picker "$PROJECT_EXT_OPEN_CANCEL_PICKER_REMOTE" "$PROJECT_EXT_OPEN_CANCEL_PICKER_XML" "$PROJECT_EXT_OPEN_CANCEL_PICKER_PNG" "Open"; then exit 1; fi
if ! wait_for_new_project_marker "PROJECT_OPEN_CANCELLED" "$open_cancel_marker_before" 20; then
  echo "ERROR: Open cancellation was not truthfully reported"; exit 1
fi
open_cancel_line_ext="$(adb_bounded 5 shell logcat -d -v brief -s MpcGroovebox:I 2>/dev/null | grep -F -- "PROJECT_OPEN_CANCELLED" | tail -n 1 || true)"
open_cancel_sha_ext="$(sed -nE 's/.*stateSha256=([0-9a-f]{64}).*/\1/p' <<<"$open_cancel_line_ext")"
if [[ "$open_cancel_sha_ext" != "$project_saved_sha_ext" ]]; then
  echo "ERROR: canceled Open changed the authoritative Project state"; echo "$open_cancel_line_ext"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$PROJECT_EXT_CANCEL_PNG"; then
  echo "ERROR: could not capture Open cancellation state"; exit 1
fi
if ! dump_device_ui "$DEVICE_DUMP" "$PROJECT_EXT_CANCEL_XML"; then
  echo "ERROR: could not capture Open cancellation hierarchy"; exit 1
fi

# Build a valid v1 fixture from the actual saved file, not the older native
# codec-only fixture whose PROGRAMS section intentionally violates runtime rules.
# Search the real shared-storage mount. On Android, /sdcard is a symlink;
# toybox find without -L does not necessarily traverse that starting path.
device_project_path="$(adb_bounded 10 shell 'find /storage/emulated/0 -type f -name "Untitled.mpcproj" 2>/dev/null | head -n 1' | tr -d '\r')"
if [[ -z "$device_project_path" ]]; then
  echo "ERROR: could not find the actual SAF saved Project file"; exit 1
fi
if ! adb_bounded 30 pull "$device_project_path" "$PROJECT_V2_HOST"; then
  echo "ERROR: could not pull the real SAF saved Project to derive a v1 fixture"; exit 1
fi
if ! python3 - "$PROJECT_V2_HOST" "$PROJECT_V1_HOST" "$PROJECT_V1_CANONICAL_HOST" "$PROJECT_MALFORMED_HOST" <<'PY'
import pathlib, sys
source_path, legacy_path, canonical_path, malformed_path = map(pathlib.Path, sys.argv[1:])
lines = source_path.read_text(encoding="utf-8").splitlines()
if not lines or lines[0] != "MPCMK2_PROJECT 2":
    raise AssertionError("expected the actual Save As document in v2 format")
if not any(line.startswith('PROGRAM "') for line in lines):
    raise AssertionError("v1 fixture must retain a runtime-valid Program")
if not any(line.startswith("PLACEMENT ") for line in lines) or not any(line.startswith("NOTE ") for line in lines):
    raise AssertionError("source fixture needs a placement and non-default MIDI events")
legacy, canonical = [], []
for line in lines:
    if line.startswith("PLACEMENT "):
        continue
    if line.startswith("TRACK "):
        prior = line.rsplit(" ", 1)[0]
        legacy.append(prior)
        canonical.append(prior + " 0")
    else:
        legacy.append(line)
        canonical.append(line)
legacy[0] = "MPCMK2_PROJECT 1"
canonical[0] = "MPCMK2_PROJECT 2"
legacy_path.write_text("\n".join(legacy) + "\n", encoding="utf-8")
canonical_path.write_text("\n".join(canonical) + "\n", encoding="utf-8")
malformed_path.write_text("not a valid MPCMK2_PROJECT document\n", encoding="utf-8")
if any(line.startswith("PLACEMENT ") for line in legacy + canonical):
    raise AssertionError("placement-free fixture/canonical oracle contains a clip")
if not any(line.startswith('TRACK "') for line in legacy) or not any(line.startswith("NOTE ") for line in legacy):
    raise AssertionError("legacy fixture lost its Track or MIDI events")
print("Derived valid placement-free v1 fixture and canonical-v2 state oracle from real SAF file.")
PY
then
  echo "ERROR: failed to produce checked v1/malformed fixtures"; exit 1
fi
expected_v1_sha_ext="$(sha256sum "$PROJECT_V1_CANONICAL_HOST" | awk '{print $1}')"
if [[ ! "$expected_v1_sha_ext" =~ ^[0-9a-f]{64}$ ]]; then
  echo "ERROR: could not hash canonical v1 state"; exit 1
fi
# DocumentsUI retains the folder used by Save As (the dedicated Browser
# fixture folder). Put both test documents in that same visible provider folder
# so the actual picker can select them without an unrelated navigation guess.
if ! adb_bounded 10 shell mkdir -p "$FAVORITE_FOLDER_DEVICE"; then
  echo "ERROR: could not prepare the Project acceptance fixture folder"; exit 1
fi
if ! adb_bounded 30 push "$PROJECT_V1_HOST" "$FAVORITE_FOLDER_DEVICE/Legacy-v1-no-placements.mpcproj"; then
  echo "ERROR: could not push valid v1 Project fixture beside the saved Project"; exit 1
fi
if ! adb_bounded 30 push "$PROJECT_MALFORMED_HOST" "$FAVORITE_FOLDER_DEVICE/Malformed-project.mpcproj"; then
  echo "ERROR: could not push malformed Project fixture beside the saved Project"; exit 1
fi

# The app is still in Project context after cancel; malformed input must reject
# without replacing the saved model and the Project route must remain usable.
open_project_document "Malformed-project.mpcproj"
if ! wait_for_new_project_marker "PROJECT_OPEN_REJECTED" "$OPEN_PROJECT_REJECTED_BEFORE" 45; then
  echo "ERROR: malformed Project did not report a native format rejection"; exit 1
fi
rejected_line_ext="$(adb_bounded 5 shell logcat -d -v brief -s MpcGroovebox:I 2>/dev/null | grep -F -- "PROJECT_OPEN_REJECTED" | tail -n 1 || true)"
rejected_sha_ext="$(sed -nE 's/.*stateSha256=([0-9a-f]{64}).*/\1/p' <<<"$rejected_line_ext")"
if [[ "$rejected_sha_ext" != "$project_saved_sha_ext" ]] || ! grep -Fq -- "PROJECT_LOAD_FAILED" <<<"$rejected_line_ext"; then
  echo "ERROR: rejected Project changed the active native state or omitted truthful error"
  echo "$rejected_line_ext"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$PROJECT_EXT_REJECT_PNG"; then
  echo "ERROR: could not capture malformed load error"; exit 1
fi
if ! dump_device_ui "$DEVICE_DUMP" "$PROJECT_EXT_REJECT_XML"; then
  echo "ERROR: could not capture malformed load hierarchy"; exit 1
fi
if ! grep -Fq -- "currentPage=PROJECT" <<<"$rejected_line_ext"; then
  echo "ERROR: malformed-file rejection lost the Project context"; echo "$rejected_line_ext"; exit 1
fi

# Open the v1 file through the actual Android picker. Its canonical v2 state has
# identical Program/Track/Pattern/MIDI fields but no Pattern placements.
open_project_document "Legacy-v1-no-placements.mpcproj"
if ! wait_for_new_project_marker "PROJECT_OPEN_COMPLETE" "$OPEN_PROJECT_COMPLETE_BEFORE" 45; then
  echo "ERROR: runtime-valid placement-free v1 Project was not loaded from SAF"; exit 1
fi
legacy_line_ext="$(adb_bounded 5 shell logcat -d -v brief -s MpcGroovebox:I 2>/dev/null | grep -F -- "PROJECT_OPEN_COMPLETE" | tail -n 1 || true)"
legacy_sha_ext="$(sed -nE 's/.*domainSha256=([0-9a-f]{64}).*/\1/p' <<<"$legacy_line_ext")"
if ! grep -Fq -- "formatVersion=1" <<<"$legacy_line_ext" \
    || ! grep -Fq -- "persistedDomainExact=not-applicable" <<<"$legacy_line_ext" \
    || ! grep -Fq -- "TRACKS=1" <<<"$legacy_line_ext" \
    || ! grep -Fq -- "PLACEMENTS=0" <<<"$legacy_line_ext" \
    || [[ "$legacy_sha_ext" != "$expected_v1_sha_ext" ]]; then
  echo "ERROR: v1 Open fabricated placements or changed persisted MIDI/domain values"
  echo "Expected canonical state: $expected_v1_sha_ext"; echo "$legacy_line_ext"; exit 1
fi
if ! adb_bounded 10 exec-out screencap -p > "$PROJECT_EXT_V1_PNG"; then
  echo "ERROR: could not capture v1-loaded state"; exit 1
fi
if ! dump_device_ui "$DEVICE_DUMP" "$PROJECT_EXT_V1_XML"; then
  echo "ERROR: could not capture v1-loaded accessibility hierarchy"; exit 1
fi
echo "Extended Project UI acceptance passed: exact v2 state, process restart, Save As/Open cancel, malformed-load preservation and placement-free v1 canonical-state equality."
