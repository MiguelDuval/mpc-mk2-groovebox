# UI Migration Safe-Change Contract

## Purpose

The UI migration is large. The engine is valuable and must remain protected.

This file is the engineering firewall between a major presentation refactor and the working musical core.

---

## 1. Golden rule

Change presentation first. Change semantics only when the current model cannot express truthful MPC 3.9 behavior.

Never change realtime audio implementation merely to simplify UI code.

---

## 2. Layer ownership

| Layer | UI may read | UI may change |
|---|---|---|
| Android Activity/platform | lifecycle, views, permissions, MIDI status | composition/orchestration |
| UI state/navigation | renderable state | freely |
| MkII semantic adapter | semantic actions/state | missing semantic mappings |
| Domain | musical state/commands | missing truthful concepts only |
| Sequencer | read-only state/semantic commands | non-realtime semantic gaps only |
| Audio engine | status/state | no UI-driven realtime redesign |
| Audio callback | nothing UI-specific | prohibited |
| MIDI transport | device/port status | boundary fixes only |
| Feedback encoder | semantic state | feedback mapping improvements |
| Persistence | project/UI state where required | only deliberate schema additions |

---

## 3. Forbidden coupling

Never:

- pass Android Views into domain/native;
- expose Tracktion types in UI state;
- read raw MIDI values in screen classes;
- make widget-local state the source of truth for musical state;
- perform waveform decoding during expensive layout work;
- rebuild the complete UI for every MIDI byte;
- send controller feedback merely because a widget redraws;
- put blocking work in the realtime audio path;
- couple a visual animation to audio scheduling.

---

## 4. Preferred data flow

Native/domain snapshot
-> MpcUiState
-> ContextView.render(state)
-> SemanticAction
-> domain/native
-> new state
-> phone indication + controller feedback

Hardware path:
MIDI
-> MkII decoder
-> MkII semantic adapter
-> SemanticAction
-> UI/domain
-> feedback

Touch path:
Touch
-> SemanticAction
-> UI/domain
-> feedback

When touch and hardware mean the same thing, both must dispatch the same semantic command.

---

## 5. Migration checkpoints

Every phase must leave the application runnable.

### Shell checkpoint

Old contexts can temporarily render inside the new shell.

### Main checkpoint

New Main Mode can replace the old MAIN page without changing sampler/sequencer behavior.

### Grid checkpoint

Existing Grid event semantics and zoom policies remain reusable.

### Sample checkpoint

Existing waveform/sample operations remain reusable.

### Browser checkpoint

Existing file loading is wrapped by a semantic provider rather than destroyed.

### Mixer checkpoint

Existing volume/pan operations remain the backend of the new mixer UI.

---

## 6. Test gates

For each phase:

- existing unit tests remain green;
- relevant semantic-routing tests run;
- Android UI smoke covers the new context;
- hardware navigation regression is added when the physical behavior is affected;
- GitHub Actions result is inspected before claiming success.

A screenshot is never sufficient acceptance evidence.

---

## 7. Roll-forward policy

This migration is permanent.

Do not maintain two competing product UIs indefinitely.

Once a new context reaches equal or greater functional coverage:

- remove duplicate old UI;
- keep domain/audio features;
- record removal in the migration log;
- preserve backward recovery through Git history rather than a second product UI.

---

## 8. Recovery

Protected baseline:
a1c73aa52d51689b117fa1b2b27a0a3f11cb869e

Migration starting point:
a809196fab09de2be87c9019377f7756f3673d3d

Never reset or force-push main.

Recover forward on a feature branch.

---

## 9. Context acceptance

A context is accepted when:

- its shell placement matches the canonical MPC information architecture;
- its hardware entry path is documented;
- Data Dial focus is visible;
- bottom functions are explicit;
- current state is visible;
- controller feedback is synchronized where available;
- lower-layer behavior is covered by tests;
- transport/audio behavior remains stable.

