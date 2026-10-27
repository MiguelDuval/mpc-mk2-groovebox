# MPC Studio MkII ↔ MPC One Integration Contract

**Status:** Canonical cross-device control-equivalence and routing specification  
**Reviewed:** 2026-10-18  
**Target interaction model:** MPC Standalone OS 3.9 / MPC One-class workflow  
**Primary physical input device:** Akai MPC Studio MkII  
**Product:** Original Android groovebox; Android is the display, compute, audio host and owner of musical state.

## 1. Decision and purpose

**This document is necessary.** The project already documents two sides of the problem: MPC Standalone OS 3.9 user actions and the Studio MkII's physical MIDI/SysEx protocol. The missing bridge was an explicit crosswalk between the action a user expects from an MPC One control and the physical gesture available on the Studio MkII.

Without that bridge, a button can be decoded correctly but routed to the wrong context; an MPC 2/Desktop shortcut can be mistaken for an MPC 3.9 workflow; two similar buttons can silently have their primary and secondary actions inverted; or developers can invent hardware controls the Studio does not have.

This document links six distinct things:

1. **MPC One reference action** — the intended musical/user action documented for the MPC 3.9 target.
2. **Studio MkII gesture** — a real physical button, pad, modifier chord, dial or Touch Strip interaction.
3. **Controller protocol** — the input/output message family owned by the hardware adapter.
4. **Product semantic action** — an application-level command independent of raw MIDI values.
5. **Destination and state mutation** — the real Android route and existing domain operation, or a truthful PARTIAL / RESERVED result.
6. **Feedback and acceptance** — how the controller reflects resulting state and what tests still need to prove.

### Main conclusion

The two devices share MPC terminology, but **there is no universal one-button-to-one-button correspondence**. The Studio MkII is a host-driven controller with a 4×4 pad matrix, Data Dial, +/- buttons, a one-dimensional Touch Strip and a compact 160×80 LCD. MPC One has its own full touchscreen, transport and mode controls and eight Q-Link knobs. Some controls are equivalent, some Studio controls provide additional direct-focus shortcuts, and some MPC One actions have no dedicated physical Studio control.

Our app should reproduce the useful MPC One 3.9 action and state transition—not MPC Desktop navigation, a miniature copy of the Studio LCD, or fake one-to-one hardware parity. For non-identical secondary gestures, the default is to preserve the Studio's printed/documented function and provide an alternate route to the MPC One action. Any deliberate remapping of a printed gesture requires an explicit product decision and paired regression tests.

## 2. Compatibility and version boundary

This is a **custom Android host integration**, not a claim that Akai supports the Studio MkII as an MPC 3 standalone controller.

