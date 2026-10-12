#!/usr/bin/env bash
set -euo pipefail

APK="android/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="com.miguelduval.mpcmk2groovebox.debug"
ACTIVITY="$PACKAGE/com.miguelduval.mpcmk2groovebox.MainActivity"
DEVICE_DUMP="/sdcard/window_dump.xml"
DUMP="/tmp/mpc-groovebox-ui.xml"
SMOKE_MODE_EXTRA="mpc.groovebox.smoke.mode"
MAIN_ACTIVITY_SOURCE="android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MainActivity.java"
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
  '"TRACK TYPE • DRUM is the only implemented Track Type"' \
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

for required in   '"PLACES", "CONTENT", "EXPANSIONS"'   'void onNavigationItemSelected(String section, String item);'   'void onNavigateUp();'   'void onChooseFolder();'   'void onLocalScanRequested();'   'void onFileSelected(MpcBrowserFile file);'   'void onAuditionSelectedFile();'   'void setSection(String section)'   'void setLocalFiles(List<MpcBrowserFile> files, String status)'   'matchesBrowserQuery('   'addFileResult('   'MPC Browser scan local audio files'   'SCAN LOCAL AUDIO…'   'NO LOCAL WAV FILES'   'button.setContentDescription('   '"MPC Browser navigation " + section + " " + item'   'search.setContentDescription("MPC Browser search files")'   'clear.setContentDescription("MPC Browser clear search")'   'destination.setContentDescription("MPC Browser target Pad and Layer")'   'currentSample.setContentDescription("MPC Browser current sample")'   'DRUMS'   'INSTRUMENTS'   'OPEN STORAGE…'   'CHOOSE FOLDER…'   'setUserFolderFiles('   'MPC Browser open Android storage picker'   'MPC Browser choose a local folder for Browser results'   'CURRENT SAMPLE'; do
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
require_source_contains "$BROWSER_SOURCE" 'MPC Browser choose a local folder for Browser results'
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
for required in   'UI_AUDIT_TIMEOUT_SECONDS=120'   'audit_deadline=$((SECONDS + UI_AUDIT_TIMEOUT_SECONDS))'   'adb_bounded()'   'timeout --signal=TERM --kill-after=3s'   'UI_MAIN_CAPTURE_READY'   'mpc-main-capture-release'   'mpc-groovebox-main-ready-uiautomator.xml'   'UI_BROWSER_CAPTURE_READY'   'mpc-browser-capture-release'   'mpc-groovebox-browser-uiautomator.xml'   'UI_STORAGE_PICKER_LAUNCH_REQUESTED'   'UI_STORAGE_PICKER_RETURNED'   'mpc-groovebox-storage-picker.png'   'UI_INTERACTION_COMPLETE'   'UI_HIERARCHY_FAILED:'   'UI_INTERACTION_FAILED:'   'uiautomator dump' 'tap_ui_picker_result_row()'; do
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

echo "Installing debug APK (hard limit 120s)..."
if ! adb_bounded 120 install -r "$APK"; then
  echo "ERROR: adb install failed or timed out after 120s"
  exit 1
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

echo "Capturing startup UI screenshot..."
if ! adb_bounded 15 exec-out screencap -p > /tmp/mpc-groovebox-startup.png; then
  echo "ERROR: startup screenshot capture failed or timed out after 15s"
  exit 1
fi

echo "Allowing startup path to settle..."
sleep 5

