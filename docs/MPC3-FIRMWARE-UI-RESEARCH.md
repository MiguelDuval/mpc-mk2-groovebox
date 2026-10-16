# MPC Firmware UI Research — implementation handoff

**Purpose:** preserve the useful UI evidence from one supplied firmware image and turn it into fast, safe Android implementation decisions. This is a static research summary, not a complete firmware reverse-engineering report.

**Research date:** 2026-10-16  
**Analysed image:** `MPC-3.9.1-Gen1-update.img`  
**Internal firmware version:** `3.9.1.2`  
**SHA-256:** `4c0f7797a006313fad206f9513eca8b3823e834c9e0941a6b5f088386cb835d6`  
**Size:** 162,220,784 bytes  
**Scope boundary:** This result is for the supplied **Gen1** image. It does not verify the separately mentioned `MPC-3.9.1-Gen2-update.img`.

## 1. How to use this research

Use it as an evidence map when implementing UI behavior. Prefer direct help/resource evidence over guesses, and prefer official MPC 3.9 documentation or a physical test whenever static evidence is incomplete.

Confidence labels:
- **HIGH** — explicitly present in a structured resource/help entry or a directly identified binary string/path.
- **MEDIUM** — several static clues support the interpretation, but runtime behavior or call sites were not traced.
- **LOW / unresolved** — plausible but still needs a targeted test or code-level correlation.

A resource proving that a button label or icon exists does **not** prove the exact handler, transition, focus policy, or timing of the visual update. There were no running-firmware screenshots, runtime traces, hardware interaction recordings, or exhaustive disassembly in this pass.

## 2. Source identity and extraction facts

The image has the `AZ01` update-header marker and an XZ-compressed Linux ext filesystem payload starting at byte offset 340. The stored and independently calculated payload SHA-1 match. The filesystem is approximately 441 MB and the read-only inventory found 11,572 reachable paths. The original image was not modified; no firmware executable was run or flashed.

Useful source identifiers (paths are inside the firmware filesystem):
- Version: `/usr/share/Akai/SME0/VERSION` → `3.9.1.2`
- Build marker: `/usr/share/Akai/SME0/VCSVERSION` → `f3af35cdc95`
- Main app: `/usr/bin/MPC` — 112,222,004 bytes, stripped 32-bit ARM EABI5 ELF; SHA-256 `bc054a3f3ba02c2d33ac9a515a4a8638964da779223502286d6a64b517bf1426`; Build ID `8371199f945ecc40f79cb19c17ff5de72810a91c`.
- Main application binary contains JUCE-related names/strings. Its direct dependencies do not show an obvious Qt/QML UI path. The safest conclusion is that the main application follows a native C++/JUCE-related path while selected GUI/page data is externalized in ACVS/TUI resources; do not generalize beyond inspected evidence.

The local private research package contained validated JSON inventories of 21 screen/context records, 26 interaction records, 13 state records, 51 ACVS help entries, 12 layout JSON files, 33 Program Edit/component/Q-Link JSON sources and 759 ACVS PNG assets. The private package and extracted resources are not part of this public repository.

## 3. Highest-value evidence locations

| Firmware path / evidence | Why it matters | Confidence / limitation |
|---|---|---|
| `/usr/share/Akai/SME0/Resources/ACVS/layout/Help.json` | 51 help entries for Toolbar, Browser, Inspector, Grid, mixers, Q-Link and related panels | HIGH for text-described capability; not a runtime event trace |
| `/usr/share/Akai/SME0/Resources/ACVS/layout/{Toolbar,StatusBar,MainBody,Inspector,Browser,FileBrowser,GuiSequenceEditor,MainQLinks,PadPanel,BankPanel,ProjectInfo,QLinkAssignPanel}.json` | Shared-region geometry/configuration values | HIGH as resource values; runtime scaling is unknown |
| `/usr/share/Akai/SME0/Resources/Pages/Program Edit/Drum/TUI.json` | Structured Program Edit component tree and tab descriptions | HIGH for schema; no runtime layout capture |
| `/usr/share/Akai/SME0/Resources/Pages/Program Edit/Keygroup/TUI.json` | Rich parameter/plotter/envelope/filter/XY control schema | HIGH for schema |
| `/usr/share/Akai/SME0/Resources/Pages/Program Edit/Midi/TUI.json` | MIDI page description and editing control tree | HIGH that partial resource exists; explicitly marked `PartialDescription` |
| `/usr/share/Akai/SME0/Resources/Pages/Program Edit/Components/Generic Knob.json` and Plugin counterpart | Focus state, Q-Link/Data action and overlay action contract for this component | HIGH for this component only; not a global rule for every knob |
| `/usr/share/Akai/SME0/Resources/ACVS/graphics/**` | Mode, transport, browser, sequencer, mixer and state-icon vocabulary | HIGH that resources exist; semantic runtime mapping is not traced |
| `/usr/bin/MPC` targeted strings | Mode/transport labels and screen identifiers at known offsets | HIGH for string existence; MEDIUM/LOW for exact runtime role without cross-references |

