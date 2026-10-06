#!/usr/bin/env bash
set -euo pipefail

APK="android/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="com.miguelduval.mpcmk2groovebox.debug"
ACTIVITY="$PACKAGE/com.miguelduval.mpcmk2groovebox.MainActivity"
DEVICE_DUMP="/sdcard/window_dump.xml"
DUMP="/tmp/mpc-groovebox-ui.xml"
SMOKE_MODE_EXTRA="mpc.groovebox.smoke.mode"
MAIN_ACTIVITY_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MainActivity.java"
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
  'case SOUNDS: return "♫";' \
  'case XYFX: return "✣";'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: MPC shortcut glyph contract missing: $required"
    exit 1
  fi
done

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
  '"TRACK TYPE • DRUM is the only implemented Track Type"' \
  "buildMpcMenuTile" \
  "styleMpcMenuFooterButton" \
  "Main Mode BPM" \
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
  "uiAuditSmokeMode ? View.VISIBLE : View.GONE" \
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

if ! grep -Fq -- "uiAuditSmokeMode ? View.VISIBLE : View.GONE" "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: normal Main must not expose diagnostic footer as permanent UI"
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
  'TRACK_TYPE_SELECT' \
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
  'MpcTrackTypeIconDrawable.Type.CLIP' \
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
for required in   'MpcShortcutIconDrawable'   'new MpcShortcutIconDrawable('   'dp(MPC_SHORTCUT_SELECTION_WIDTH_DP)'   'button.setSelected(selected)'   'setSelected(selected)'   'case BROWSER:'   'case GRID:'   'case STEP:'   'case TRACK_VIEW:'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE" &&      ! grep -Fq -- "$required" "android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcShortcutIconDrawable.java"; then
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

if ! grep -Fq -- 'TRACK TYPE • DRUM is the only implemented Track Type' "$MAIN_ACTIVITY_SOURCE"; then
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
echo "Running MPC Browser workspace composition preflight..."
for required in   'body.addView(center, new LayoutParams(0, -1, 1))'   'TARGET • PAD'   'Browser target context • state only'; do
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

for required in   '"PLACES", "CONTENT", "EXPANSIONS"'   'FILTER Buttons'   'OPEN STORAGE…'   'CURRENT SAMPLE'; do
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
for required in   'case MENU:'   'NEW PROJECT'   'SAVE'   'PREFERENCES'   'MIDI / CONTROL'   'EDIT SHORTCUTS'   'BACK'; do
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

echo "Running Android runtime startup smoke..."
echo "Running MPC Main UI audit contract preflight..."
for forbidden in   'shortcut.setContentDescription("MPC shortcut " + mode.label())'; do
  if grep -Fq -- "$forbidden" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: stale factory shortcut accessibility override detected: $forbidden"
    exit 1
  fi
done
for required in   'String[] mainShellExpectedDescriptions'   'MPC One Main Toolbar'   'MPC shortcut CHANNEL MIXER'   'MPC shortcut SOUNDS'   'MPC shortcut XY'   'MPC shell compact track program context'   'Main Track visual hierarchy • Track / Program / workspace section'   'clickMpcToolbarMenuForAudit()'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: current MPC Main UI audit contract missing: $required"
    exit 1
  fi
done
for required in   'UI_AUDIT_TIMEOUT_SECONDS=120'   'UI_INTERACTION_COMPLETE'   'UI_HIERARCHY_FAILED:'   'UI_INTERACTION_FAILED:'   'timeout 30s adb shell uiautomator dump'; do
  if ! grep -Fq -- "$required" "$0"; then
    echo "ERROR: runtime smoke synchronization contract missing: $required"
    exit 1
  fi
done
echo "Installing debug APK..."
adb install -r "$APK"

echo "Clearing logcat and launching MainActivity..."
adb logcat -c
adb shell am force-stop "$PACKAGE"
adb shell am start -W -n "$ACTIVITY" --es "$SMOKE_MODE_EXTRA" "ui-audit" 2>&1 | tee /tmp/mpc-groovebox-am-start.txt

echo "Allowing startup path to settle..."
sleep 5

