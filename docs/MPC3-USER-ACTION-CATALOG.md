# MPC 3.9 User-Action Catalog — Android implementation index

**Status:** Canonical action-level index for UI/interaction implementation  
**Target reference:** MPC Standalone OS 3.9 on MPC One / One+ class hardware  
**Product:** MPC MkII Groovebox for Android, with MPC Studio MkII as primary physical controller  
**Catalog revision:** 2026-10-18  
**Implementation branch at catalog creation:** feature/mpc-one-ui-fidelity  
**Purpose:** Connect each documented user action to its source, the firmware evidence that may clarify it, the current Android implementation, and a test/acceptance path.

This is the action-oriented companion to [MPC3-9-OS-KNOWLEDGE-BASE.md](MPC3-9-OS-KNOWLEDGE-BASE.md), [MPC3-REFERENCE-INDEX.md](MPC3-REFERENCE-INDEX.md), and [MPC3-UI-SCREEN-BLUEPRINT.md](MPC3-UI-SCREEN-BLUEPRINT.md). Read this catalog before implementing a substantial MPC-style interaction. It is intended to reduce repeated research and make the next action in a user workflow explicit.

## 1. Scope and truthfulness contract

The catalog covers documented, high-value MPC 3.9 interactions that are relevant to our Android UI, including actions that are not yet implemented. It deliberately distinguishes:
- **Reference behavior:** what official Akai documentation describes.
- **Firmware evidence:** clues from the analysed Gen1 firmware image. Static evidence may reveal a resource, control description, label or action metadata, but does not automatically prove runtime behavior.
- **Product decision:** what our local-first Android product should implement, omit, or mark unavailable.
- **Implementation status:** what the current app appears to support according to code/docs and the latest available acceptance evidence.
- **Acceptance evidence:** which test or device observation demonstrates the action really works.

This is an action catalog, not a claim of total reverse-engineering. It does not treat every resource/string as a proven interaction, and it does not invent unsupported behavior. When the manual and firmware evidence are incomplete, record the uncertainty and implement only a truthful, testable behavior.

For physical MPC Studio MkII implementation, also consult [MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md](MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md). Its scope firewall excludes MPC Desktop and DAW workflows; use it to link protocol facts to code, tests and physical confidence.

### Product-specific exclusions

The reference taxonomy is preserved truthfully, but our product is a local instrument. Do not introduce Splice, subscription content, online stores, expansion marketplaces, advertisements, upsells, mandatory accounts, or unnecessary vendor-cloud dependencies. Such reference actions must be marked **REFERENCE ONLY — intentionally excluded**. Local Places, user folders, sample browsing and file loading remain in scope when backed by real providers and operations.

### Status vocabulary

- **VERIFIED — named evidence:** a specific existing automated test/build or physical test exercised the behavior. Historical verification is not automatic proof that every later HEAD is green.
- **IMPLEMENTED — reverify at acceptance boundary:** source and project records indicate an implementation, but the listed evidence may be older than the current HEAD or may not cover the whole user workflow.
- **PARTIAL:** a real subset works; the remaining scope is explicitly listed.
- **GAP:** the reference action is required for the target but no sufficient implementation is currently established.
- **RESERVED / UNAVAILABLE:** deliberately not exposed as a working feature because the necessary semantic/backend contract does not exist.
- **REFERENCE ONLY — excluded:** real MPC behavior documented for completeness, deliberately not part of this product.
- **UNKNOWN / NEEDS INVESTIGATION:** sources are insufficient to specify the exact behavior.

A status never means that a visible label alone proves a working feature.

## 2. How an agent must use this catalog

For each UI action being changed:

1. Find the action ID below and read its source references (S-xx) and relevant firmware evidence (F-xx).
2. Check the linked knowledge-base/blueprint section and current implementation files. Treat the live source and latest GitHub Actions evidence as more authoritative than a stale status cell.
3. Trace the real view/listener, current UI state, semantic/domain operation and tests before editing. Do not create duplicate musical state in the view.
4. If the action is missing from the catalog, add an entry before implementing it. Record what is known, what is uncertain, intended state change, backend dependency and acceptance scenario.
5. Implement the smallest vertical slice; validate the changed state, navigation/return behavior and relevant persistence. For musical edits, check preservation of unrelated events and Undo/Redo where supported.
6. Update this catalog and the relevant roadmap/status matrix when status changes. Record the exact tested SHA and run. Keep physical-controller verification separate from emulator evidence.

**Priority of evidence:** latest source and exact-SHA test results → direct physical MPC observation for behavior depending on hardware → official MPC 3.9 user guide/support article → structured firmware help/TUI/layout evidence → binary strings/resource names → inference. If evidence conflicts, investigate instead of silently choosing the most convenient interpretation.

## 3. Source registry

Use the official MPC 3.9 guide as the primary user-workflow source. Page numbers below are guide section/page anchors already indexed in this repository; verify against the actual v3.9 guide if a precise gesture or detail is not stated in the text.