Useful main-binary string anchors (file offsets, not virtual addresses):
- `0x4a9b824` — `The Main mode button`
- `0x4a9b83c` — `The transport Play button`
- `0x4a9b858` — `The transport Stop button`
- `0x4a9b87c` — `The transport Record button`
- `0x4a9ba24` — `The Step Sequencer button`
- `0x4a9c5a8` — Sampler mode button
- `0x4a9c5cc` — Program Edit mode button
- `0x4a9c5fc` — Program Mixer mode button
- `0x4a9c624` — Sequence Edit mode button
- `0x4a9c650` — Sample Edit mode button
- `0x4a9c674` — Song mode button
- `0x4a9c68c` — Step Sequence mode button
- `0x4a9c6b4` — Next Sequence mode button
- `0x4a9c6e0` — Track Mute mode button
- `0x4a9c708` — Pad Assign mode button
- `0x4a9c828` — Browser mode button
- `0x4a9c8c8` — Pad Mixer mode button
- `0x4a9c8f4` — Channel Mixer mode button
- `0x4a9c91c` — XYFX mode button
- `0x4a9c940` — Pad Perform mode button
- `0x4a9cf20` — Arrange button
- `0x4a9cf84` — Record To Arrange button
- `0x4aa6d28` — Track View; `0x4aa6dd4` — Grid View
- `0x4b10bac` — Return to Arrangement
- `0x4b09680` / `0x4b096a4` — horizontal zoom in/out at playhead
- `0x4b0eecc` / `0x4b0eee8` — Step Sequencer Current Bar / Select Pad

Treat these as search anchors for a future targeted cross-reference pass, not a proven navigation graph.

## 4. Recovered screen/context vocabulary

The static evidence identifies the following UI contexts or context families: Main, Browser/Media Browser/File Browser, Sampler, Program Edit, Program Mixer, Sequence Edit, Sample Edit, Song/Arrangement, Track View, Grid View/Grid Editor, Step Sequencer, Next Sequence, Track Mute, Pad Assign, Pad Mixer, Channel Mixer, XYFX and Pad Perform. Program Edit has Drum, Keygroup and MIDI-specific TUI descriptions; other program types/components appear in the resource inventory too.

The names are evidence-backed, but this is **not** a fully verified screen-transition graph. Main, Arrangement, Browser and Grid do not have complete declarative screen trees in the inspected resource set. No full-screen screenshot of running firmware was found. The contact sheets in the private research package show individual graphic assets, not captured screens.

## 5. Geometry and visual rules that can safely inform layout

The following are exact resource configuration values, **not verified physical screen pixels** and not guaranteed to be unscaled:

| Region | Configured geometry |
|---|---|
| Toolbar | top, full width, 55 units high |
| Status Bar | bottom, full width, 34 units high; bottom padding 1 |
| Main Body | full workspace; padding left 208, right 397, top 58, bottom 35 |
| Inspector | left-aligned, width 206; left padding 4, top 58, bottom 36 |
| Browser | right-aligned, width 294; right padding 4, top 56, bottom 27 |
| File Browser | right-aligned, width 294; top 58, bottom 27 |
| GUI Sequence Editor | full workspace; left/right padding 4, bottom reservation 338 |
| Main Q-Links panel | bottom-left, 265 × 265 |
| Pad panel | x=271, bottom-aligned, 264 × 310 |
| Bank panel | x=271, bottom-aligned, 264 × 26, offset by 310 bottom units |
| Project Info | bottom panel, height 336, left inset 842, right inset 4 |
| Q-Link assignment panel | bottom panel, 265 × 76, left inset 4, bottom offset 260 |