echo "Waiting for application-side UI audit to complete..."
UI_AUDIT_TIMEOUT_SECONDS=120
ui_audit_complete=0
main_runtime_capture_done=0
browser_runtime_capture_done=0
storage_picker_capture_done=0
audit_deadline=$((SECONDS + UI_AUDIT_TIMEOUT_SECONDS))
audit_poll=0
while (( SECONDS < audit_deadline )); do
  log_snapshot="$(adb_bounded 5 logcat -d -v brief 2>/dev/null || true)"

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
print("Ready Main accessibility controls verified:", len(required))
PY
    then
      echo "ERROR: ready Main hierarchy failed canonical control checks"
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
    tile_bounds = None
    row_bounds = None
    while candidate is not None:
        bounds = candidate.attrib.get("bounds", "")
        cls_candidate = candidate.attrib.get("class", "").lower()
        resource_id = candidate.attrib.get("resource-id", "")
        if resource_id == "com.android.documentsui:id/item_root" and bounds.startswith("[") and bounds.endswith("]"):
            row_bounds = bounds
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
    # DocumentsUI's item_root is a non-clickable LinearLayout; its left
    # thumbnail area may behave as preview. Prefer the actual filename center
    # (a real row child) and leave the separate Preview control untouched.
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
    # In grid mode, filenames and their MaterialCardView can be exposed as
    # non-clickable; use the tile body before falling back to the text bounds.
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
  if ! adb_bounded 10 shell input tap "$tap_x" "$tap_y"; then
    echo "ERROR: could not tap DocumentsUI result row $expected_text at $tap_x,$tap_y"
    return 1
  fi
  # On some Android 16 DocumentsUI builds a list row takes focus on the first
  # tap and opens only on the next tap. Keep the activity check inside the
  # platform double-tap interval; never tap again after the app has resumed.
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
  for ((attempt=1; attempt<=max_attempts; attempt++)); do
    if ! dump_device_ui "$remote_path" "$hierarchy_path"; then return 1; fi
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
  echo "ERROR: timed out waiting for UI target: $expected_description"
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
TREE_FOLDER_NAME="MPC-Browser-Tree"
TREE_SAMPLE_NAME="Browser-Tree-Only.wav"
TREE_FOLDER_DEVICE="/sdcard/Download/$TREE_FOLDER_NAME"
TREE_SAMPLE_DEVICE="$TREE_FOLDER_DEVICE/$TREE_SAMPLE_NAME"
TREE_MEDIA_QUERY="/tmp/mpc-browser-tree-not-in-mediastore.txt"
TREE_PICKER_XML="/tmp/mpc-groovebox-browser-tree-picker-uiautomator.xml"
TREE_PICKER_REMOTE="/sdcard/mpc-groovebox-browser-tree-picker-uiautomator.xml"
TREE_BROWSER_XML="/tmp/mpc-groovebox-browser-tree-results-uiautomator.xml"
TREE_BROWSER_REMOTE="/sdcard/mpc-groovebox-browser-tree-results-uiautomator.xml"
TREE_RESTORED_XML="/tmp/mpc-groovebox-browser-tree-restored-uiautomator.xml"
TREE_RESTORED_REMOTE="/sdcard/mpc-groovebox-browser-tree-restored-uiautomator.xml"

if ! adb_bounded 10 shell mkdir -p "$TREE_FOLDER_DEVICE"; then
  echo "ERROR: could not create user-folder test fixture"; exit 1
fi
if ! adb_bounded 10 shell touch "$TREE_FOLDER_DEVICE/.nomedia"; then
  echo "ERROR: could not protect the tree fixture from MediaStore indexing"; exit 1
fi
if ! adb_bounded 30 push "$LOCAL_SAMPLE_HOST" "$TREE_SAMPLE_DEVICE"; then
  echo "ERROR: could not copy tree-only WAV fixture"; exit 1
fi
if ! adb_bounded 10 shell content query \
    --uri content://media/external/audio/media \
    --projection _display_name \
    --where "_display_name='$TREE_SAMPLE_NAME'" > "$TREE_MEDIA_QUERY" 2>&1; then
  echo "ERROR: could not verify MediaStore exclusion for tree fixture"
  cat "$TREE_MEDIA_QUERY" || true
  exit 1
fi
if grep -Fq -- "$TREE_SAMPLE_NAME" "$TREE_MEDIA_QUERY"; then
  echo "ERROR: tree-only WAV appeared in MediaStore, so provider isolation cannot be proven"
  cat "$TREE_MEDIA_QUERY" || true
  exit 1
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
    "MPC Browser choose a local folder for Browser results" 15; then
  echo "ERROR: USER FOLDER did not expose its Choose Folder action"; exit 1
fi
if ! tap_ui_description "$TREE_BROWSER_XML" "MPC Browser choose a local folder for Browser results"; then
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
# Android 16 can expose the internal-storage directory list while keeping
# the roots-drawer control visible. A visible singular "Download" is the real
# child folder. Do not tap the drawer's plural "Downloads" provider root: it
# is not guaranteed to map to /sdcard/Download or expose our test fixture.
if grep -Fq -- 'text="Download"' "$TREE_PICKER_XML"; then
  if ! tap_ui_picker_result_row "$TREE_PICKER_XML" "Download"; then
    echo "ERROR: could not open the physical Download folder"; exit 1
  fi