| ID | Source | Main use |
|---|---|---|
| S-01 | [MPC Standalone OS User Guide v3.9 (official PDF)](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf) | Canonical Standalone UI/workflow reference. Main p.15; Browser p.20; Linear Arranger p.21; Step Sequencer p.29; Sampling p.34; Sample Editing p.36; XY Pad p.38; Tracks p.45; Menu p.56; Toolbar p.57; Project p.60; Preferences p.61; Audio/MIDI/Sequencer settings pp.62–65; Project Load/Save pp.67–68. |
| S-02 | [MPC3 FAQ (official Akai)](https://support.akaipro.com/en/support/solutions/articles/69000857771-mpc3-faq) | MPC3 supported hardware, Main Mode, Linear Arranger, mixers, XL Channel Strip, one-to-one Track workflow, sample layers and the unified Track/Program model. |
| S-03 | [Understanding the MPC Browser (official Akai)](https://support.akaipro.com/en/support/solutions/articles/69000871930-akai-pro-mpc-series-understanding-the-mpc-s-browser) | Places, Content, Expansions, search, filters, favourites, audition, options, file management. |
| S-04 | [Loading MPC2 Projects in MPC3 (official Akai)](https://support.akaipro.com/en/support/solutions/articles/69000873048-mpc-series-loading-mpc2-projects-in-mpc3) | Track/program unification and migration behavior; useful for terminology, not a demand for MPC2 compatibility in our format. |
| S-05 | [Output Routing Basics (official Akai)](https://support.akaipro.com/en/support/solutions/articles/69000868280-akai-pro-mpc-series-output-routing-basics) | Difference between track/channel strips, Pad Mixer, Channel Mixer and context-sensitive strip views. |
| S-06 | [Editing the Count-In and Metronome (official Akai)](https://support.akaipro.com/en/support/solutions/articles/69000857890-akai-pro-mpc-series-editing-the-count-in-and-metronome) | Metro, count-in, rate, sound, volume and routing. |
| S-07 | [Tuning Basics (official Akai)](https://support.akaipro.com/en/support/solutions/articles/69000874731-akai-mpc-series-tuning-basics) | Track Edit entry by waveform double-tap, eight sample layers, per-pad layer tuning versus Global tuning, plugin tuning context. |
| S-08 | [Saving Projects with Auto Save (official Akai)](https://support.akaipro.com/en/support/solutions/articles/69000864617-akai-pro-mpc-series-saving-projects-with-auto-save) | Menu > Save > Project / Project As, Auto Save settings and saved project data. |
| S-09 | [New Project Dialog (official Akai)](https://support.akaipro.com/en/support/solutions/articles/69000857945-akai-pro-mpc-series-new-project-dialog) | New Project behavior and loading defaults. |
| S-10 | [Saving/backing up projects to an external drive (official Akai)](https://support.akaipro.com/en/support/solutions/articles/69000860953-mpc-standalone-the-best-way-to-save-and-back-up-projects-to-an-external-drive) | Project file and companion project-data folder behavior on MPC hardware. Do not copy this proprietary format as an assumption for our codec. |
| S-11 | [How to Save Samples (official Akai)](https://support.akaipro.com/en/support/solutions/articles/69000858918-mpc-series-how-to-save-samples) | Save Sample and Save Current Sample/Program workflows. |
| S-12 | [Reassigning Q-Links in a Program (official Akai)](https://support.akaipro.com/en/support/solutions/articles/69000858717/) | Q-Link Edit entry, program/track context, reassignment of parameters. |
| S-13 | [Pitch Bend without a MIDI Keyboard (official Akai)](https://support.akaipro.com/en/support/solutions/articles/69000872665-akai-pro-mpc-series-how-to-pitch-bend-without-a-midi-keyboard) | Example of assigning a controllable pitch parameter and Q-Link ranges/response. Applies to supported program types; not a universal control rule. |
| S-14 | [MPC Live III FAQ (official Akai)](https://support.akaipro.com/en/support/solutions/articles/69000868537-akai-pro-mpc-live-iii-frequently-asked-questions) | Supplementary current MPC Step Edit vocabulary such as Velocity, Ratchet, Probability and Nudge Ticks. This is not standalone MPC One hardware proof; use S-01 for the target platform when available. |
| S-15 | [Understanding/loading Programs (Akai support reference indexed in repository)](https://support.akaipro.com/en/support/solutions/articles/69000804211-akai-pro-mpc-%E3%82%B7%E3%83%AA%E3%83%BC%E3%82%BA%EF%BD%9C%E3%83%97%E3%83%AD%E3%82%B0%E3%83%A9%E3%83%A0%E3%81%AB%E3%81%A4%E3%81%いて) | Program as instrument/container, pad assignment differences by program/track type, program loading and editing context. |
| S-16 | [MPC Studio MkII MIDI/SysEx charts (community reverse engineering)](https://github.com/bcrowe306/MPC-Studio-Mk2-Midi-Sysex-Charts) | Primary non-manufacturer reference for button/pad input, LED CC, RGB SysEx, Touch Strip, Jog Wheel and LCD packet research. Protocol facts only; physical test remains authority. See [controller traceability](MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md). |
| S-17 | [MPC Studio MkII Ableton remote script (community reference)](https://github.com/gstepniewski/MPC-Studio-Mk2-Ableton-Midi-Remote-Script) | Optional hardware-message cross-check only. Explicitly exclude its Ableton/DAW commands, navigation and workflow from our app. |
| S-18 | [Akai Pro MPC Studio MKII FAQ](https://support.akaipro.com/en/support/solutions/articles/69000799777-akai-pro-mpc-studio-mkii-frequently-asked-questions) | Official controller hardware facts: USB MIDI, bus power, TRS MIDI In/Out and Touch Strip intended uses. Ignore desktop setup, plug-in, licensing and bundled-content instructions. |
| S-19 | [Akai Pro MPC Studio MKII Troubleshooting Guide](https://support.akaipro.com/en/support/solutions/articles/69000800207-akai-pro-mpc-studio-mkii-troubleshooting-guide) | Hardware discovery/connectivity and generic MIDI-monitor troubleshooting context. Ignore desktop driver/updater walk-throughs unless investigating the controller firmware itself as a separate task. |
| S-20 | [MPC Studio Academy playlist (Akai-linked YouTube)](https://www.youtube.com/playlist?list=PLyawkFIWnO1PkLpXAN1cciuvoXbrt7M9U) | Candidate tutorial evidence only for visible/explicit physical gestures or feedback. Full transcripts not reviewed in the 2026-10-18 pass; do not claim specific video findings without title/timestamp. |
| S-21 | [MPC Studio Masterclass playlist (Akai-linked YouTube)](https://www.youtube.com/playlist?list=PLyawkFIWnO1O6ipwAPUWnZgA5wxp2g0fL) | Candidate source for clearly observable controller behavior; exclude MPC Desktop and DAW steps. Full transcripts not reviewed in the 2026-10-18 pass. |
| S-22 | [Akai-authored MPC Studio MkII Quickstart Guide (third-party PDF mirror)](https://imagescdn.juno.co.uk/manual/846279-01U.pdf) | Physical control labels and printed gesture hints. Manufacturer-authored but mirrored rather than Akai-hosted; do not treat host-specific screen operations as product requirements. |
| S-23 | [MPC Studio Masterclass Full (official Akai Pro video)](https://www.youtube.com/watch?v=TTx7yxS_le4) | Official controller-related tutorial identified from Akai's channel. Its description is an end-to-end music-making walkthrough; use only controller gestures that can be isolated at a verified timestamp. The full transcript was not available in this review, so no control mapping is inferred from it. |


## 4. Firmware research registry

| ID | Repository evidence | How to use it | Limitation |
|---|---|---|---|
| F-01 | [MPC3-FIRMWARE-UI-RESEARCH.md](MPC3-FIRMWARE-UI-RESEARCH.md) | Best first entry for useful UI findings from the supplied MPC-3.9.1-Gen1 image: Help.json, Browser selection-versus-load text, Grid help, Inspector, toolbar, mixer scope, bank panel, Q-Link and component actions. | Static evidence for Gen1 firmware version 3.9.1.2. It is not an executed runtime trace and does not establish Gen2 behavior. |
| F-02 | [MPC-3.9.1-GEN1-FIRMWARE-STATIC-ANALYSIS.md](research/MPC-3.9.1-GEN1-FIRMWARE-STATIC-ANALYSIS.md) | Details source identity, filesystem paths, resource inventory, TUI schemas and binary string/type-information anchors. | No function-level decompilation, running firmware, complete physical-screen captures or live hardware interaction trace in the recorded pass. |
| F-03 | [MPC-3.9.1-GEN1-FIRMWARE-ANALYSIS.json](research/MPC-3.9.1-GEN1-FIRMWARE-ANALYSIS.json) | Machine-readable inventory for targeted follow-up analysis. | Metadata is evidence of names/counts, not automatic proof of behavior. |
| F-04 | Firmware ACVS Help.json, layout JSON and TUI component descriptions, as summarized in F-01 | Investigate exact resource entry/component when a specific UI question is blocked. | Original extracted resources reside in the private research package, not as a public resource bundle. Do not repeatedly crawl/extract the whole image. |

Key firmware-supported clues to use selectively:
- The File Browser help distinguishes a single click that selects a file from a double-click that loads it. Exact load destination, error/confirmation sequence and assignment effects are not fully established by that entry.
- The Grid help supports insert/delete/edit capability; it does not establish every gesture or every event-property rule.
- The Inspector help describes sequence/track adjustment; the complete field list is not established by that entry.
- Layout resources describe configured units for shared panels; they are not validated physical pixel measurements of MPC One.
- Structured Program Edit TUI files describe component/page schemas for several program types. The MIDI description is marked partial; do not assume every page schema is complete.
- Binary strings such as Track View, Grid View, Arrange, Record To Arrange and Return to Arrangement are search anchors, not a complete navigation graph.
- Generic Knob action metadata is component-specific. Do not generalize Q-Link/Data or Show Overlay behavior to every parameter control.

## 5. Implementation and acceptance registry

Implementation files are entry points, not a substitute for tracing current callers and state.

| ID | Current implementation/reference | Use |
|---|---|---|
| I-01 | MpcShell.java, MpcMainIconDrawable.java, MpcShortcutRailItemView.java | Shared shell, shortcut rail and visual components. |
| I-02 | MainActivity.java, MpcUiState.java, MpcNavigationController.java, MpcModeRegistry.java | Current route wiring, selected musical state, semantic navigation and availability truthfulness. MainActivity remains a migration scaffold; do not replace it with a new monolith. |
| I-03 | MpcBrowserView.java, MpcBrowserFile.java, MpcLocalAudioCatalog.java, MpcDocumentTreeCatalog.java | Browser presentation, local-file model/provider and user-folder discovery. |
| I-04 | MpcArrangeView.java; native MpcArrangementOps.h and MpcPatternOps.h | Arrangement interaction projection and native Pattern/placement operations. |
| I-05 | MpcTrackEditView.java, MpcMainMixerStripView.java, MpcPadMixerView.java | Track Edit, visible channel-strip region and Pad Mixer subset. |
| I-06 | MpcSequenceStepPagePolicy.java, MpcSequenceZoomPolicy.java, MpcArrangeViewTest.java and other Android unit tests | Focus/zoom policies and isolated view/model contracts. |
| I-07 | MpcProjectPersistence.h/.cpp, MpcProjectState.h/.cpp, MpcProjectPersistenceTests.cpp | Native versioned project codec and domain state. The codec is not equivalent to a connected Android Save/Open user workflow. |
| I-08 | NativeEngine.cpp, AudioEngine.cpp/.h, project native tests | Existing semantic/native audio, sampler and sequencer paths. Inspect relevant methods and preserve realtime boundaries. |
| I-09 | tests/android_ui_smoke.sh and .github/workflows/android-build.yml | Real Android emulator touch acceptance, UI hierarchy/screenshots and full integration workflow. |
| I-10 | docs/IMPLEMENTATION-ROADMAP.md and docs/SESSION-STATE.md | Dated live task queue and last checkpoint. Verify claims against current GitHub HEAD and exact-SHA Actions before reporting. |

## 6. Global shell, transport and focus actions

Manual anchors: S-01 Toolbar p.57, Main p.15, Function Buttons section, Preferences pp.61–65. Firmware clues: F-01 Help/Toolbar/StatusBar/Inspector and transport strings. Product layout contract: I-01/I-02.

| ID | User action | Expected reference result / state contract | Current app mapping and status | Acceptance path |
|---|---|---|---|---|
| G-001 | Open Menu from the toolbar | Navigate to the mode launcher; selected musical context/transport must not be silently reset. | Menu route exists; check current route table in I-02. | Runtime menu navigation; confirm return and selected Sequence/Track remain coherent. |
| G-002 | Open Browser from toolbar/shortcut | Enter Browser with current target context preserved. | Real Browser route; local workflow is partial/implemented in slices. | Select a WAV, return to intended context and verify target Pad/Layer. |
| G-003 | Start/stop transport | Play/Stop controls and physical transport act on the same sequencer state; transport must continue when the user navigates between views unless the user stops it. | Existing sequencer/transport path; preserve it during UI work. | Check state before/during/after navigation and inspect native transport state. |
| G-004 | Start/stop recording | Record control must enter/exit the supported recording workflow, not merely change its label. | Main RECORD opens Sampler; recorder subset exists. | Record a fixture, stop and check actual sample/state outcome. |
| G-005 | Change Timing Correct | Open/apply the supported timing-correct settings and route values to the real sequence semantic. | Existing dialog and native setters are recorded in the reference index. | Change a supported value and verify sequence state and repeat entry. |
| G-006 | Open Metro / Count-In settings | Configure metronome enable, count-in, rate, sound, volume and routing per available backend. | GAP: project knowledge base says Metro is not backed by a real metronome backend. Do not imply availability from the toolbar icon. | Not complete until settings and audible/state behavior are implemented and tested. |
| G-007 | Toggle automation / open automation context | Expose contextual automation and edit real automation state. | GAP / RESERVED until semantic automation exists. | No fake automation button behavior; add acceptance only after backend contract exists. |
| G-008 | Read MIDI IN/OUT status and open MIDI/Control | Status must reflect actual MIDI ports/state, and navigation opens a real configuration route. | PARTIAL: MIDI page/routing exists; full management not established. | Connect/disconnect real supported MIDI endpoint and verify status/route. |
| G-009 | Change one of the five shortcuts | Keep exactly five canonical high-frequency shortcut slots and navigate to the configured destination. | Five-slot rail exists; configuration/persistent reorder should be checked against current implementation. | Verify all five slot identities, selection state, and persisted customization if offered. |
| G-010 | Change focused field with Data Dial or +/- | Focused field is explicit and only its corresponding semantic value changes; field selection is not a duplicate local editor. | Sequence/Track/Program and some parameter focus paths exist; coverage varies by context. | Focus field, turn dial/press +/- and inspect authoritative state plus focus indication. |
| G-011 | Open contextual Function Bar action | Bottom actions change by context and expose only meaningful/enabled operations. | Function Bar exists; arrangement actions are backed by native operations. | Verify available/enabled actions for no-selection, valid-selection and invalid-operation states. |
| G-012 | Hide/show XL Channel Strip region | Show/hide changes the actual strip region while preserving the workspace and selected context. | Shell show/hide and restore affordance exist; Main geometry fix has emulator evidence. | Check bounds, old duplicate mixer absence and retained focus/context. |
| G-013 | Use Back/return from a deep context | Return to the meaningful prior context and preserve transport/Sequence/Track/Pad/Layer where expected. | PARTIAL: navigation history is primarily Mode-based; complete subcontext/focus restoration is a known refinement. | Cover every implemented deep route and verify selected state and Back semantics. |
| G-014 | Follow playhead / current time-counter behavior | When supported, the playhead/time display follows its documented mode rather than changing musical state. | Firmware Help mentions Follow Now modes, but applicability may be shared/host UI; exact Standalone behavior needs corroboration. | UNKNOWN until confirmed for MPC One and mapped to an explicit product requirement. |

## 7. Main Mode actions

Manual anchor: S-01 Main p.15 and Tracks p.45; S-02; S-07. Firmware clues: F-01 recovered context vocabulary and Main-related string/type-info anchors. Product blueprint: I-02 and MPC3-UI-SCREEN-BLUEPRINT.md.

| ID | User action | Expected result / state contract | Current app mapping and status | Acceptance path |
|---|---|---|---|---|
| MAIN-001 | Select Sequence | Selected Sequence changes in the shared project model; return to Main shows the same selection. | Implemented in the documented routing matrix. | Select a second Sequence and verify display, data and return. |
| MAIN-002 | Change BPM | Data Dial/+/- modifies the selected Sequence tempo through its semantic setter. | Implemented for the current supported field. | Change tempo, reopen context and check authoritative Sequence state. |
| MAIN-003 | Change sequence bars/length | Update the supported Sequence length; do not confuse this with Arrangement clip length. | Implemented for the supported Sequence model. | Change length and verify all relevant displays/state. |
| MAIN-004 | Set loop START/END | Edit loop bounds in Sequence time units. | Native sequence loop setters exist. | Change bounds, navigate away/back and check exact values. |
| MAIN-005 | Toggle LOOP | Enable/disable the actual Sequence loop. | Implemented through native Sequence state. | Toggle twice and inspect state, including transport interaction. |
| MAIN-006 | Select Track | Change selected Track once, update Track header/strip/context consistently. | Native Track selection path exists. | Select adjacent Tracks through touch and hardware-equivalent path; verify no stale Program/Layer display. |
| MAIN-007 | Select a Track-owned Program | Display/select the Program belonging to the selected supported Track type. | Implemented for Drum Program subset; non-Drum breadth is partial. | Change Track then verify Program ownership and actual assigned pad/program state. |
| MAIN-008 | Choose/change Track Type | MPC3 guide describes Audio, Drum, Keygroup, Plugin, MIDI and CV types. A change requires a real conversion/create command and safe state transition. | RESERVED: existing-track Track Type setter is not established. Do not route a tap to a fake chooser or equate Clip with Audio. | First reconcile chooser vocabulary with S-01 and firmware selector assets; test actual domain conversion before enabling. |
| MAIN-009 | Select Pad and Pad Bank | Hardware-selected Pad/Bank and UI selection refer to the same target. | Pad state/controller routing exists; current physical confirmation remains pending. | Emulator/model test for state plus separate physical MkII pad/bank test. |
| MAIN-010 | Select Sample Layer | Selected Layer follows shared context when entering Track Edit or returning to Main. | Partial, backed by existing layer state. | Switch layer, enter/exit editor and confirm state preservation. |
| MAIN-011 | Double-tap waveform / open Track Edit Samples | Open the contextual Track Edit view for the selected pad/layer, not a random/default target. | Route exists; tuning workflow has emulator acceptance. | Open from waveform and verify same Track/Pad/Layer and sample identity. |
| MAIN-012 | Browse for a sample from an empty Pad | Open Browser with intended assignment target preserved. | Real local Browser load route exists; end-to-end return/picker edge cases require exact-SHA re-verification where relevant. | Pick a real file and verify target identity after return. |
| MAIN-013 | Open Sampler with RECORD | Enter recording UI and connect controls to actual recorder state. | Recorder subset implemented; inspect current native bridge for exact supported actions. | Record → Stop → Assign → verify sample on intended Pad/Layer. |
| MAIN-014 | Switch Track View ↔ Arrangement view in Main | Change presentation of the same selected Track/Sequence; do not create duplicate state. | Both presentations/routes exist; Arrangement workflows have current evidence. | Toggle several times while keeping selected Track, loop and transport stable. |
| MAIN-015 | Main Function Bar: New Track | Add a genuine Sequence Track and refresh selection/list. | Native add-track operation is recorded as available. | Add a Track and verify count, identity and selected Track. |
| MAIN-016 | Main Function Bar: Rec Arm | Toggle real recording-arm state on selected Track. | Implemented for the current Track state. | Toggle, navigate and verify state; physical LED feedback separately. |
| MAIN-017 | Main Function Bar: Track - / Track + | Select adjacent Track using the shared selected Track context. | Implemented for current Track-selection semantic. | Exercise lower/upper boundaries and confirm no invalid index. |
| MAIN-018 | Main Function Bar: Mute | Toggle selected Track mute in real Sequence state. | Implemented in Main/Track View. | Toggle and inspect the shared state; audio behavior must be tested if claimed. |
| MAIN-019 | Main Function Bar: Solo | Solo selected Track. | RESERVED: no established semantic solo backend. | Keep unavailable until the domain/audio route exists; never simulate it with Mute. |
| MAIN-020 | Edit TRANSPOSE | Change musical transposition using a real sequence/program semantic. | RESERVED / visibly unavailable in current contract. | Implement only after identifying the correct scope and native setter. |

## 8. Menu and mode navigation

Manual anchor: S-01 Menu p.56, Shortcuts and Function Buttons sections. Firmware clues: F-01 mode vocabulary and binary strings. Product decision: retain factory shortcut identity while being truthful about unavailable backends.

| ID | User action | Expected result / product decision | Current app mapping/status | Acceptance path |
|---|---|---|---|---|
| MENU-001 | Open Menu | Show the mode launcher without changing musical state. | Route exists; current latest menu smoke must be checked on exact HEAD. | [Runtime Menu navigation test](https://github.com/MiguelDuval/mpc-mk2-groovebox/commit/8e4e52e448017c4129dc96207b66aee3cb8e0a53) added a navigation trace; Build #2816 was in progress at catalog creation. |
| MENU-002 | Enter Main, Track View, Browser, Grid, Step, Track Edit, Sample Edit, Sampler, Arrange, Pad Mixer or Next Sequence | Each entry opens its actual semantic context and retains valid project selection. | Routes exist; individual capabilities vary by context. | Check action-specific acceptance row and exact current source/tests. |
| MENU-003 | Open Channel Mixer shortcut/context | Open a true multi-channel mixer only when level/pan/routing/send/insert state exists. | RESERVED; factory shortcut identity remains Channel Mixer. | Verify it reports unavailable rather than opening unrelated UI. |
| MENU-004 | Open Sounds shortcut/content destination | Browse genuine available local content only. | RESERVED for factory Sounds/expansion backend; local Browser categories can be implemented independently. | Do not fabricate sample/preset rows. |
| MENU-005 | Open XY/XYFX | Open live XY performance/effect macro only when its semantic and FX backend exist. | RESERVED. | Keep visibly unavailable until the complete control-to-parameter path is real. |
| MENU-006 | Open 16 Levels | Map all 16 pads to velocity levels for a selected source pad. | RESERVED; performance model not established. | Test source pad, all levels and actual velocity output before enabling. |
| MENU-007 | Open Pad Perform | Apply documented note/chord/scale/performance mapping. | RESERVED. | Do not imply chord/scale functionality from a decorative page. |
| MENU-008 | Open List Edit | Show list-based event inspection/editing and mutations. | RESERVED; dedicated list-editor backend not established. | Requires real event list, value setters and edit-history acceptance. |
| MENU-009 | Open Project/New/Save/Open from Menu | Expose real lifecycle actions and confirmation where needed. | PARTIAL at domain level; native codec exists but app Save/Open UI route is the active gap. | Use PROJ-001 through PROJ-008 acceptance criteria. |

## 9. Browser and local-file actions

Manual anchor: S-01 Browser p.20; S-03; S-11. Firmware clue: F-01 Help.json distinguishes selection from load. Product rule: Android Document Provider is a storage primitive below Browser, not the product Browser itself.

| ID | User action | Expected result / state contract | Current app mapping/status | Acceptance path |
|---|---|---|---|---|
| BROW-001 | Enter Browser and choose Places / Content / Expansions | Change semantic category/location and refresh relevant navigation/results. | Section navigation exists; results/provider depth varies. | Each section must show its own valid navigation and not stale entries from another section. |
| BROW-002 | Navigate Places: Internal, MPC Documents, Connected Storage | Show actual accessible locations, not invented folders. | Local/Document-tree providers exist in source; verify each provider's permission/error states. | Test location change with real files and empty/error states. |
| BROW-003 | Navigate local Content categories: Drums, Instruments, Samples, Demos, My Files | Filter or query real local content matching the category. | PARTIAL; generic categories must not masquerade as working results without a provider. | Seed distinct file types and verify category-to-results relation. |
| BROW-004 | Navigate Expansions / User Content | Show local user content only when an actual provider/path exists. | PARTIAL / provider-dependent. | Verify accessible/empty/unavailable folders truthfully. |
| BROW-005 | Enter search text / clear search | Search filters actual semantic Browser results; Clear restores them. | Local WAV search is implemented and historically emulator-tested. | Positive match, negative match, clear and selection-state smoke. |
| BROW-006 | Apply Projects, Patterns, Kits, Plugin Presets, Samples or All filter | Filter changes actual supported results. | Local file filtering is implemented for supported catalog; broader formats remain partial. | Verify sample matches/no-match and unsupported types are not falsely shown as loadable. |
| BROW-007 | Choose a result | Selection/highlight changes, but does not imply that the file has been loaded or assigned. | Browser selection state exists. Firmware Help supports select-vs-load distinction (F-01); runtime details are not fully traced. | Assert selected identity separately from current sample/program identity. |
| BROW-008 | Audition/preview a selected WAV | Hear the selected real file through a supported playback path; selection and destination are not conflated. | Genuine local WAV audition has historical emulator evidence via MediaPlayer. | Play the selected file, switch selection and verify actual preview changes; stop/release playback as required. |
| BROW-009 | Load/assign the selected WAV | Update the intended Pad/Layer through the sampler/domain loader. | Native sample assignment and a local WAV workflow have historical emulator evidence. | Verify sample identity and target Pad/Layer after assignment and context return. |
| BROW-010 | Preserve Pad/Layer target while opening/returning from system picker | A picker result must return to the product Browser/load flow without losing the intended musical destination. | PARTIAL: Document UI/folder interactions exist, but acceptance must be checked against current exact HEAD. | Select a real WAV in Android picker; return; verify destination and actual assignment, including cancel behavior. |
| BROW-011 | Configure a user folder / persistent favourite 1–5 | URI/folder permission and favorite identity survive required process restart. | USER FOLDER and FAVORITE 1 component acceptance passed on exact SHA `c721e93fde80e9bb0e45b94ce375ec17e20b2692` in Build #3048 / run `39671703344`: real provider-root and child selection, matched fresh root PNG/XML, separate persisted URI/preference keys, Browser WAV result and force-stop/relaunch restoration. The overall workflow failed later at Project Menu capture; that does not invalidate this component result. | Preserve matched root/child evidence and real SAF grant/file/filter/restart checks; full Project SAVE/SAVE AS/OPEN remains a separate pending gate. |
| BROW-012 | Open Browser Options | Change real options such as metadata visibility, search-subfolder behavior or visibility of system/project-data folders only when implemented and relevant. | GAP/PARTIAL; source index lists Browser Options as incomplete. | Each option must demonstrably change query/presentation; omit unavailable decorations. |
| BROW-013 | Show/hide metadata or project-data folders | Change result metadata/visibility based on a persisted setting where required. | Not established as complete for product Browser. | Verify setting effects on real results and restoration after reopen. |
| BROW-014 | Choose Sample Assign / load to pool | Use an actual sample-pool/assignment model distinct from generic file highlighting. | RESERVED until real pool semantics exist. | Do not label generic selection as Sample Assign. |
| BROW-015 | Browse Splice or paid expansion storefront | Official reference taxonomy may mention this, but product must remain local-first. | REFERENCE ONLY — intentionally excluded. | Confirm no subscription, store or upsell route is present in product UI. |
| BROW-016 | Handle empty results, permission denial, inaccessible drive or cancel | Preserve state, explain the real failure, and allow safe retry/back. | Required acceptance behavior; per-provider coverage must be checked in source/tests. | Test empty, denied, removed-volume, cancel and successful paths without stale selection. |

## 10. Track View and track-state actions

Manual anchor: S-01 Tracks p.45 and Track View descriptions; S-02. Current semantic sources: I-02/I-08.

| ID | User action | Expected result / state contract | Current app mapping/status | Acceptance path |
|---|---|---|---|---|
| TRK-001 | Select a Track row | Selected Track changes across Main/Track View/Arrange/Editor and does not create separate per-screen selection. | Track selection path exists. | Select by touch and adjacent Track controls; verify all visible contexts agree. |
| TRK-002 | Create a Track | Add a real Track in current Sequence with correct identity/type/state. | Implemented for current model. | Verify count, selection, and valid defaults. |
| TRK-003 | Toggle REC ARM | Update track-arm state. | Implemented in supported subset. | Re-entry check plus hardware feedback test if claimed. |
| TRK-004 | Toggle Mute | Update Track mute state. | Implemented in Main/Track View. | Shared-state assertion; real audio mute test if the claim includes audible behavior. |
| TRK-005 | Toggle Solo | Solo a Track independently of Mute. | RESERVED; no real semantic backend established. | Do not alias to mute. |
| TRK-006 | Move Track selection with Track +/- or Data Dial | Select a valid adjacent Track and update shared context. | Implemented for selection subset. | Check first/last Track bounds and active focus. |
| TRK-007 | Duplicate/Delete Track | Mutate Sequence Track list and reconcile dependent Program/sample state. | Not established by current action evidence; treat as GAP until source and test confirm. | Require native operation, index normalization, Undo/Redo as designed and no stale references. |
| TRK-008 | Open Track Settings / I/O / routing | Expose only actual Track settings and supported routing values. | PARTIAL / RESERVED for unsupported I/O and broader routing. | Validate every displayed editable field against a real setter/provider. |

## 11. Arrangement actions

Manual anchor: S-01 Linear Arranger p.21, Tracks p.45 and Function Buttons. Firmware clues: F-01 Help and binary strings for Arrange, Track View, Grid View, zoom-at-playhead and Return to Arrangement. Current model: I-04 and I-08.

| ID | User action | Expected result / state contract | Current app mapping/status | Acceptance path |
|---|---|---|---|---|
| ARR-001 | Enter Arrangement from Main/Menu | Show the selected Sequence/Track timeline and keep shared selection/transport. | Implemented presentation/route; partial product scope remains. | Enter/exit and verify same Sequence, Track and transport. |
| ARR-002 | Select a Track lane | Select its real Track and use that context for lane actions. | Implemented in current Arrangement projection. | Touch lane; inspect selected Track and subsequent action targets. |
| ARR-003 | Navigate/zoom horizontal timeline | Update viewport/cursor scale without modifying musical events. | Zoom policy exists; verify all current zoom buttons/gestures. | Assert event ticks stay unchanged while viewport changes. |
| ARR-004 | Navigate/zoom vertically or fit view | Change visible lanes/range without changing event data. | Partial; test against current MpcSequenceZoomPolicy and live view. | Check first/last lanes and geometry at supported orientations. |
| ARR-005 | Move loop brace/end | Change Sequence loop bounds, not event/clip content. | Real loop-brace drag/restore is emulator-verified in Build #2781 (historical exact SHA listed in roadmap). | Drag end, assert loop state; restore original and verify. |
| ARR-006 | Select a MIDI event | Resolve the actual event identity; selection is distinct from an empty insertion cursor. | Implemented and emulator-tested. | Tap event and verify Track/tick/note selection from native projection. |
| ARR-007 | Move event by one quantize-grid interval | Change only destination tick; preserve pitch, duration, velocity, probability, ratchet and nudge; reject collision/bounds errors. | MOVE ± GRID is emulator-verified in Build #2781. | Move forward/back and assert full event equality except tick; reject invalid target. |
| ARR-008 | DUP selected event | Add a new native event preserving musical fields and respecting Pattern bounds/collisions. | Implemented and verified with Undo/Redo in Build #2785 / #2791. | DUP → UNDO → REDO → UNDO; compare serialized Pattern to baseline. |
| ARR-009 | COPY selected event | Put a value-owned copy into native clipboard without changing Pattern. | Implemented and verified in Build #2789 / #2791. | COPY must leave Pattern unchanged and set valid clipboard identity/value. |
| ARR-010 | CUT selected event | Remove exactly the selected event and store its complete value for Paste; undo restores it. | Implemented and verified in Build #2791. Some older knowledge-base text is stale about CUT; current roadmap and exact-SHA test record take precedence. | CUT → UNDO → REDO → UNDO and compare exact Pattern state. |
| ARR-011 | PASTE copied/cut event onto selected event target | Place copied event at the documented target/grid rule while preserving all musical fields except destination tick. | Implemented and verified for selected-event path. | PASTE + Undo/Redo and compare event values. |
| ARR-012 | Tap empty timeline to set paste cursor | Select an insertion target without selecting/fabricating a MIDI event; cursor snaps to grid and remains within Pattern. | Implemented and verified in Build #2793. | Empty tap must leave Pattern unchanged; verify accessible Track/tick cursor label. |
| ARR-013 | PASTE at empty cursor | Paste exactly at cursor tick when clipboard and valid cursor coexist. | Implemented and verified in Build #2793. | CUT → empty cursor → PASTE → UNDO → REDO → UNDO → restore CUT; verify exact source state. |
| ARR-014 | Place, move, split, delete or duplicate Pattern placement | Operate on explicit PatternPlacement identity/start/length/source offset, distinct from editing notes inside the Pattern. | Native operations and real-touch placement/edit scenarios completed; Build #2807 and regression Build #2808 are documented. | Create multiple placements for one Pattern, edit placement only, verify source MIDI unchanged and edit history. |
| ARR-015 | Save/reopen Arrangement placement state through Project UI | Restore placement identity, Pattern link, timeline position, length and source offset after a real file open. | VERIFIED by Android Build #2856, run 39162451542, exact SHA 617511efb820e96c6d9576051fa30012a3368db1; real SAF Save/Open plus force-stop/relaunch produced exact canonical Project SHA with MIDI fields and placement tuple intact. | Keep this covered by the full lifecycle regression; reverify at each changed test/app SHA. |
| ARR-016 | Double-tap event/overview to open Grid | Drill into the appropriate supported editor while retaining selected Track/event/time as specified. | Route exists for supported Drum workflow; non-Drum support is partial/unavailable. | Confirm return to Arrangement restores context and does not lose transport. |
| ARR-017 | Record To Arrange / record arrangement performance | Capture real supported performance into Arrangement/Sequence state. | Firmware strings are evidence anchors only; complete UI/backend route not established. | UNKNOWN/GAP until a documented Standalone workflow and a working recorder route are verified. |

## 12. Grid actions

Manual anchor: S-01 Grid section; F-01 help describes Grid event insert/delete/edit but not exact gesture mapping. Implementation: grid is currently a Drum-focused custom Canvas plus native Sequencer state.

| ID | User action | Expected result / state contract | Current app mapping/status | Acceptance path |
|---|---|---|---|---|
| GRID-001 | Open Grid for a supported Drum Track | Show the event grid for selected Track/Sequence and correct pad. | Implemented route for supported Drum tracks. | Open from Menu/Arrangement, check Track/Pad/context, then Back. |
| GRID-002 | Draw/add a note | Insert a native event at the selected Pad/time with defined velocity/default parameters. | Real touch addition has emulator evidence in Build #2781. | Add Pad 01 / Step 34, verify event state and that Step 33 stays unchanged. |
| GRID-003 | Erase a note | Remove exactly the selected/target note, leaving unrelated events unchanged. | Real touch erase has emulator evidence in Build #2781. | Draw then erase temporary note and compare seeded Pattern state. |
| GRID-004 | Select an event/cell | Selection projection comes from authoritative native state, not duplicate Canvas state. | Implemented for selected-cell projection. | Verify hit target, selected Pad/time/note and cross-view consistency. |
| GRID-005 | Change zoom/magnification | Modify viewport/visible grid density without changing note data. | Zoom policy exists; complete gestures should be checked individually. | Zoom in/out and assert event identity/timing unchanged. |
| GRID-006 | Navigate time/pad range | Move viewport or selected Pad/range and preserve event content. | Partial; current Drum-focused scope. | Check boundary navigation, pad identity and redraw. |
| GRID-007 | Edit event parameters from Grid | Modify only the intended musical field on the selected active cell through native commands. | Native-backed PARAM cycling routes VEL / PROB / RAT / NUDGE / DUR ± to the existing native setters. All five fields passed real-touch isolation/restore in Android Build #2893 / run 39232129363 on exact SHA 0d0558704ed73f8c6c27c528178cc049858d2386. Drum-only. | Real-touch DRAW → SELECT → one-field mutation/restoration for each supported parameter, then ERASE and verify empty-state controls disable; unrelated fields remained exact. |
| GRID-008 | Use melodic piano-roll Grid for Keygroup/Plugin/MIDI/CV | Display/edit the appropriate pitch/time representation. | GAP/RESERVED for non-Drum editors in current contract. | Do not route unsupported Track Types into the Drum editor. |

## 13. Step Sequencer actions

Manual anchor: S-01 Step Sequencer p.29; S-14 is supplementary step-parameter vocabulary, not canonical MPC One hardware evidence. Implementation: I-06/I-08.

| ID | User action | Expected result / state contract | Current app mapping/status | Acceptance path |
|---|---|---|---|---|
| STEP-001 | Enter Step Sequencer and select Pad | Show 16-step view for intended Pad and current Sequence/Track. | Real-touch PAD + / PAD − round-trip passed Android Build #2901 / run 39242015786 on exact SHA `a681303fe3d411c0df99d4ce7dc90e8f2caaac76`. | Selected Pad 2, retained absolute Step 02 / EMPTY with the matching visible cell, returned to Pad 1, and preserved Step 01 plus the unchanged Pattern. Pad 2/Pad 1 screenshots and accessibility XML archived. |
| STEP-002 | Select current bar/page | Change visible bar/step page without corrupting event ticks or selection. | Real-touch page 1 → page 2 → page 1 acceptance passed Android Build #2900 / run 39236521987 on exact SHA `a285ed9bcdb3426e71f85a600838f6589bc121d9`. | The selected absolute cursor moved Step 02 → Step 18 → Step 02 while the corresponding cells and full Pattern state remained unchanged. Screenshots and accessibility hierarchies archived. |
| STEP-003 | Add a step/event | Create event at the selected step for the selected Pad. | Real touch add/edit/remove flow is emulator-verified in Build #2781. | Add a temporary step and inspect native state. |
| STEP-004 | Remove/erase a step | Delete only the intended event. | Real touch remove flow is emulator-verified in Build #2781. | Erase temporary step; preserve other seeded events. |
| STEP-005 | Edit Velocity | Update only the selected event velocity. | Real-touch VEL field-isolation and exact inverse restore passed Android Build #2900 / run 39236521987 on SHA `a285ed9bcdb3426e71f85a600838f6589bc121d9`. | VEL 100 → 101 → 100; the complete native VEL/PROB/RAT/NUDGE/DUR projection returned exactly to baseline and Step 01 remained unchanged. Screenshot and accessibility evidence archived. |
| STEP-006 | Edit Probability | Update the selected event probability via the actual parameter/decrement control. | Real-touch PROB field-isolation and exact inverse restore passed Android Build #2900 / run 39236521987 on SHA `a285ed9bcdb3426e71f85a600838f6589bc121d9`. | PROB 127 → 126 → 127; VEL/RAT/NUDGE/DUR, selected Step identity and Step 01 remained unchanged. Changed/restored screenshots and hierarchies archived. |
| STEP-007 | Edit Ratchet | Change note retrigger count/timing only for selected event. | Real-touch acceptance passed Android Build #2893 / run 39232129363 on exact SHA 0d0558704ed73f8c6c27c528178cc049858d2386. | Touch PARAM → RAT, change exactly one supported increment, verify VEL/PROB/NUDGE/DUR and Step 01 remain unchanged, then inverse-edit to restore all five fields. Native ratchet-expander tests cover tick expansion; physical/audio-output audition remains separate. |
| STEP-008 | Edit Nudge Ticks | Shift event timing at its supported semantic resolution without accidental note loss. | Real-touch Step Edit NUDGE and Grid NUDGE field-isolation/restore passed Android Build #2893 / run 39232129363 on exact SHA 0d0558704ed73f8c6c27c528178cc049858d2386. | Step NUDGE + moved 60 ticks, preserved all unrelated fields and selection, then returned to baseline. Grid NUDGE 0 → 60 → baseline restored with all five fields exact; Step 01 remained unchanged. |
| STEP-009 | Edit Duration | Change selected event duration without changing pitch/other fields. | Real-touch Step Edit DURATION and Grid DURATION field-isolation/restore passed Android Build #2893 / run 39232129363 on exact SHA 0d0558704ed73f8c6c27c528178cc049858d2386. | Step DUR + increased by one quarter-grid increment and restored baseline without changing velocity/probability/ratchet/nudge; Grid DUR 240 → 300 → baseline restored with all five fields exact; Step 01 remained unchanged. |
| STEP-010 | Use physical pads as step buttons | Map pads to steps consistently in the relevant Step context. | Controller policy/semantic routing exists; real physical-device acceptance remains pending. | Separate automated decoder/policy checks from physical MkII test. |

## 14. Track Edit, Program Edit and instrument parameters

Manual anchors: S-01 Tracks p.45; S-07 tuning article; S-12/S-13 for Q-Link and supported Program controls. Firmware evidence: F-01 structured Program Edit TUI schemas. Drum/Keygroup schemas are richer than partial MIDI/plugin descriptions. Do not infer support for all program types from one schema.

| ID | User action | Expected result / state contract | Current app mapping/status | Acceptance path |
|---|---|---|---|---|
| EDIT-001 | Open Track Edit from selected pad/waveform | Open editor for the current Track/Pad/Layer and preserve the target on exit. | Implemented for current sample-edit subset. | Entry/exit test with non-default Track/Pad/Layer. |
| EDIT-002 | Select Samples/Global or supported editor tab | Switch parameter scope without changing musical target unexpectedly. | Partial; implement only tabs with true backing state. | Verify per-layer versus Global scope and return. |
| EDIT-003 | Change layer TUNE | Adjust the selected sample layer tuning in semitones. | Verified by real-scroll/touch Track Edit flow in Build #2781. | One-semitone change, leave/re-enter, confirm value, restore fixture. |
| EDIT-004 | Change layer gain/level | Adjust actual sample-layer gain. | Real-touch field-isolation/restore passed Android Build #2902 / run 39244891264 on exact SHA `b2aa9cacf21a99b1ad51ef4046e55e9537a8754a`. | GAIN 100% → 90% → 100%; TUNE/PAN unchanged; inverse touch restored exact original value. Screenshots and hierarchies archived. Audible gain verification remains separate from UI-state acceptance. |
| EDIT-005 | Change layer pan | Adjust actual sample-layer pan. | Real-touch field-isolation/restore passed Android Build #2902 / run 39244891264 on exact SHA `b2aa9cacf21a99b1ad51ef4046e55e9537a8754a`. | PAN C → L100 → C through real touches; GAIN/TUNE unchanged. Changed/restored screenshots and hierarchies archived. Audible stereo response still requires separate physical/audio validation. |
| EDIT-006 | Change pad Level/Pan/Tune | Mutate selected Pad parameters through the shared semantic audio setters. | Pad Mixer and strip subset support native Level/Pan/Tune. | Verify setters, value display and state persistence through navigation. |
| EDIT-007 | Edit velocity ranges | Change supported layer velocity ranges and keep range state valid. | VERIFIED by Android Build #2907 / run 39286224809 on exact SHA `b0b6e66bd69d051595fcddd8d048b60a38eed06c`. The live-bounds scroll resolver exposed the controls and both real-touch mutations passed. | MAX and MIN each changed by one and inverse-restored; `0 ≤ MIN ≤ MAX ≤ 127`; GAIN/TUNE/PAN unchanged. Changed/restored screenshots and accessibility XML are in UI artifact 12204147729. |
| EDIT-008 | Edit sample Start/End region | Modify the real sample region rather than only moving visual handles. | VERIFIED by Android Build #2907 / run 39286224809 on exact SHA `b0b6e66bd69d051595fcddd8d048b60a38eed06c`. | Real Start and End controls changed one boundary at a time; the opposite boundary stayed fixed; inverse touches restored the exact region and GAIN/TUNE/PAN were preserved. Evidence is in the run's UI artifact. |
| EDIT-009 | Edit amp envelope/filter cutoff | Update real supported pad envelope and filter state through the semantic/native-backed setters. | New real-touch Attack and cutoff round-trip acceptance is in the current feature slice; exact-HEAD CI pending. | Change Attack by 100 ms while preserving Decay/Sustain/Release, inverse-restore; change cutoff by 500 Hz while preserving envelope state, inverse-restore; archive changed/restored screenshots and accessibility XML. |
| EDIT-010 | Use LFO / Modulation / Effects / Edit All Layers | Edit these advanced parameters atomically or per documented scope. | RESERVED: backend/semantic coverage absent for listed capabilities. | Do not draw active controls until a real contract and tests exist. |
| EDIT-011 | Edit Plugin/Keygroup/MIDI/CV program pages | Show controls appropriate to actual supported Track/Program type. | PARTIAL/RESERVED depending on type; firmware TUI schemas inform research but are not complete runtime evidence. | Per-type test; reject unsupported type rather than present Drum controls incorrectly. |
| EDIT-012 | Save Current Sample or Save Current Program | Persist a sample/program to a user-selected location and expose truthful completion/error state. | No complete product UI workflow established by current catalog evidence. | Use S-11 as reference; implement only with a real file operation and round-trip check. |

## 15. Sample Edit and Sampler actions

Manual anchors: S-01 Sampling p.34 and Sample Editing p.36; S-11 for saving samples. The firmware UI index records related page/component vocabulary but does not prove the exact Android gesture contract.

| ID | User action | Expected result / state contract | Current app mapping/status | Acceptance path |
|---|---|---|---|---|
| SAMP-001 | Enter Sampler / begin Record | Start a real capture using current supported input/recording state. | Recorder subset/nativeAudioStartRecording exists. | Start, verify recording telemetry/state, stop safely. |
| SAMP-002 | Stop recording | Finalize captured data and leave recorder in a consistent state. | Supported recording subset exists. | Confirm buffer/sample is valid and no stale recording state remains. |
| SAMP-003 | Toggle input Monitor | Start/stop actual monitoring through audio engine. | Supported native monitor route recorded in knowledge base. | Monitor on/off state and actual audio path test, with no realtime UI work. |
| SAMP-004 | Adjust Threshold | Change the actual sampler threshold. | Native threshold route recorded. | Verify threshold value and threshold-trigger behavior where available. |
| SAMP-005 | Assign recording to Pad/Layer | Assign the captured sample to the chosen destination. | Native recording assignment operation exists. | Record → assign → leave/re-enter Track Edit and verify sample identity. |
| SAMP-006 | Select current sample layer in Sample Edit | Keep layer selection coherent with Main/Track Edit. | Partial, backed by shared sample-layer state. | Change layer and navigate back/forward. |
| SAMP-007 | Adjust sample Start/End and audition region | Preview/use the selected sample region, not an unrelated file. | Region/waveform UI exists; per-action runtime evidence required. | Change region; audition; verify actual region and playback cursor. |
| SAMP-008 | Crop region | Replace sample data/region according to the supported Crop semantic. | Exact native frame/region assertions passed on PR Build #3039 / run `39643908281`, SHA `09cbc15c702c2d1b1989da7d453e50820c71c993`; full workflow remains red at later FAVORITE 1 hierarchy timeouts. | Fixture: 3,840→2,840 frames after Start=1,000; assert native range resets to `0..2,840`, then retain green full-workflow SAF/persistence evidence. |
| SAMP-009 | Chop into 4/8/16 regions | Generate the selected chop plan and map regions to the intended state. | Exact native per-pad range assertions passed on PR Build #3039 / run `39643908281`, SHA `09cbc15c702c2d1b1989da7d453e50820c71c993`; overall workflow remains red at later FAVORITE 1 hierarchy timeouts. | For the 2,840-frame fixture, verify contiguous, non-overlapping per-pad coverage for CH4/CH8/CH16, then retain green full-workflow SAF/persistence evidence. |
| SAMP-010 | Zoom/waveform play/audition | Change visual range or trigger actual supported playback without mutating sample data. | **SAMP-010 subacceptance passed inside Build #3049 / run `39672208687` on exact SHA `816e11a70a1368d63d0137c72aa4154774c5de7b`: zoom-in VIEW 10–90%, zoom-out/RESET VIEW 0–100%, native sample region/frame values unchanged, and actual Pad audition trigger observed.** Full PR run remains failure later at shared Project Menu XML dump; not integrated into PR #23. | Keep the zoom/audition tests and all unique PNG/XML evidence. After primary Project lifecycle becomes green, synchronize history-preservingly and run the isolated PR on a compatible base before integration. |

## 16. Mixer and channel-strip actions

Manual anchor: S-05 and S-01 Mixer Strips/Channel Mixer/Pad Mixer. Firmware clue: F-01 Help distinguishes Channel Mixer scope from Pad Mixer scope. Do not invent levels/meters/routing.

| ID | User action | Expected result / state contract | Current app mapping/status | Acceptance path |
|---|---|---|---|---|
| MIX-001 | Show/hide XL Channel Strips | Toggle the left strip region; preserve workspace sizing and selected context. | Implemented and emulator-verified at Main layout level. | Inspect live bounds; confirm duplicate legacy panel remains absent. |
| MIX-002 | Change Pad Mixer Level | Set real selected-pad level. | Native setter and current UI route exist. | Change/read/restore exact value. |
| MIX-003 | Change Pad Mixer Pan | Set real selected-pad pan. | Native setter and current UI route exist. | Change/read/restore exact value. |
| MIX-004 | Change Pad Mixer Tune | Set real selected-pad tuning. | Native setter and current UI route exist. | Change/read/restore exact value. |
| MIX-005 | Change Channel Mixer level/pan | Edit per-channel strip state. | RESERVED: full multichannel mixer backend not established. | No fake sliders/meters. |
| MIX-006 | Edit output routing, sends, returns, submixes or inserts | Change real routing/FX graph and display authoritative route state. | RESERVED / GAP. | Needs actual routing/FX semantic model and audio verification before UI availability. |
| MIX-007 | Switch context strip target (Track/Pad/Keygroup) | Show the documented target scope for the selected Track Type/context. | Partial; context selector/complete type coverage not established. | Verify that displayed controls modify the selected target only. |
| MIX-008 | Display meters/status | Meter and activity indication must reflect real engine data. | No fabricated meter claims permitted; audit each displayed value source. | Inject/play audio and compare UI readout to genuine telemetry. |

## 17. Project, files and persistence actions

Manual anchors: S-01 Project p.60 and Project Load/Save pp.67–68; S-08/S-09/S-10/S-11. The target reference guides lifecycle vocabulary; our project codec and file format remain our own.

| ID | User action | Expected result / state contract | Current app mapping/status | Acceptance path |
|---|---|---|---|---|
| PROJ-001 | New Project | Create a real empty/default Project and reset related state safely. | GAP in the full user-facing Project lifecycle. | Implement real route and safe model replacement; preserve/stop transport/recording as designed. |
| PROJ-002 | Save Project | Serialize current Sequence/Track/Pattern, notes, placements and supported metadata to a chosen document. | VERIFIED on exact SHA 617511efb820e96c6d9576051fa30012a3368db1 in Android Build #2856 / run 39162451542: real SAF Save As → mutation → Open matched the full canonical domain SHA. | Keep exact-domain assertion in the full lifecycle regression; test overwrite/provider-write failure separately. |
| PROJ-003 | Save Project As | Choose a new destination/name without mutating the wrong existing file. | VERIFIED for ACTION_CREATE_DOCUMENT Save As, real DocumentsUI selection and cancellation with unchanged native state on Build #2856 / run 39162451542, exact SHA 617511efb820e96c6d9576051fa30012a3368db1. Provider overwrite/failed-write recovery and independently reopening two documents remain uncovered. | Add provider-error/overwrite and two-document isolation acceptance before calling the entire Save As contract complete. |
| PROJ-004 | Open Project | Replace model atomically; reconcile indices, selected context, history and sample references. | VERIFIED on exact SHA 617511efb820e96c6d9576051fa30012a3368db1, Android Build #2856 / run 39162451542: real SAF Open restored exact canonical state after mutation and process restart; cancelled Open and malformed input preserved the active SHA and Project context. | Retain lifecycle regression; separately consider provider read/I/O failure messaging and sample relink behavior. |
| PROJ-005 | Preserve Pattern placements across save/load | Restore each placement identity, Pattern link, timeline position, length and source offset after a real file open. | VERIFIED on exact SHA 617511efb820e96c6d9576051fa30012a3368db1 in Build #2856 / run 39162451542: canonical SHA round-trip includes placement ID project-persistence-clip-01, Pattern reference, start 480, length 720 and source offset 120, plus two non-default MIDI events. | Continue asserting full canonical state on future Project codec/UI changes. |
| PROJ-006 | Load a placement-free legacy-format fixture | Accept the documented older schema/fallback without fabricating placements. | VERIFIED on exact SHA 617511efb820e96c6d9576051fa30012a3368db1 in Build #2856 / run 39162451542: the runtime-valid v1 fixture was opened through real SAF, reported zero placements and matched the host canonical-v2 state oracle for retained Program/Track/Pattern/MIDI data. | Keep v1 fixture and canonical-state oracle in regression CI. |
| PROJ-007 | Save sample or program | Write real supported media/program content to a user-selected destination. | No complete product user flow established in the current verified queue. | Use S-11 as behavior reference; verify existence, re-openability and meaningful failure messages. |
| PROJ-008 | Auto Save / recovery | Persist recoverable project state on policy/timer and recover safely after interruption. | Not implemented as a complete product flow. Akai Auto Save is a reference, not a requirement to copy its file layout. | Only implement after the manual Save/Open lifecycle; test interrupted save and recovery integrity. |
| PROJ-009 | Select/cancel Android document or folder picker | Preserve prior state when canceled; on success persist appropriate URI grants and return to correct product context. | Save As cancel, Open cancel and malformed Project rejection were verified through actual DocumentsUI on exact SHA 617511efb820e96c6d9576051fa30012a3368db1 / Build #2856. Cancel paths retained the exact state SHA; malformed load retained Project context and reported PROJECT_LOAD_FAILED. | Still test explicit provider read/write errors, revoked permissions and unavailable provider behavior. |
| PROJ-010 | Import MPC2/official MPC project files | Provide true conversion with compatibility rules and tested semantics. | REFERENCE ONLY / not claimed: our format is not MPC project-compatible unless separately engineered and tested. | Do not advertise compatibility merely because MPC2 import is documented by Akai. |

## 18. Preferences, MIDI/Control, automation and performance

Manual anchors: S-01 Preferences pp.61–65, XY p.38, S-06 for metronome. Firmware evidence: F-01 Q-Link and toolbar/help clues. Community controller references S-16/S-17 are not physical-test substitutes.

| ID | User action | Expected result / state contract | Current app mapping/status | Acceptance path |
|---|---|---|---|---|
| SYS-001 | Open Audio Settings | Select real input/output device and supported audio policy/rate/buffer; show live diagnostics truthfully. | Audio settings subset and audio engine integration are present in project docs. | Reopen settings and verify chosen device/policy against actual runtime diagnostics. |
| SYS-002 | Open MIDI/Sync settings | Configure actual available MIDI port/clock/sync behavior. | MIDI page exists; deeper MIDI/Sync settings are PARTIAL/RESERVED. | Test real port connection, transport/clock if supported and disconnect/error recovery. |
| SYS-003 | Open Sequencer settings | Configure actual supported sequencer policies rather than placeholders. | RESERVED taxonomy/category until setters and state exist. | Any enabled setting must change authoritative sequencer state and survive expected return/reopen. |
| SYS-004 | Change metronome/count-in | Use the supported backend for enable, mode, rate, sound, volume and routing. | GAP; see G-006. | Audible and state-based acceptance required. |
| SYS-005 | Enter Q-Link Edit / reassign a macro | Map a supported control to a real parameter with explicit range and source/scope. | REFERENCE behavior documented by Akai; product controller has no dedicated Q-Link knob bank, and full equivalent is not established. | Keep RESERVED until a coherent user-control mapping and parameter setter exists. |
| SYS-006 | Toggle Q-Link Learn / Momentary / Inspector | Learn a real selected parameter; Momentary restores prior value on release if supported; Inspector shows correct scope. | Firmware Help describes these terms, but runtime/physical MkII behavior is not verified. Treat as research-only requirements until product mapping is designed. | Specific control-to-parameter tests and physical verification before enabling. |
| SYS-007 | Save Global vs Project Q-Link maps | Persist correct mapping scope across app/project lifecycle. | No established product implementation. | Test distinct global/project maps, cold restart and project reopen only after Q-Link exists. |
| SYS-008 | Use MIDI IN/OUT/controller setup | Discover real endpoints and route semantic controls without scattering raw MIDI numbers into UI. | Adapter and semantic routing foundation exists; complete settings/control management is partial. | Exercise supported endpoint and inspect adapter/event route; physical MkII acceptance separately. |
| SYS-009 | Next Sequence: select/queue/launch | Select or launch the intended Sequence and show current/queued state truthfully. | PARTIAL: mode exists but full live launcher semantics remain incomplete. | Test sequence selection/launch and transition boundary; do not fake queue state. |
| SYS-010 | XY/XYFX performance | XY movement changes supported live FX/macro parameters. | RESERVED. | Require real automation/FX setter and performance-state test. |
| SYS-011 | Automation lane/record/edit | Record or edit automation against genuine parameters and playback state. | GAP / RESERVED: automation backend not established. | Verify parameter capture, playback and save/load before marking functional. |
| SYS-012 | Change user preferences persistently | A setting that promises persistence survives restart and affects the named subsystem. | Audio subset exists; broader preference taxonomy is partial. | For each preference, assert actual consumer and persistence; omit decorative options. |

## 19. MPC Studio MkII hardware actions and feedback

Reference anchors: S-16/S-17 plus repository hardware documents and current decoder/policy tests. This table intentionally separates software/emulator evidence from physical hardware acceptance.

| ID | User action | Expected result / state contract | Current app mapping/status | Acceptance path |
|---|---|---|---|---|
| HW-001 | Press physical Play/Stop/Record | Controller semantic action reaches the same transport/recorder command as the UI. | Decoder/routing policy tests exist. | Unit/CI checks plus physical device test while observing actual transport/recording. |
| HW-002 | Press a hardware pad with velocity/aftertouch | Resolve Pad/note/velocity into the selected context and real instrument/sampler. | Decoder and semantic routing coverage exists; physical behavior still pending. | Verify note/velocity/aftertouch on actual MkII and audio output. |
| HW-003 | Turn Data Dial / press +/- | Change only the currently focused semantic field/parameter. | Focus/dial routing exists for selected contexts; scope coverage varies. | Exercise Main field, Pad Mixer parameter and Step parameter separately. |
| HW-004 | Use hardware Track/Sequence navigation | Update shared selected Sequence/Track and all connected views. | Software route exists; physical path needs hardware confirmation. | Select with real controls and compare UI/context. |
| HW-005 | Use Note Repeat or step-edit controls | Apply supported rate/step action and maintain the corresponding hardware/UI state. | Note Repeat timing/LED policy tests exist in project history; full physical acceptance remains distinct. | Exact rate/state test and physical controller observation. |
| HW-006 | Send LED/pad-color feedback | LEDs reflect semantic application state and do not resend inconsistent/stale modes. | State-driven feedback slices exist; complete hardware validation is not claimed. | Capture controller feedback on real hardware after state changes. |
| HW-007 | Update the 160×80 controller LCD | LCD displays the intended screen/parameter feedback. | Protocol/reference work exists; current full LCD behavior is not claimed as physically verified. | Physical capture and documented expected output required. |
| HW-008 | Touch Strip and bank feedback | Resolve Touch Strip/Pad Bank input and reflect valid active state. | Policies/protocol references exist, but actual-device verification is pending. | Test position, direction, release and LED/bank state on MkII. |

## 20. Known contradictions and investigation queue

This section exists to prevent agents from treating every old status cell as current truth.

1. **Stale Arrangement entries:** some older sections of MPC3-9-OS-KNOWLEDGE-BASE.md still say CUT or explicit clip placements are absent. The current 2026-10-18 top-level roadmap records CUT/COPY/PASTE/DUP and PatternPlacement operations as implemented and emulator-verified. Use the current dated roadmap and exact-SHA CI evidence; reconcile stale rows when that part of the knowledge base is next maintained.
2. **Project persistence boundary:** the Android SAF Save/Save As/Open route is now present in current branch source, so the earlier claim that the UI route is not connected is superseded. Build #2808 proves the earlier native-codec slice only; Build #2819 was still in progress on the prior HEAD when this audit began. The current smoke checks restored Track/placement counts after a real Save As → in-memory mutation → Open sequence, but not exact placement tuples/MIDI fields through Android or process restart. Refresh live Actions before claiming the route verified; native detailed-field and v1 tests remain separate evidence.
3. **Browser coverage:** search/filter/audition/local WAV assignment have historical emulator evidence. Browser options, full semantic category coverage and some picker/folder lifecycle edges need checking against current source/tests before a broad completion claim.
4. **Track types:** guide evidence identifies Audio, Drum, Keygroup, Plugin, MIDI and CV. Firmware selector assets support that vocabulary. Our Track Type chooser/model must be audited before exposing conversion; Clip/Arrangement concepts must not silently replace Audio.
5. **Firmware scope:** only the supplied Gen1 image was analysed. Gen2 is not covered. Static TUI/help/layout metadata does not supply full runtime traces.
6. **Physical controller:** no emulator result closes the physical MPC Studio MkII validation requirement.
7. **Exact HEAD:** the creation-time note above was historical. At this audit, PR #23 was open at HEAD `1dd5742331642e34713636a1ac811752e5fb1bb9`; Android Build #2819 (`39136433515`) was still running its emulator smoke step. This audit advances the branch again, so the new HEAD requires its own current workflow result; never re-use an in-progress or older SHA as green proof.

## 21. Acceptance recipe for any catalog action

A meaningful action is ready to be called complete only when its applicable parts are covered:

1. **Source:** expected behavior is traceable to an official reference or clearly labelled inference.
2. **Entry/target:** action enters the correct context and uses the intended Sequence, Track, Program, Pad, Layer, event or file.
3. **State change:** the authoritative native/domain/platform state changes as expected; a highlight/toast is insufficient.
4. **Non-target invariance:** unrelated tracks/events/parameters remain unchanged.
5. **Navigation:** Back/return preserves the intended context and state.
6. **Persistence:** claim only the level actually tested (in-memory, context re-entry, process restart or disk round-trip).
7. **Errors/cancel:** invalid target, collision, unavailable backend, cancellation and permission failure do not fabricate a successful result or corrupt state.
8. **Visual/accessibility:** real landscape runtime hierarchy and screenshot show usable targets and truthful status.
9. **Build evidence:** relevant tests pass and the final integration workflow belongs to the exact code SHA; inspect requested APK/evidence artifacts at acceptance boundary.
10. **Hardware:** where behavior depends on physical MkII input/feedback, report physical verification separately.

## 22. Maintenance rules

- Add new actions under the appropriate stable prefix and assign a unique ID. Do not recycle IDs.
- Keep one row per user intent. Split a combined row when actions have different prerequisites, state mutations or acceptance paths.
- For each material status change, update the action row and the current dated section of IMPLEMENTATION-ROADMAP.md. Do not rewrite history; mark older entries superseded.
- Source changes must record URL, source date/version, relevant section/page and confidence. If firmware evidence is used, add the exact resource/path and its limitation.
- Prefer a small source link and concise finding over copying proprietary text or storing extracted Akai images/assets in the product repository.
- Never claim complete feature parity merely because every reference action has a row. A catalog row can explicitly remain GAP/RESERVED.
- Do not repeat full firmware extraction. Inspect one specific resource or binary path only when a specific unresolved action needs clarification.
- Keep product exclusions marked as such without rewriting Akai's documented reference behavior.