Program Edit TUI canvas commonly uses 1280×628 units:
- Drum LFO example: plotter at (8,124), size 1264×220, line `#D31145`; a “Black Fade” image at (8,112), size 1264×244; a label instance names Titillium Web Regular at 30 units.
- Keygroup filter example: two 628×272 plotters at (8,8) and (644,8); line `#D31145`, fill `#1A1A1D`.
- A generic knob focus component specifies fill `10ffffff` and a red outline `ffe81f40`, inset 4 with width 1. This is component-level evidence only, not proof of a global selected palette.
- Twelve Liberation-family TTFs were found under `/usr/share/fonts/ttf`; the TUI names “Titillium Web”, but that font was not found at that path. Whether bundled elsewhere or substituted is unresolved.

## 6. Documented interactions with the strongest support

1. **File Browser: select vs load.** Help states single click selects a file; double-click loads it. Destination, assignment side effects, progress/errors and confirmation UI are unknown.
2. **Grid editing capability.** Drum Grid help says it supports inserting, deleting and editing events. The exact gestures and which properties are edited by each tool are not recovered.
3. **Inspector.** Help says it adjusts the current sequence and track; specific field list and repaint behavior remain unknown.
4. **Track type control.** Help confirms it chooses a Track type; the full option list and confirmation route are not established by that entry.
5. **Mode-dependent bottom panel.** Help says bottom-panel contents vary with current MPC mode and the status-bar full-screen action can hide the panel. The mode-switch handler is not traced.
6. **Toolbar.** Help describes main menu controls, toolbar customization from View, Follow Now (off/paged/continuous), global automation, master VU meter and MIDI IN/OUT status.
7. **Mixer scope.** Channel Mixer adjusts channel levels; Pad Mixer adjusts relative levels of pad channels within a track. Keep these scopes distinct in Android.
8. **Pad bank.** Bank Panel selects the current MPC pad bank. The exact selected/disabled visual-state mapping is not fully traced.
9. **Q-Link.** Help describes Project/Track assignment modes and fixed assignment groups, plus Learn, Momentary and Inspector. Learn assigns the parameter manipulated in the app to the selected Q-Link; Momentary restores the prior value on release according to help text. Physical controller/LED/LCD feedback is not verified.
10. **Parameter knob overlay.** Generic Knob TUI maps mouse-down to Q-Link/Data and double-click or Enter to “Show Overlay”. Focus fill/outline are described for this component. Do not assume every knob shares this behavior.
11. **MIDI Learn persistence.** Help says the Global map persists across app opens and Project map is saved/recalled with the project. No save/reload experiment was run.
12. **Grid/Arrangement vocabulary.** Strings identify Track View, Grid View, Return to Arrangement, Arrange, Record To Arrange, horizontal zoom-at-playhead and Step Sequencer Current Bar/Select Pad. They establish terminology and investigation anchors; runtime behavior still needs direct verification.

## 7. Observable state to represent in the Android UI

The app should model only state needed to render truthful, usable behavior; do not copy firmware internals. Existing project state should remain authoritative. Add a state only if the current UI/navigation model cannot represent it.

- **Navigation:** active context/mode, context history where BACK is meaningful, active tab and open overlay.
- **Musical selection:** selected sequence, track, program/track type, pad and pad bank, selected note/event, selected mixer target (channel vs pad).
- **Browser:** current browser/place/filter/search, selected file, and separately the sample/program assignment actually loaded. Do not equate highlight with load.
- **Editing focus:** current tool, focused parameter, current Q-Link assignment, Learn/Momentary state where supported.
- **Transport/time:** actual transport state and playhead position independent of Follow Now/scroll policy.
- **Presentation:** contextual bottom functions, visible/hidden lower panel, page-specific inspector/browser, focus/selection/pressed/disabled state where supported by evidence.

Required transition format for implementation and tests:
**initial state → semantic user action → resulting state → visible result → evidence/test**.

For example, a browser test should independently assert selection and loading rather than assuming a click did both. An Arrange↔Grid test should verify the musical selection/event and relevant pattern data remain intact after navigation, not only that a screenshot title changed.

## 8. Implementation rules