---

## 10. Migration log

For each meaningful refactor record:

- date;
- branch;
- old context;
- new context;
- source reference;
- files changed;
- lower-layer files touched, if any;
- tests;
- Actions result;
- physical verification;
- remaining gaps.

This makes the global rewrite auditable across future sessions.


## 2026-10-02 Phase 1/2 implementation checkpoint

Branch: `work/mpc3-ui-migration-master-spec`  
Checkpoint head: `5dadbbfa6e7c1485d5098f1bd2f6c795a0472ddf`

The presentation migration now has its first executable boundary:

- `MpcUiState` is the UI/navigation snapshot and does not own audio or realtime objects.
- `MpcNavigationController` owns mode, subcontext, focus, history and exactly five configurable shortcuts.
- `MpcShell` owns the Toolbar / Shortcuts / compact context / Workspace / Function Bar composition.
- `MainActivity` keeps legacy workspace implementations as temporary render adapters while the shell becomes the canonical composition.
- MPC Studio MkII Main and Track View semantic navigation now enter different contexts; the previous routing of both actions to the Main workspace is removed.
- Browser, Grid, Sampler and Channel Mixer are exposed through the five initial shortcut slots.
- The Android UI smoke audit now verifies the five-shortcut shell before exercising the existing Grid/Step/Sample/record workflows.

No realtime audio, sampler, sequencer scheduler/clock, raw MIDI decoder or hardware SysEx layer was changed by this slice.

Verification:
- GitHub Actions: no completed status was available at this checkpoint; do not treat the branch as CI-green.
- Physical MkII verification: pending.


## 2026-10-02 Track Edit geometry checkpoint

Branch: `work/mpc3-ui-migration-master-spec`

The bounded Track Edit workspace was brought into structural alignment with the documented MPC layout:
top TRACK/PAD context → scrollable parameter workspace → persistent bottom tab bar.

The change is presentation-only. Existing Drum semantic setters, sample-region editing and MkII layer focus remain the backend; unsupported LFO/Modulation/Effects and atomic Edit All Layers behavior stay reserved.

Verification target:
- Android source preflight includes the Track Edit geometry/content-description contract;
- GitHub Actions must re-run for the new commit;
- physical MkII verification remains pending.


## 2026-10-03 Persistent MPC context / buildability checkpoint

Branch: `work/mpc3-ui-migration-master-spec`

The recovered full project tree was preserved while the next UI slice was hardened:

- corrected the Main smoke-test assertion so the documented Track-state row remains immediately above the performance canvas;
- aligned `MpcShell` to the current implementation geometry contract: 44dp Toolbar, 48dp shortcut rail, 210dp context/channel rail, 40dp Function Bar;
- restored four missing `MainActivity` JNI bridge entry points for launcher/step-edit context compatibility and the existing step-edit parameter policy;
- added the persistent Main context rail for Sequence + BPM, Track, Program, Pad, semantic Data Dial focus/subcontext and sequence overview;
- separated persistent context from Mixer Strip detail visibility so hiding the mixer does not hide the canonical context state;
- kept all changes on a normal fast-forward commit chain from recovery HEAD; no files were deleted or replaced by a repository-wide tree rewrite.

Files changed in this slice:
- `MainActivity.java`
- `MpcShell.java`
- `NativeEngine.cpp`
- `tests/android_ui_smoke.sh`
- `docs/MPC3-UI-SCREEN-BLUEPRINT.md`
- this contract

Verification:
- source-level Main/shell/context contracts pass;
- Java native declaration audit: 136/136 declarations have matching C++ JNI definitions;
- existing step-edit policy assertions pass in an isolated native compile/test;
- required project tree files remain present;
- GitHub Actions/status for the current commits: no completed workflow/status result is available through the connected GitHub checks, so CI is not considered green;
- local Android/Gradle build was not executed because no local checkout/build environment was available;
- physical MPC Studio MkII verification remains pending.

