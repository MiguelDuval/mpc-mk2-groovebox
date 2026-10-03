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