elif grep -Eq 'content-desc="Show roots"|content-desc="Show navigation drawer"' "$TREE_PICKER_XML"; then
  if ! tap_ui_description "$TREE_PICKER_XML" "Show roots"; then
    if ! tap_ui_description "$TREE_PICKER_XML" "Show navigation drawer"; then
      echo "ERROR: DocumentsUI exposed a root screen but its navigation control was not actionable"
      exit 1
    fi
  fi
  # If the picker started in Recents, select the actual device storage root,
  # then navigate into its physical Download directory rather than Downloads
  # from the virtual media/provider roots.
  tree_storage_root=""
  for candidate in "Android SDK built for x86_64" "Internal storage" "Internal Storage"; do
    if grep -Fq -- "text=\"$candidate\"" "$TREE_PICKER_XML"; then
      tree_storage_root="$candidate"
      break
    fi
  done
  if [[ -z "$tree_storage_root" ]]; then
    echo "ERROR: roots drawer did not expose the internal device-storage root"
    python3 - "$TREE_PICKER_XML" <<'PY'
import sys, xml.etree.ElementTree as ET
for n in ET.parse(sys.argv[1]).getroot().iter("node"):
    t=n.attrib.get("text","").strip(); d=n.attrib.get("content-desc","").strip()
    if t or d: print(f"text={t!r} desc={d!r}")
PY
    exit 1
  fi
  if ! tap_ui_text "$TREE_PICKER_XML" "$tree_storage_root"; then
    echo "ERROR: could not open the internal device-storage root"; exit 1
  fi
  if ! wait_for_ui_text "$TREE_PICKER_XML" "$TREE_PICKER_REMOTE" "Download" 10; then
    echo "ERROR: internal device-storage root did not expose Download"; exit 1
  fi
  if ! tap_ui_picker_result_row "$TREE_PICKER_XML" "Download"; then
    echo "ERROR: could not activate the physical Download folder row"; exit 1
  fi
else
  echo "ERROR: tree picker exposed neither root navigation nor a Download folder"
  python3 - "$TREE_PICKER_XML" <<'PY'
import sys, xml.etree.ElementTree as ET
for n in ET.parse(sys.argv[1]).getroot().iter("node"):
    t=n.attrib.get("text","").strip(); d=n.attrib.get("content-desc","").strip()
    if t or d: print(f"text={t!r} desc={d!r}")
PY
  exit 1
fi
# DocumentsUI may focus a folder row without opening it on the first touch.
# Confirm the next-level folder is visible; if not, activate the focused physical
# Download row with keyboard semantics. This fallback applies whether the picker
# began at the storage root or arrived there through the roots drawer.
if ! wait_for_ui_text "$TREE_PICKER_XML" "$TREE_PICKER_REMOTE" "$TREE_FOLDER_NAME" 4; then
  if ! adb_bounded 5 shell input keyevent KEYCODE_ENTER; then
    echo "ERROR: could not activate the focused physical Download row"; exit 1
  fi
  if ! wait_for_ui_text "$TREE_PICKER_XML" "$TREE_PICKER_REMOTE" "$TREE_FOLDER_NAME" 4; then
    if ! adb_bounded 5 shell input keyevent KEYCODE_DPAD_CENTER; then
      echo "ERROR: could not open the focused physical Download row"; exit 1
    fi
  fi
fi
if ! wait_for_ui_text "$TREE_PICKER_XML" "$TREE_PICKER_REMOTE" "$TREE_FOLDER_NAME" 10; then
  echo "ERROR: physical Download did not reveal the prepared folder"; exit 1
fi
if ! tap_ui_picker_result_row "$TREE_PICKER_XML" "$TREE_FOLDER_NAME"; then
  echo "ERROR: could not activate the prepared document folder row"; exit 1
fi
if ! wait_for_ui_text "$TREE_PICKER_XML" "$TREE_PICKER_REMOTE" "$TREE_SAMPLE_NAME" 4; then
  if ! adb_bounded 5 shell input keyevent KEYCODE_ENTER; then
    echo "ERROR: could not activate the focused test-folder row"; exit 1
  fi
  if ! wait_for_ui_text "$TREE_PICKER_XML" "$TREE_PICKER_REMOTE" "$TREE_SAMPLE_NAME" 4; then
    if ! adb_bounded 5 shell input keyevent KEYCODE_DPAD_CENTER; then
      echo "ERROR: could not open the focused test-folder row"; exit 1
    fi
  fi
fi
if ! wait_for_ui_text "$TREE_PICKER_XML" "$TREE_PICKER_REMOTE" "$TREE_SAMPLE_NAME" 10; then
  echo "ERROR: prepared folder did not expose the real WAV child; refusing to grant a parent folder"; exit 1
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
    if grep -Eiq 'text="Allow"|text="ALLOW"' "$TREE_PICKER_XML"; then
      if ! tap_ui_text "$TREE_PICKER_XML" "Allow"; then
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
if ! adb_bounded 30 shell am start -W -n "$ACTIVITY"; then
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
echo "Android document-tree selection, real WAV results, filters and persisted access restoration passed."

echo "Android runtime startup and local Browser workflow smoke passed."