Remaining gaps:
- obtain a real Android/native CI or local build result;
- run emulator/static smoke in the actual repository checkout;
- perform physical MkII interaction verification after the next runnable checkpoint.


## 2026-10-03 Persistent context hierarchy checkpoint

The compact persistent context rail may receive presentation-only refinements that:

- preserve the fixed shell width and persistent visibility;
- use small section captions and compact value fields to establish Sequence → Track → Program → Pad → Data Dial hierarchy;
- project semantic Main Data Dial focus onto the corresponding context field without adding a second focus state;
- keep mixer-detail visibility separate from the persistent context layer.

Forbidden regressions remain: no domain ownership in the shell, no hiding of canonical context when Mixer Strip details are toggled, no duplicate transport controls, and no realtime/audio-thread coupling.

Verification target:
- source smoke checks lock the hierarchy helper, compact field heights and focus projection;
- GitHub Actions and physical MkII verification remain separate acceptance gates.

### 2026-10-04 Main Track type fidelity correction

The Main Track header presents **one** Track Type icon beside the Track identity. The six documented Track Types (Drum / Keygroup / Plugin / MIDI / Clip / CV) are selector choices inside the Track Type context, not six persistent header controls. Unsupported types may remain unavailable in the selector, while the Main header always shows only the currently selected type. No audio, MIDI, scheduler, or native realtime path changed.

### 2026-10-04 Toolbar project-entry increment

The Toolbar now includes a compact project/browser entry beside project identity. This is presentation/navigation only and routes into the existing Browser semantic context. No project persistence, audio, MIDI, or transport ownership changed.

## 2026-10-04 MPC One visual-fidelity checkpoint

Branch: `feature/mpc-one-ui-fidelity`

The next Main/shell presentation slice moves the implementation closer to the physical MPC One / documented MPC3 visual language without changing realtime or domain ownership:

- the persistent Toolbar now uses the MPC One red status-bar treatment and keeps the legacy page title out of the visible chrome;
- Toolbar time-counter text is compact and status-oriented;
- shortcut buttons use an icon-first, flat selected-state treatment instead of generic Android button chrome;
- Function Bar controls use flat graphite surfaces with stable left-to-right semantic ordering;
- Main Track now exposes the selected Track's Program directly below the Track identity band, matching the documented MPC Main information hierarchy;
- the Main Track header now has a compact Track-type icon cluster; unsupported types remain explicitly unavailable;
- the persistent left context rail remains the canonical glance/entry surface and shares the same Track/Program state.

This is a presentation/navigation slice. No realtime audio callback, sampler scheduler, sequencer clock, raw MIDI decoder, or native audio ownership changed.

External visual reference used during implementation:
- documented MPC3 Main Mode / MPC One workflow screenshots;
- official Akai MPC3 support material and MPC One documentation.

Acceptance:
- all existing source-level UI contracts remain green;
- GitHub Actions must build and run the Android emulator smoke test for the new commit;
- physical MkII verification remains a separate hardware gate.

## 2026-10-03 Compact context sizing checkpoint

The persistent context panel may use WRAP_CONTENT/content-sized geometry. A fixed height must not be reintroduced solely to reserve space for the optional Mixer Strip detail layer.

## 2026-10-03 Main section framing checkpoint

Main presentation may consolidate the Sequence and Track/Arrangement regions into flat framed surfaces, provided the change:

- does not create a second semantic focus model;
- preserves Track/Arrangement as sibling local views;
- keeps Loop as a dedicated Sequence control;
- avoids duplicate Arrangement navigation/edit controls.

No realtime, native sequencing or MIDI ownership may be introduced by framing changes.

## 2026-10-03 Main geometry fidelity checkpoint

The latest bounded Main UI slice tightened the MPC-facing presentation without changing domain/state ownership:

- MainActivity.showMainPage() now uses explicit 4dp/2dp/40dp/36dp geometry constants;
- Main Track/Arrangement header and Track-state row use the compact 40dp band;
- Main presentation helpers use zero-radius rectangular surfaces;
- MpcShell shortcut/context rails use 4dp content insets while preserving 44dp Toolbar, 48dp shortcut rail, 210dp context/channel rail and 40dp Function Bar;
- smoke source preflight now locks these geometry invariants.

Verification remains source-level because no Android build/physical MkII run is available in this environment; GitHub Actions/status for the current commit is still not reported by the connector.

- The Main-specific action helper prevents legacy rounded button styling from leaking into the MPC Main surface; legacy pages remain unchanged.

- Main Toolbar remains presentation-only: the latest pass stabilizes geometry and touch targets without adding new domain ownership or transport semantics.


## 2026-10-05 MPC One mixer affordance iconography checkpoint

Branch: `feature/mpc-one-ui-fidelity`

Old context:
- Main XL Channel Strip visibility and Track/Pad selection used Unicode glyphs on Android Buttons.

New context:
- XL Channel Strip visibility uses deterministic original vector iconography with an eye/show-hide semantic.
- Main Track/Arrangement Track/Pad selector uses deterministic single-pad / four-squares iconography.
- Icon state remains derived from existing `MpcUiState` mixer visibility/pad-mode state; no second state model was introduced.

Source reference:
- Akai Professional, “Output Routing Basics” — documents the XL Channel Strips, the top icon used to show/hide them, and the bottom-right single-pad/four-squares Track/Pad selector.

Files changed:
- `android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMixerStripIconDrawable.java`
- `android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcMainMixerStripView.java`
- `android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MainActivity.java`
- `tests/android_ui_smoke.sh`

Lower-layer files touched:
- None.

Tests:
- Added source-level regression gate requiring deterministic mixer icon integration.
- Existing XL Channel Strip collapse and Data Dial focus gates remain enabled.
- Source contract verification passes against the feature branch.

Actions:
- Android Build was initiated for the updated branch; final runtime/emulator result pending at this checkpoint.

Physical verification:
- MPC Studio MkII verification remains pending.

Remaining gaps:
- Complete Android emulator smoke without System UI ANR interference.
- Perform physical MkII workflow verification.


## Migration checkpoint — 2026-10-05 MPC 3.x factory Shortcut Rail order

- Date: 2026-10-05
- Branch: `feature/mpc-one-ui-fidelity`
- Previous context: truthful-only implemented default shortcuts (Browser / Track View / Grid / Step / Pad Mixer).
- New context: canonical MPC 3.x factory Shortcut Rail order (Browser / Channel Mixer / Pad Mixer / Sounds / XY).
- Source: MPC Standalone OS v3.5 User Guide, which documents five default Main shortcuts as Browser, Channel Mixer, Pad Mixer, Sounds, and XY. The current application keeps Channel Mixer / Sounds / XY behavior reserved until their backends exist, while preserving the hardware-facing visual order.
- Files changed: `MpcModeRegistry.java`, `MpcNavigationControllerTest.java`.
- Lower-layer files: none.
- Tests: focused navigation defaults test updated; full Android Actions gate required before acceptance.
- Physical verification: pending on MPC Studio MkII.
- Remaining gap: implement the three reserved destination backends rather than changing the factory Shortcut Rail order.


## 2026-10-05 MPC Pull-Down Menu checkpoint

- Added `MpcPullDownPanelView` as a presentation-only shell overlay.
- Wired it from Main transport-position gesture and dismissal scrim; no navigation/audio ownership was moved into the panel.
- Control page reads current project/Sequence/BPM/MIDI/Audio state through MainActivity's existing state path.
- Q-Link page preserves documented field vocabulary but marks unsupported operations RESERVED.
- Lower layers touched: none.
- Required acceptance: source/unit gates + GitHub Actions Android emulator smoke + physical MkII verification.
