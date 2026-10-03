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
  "Main Mode Track / Arrangement context header" \
  "Main Mode selected track" \
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
main_identity_view=$(grep -n -m1 'mainTrackViewButton = actionButton(' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$main_identity_start" || -z "$main_identity_type" || -z "$main_identity_view" || "$main_identity_type" -le "$main_identity_start" || "$main_identity_view" -le "$main_identity_type" ]]; then
  echo "ERROR: Main Track identity band source boundary is missing"
  exit 1
fi
main_identity_block=$(sed -n "$main_identity_type,$((main_identity_view - 1))p" "$MAIN_ACTIVITY_SOURCE")
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
if ! grep -Fq -- 'mainTrackTypeField = buildMainTrackTypeSelector();' <<<"$main_identity_block"; then
  echo "ERROR: unified Main Track identity ownership contract is missing"
  exit 1
fi

if ! grep -Fq -- 'Main Arrangement Edit RESERVED' "$MAIN_ACTIVITY_SOURCE" || ! grep -Fq -- 'trackContextHeader.addView(arrangementEdit,' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Arrangement edit action must stay attached to the unified Track/Arrangement header boundary"
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

if ! grep -Fq -- 'actionButton("−", v -> adjustMainLayer(-1))' "$MAIN_ACTIVITY_SOURCE" || ! grep -Fq -- 'actionButton("+", v -> adjustMainLayer(1))' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main LAYER must expose compact previous/next layer controls"
  exit 1
fi
if ! grep -Fq -- 'MpcUiState.DataDialFocus.SAMPLE_LAYER' "$MAIN_ACTIVITY_SOURCE" || ! grep -Fq -- 'LAYER • DATA DIAL / +/-' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main LAYER field must retain semantic Data Dial focus entry"
  exit 1
fi
if ! grep -Fq -- 'Button loop = actionButton("↻"' "$MAIN_ACTIVITY_SOURCE" ||
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

echo "Running MPC3 Toolbar geometry preflight..."
for required in \
  'MPC_TOOLBAR_INSET_DP = 6' \
  'MPC_TOOLBAR_CONTROL_HEIGHT_DP = 34' \
  'MPC_TOOLBAR_GAP_DP = 2' \
  'MPC_TOOLBAR_MENU_WIDTH_DP = 38' \
  'MPC_TOOLBAR_PROJECT_WIDTH_DP = 132' \
  'MPC_TOOLBAR_TIMING_WIDTH_DP = 60' \
  'MPC_TOOLBAR_METRO_WIDTH_DP = 58' \
  'MPC_TOOLBAR_AUTO_WIDTH_DP = 48' \
  'MPC_TOOLBAR_TRANSPORT_WIDTH_DP = 40' \
  'bar.setContentDescription("MPC Main Toolbar")' \
  'BAR 001  BEAT 1  TICK 000' \
  'MPC Toolbar Menu'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: MPC3 Toolbar geometry/content contract missing: $required"
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
  'MPC_MAIN_RADIUS_DP = 0' \\
  'private Button mainActionButton(' \\
  'page.setPadding(dp(MPC_MAIN_CONTENT_GUTTER_DP), dp(2)' \
  'sequenceCard.setPadding(0, 0, 0, dp(MPC_MAIN_SECTION_GAP_DP));' \
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
if ! grep -Fq -- 'MPC presents Track / Arrangement as contextual headers' <<<"$main_view_switch_block"; then
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
if ! grep -Fq -- 'MPC Track View context header' <<<"$track_view_block"; then
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
for required in 'strip.setOnClickListener(v -> {' 'String trackIdentityStatus =' 'trackMetadataSeparator'; do
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

if ! grep -Fq -- 'Track −/+ is a high-frequency Main action, not a navigation command' "$MAIN_ACTIVITY_SOURCE" || ! grep -Fq -- 'mainTrackArrangementHost.getChildAt(1).getVisibility()' "$MAIN_ACTIVITY_SOURCE" || ! grep -Fq -- 'MpcUiState.DataDialFocus.TRACK' "$MAIN_ACTIVITY_SOURCE"; then
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

if ! grep -Fq -- 'compactMixerStripModeToggle = actionButton("□  ▦",' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: Main Track lower-right selector must expose the single-pad / four-squares pair"
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
if ! grep -Fq -- 'mainTrackSampleActionButton.setText("SAMPLE EDIT");' "$MAIN_ACTIVITY_SOURCE" || \
   ! grep -Fq -- 'v -> showSamplePage());' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: loaded Main Track sample must expose SAMPLE EDIT"
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
if [[ -z "$track_detail_line" || -z "$track_canvas_line" || "$track_detail_line" -ge "$track_canvas_line" ]]; then
  echo "ERROR: Main Track state row must remain directly above the performance canvas"
  exit 1
fi

echo "Running Main Track/Arrangement horizontal canvas preflight..."
main_pad_split=$(grep -n -m1 '0, ViewGroup.LayoutParams.MATCH_PARENT, 0.36f' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
main_wave_split=$(grep -n -m1 '0, ViewGroup.LayoutParams.MATCH_PARENT, 0.64f' "$MAIN_ACTIVITY_SOURCE" | cut -d: -f1)
if [[ -z "$main_pad_split" || -z "$main_wave_split" || "$main_pad_split" -ge "$main_wave_split" ]]; then
  echo "ERROR: Main Track touch-pad / waveform split is missing or reversed"
  exit 1
fi
if grep -Fq -- '0.52f' "$MAIN_ACTIVITY_SOURCE" || grep -Fq -- '0.48f' "$MAIN_ACTIVITY_SOURCE"; then
  echo "ERROR: obsolete near-equal Main Track pad/waveform split remains"
  exit 1
fi
# The Track state row is intentionally rendered immediately above the performance canvas
# (see the positive ordering assertion above). Do not assert the inverse here.

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

echo "Running persistent MPC context hierarchy preflight..."
for required in \
  'compactContextCaption("SEQUENCE")' \
  'compactContextCaption("PROGRAM")' \
  'compactContextCaption("DATA DIAL")' \
  'private TextView compactContextField(' \
  'private void setCompactContextFocus(' \
  'private String compactDialFocusLabel(' \
  'dp(14)' \
  'dp(36)' \
  'dp(34)' \
  'active ? DANGER : LINE' \
  '"DIAL\n" + compactDialFocusLabel(dialFocus)'; do
  if ! grep -Fq -- "$required" "$MAIN_ACTIVITY_SOURCE"; then
    echo "ERROR: persistent MPC context hierarchy contract missing: $required"
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