- The official [MPC Standalone OS v3.9 User Guide](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf) is the primary reference for the MPC One / One+ target interaction model and MPC 3.9 Main, Toolbar, Browser, Arrangement, Grid, Step, Sample Edit, Menu and Project workflows.
- The official [MPC Standalone OS v3.7 User Guide](https://cdn.inmusicbrands.com/Software/37/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.7.pdf) explicitly excludes MPC Studio MkII from its MPC 3 standalone hardware scope. We therefore implement a custom host bridge; do not present this as an officially supported Akai hardware pairing.
- The official [MPC Studio MkII FAQ](https://support.akaipro.com/en/support/solutions/articles/69000799777-akai-pro-mpc-studio-mkii-frequently-asked-questions) describes the controller as USB-MIDI, covers its bus power and TRS MIDI breakout ports, and identifies the Touch Strip as an assignable real-time control.
- The manufacturer-authored [Studio MkII Quickstart Guide](https://imagescdn.juno.co.uk/manual/846279-01U.pdf) is available from a third-party mirror. It is useful for the physical control inventory, printed labels, modifiers and gesture intent, but it is not an Akai-hosted download.
- The official [MPC3 FAQ](https://support.akaipro.com/en/support/solutions/articles/69000857771-mpc3-faq) and the repository's MPC 3.9 knowledge base define the target UI and Track/Program vocabulary.
- The Studio Quickstart often describes MPC 2-era or host-software screens. Reuse the physical gesture, not its desktop window/menu workflow. In MPC 3.9, the selected Track is authoritative; do not create a parallel legacy Program model merely because the Studio has a Program Select button.
- The firmware static research index can clarify a target UI label or interaction, but a firmware string/layout entry does not prove a physical Studio gesture or a runtime transition.

### Evidence hierarchy

| Level | Evidence | Appropriate use |
|---|---|---|
| A — physical observation | Raw MIDI plus visible/musical outcome captured on a real Studio MkII with app/build/port recorded. | Final authority for that exact behavior. |
| B — manufacturer source | Official Akai manual/support article or manufacturer-authored Quickstart. | Strong for button identity and intended action, not raw protocol proof. |
| C — independent protocol research | Public Studio MkII Note/CC/SysEx charts and proof-of-concept tools. | Best available candidate protocol map; live edge cases remain open. |
| D — code and automated tests | This repository's control map, decoder, semantic adapter, UI routes, tests and exact-SHA CI evidence. | Proves what the exercised implementation did, not all physical hardware behavior. |
| E — tutorial inference | A named video/article with a reviewed passage or timestamp showing a physical control. | Use only when the actual control/gesture is clear and separable from host software. |

Do not promote a behavior to physically CONFIRMED based only on a label, source branch, emulator test, old APK, firmware string or unreviewed video. The Akai-linked MPC Studio Academy and Masterclass playlists are indexed in existing documentation, but full transcripts were not accessible during this research pass. This document does not invent tutorial timestamps or claim a complete video-by-video audit.

## 3. Mandatory input-to-state architecture

All Studio-driven actions must follow the existing boundary:

MPC Studio MkII USB-MIDI  
→ Android device and MIDI port discovery / permission  
→ MpcStudioMk2InputDecoder  
→ stateful hardware semantic adapter (Shift, Mode, Locate, Note Repeat, Copy/Delete, banks and press/release lifecycle)  
→ semantic action or contextual-focus command  
→ MPC 3.9-inspired navigation and the existing domain command  
→ authoritative Track / Sequence / Pattern / Pad / Layer / Project state  
→ UI refresh plus state-driven Button LED / Pad RGB / Touch Strip LED / LCD feedback.

Rules:

1. Raw MIDI numbers stay in the hardware map/decoder. UI listeners and domain functions consume semantic action names, not raw Note or CC values.
2. The Android touchscreen is the full-size product UI. The Studio LCD is a glanceable context mirror; it is not a miniature Main screen or a touchscreen.
3. Resolve press/release, Shift, Mode+Pad, Locate, momentary/latch and other gesture state in the semantic adapter. Do not reimplement chord logic inside individual screens.
4. A physical control and an on-screen control that mean the same thing should dispatch the same semantic/domain operation. Do not create parallel copies of musical state in the view.
5. LEDs and LCD mirror resulting app state; they must not merely echo the last incoming button event. Cache redundant feedback where the existing transport supports it.
6. Preserve the audio callback, sampler, sequencer scheduler, native edit history, MIDI transport, Project serializer and SysEx/protocol boundaries. This document is a mapping contract, not permission to rewrite those layers.
7. If a backend or semantic contract is missing, mark the feature RESERVED / UNAVAILABLE. Do not redirect it to an unrelated screen to make the button look functional.
8. Keep the product local-first. Splice, paid content stores, expansion marketplaces, upsells, advertisements, mandatory accounts and nonessential vendor-cloud services are reference-only exclusions. The useful local file/sample workflow remains in scope.

## 4. MPC One ↔ Studio MkII control-equivalence matrix

The raw-value details and full protocol tables remain in the [Studio MkII Reverse-Engineering Traceability](MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md) document. This matrix is about semantic equivalence, route and current gap.

| MPC One action / control | Studio MkII physical equivalent | Product route / semantic contract | Status and caution |
|---|---|---|---|
| Touchscreen / Main workspace | Android touchscreen is the actual full UI; Studio LCD is supplementary. | Use the canonical MPC 3.9-inspired Toolbar, five shortcuts, XL Channel Strip region, workspace and Function Bar. | The target shell is documented elsewhere. Never try to render the full UI on the 160×80 LCD. |
| Data Dial | Jog rotation: CC 100; push: Note 111, per public charts. | Rotation navigates a focused selector or edits its value; push commits/enters the focused item. Context owns focus and increment size. | Context-aware routing exists. Current-build physical rotation direction/value and push behavior still need spot checks. |
| +/- fine adjustment | Studio Plus / Minus (Notes 54 / 55). | Increment/decrement the same focused field/selector used by the Data Dial. | Semantic routing exists; validate field bounds, wrapping/clamping and the actual physical increment. |
| Undo / Redo | Undo (Note 67); Shift+Undo = Redo. | Invoke existing edit history. Only operations actually recorded in that history can promise undo/redo. | Semantic route exists; history coverage varies by edit. Do not claim global project-wide history parity. |
| Shift | Studio Shift (Note 49). | Stateful modifier for the documented secondary action. Release normally restores the unmodified gesture unless the action is a toggle. | Adapter tracks Shift. Test short taps, holds, releases and Shift held while pads/buttons change. |
| Pads 1–16 | Same 4×4 matrix; bottom-left is Pad 1. Non-sequential note map on MIDI channel 10. | Trigger selected Pad/sample, record note events when recorder is armed, or temporarily target the current performance mode (mute, locator, launcher, 16 Level, Repeat, Copy/Delete). Preserve velocity and only supported pressure behavior. | Input/domain paths and map exist. Never calculate the Note from visible pad index; use the explicit map. Per-context physical acceptance remains necessary. |
| Pad Banks A–D / E–H | Four Studio bank buttons; Shift selects E–H (Notes 35–38). | Change logical pad-bank offset, labels and relevant LED state. In Sequence Launcher, the same controls address its bank only while that mode owns the pads. | Bank and launcher paths exist, but normal performance breadth and all feedback states still need validation. |
| Full Level / Half Level | Studio Full Level; Shift+Full Level = Half Level (Note 39). | Full Level sets velocity to 127; Half Level sets 64; with both inactive preserve physical velocity. Apply before trigger/record routing. | Semantic/velocity path exists; check physical velocity and that indicator LEDs match state. |
| 16 Level / Notes (Pad Perform) | Studio 16 Level (Note 40); Shift+16 Level is a Notes shortcut in the Quickstart. | 16 Level maps pads to value steps for a selected supported parameter. Notes/Pad Perform scales or chords are a separate feature and only enabled with a real implementation. | Basic 16-Level velocity routing exists. Full parameter modes and Notes/Pad Perform parity are not assumed complete; reserve unsupported subtypes. |
| Copy / Delete | Studio Copy (Note 122); Shift+Copy = Delete. | Target-aware Pad copy selects a source and destination(s) and commits safely; Delete requires explicit targets. Arrangement/Grid event copy is a separate domain operation and clipboard. | Semantic Copy/Delete and edit history exist; physical verification pending. Never confuse pad-content copy with Pattern-event copy. |
| Note Repeat / latch | Studio Note Repeat (Note 11); Shift gesture latches/unlatches; Touch Strip can select repeat division. | Momentary repeat while held; Shift gesture toggles latch. Scheduling must use the native transport clock. | Routed to scheduler; repeat hits are currently performance-only and are not yet queued into sequence recording. The UI must not imply recorded repeats. |
| Erase | Studio Erase (Note 9); live Erase+Pad targets a pad/event. | While playing, use the supported current-playhead single-event rule. Stopped broad erase requires a real, guarded context/window; never imply the entire project can be erased. | Live single-event erase exists. The full stopped Erase window remains a gap. |
| Main | Studio Main (Note 52). | Navigate to canonical Main without resetting project, selection, playhead or transport. | Main route exists; return-state behavior should be spot checked across supported contexts. |
| Grid | Studio Mode+Pad 2 (printed Grid Editor destination); Android Grid/Arrange affordance. | Enter the actual SEQ → GRID context, preserving selected Track, Sequence, Pattern and event state where relevant. | Grid editor exists. Mode+Pad is the clear Studio hardware shortcut; distinguish it from Studio Shift+Main. |
| Browse | Studio Browse (Note 50). | Open the product's local-first Browser and preserve selected Pad/Layer target. Places/results must map to real providers/actions. | Route exists, but the semantic local result/provider workflow remains partial. Android SAF is a storage primitive, not the entire Browser. |
| Save Project | Studio Mode+Pad 16 (printed Save destination); real Android Toolbar/Menu Project route. | Invoke the genuine Project Save / Save As route, expose errors and preserve current project if a save fails or is cancelled. | Native codec and first Android SAF Save/Save As/Open route exist; exact-HEAD end-to-end hardening remains a separate queue item. Shortcut label alone is not persistence proof. |
| Menu / Preferences | No dedicated Studio Menu/Prefs key. Use Android Toolbar/Menu or an explicitly supported Mode+Pad route. | Open actual application Menu/Preferences state. Preference rows without a backend remain RESERVED. | Main shell has a route and a subset of preferences. A bare Mode press must not be treated as Menu. |
| Track Mix / Pad Mix | Studio Mode+Pad 8 = Channel Mixer; Mode+Pad 7 = Pad Mixer. | Navigate to a supported Track/Channel Mixer or Pad Mixer context while preserving selected Track/Pad. Prefer MPC 3.9 mixer / XL Strip architecture over replicating a legacy host window. | Pad Level/Pan/Tune and XL Strip foundations exist; full Track mixer routing, FX, sends and returns are not complete. |
| Track Mute / Pad Mute | Studio Pad Mute; Shift+Pad Mute = Track Mute (Note 4). MPC One presents Track Mute with Shift for Pad Mute. | Both scopes must be reachable. The normal Studio gesture targets pads; shifted gesture targets tracks. | Both contexts/pad targets route. Preserve Studio's printed primary/secondary meaning unless a deliberate chord-parity profile is approved. |
| Next Sequence | Studio Mode+Pad 9. | Enter Sequence Launcher/selection using the existing Sequence model; bank, queued state, cancellation and LED state must remain coherent. | Launcher foundation exists. Current physical bank/LED behavior is not thereby verified. |
| XYFX | No physical Studio two-axis pad. Studio Mode+Pad 14 is labelled Looper, not XYFX. The Touch Strip is one-dimensional. | XYFX may be a touchscreen action only if a genuine XY feature is implemented; otherwise RESERVED. Do not alias Looper to XYFX. | No direct physical XY equivalent. |
| Step Sequencer | Studio Mode+Pad 15 (printed Step Sequence destination). Pads become steps only when product mode says so. | Open the actual SEQ → STEP context and mutate the existing event model. | Step editor and edit primitives exist. Verify selected Pad/Track, step selection and return behavior with the physical device. |
| Timing Correct (TC) | TC On/Off; Shift+TC = Config (Note 15). | Toggle the real recorder Timing Correct policy or open actual TC settings. | Toggle is routed to sequence recorder policy; full config parity needs action-level verification. |
| Quantize | Studio has a direct Quantize key (Note 12); MPC One has no matching dedicated top-panel key. | Apply actual quantize to supported selected Pattern/notes using current grid and TC settings. Shift variant requires a real explicit selection contract. | Selected Drum Pattern quantize is routed. Shift+Quantize remains reserved pending a selection contract. |
| Sampler / audio recording | Studio Mode+Pad 13. Transport Record is a different command. | Enter Sample Record/Sampler; capture audio only when permission/device/backend is ready. Do not confuse MIDI sequence recording with recording audio. | Sampler foundations exist; full record/import/assign cycle remains partial. |
| Sample Edit | Studio Mode+Pad 5; direct Sample Select/Start/End/Tune controls are additional parameter shortcuts. | Open supported Sample/Track Edit for current Pad/Layer. Parameter keys focus the matching real domain-owned field; dial/+/- adjust it. | Shared waveform/region and per-layer tuning paths exist in part; complete sample loading and layer browsing remain partial. |
| Program Edit | Studio Mode+Pad 6; Program Select / Track Type is an additional direct control (Note 14). | Enter only a supported editor for the selected MPC 3.9 Track type. The selected Track remains authoritative. | Drum Program selection has a path; Track Type mutation and several program-type editors remain RESERVED. Do not recreate an MPC 2 owner model. |
| Tap Tempo / Master | Studio Tap Tempo / Master (Note 53). | Taps update the same sequence tempo state used by the native clock. Master-tempo policy must be a separate supported state transition. | Tap Tempo is routed; Master behavior is separate and partial/reserved. |
| Record / Overdub / Stop / Play / Play Start | Studio dedicated buttons: Notes 73, 80, 81, 82, 83. | All dispatch to the one native transport/recorder surface. Record arms, Play resumes, Play Start begins from sequence start, Overdub preserves existing material and Stop stops transport. | Routes exist. Current physical press behavior, transport LEDs, double-Stop and Shift+Stop need targeted confirmation. |
| Step < / > | Studio Step buttons (Notes 68 / 69). | Move stopped edit/playhead cursor by one grid step. Locate+Step may navigate events only if implemented. This is not the note's signed microtiming Nudge parameter. | Stopped Step navigation exists; Locate combinations are not all accepted. |
| Bar <</>> | Studio Bar buttons (Notes 71 / 72). | Move by one bar. Locate+Bar can jump to start/end only if implemented. | Stopped bar navigation is routed in supported contexts; modifier behavior needs verification. |
| Locate / markers | Studio Locate (Note 70) plus pads. | Pads 9–14 set/select locator slots, Pads 1–6 jump; all targets must use semantic actions. | Six slots and contextual routing exist, including a local gesture threshold; physical hold/timing edge cases remain unconfirmed. |
| Zoom / vertical zoom | Studio Zoom; Shift+Zoom = alternate vertical axis (Note 66). | Contextual horizontal zoom in supported waveform/Grid/timeline views; shifted zoom changes a supported vertical dimension, never the global shell. | Sample waveform and Grid/timeline zoom paths exist in different scopes; physical verification pending. |
| Track Select / Sequence Select | Studio Track Select; Shift+Track Select = Sequence Select (Note 13). | Focus an existing selector; Data Dial/+/- operate on Track or Sequence using current project state and explicit queue/select rules. | Semantic routes exist. Playing-versus-stopped selection and state persistence need action-level verification. |
| Program Select / Track Type | Studio Program Select; Shift+Program Select = Track Type (Note 14). | Select a supported Program for the chosen Track. Track Type changes only when a real setter exists. | Program selection works for supported Drum contexts; Track Type is decoded but not routed. Keep unavailable choices truthful. |
| Sample Select / Start / End / Tune / Fine | Studio Notes 42, 33, 34, 79; Shift+Start = Loop Start, Shift+Tune = Fine. | Focus selected Pad/Layer sample fields; dial/+/- adjust the real region/tune value. The same model drives touch and hardware. | Start/End/Tune are routed; repeated-press layer cycling, fine resolution and eight-layer parity need device/functional tests. |
| Automation Read/Write | Studio Automation R/W (Note 75). | Route only when a real automation state/domain exists. Read/Write and global automation enable are distinct commands. | Decoded but the full automation domain is not implemented; keep RESERVED. |
| Touch Strip / Config | Strip position CC 33, touch Note 78 and mode button Note 0 per public source; segments use CC 57–65. | Continuous input is contextual: repeat division or a parameter/range explicitly exposed by the active page. Configuration cycles only supported semantic modes. | Contextual sample values exist; complete mode/LED synchronization is partial. Channel handling must not be guessed from the map. |
| Q-Link knobs / Q-Link button | No physical bank of eight Studio knobs. Studio has one Data Dial and one one-dimensional Touch Strip. | Use field focus + Data Dial/+/- and a contextual Touch Strip for supported parameters. UI macro bindings require real parameter destinations. | This is not full physical Q-Link parity. Never invent knobs or present unsupported macro controls as working. |
| Looper | Studio Mode+Pad 14 (printed Looper destination). | Route to Looper only if a real loop record/playback domain is implemented. | RESERVED until backend exists; never rebrand it XYFX, Arrangement or another unrelated route. |
| Song Mode / Linear Arrangement | Studio Mode+Pad 10 is the legacy Song destination. MPC 3.9 uses a Linear Arranger / Arrangement context. | Route to the MPC 3.9 Arrangement only by an explicit product mapping using the actual Pattern-placement model; the two labels are not automatically equivalent. | Arrangement and placements have foundations, but Song-specific behavior is not the same contract and must not be inferred from the key label. |
| List Editor | Studio Mode+Pad 4. | Open a real event-list editor only after event display, selection, mutation and acceptance exist. | RESERVED; do not route to Grid/Step as a cosmetic substitute. |

## 5. Studio Mode+Pad destination bridge

Mode on the Studio is a modifier: hold Mode and press one pad to select the printed shortcut. A bare Mode press is not the MPC One Menu action.

| Gesture | Printed Studio destination | Proposed MPC 3.9-inspired product route | Boundary |
|---|---|---|---|
| Mode + Pad 1 | Track View | Supported Track context/view for current Track | Preserve Track and transport. |
| Mode + Pad 2 | Grid Editor | SEQ → GRID | Real Grid route; verify physical destination. |
| Mode + Pad 3 | Wave Editor | Selected Sample waveform / Sample Edit | Partial sample workflow; no separate legacy desktop page requirement. |
| Mode + Pad 4 | List Editor | Future event-list editor | RESERVED until real editor and operations exist. |
| Mode + Pad 5 | Sample Edit | Sample/Track Edit for selected Pad/Layer | Partial; preserve the target. |
| Mode + Pad 6 | Program Edit | Track Edit / supported Program editor | Track-type dependent; unsupported editors remain RESERVED. |
| Mode + Pad 7 | Pad Mixer | Supported Pad Mixer / parameter context | Level/Pan/Tune foundations; not full FX/routing parity. |
| Mode + Pad 8 | Channel Mixer | MPC 3.9 Track/mixer context | Partial; prefer canonical XL Strip/Main architecture. |
| Mode + Pad 9 | Next Sequence | SEQ → LAUNCH / Sequence selection | Use current Sequence Launcher model. |
| Mode + Pad 10 | Song | Arrangement only by explicit product decision | Not automatically equivalent to MPC 3.9 Linear Arranger. |
| Mode + Pad 11 | MIDI Control | Supported app MIDI/Control context | Partial/reserved; no DAW workflow is implied. |
| Mode + Pad 12 | Media / Browser | BROWSE | Local-first provider workflow; omit commercial-content services. |
| Mode + Pad 13 | Sampler | Sample Record / Sampler | Real audio permissions and backend are required. |
| Mode + Pad 14 | Looper | Looper | RESERVED until backend; not XYFX. |
| Mode + Pad 15 | Step Sequence | SEQ → STEP | Use existing Step context/model. |
| Mode + Pad 16 | Save | Project Save action / route | Must invoke actual Save/SAF/codec path and report errors. |

**Unresolved physical mapping:** the public charts README and current app map Mode as MIDI Note 114, while the upstream Go POC controls.yaml labels Mode Note 1. Keep Note 114 as the best-supported current candidate, record the conflict, and obtain a raw press/release capture from the physical unit before changing the decoder.

## 6. Non-identical gestures: explicit policy required

These differences are the most likely causes of accidental feature regressions.

### Main / Grid versus Main / Track View

The MPC One control is labelled Main / Grid. The Studio MkII Quickstart labels its button Main / Track View, with the secondary function therefore differing.

**Default policy:** direct Main always navigates to canonical Main. Grid is available through the Studio's Mode+Pad 2 and the app's real Grid affordance. Keep Shift+Main aligned with the Studio's documented Track View function unless the product owner explicitly chooses an MPC One chord-parity profile. If that profile is chosen, remap Shift+Main to Grid and retain Track View through Mode+Pad 1; implement and test both as adapter semantics, not in screen listeners.

### Browse / Save versus Browse / Up

MPC One uses Shift+Browse for Save; Studio MkII uses Shift+Browse for Browser Up.

**Default policy:** Browse opens Browser and Shift+Browse navigates to the parent folder. Save stays available through Mode+Pad 16 and the actual Android Project route. A chord-parity profile may reassign Shift+Browse to Save only after a separate tested Browser Up gesture is specified.

### Track Mute / Pad Mute order

MPC One exposes Track Mute with shifted Pad Mute; Studio MkII exposes Pad Mute with shifted Track Mute.

**Default policy:** preserve the physical Studio legend: normal Pad Mute targets Pads, shifted Pad Mute targets Tracks. Provide both actions, but do not silently invert the primary/secondary behavior. Acceptance must prove target state changes, not merely LED activation.

### Next Sequence / XYFX

Mode+Pad 9 is the Studio's Next Sequence destination. Mode+Pad 14 is Looper, not XYFX. The Touch Strip is one-dimensional, so it does not supply a direct two-axis physical XY surface.

**Policy:** never alias Looper to XYFX. XYFX may be available via touch only if its own product backend/gesture is real; otherwise it stays RESERVED.

### Touchscreen / Q-Link controls

The Android screen is the full product display; the Studio LCD is only a compact status companion. The Studio has no physical bank of eight Q-Link knobs.

**Policy:** use focus + Data Dial/+/- and a contextual Touch Strip for actual supported parameter values. Do not create phantom physical controls or claim simultaneous one-to-one Q-Link parity.

### Program / Track Type under MPC 3.9

Studio's Program Select and Track Type labels derive from host workflow concepts that do not automatically match the MPC 3.9 unified Track model.

**Policy:** Program Select can change a real supported program on the selected Track. Track Type remains RESERVED until a real setter and valid creation/migration rules exist. Do not create duplicate legacy MPC 2 state to mimic a label.

## 7. Route and state-continuity contract

| Workflow | Required state transition | Return/continuity invariant | Acceptance proof |
|---|---|---|---|
| Main → Track/Sequence select | Focus a selector, then change selection through the existing domain operation. | Project, loop, playhead and transport stay intact. | Main shows the selected entity and downstream editors use the same selection. |
| Main → Grid / Step | Navigate by Mode+Pad 2 / 15 or real on-screen control. | Keep Track, Sequence, Pad and Pattern context unless the action explicitly changes one. | Real event creation/edit is visible in authoritative state; no UI-only duplicate. |
| Browser → assign sample | Browse a real local provider, select/preview if available, then Load/Assign to selected Pad/Layer. | Preserve the destination Pad/Layer; only report success after assignment succeeds. | Check the actual selected file and resulting layer state, including cancellation/error. |
| Sample parameter shortcut | Start/End/Tune/Sample Select focuses a real field. | A focus action does not mutate values; dial/+/- changes only the intended field and preserves unrelated fields. | Observe exact delta, then leave/re-enter route and verify persisted state. |
| Pad / Track Mute | Enter mode, use pads as targets, update the corresponding mute state. | Return to the prior route without changing transport or selected Sequence. | Correct target changes; other targets remain unchanged; feedback matches state. |
| Note Repeat | Hold or latch Repeat and use project-clock scheduling. | Release/unlatch stops repeats; scheduler does not depend on UI frame rate. | Verify physical pad output and division. Sequence-recorded Repeat is a separate gap. |
| Transport | Buttons call existing native transport/recorder operations. | Preserve or reset playhead only according to the specific command. | Check recorder state, playhead, resulting notes and transport LEDs. |
| Project Save / Open | Controller shortcut and touchscreen call the same Project route/codec. | Cancellation/error must not replace or corrupt the current Project. Sample PCM remains external as documented. | Exact persisted fields and error paths are checked; a count-only smoke is not full acceptance. |
| Hardware feedback | Domain state changes drive CC/SysEx feedback. | Avoid endless redundant sends; feedback failure must not block audio. | Check output bytes and actual LEDs/LCD on the current app build. |

## 8. Protocol facts and unresolved questions

Current best-supported Studio MkII protocol facts, fully traced in the reverse-engineering document:

- Buttons are Note messages with press/release behavior; the public chart reports button LED CCs generally equal to the corresponding Note number.
- Pads use a different MIDI channel and an explicit non-sequential Note map; physical Pad 1 is bottom-left.
- Jog rotation uses CC 100; Jog press uses Note 111; Touch Strip position uses CC 33 and touch uses Note 78 in the public table.
- Pad RGB uses SysEx; Touch Strip LED segments and Note Repeat divisions use separate CC ranges; the 160×80 LCD uses a six-region image transport.
- Current code contains decoding, semantic, LED and LCD components, but decoded input, semantic routing, backend completeness and physical acceptance are separate statuses.

Open issues—do not silently decide either from a screen label or desktop example:

1. **Mode Note conflict:** public README/current mapping says Note 114; Go proof-of-concept controls.yaml says Note 1. Capture raw Mode press and release on the real controller.
2. **Jog / Touch Strip CC channel policy:** public tables specify control channels, while the current decoder accepts CC 100 and CC 33 without enforcing a channel. Capture raw status bytes on the device before changing the policy.
3. **Feedback completeness:** representative SysEx encoders and packet tests exist; previous inventory recorded 72 upstream LCD messages and 72 paired PNGs, but all payloads were not individually decoded/reassembled. Current-build physical LCD refresh and comprehensive LED-state coverage are still not accepted.

Remember: **decoded ≠ routed; routed ≠ backend-complete; backend-complete ≠ physically verified.**

## 9. Source, implementation and test traceability

| Concern | Canonical project anchor | Use |
|---|---|---|
| MPC One target action and status | MPC3-USER-ACTION-CATALOG.md | Read documented action, product deviation, backend status and acceptance route before changing UI behavior. |
| MPC 3.9 workflow research | MPC3-9-OS-KNOWLEDGE-BASE.md and MPC3-REFERENCE-INDEX.md | Target app information architecture and source references. |
| Cross-device matrix | This document | MPC One action → Studio gesture → product semantic route, including mismatches and no-equivalents. |
| Physical protocol, source confidence and hardware acceptance | MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md | Note/CC/SysEx map, source evidence, code/tests and physical gaps. |
| Semantic action mapping | MPC-STUDIO-MKII-SEMANTIC-MAP.md | Modifier/chord state and semantic action produced by the adapter. |
| Hardware overview | HARDWARE-MPC-STUDIO-MK2.md | Compact protocol overview. |
| Raw control definitions | src/MPC/MpcStudioMk2ControlMap.h | Note/Pad map, CC values and raw control constants. |
| MIDI input decoding | src/MPC/MpcStudioMk2InputDecoder.cpp | Press/release, velocity, pressure, Jog, Touch Strip and channel behavior. |
| Modifier and gesture adapter | src/MPC/MpcStudioMk2SemanticAdapter.cpp | Shift/Mode/Locate/latch/chord lifecycle and semantic output. |
| Pad RGB packet | src/MPC/MpcStudioMk2LedProtocol.cpp | SysEx packet layout and boundaries. |
| Android command/context bridge | android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MainActivity.java | Actual listener, context, native operation and state refresh; inspect callers before editing. |
| Button LED policy | MpcHardwareFeedbackPolicy.java and MpcHardwareFeedbackPolicyTest.java | State-to-LED policy and tested encoding. |
| Studio LCD render/output | MpcStudioMk2LcdRenderer.java and MpcStudioMk2MidiMessages.java | Compact original 160×80 view and chunked SysEx encoding. |
| Decoder tests | tests/MpcStudioMk2InputDecoderTests.cpp | Protocol-to-input-event assertions. |
| Semantic tests | tests/MpcStudioMk2SemanticAdapterTests.cpp | Modifier/chord-to-action assertions. |
| Android smoke | tests/android_ui_smoke.sh | Startup and real UI workflow evidence; not a substitute for physical hardware. |
| Physical test records | PHYSICAL-TEST-CHECKLIST.md | Real device checks and unresolved items. |

Before changing a physical control, read the operating protocol and safe-change contract. Update the action catalog and tests alongside the implementation slice. Do not copy raw values from this document into UI listeners.

## 10. Bounded implementation and physical-validation queue

This is a controller-mapping queue, not a replacement for the overall roadmap's current Project persistence priority.

### P0 — foundational physical operation
1. Verify Android USB permission, device/port enumeration and opening input/output ports on the real phone; record exact port names.
2. Verify pad numbering, velocity/press/release, Shift lifecycle and all four base/shifted bank pairs.
3. Verify Data Dial direction/delta stream, dial push, +/- and focus-sensitive edits.
4. Verify Main, Browser, Grid (Mode+Pad 2), Step (Mode+Pad 15), Track/Sequence selection, Undo/Redo and transport against authoritative app state.
5. Verify Full/Half Level, momentary/latch Note Repeat, both mute scopes, Locate and Erase+Pad including release/held-modifier exits.
6. Verify core transport, selected Pad and active-mode feedback on the current build.

### P1 — content and parameter editing
1. Sample Select/Start/End/Tune, per-layer focus/cycling and sample target preservation.
2. Zoom, Step/Bar, Locate variants, Timing Correct and Quantize.
3. Mode+Pad routes for Sample Edit, Program Edit, mixers, launcher, Browser, Sampler and Save.
4. Harden Project Save/Open through real Android lifecycle, exact persisted field comparison, picker cancellation and malformed/provider-error paths.
5. Verify Pad RGB SysEx, button LED CCs, Touch Strip and Note Repeat indicator LEDs on the actual device.
6. Capture an LCD frame, verify all six chunks and prove context refresh with actual state changes.

### P2 — only after real backend contracts exist
1. Stopped Erase window and broad sequence-data deletion.
2. Track Type mutation and full supported Program Edit workflows.
3. Full Channel Mixer, effects, sends/returns and parameter macros.
4. Automation Read/Write, event List Editor, Song-specific semantics and Looper.
5. Any XYFX parity claim; no direct physical two-axis Studio control exists.
6. Sequence-recorded Note Repeat, only if the recorder/scheduler contract is extended and tested.

### Required physical-test record

Every test must record: date; Studio MkII identity if available; Android device and OS; app commit SHA and APK SHA-256; MIDI port names; exact physical gesture including press/hold/release and modifier timing; raw inbound status/channel/data bytes; semantic action and destination; authoritative before/after state; outgoing LED/SysEx evidence; audible/performance outcome when relevant; expected vs actual result; unresolved follow-up. A screenshot alone is not a physical protocol test.

## 11. Research register

### R-01 — MPC Standalone OS v3.9 guide
[Official PDF](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf). Primary target reference for MPC One / One+ controls and MPC 3.9 Main, Browser, Arrangement, Grid, Step, Sample Edit, Menu and Project. The repository's MPC3-REFERENCE-INDEX.md contains section/page anchors.

### R-02 — MPC3 FAQ and supported-hardware boundary
[Akai MPC3 FAQ](https://support.akaipro.com/en/support/solutions/articles/69000857771-mpc3-faq) and [official v3.7 User Guide](https://cdn.inmusicbrands.com/Software/37/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.7.pdf). Use the FAQ for MPC3 Main/Linear Arrangement and selected-Track vocabulary; respect the user guide's exclusion of Studio MkII from the supported MPC3 standalone hardware scope.

### R-03 — Official Studio MkII FAQ
[Akai Studio MkII FAQ](https://support.akaipro.com/en/support/solutions/articles/69000799777-akai-pro-mpc-studio-mkii-frequently-asked-questions). Retain USB-MIDI, bus power, TRS MIDI connections and Touch Strip concepts. Desktop installers, authorization, accounts, plugins and DAW workflow are excluded.

### R-04 — Studio MkII Quickstart / physical gestures
[Manufacturer-authored Quickstart PDF mirror](https://imagescdn.juno.co.uk/manual/846279-01U.pdf) and [readable transcription](https://www.manualslib.com/guide/4441664/akai-professional-mpc-studio-quickstart-guide.html). Used for Mode+Pad destinations and printed secondary gestures: Main/Track View, Track/Sequence Select, Program/Track Type, Browser/Up, Sample fields, Quantize/TC, Zoom/Vert Zoom, Pad/Track Mute, transport, Step/Bar, Locate, Repeat/Latch and Copy/Delete. Host-software page navigation is not adopted.

### R-05 — Primary public protocol reverse-engineering
[bcrowe306/MPC-Studio-Mk2-Midi-Sysex-Charts](https://github.com/bcrowe306/MPC-Studio-Mk2-Midi-Sysex-Charts). Primary independent candidate source for incoming Notes/CCs, LED feedback, RGB SysEx and LCD transport. Existing inventory checks 72 messages and 72 PNG fixtures with representative packet inspection, not a full reassembly of every payload. Mode Note and CC channel questions remain open.

### R-06 — Secondary protocol cross-check, not workflow specification
[gstepniewski/MPC-Studio-Mk2-Ableton-Midi-Remote-Script](https://github.com/gstepniewski/MPC-Studio-Mk2-Ableton-Midi-Remote-Script). May help cross-check an actual physical MIDI value only. Do not copy Ableton commands, DAW arrangement, script navigation or host-specific mappings.

### R-07 — Akai tutorial and support workflows
Akai-linked [MPC Studio Academy playlist](https://www.youtube.com/playlist?list=PLyawkFIWnO1PkLpXAN1cciuvoXbrt7M9U) and [MPC Studio Masterclass playlist](https://www.youtube.com/playlist?list=PLyawkFIWnO1O6ipwAPUWnZgA5wxp2g0fL) are indexed but **not fully transcript-audited** in this pass; no unreviewed gesture is attributed to them. Future findings require video title, timestamp, exact observed physical action and confidence.

Accessible official workflow pages used as cross-checks:
- [Understanding the MPC Browser](https://support.akaipro.com/en/support/solutions/articles/69000871930-akai-pro-mpc-series-understanding-the-mpc-s-browser) — local Places, content navigation, search, filter, favorites and audition. Keep useful local file workflows; omit paid stores/subscription destinations.
- [Tuning Basics](https://support.akaipro.com/en/support/solutions/articles/69000874731-akai-mpc-series-tuning-basics) — tuning selected Pad/Layer and eight-layer Track Edit concept.
- [How to Save Samples](https://support.akaipro.com/en/support/solutions/articles/69000858918-mpc-series-how-to-save-samples) — distinguish sample saving/loading from Project save.
- [Reassigning Q-Links in a Program](https://support.akaipro.com/en/support/solutions/articles/69000858717/) — parameters have explicit assignment/context; this does not create physical Q-Link knobs on the Studio.
- [Output Routing Basics](https://support.akaipro.com/en/support/solutions/articles/69000868280-akai-pro-mpc-series-output-routing-basics) — distinguishes mixer scope and routing semantics.

### R-08 — Current project reference documents
- MPC3-9-OS-KNOWLEDGE-BASE.md — target UI information architecture and routes.
- MPC3-USER-ACTION-CATALOG.md — action reference, product decision, backend status and acceptance criteria.
- MPC3-REFERENCE-INDEX.md — source and research index, including targeted firmware UI findings.
- MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md — hardware input/output protocol, code/test and physical confidence.
- MPC-STUDIO-MKII-SEMANTIC-MAP.md — current semantic action behavior and status.
- HARDWARE-MPC-STUDIO-MK2.md, MIDI-TRANSPORT.md and PHYSICAL-TEST-CHECKLIST.md — hardware, Android MIDI transport and real-device tests.
- DEVELOPMENT-OPERATING-PROTOCOL.md and UI-MIGRATION-SAFE-CHANGE-CONTRACT.md — scope, protected architecture boundaries and acceptance requirements.

## 12. Definition of completion

The cross-device documentation is complete when MPC One top-panel actions have a Studio gesture, an alternate touch route, or an explicit no-equivalent / RESERVED label; non-identical modifier gestures are stated rather than silently equated; raw MIDI conflicts are recorded as unresolved pending capture; and code/test anchors and physical acceptance criteria are provided.

**This document completes the mapping contract, not the hardware integration.** Current source already routes multiple Studio controls, but current-build physical Studio MkII acceptance remains pending. On each implementation milestone, update this crosswalk, the semantic map/action catalog, PHYSICAL-TEST-CHECKLIST.md and SESSION-STATE.md with the tested SHA, exact-SHA Actions evidence and remaining physical gaps.