if ! adb shell pidof "$PACKAGE" | tr -d '\r' | grep -Eq '[0-9]'; then
  echo "ERROR: MPC Groovebox process is not alive after launch"
  adb logcat -d -v brief > /tmp/mpc-groovebox-logcat.txt || true
  tail -n 250 /tmp/mpc-groovebox-logcat.txt || true
  exit 1
fi

if ! adb shell dumpsys activity activities | grep -Fq "$ACTIVITY"; then
  echo "ERROR: MainActivity is not present in activity manager after launch"
  adb shell dumpsys activity activities > /tmp/mpc-groovebox-activities.txt || true
  tail -n 250 /tmp/mpc-groovebox-activities.txt || true
  exit 1
fi

adb logcat -d -v threadtime > /tmp/mpc-groovebox-logcat.txt
if grep -Eq 'AndroidRuntime: FATAL EXCEPTION|Fatal signal [0-9]+|FATAL EXCEPTION IN SYSTEM PROCESS' /tmp/mpc-groovebox-logcat.txt; then
  echo "ERROR: Android runtime/native fatal crash detected during startup"
  grep -E -A 35 -B 5 'AndroidRuntime: FATAL EXCEPTION|Fatal signal [0-9]+|FATAL EXCEPTION IN SYSTEM PROCESS' /tmp/mpc-groovebox-logcat.txt | tail -n 250 || true
  exit 1
fi

echo "Capturing startup UI screenshot..."
adb exec-out screencap -p > /tmp/mpc-groovebox-startup.png || {
  echo "ERROR: startup screenshot capture failed"
  exit 1
}

echo "Waiting for application-side UI audit to complete..."
UI_AUDIT_TIMEOUT_SECONDS=120
ui_audit_complete=0
for ((second=0; second<UI_AUDIT_TIMEOUT_SECONDS; second++)); do
  log_snapshot="$(adb logcat -d -v brief 2>/dev/null || true)"
  if grep -Fq -- "UI_INTERACTION_COMPLETE" <<<"$log_snapshot"; then
    ui_audit_complete=1
    break
  fi
  if grep -Eq 'UI_HIERARCHY_FAILED:|UI_INTERACTION_FAILED:|UI_STARTUP_FINALIZATION_FAILED|STARTUP_NATIVE_FAILED' <<<"$log_snapshot"; then
    echo "ERROR: application-side UI audit reported failure before external UI dump"
    grep -E -A 8 -B 3 'UI_HIERARCHY_FAILED:|UI_INTERACTION_FAILED:|UI_STARTUP_FINALIZATION_FAILED|STARTUP_NATIVE_FAILED' <<<"$log_snapshot" | tail -n 120 || true
    exit 1
  fi
  if ! adb shell pidof "$PACKAGE" | tr -d '\r' | grep -Eq '[0-9]'; then
    echo "ERROR: MPC Groovebox process exited while waiting for UI audit completion"
    exit 1
  fi
  sleep 1
done

if [ "$ui_audit_complete" -ne 1 ]; then
  echo "ERROR: application-side UI audit did not reach UI_INTERACTION_COMPLETE within ${UI_AUDIT_TIMEOUT_SECONDS}s"
  adb logcat -d -v threadtime > /tmp/mpc-groovebox-logcat.txt || true
  tail -n 350 /tmp/mpc-groovebox-logcat.txt || true
  exit 1
fi

echo "Application-side UI audit completed; requesting the accessibility hierarchy now."

echo "Dumping post-audit UI hierarchy..."
if ! timeout 30s adb shell uiautomator dump >/tmp/mpc-groovebox-uiautomator.txt 2>&1; then
  cat /tmp/mpc-groovebox-uiautomator.txt || true
  echo "ERROR: post-audit uiautomator dump failed"
  exit 1
fi
if ! adb shell test -s "$DEVICE_DUMP"; then
  echo "ERROR: post-audit UI dump file was not created: $DEVICE_DUMP"
  cat /tmp/mpc-groovebox-uiautomator.txt || true
  exit 1
fi
if ! adb exec-out cat "$DEVICE_DUMP" >"$DUMP"; then
  echo "ERROR: post-audit UI dump read failed: $DEVICE_DUMP"
  cat /tmp/mpc-groovebox-uiautomator.txt || true
  exit 1
fi

echo "Android runtime startup smoke passed."