- Continue the existing Android application and current UI migration; do not rebuild its domain model/audio engine/MIDI architecture from the firmware.
- Keep the real-time audio callback, sequencer clock, MIDI transport, hardware adapter, sampler and persistence boundaries protected.
- Translate confirmed information architecture into original Android UI components and original graphics.
- Do not copy firmware code, decompiled code, proprietary artwork, binary assets or raw extracted TUI files into the app or public repository. This document is the public text-only summary; private assets stay outside GitHub.
- Prefer one end-to-end user-visible slice over broad visual scaffolding. Implement the actual semantic action, render state and test the state transition.
- Keep confirmed facts, inference and TODOs visibly separate. A string/icon is not proof of a runtime handler.
- Build and inspect the exact commit's Actions result and uploaded evidence. Do not claim that CI succeeded because a previous commit passed.

## 9. Best next research only if it unblocks a coding task

Do not launch another full firmware crawl. The smallest useful static follow-up would cross-reference 5–8 anchor strings for Main, Browser, Step Sequencer, Arrange, Grid View and Return to Arrangement in the stripped `/usr/bin/MPC` binary. Inspect only immediate caller/callee clusters if an available disassembler supports reliable xrefs. Stop if symbol stripping/tool limitations make this unproductive. If behavior can be answered from the official guide or a legitimate real-device test, prefer that.

Still-unverified items include: actual mode dispatcher and transition graph; Main/Arrangement/Grid screen captures; BACK/re-entry context persistence on firmware; step duration/velocity gestures; complete sample edit/load/assign flow; cancel/confirm behavior; selected/focused visual-state mapping; Q-Link hardware feedback; runtime scaling of geometry; and Gen2 UI differences.

## 10. Next-session technical prompt (copy/paste)

> **@SUPERPOWERS — CONTINUE MPC MKII UI IMPLEMENTATION, DO NOT RESTART**
>
> Work in `MiguelDuval/mpc-mk2-groovebox`. Continue the existing PR/feature branch; do not recreate the application. On session start, verify the live PR #23 head, branch, latest exact-HEAD Android Build result and available visual/runtime artifacts. Treat `docs/SESSION-STATE.md` as a checkpoint, not ground truth.
>
> Read `AGENTS.md`, `docs/PROJECT_MASTER_PROMPT.md`, `docs/DEVELOPMENT-OPERATING-PROTOCOL.md`, the relevant roadmap section, `docs/MPC3-UI-MIGRATION-MASTER-SPEC.md`, `docs/MPC3-REFERENCE-INDEX.md`, `docs/UI-MIGRATION-SAFE-CHANGE-CONTRACT.md`, and this firmware UI research note. Inspect current source and callers before editing.
>
> **Product goal:** increase fidelity to the documented MPC 3.9 standalone information architecture and improve complete musical workflows while preserving our own app/domain/audio/MIDI implementation. Firmware research is evidence for UI/interaction design only.
>
> **Firmware evidence boundary:** this note analyses the supplied `MPC-3.9.1-Gen1-update.img`, internal version 3.9.1.2, SHA-256 `4c0f7797a006313fad206f9513eca8b3823e834c9e0941a6b5f088386cb835d6`. It does not verify a separate Gen2 image. Direct help/resource evidence can guide implementation; binary strings and icon names alone do not prove transitions. Do not re-run a full firmware analysis or ask for the image again unless a specific blocking question requires it.
>
> **Execution:** once live status is checked, select the highest-value small UI gap visible in current source and tests—prefer a complete action → state change → visible result → regression test in Main, Track View, Arrange/Grid, Browser, Step Sequencer, Sampler or Mixer. Do not spend the session on another broad audit, redesign from scratch, cosmetic-only work or speculative refactoring. Reuse the current domain operations and state where they already work.
>
> **Constraints:** never reset/rebase/force-push or modify `main`; keep changes small and traceable on the existing feature branch. No proprietary firmware code/assets/artwork in GitHub or app resources. Do not redesign the realtime audio callback, MIDI transport, sequencer clock, hardware adapter or project persistence for a visual task. No new architecture unless a demonstrated semantic gap requires it.
>
> **Verification:** inspect the exact diff; run focused tests and the relevant Android build/emulator smoke; examine screenshot/accessibility evidence and the exact commit's Actions result. Distinguish emulator proof from physical MPC Studio MkII verification. A screenshot or source assertion alone is not acceptance. If a test fails, fix the specific cause before adding another feature.
>
> **Completion report:** name the implemented user workflow, files changed, actual commit/branch, tests and exact-SHA CI result, evidence inspected, and remaining blockers. If CI is still running or no evidence is available, say so plainly. Aim to ship one useful, verified increment—not another planning loop.
