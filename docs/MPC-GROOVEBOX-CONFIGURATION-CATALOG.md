# MPC Groovebox — Consolidated Configuration Catalog

**Updated:** 2026-10-31  
**Repository:** MiguelDuval/mpc-mk2-groovebox  
**Canonical integration branch:** checkpoint/mpc-ui-reference-and-product-20261030  
**Integration PR:** [#133 — checkpoint: consolidate accepted MPC groovebox work](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/133) — OPEN / DRAFT  
**Product baseline:** ac59c5e23f87680e69e311e25e2d45d7ab1b8f8d (feature/mpc-one-ui-fidelity)  
**Main:** unchanged.

This is the cross-branch index of configurations already implemented and independently accepted, then integrated into the non-main checkpoint. It is not a claim that every MPC 3.x feature or every open branch is complete. Detailed user-action status remains in [MPC3-USER-ACTION-CATALOG.md](MPC3-USER-ACTION-CATALOG.md); current exact-run handoff is in [SESSION-STATE.md](SESSION-STATE.md).

## Acceptance legend

- **Integrated + combined exact-head SUCCESS** — the feature slice is in the checkpoint and the combined candidate passed the full Android workflow on exact SHA #3608/#3609; the relevant screenshot/XML evidence was present in those exact-run archives.
- **Source-head accepted** — the isolated source branch passed its named exact-SHA build; this is retained as provenance, not a substitute for combined acceptance.
- **Component-level evidence** — the component's test or UI gate passed, but a later unrelated stage in the same overall workflow may have failed.
- **NOT RUN / HELD / RESERVED** — explicitly not accepted; do not promote it to implemented based on a label, source branch or neighboring feature.

## Verified configuration matrix

| Area | Integrated configuration / behavior | Source-of-record acceptance | Combined checkpoint evidence |
|---|---|---|---|
| Main shell and Channel Strip | MPC-style shell, five-shortcut rail, main workspace, XL Channel Strip geometry; duplicate legacy mixer stays hidden without breaking shell state; real Back/subcontext restoration and truthful unavailable navigation. | Product baseline ac59c5e...; shell Back PR #110, SHA 68a83fc7f9541efc14531854d1060e5969f5429f, Build #3412 / run 40547076817; Save As PR #115, SHA cdce4f1776cce5da064bfbeebbca5f7d87d06af2, Build #3403 / run 40535592616. | #3608 and #3609 passed full native/JUnit + emulator smoke. |
| Pad Mute / Track Mute | Dedicated Mute workspace with distinct 4×4 Pad and Track target grids; native mute bits are toggled/restored; empty Track slots are disabled; musical context and note data are preserved. | PR #124; source product SHA f734930cae403d0794e3d4b504e4bf8f5f7c6e84, Build #3465 / run 40591850947; PR-head Build #3466 / run 40660926245. | #3608/#3609 evidence includes Pad and Track PNG/XML; Pad 01 and Track 01 ON states are visible and structurally represented. |
| Track Solo | Native playback policy gates playable Drum Tracks upstream of the realtime audio callback; Solo is independent of Mute; Mute wins on the same Track; multiple Solo Tracks and active-Sequence scope are covered; Function Bar exposes real ON/OFF state. | PR #125; source SHA 06d08f38d2789a0c62cf64b23cdd8e8dd2b420cd, Build #3468 / run 40663060996. | #3608/#3609 include separate Solo OFF/ON screenshots and XML. OFF=selected=false; ON=selected=true; both enabled/clickable at [1570,975][1794,1080]. |
| Grid and Step Sequencer | Drum Grid event editing uses native sequencer state; Draw/Erase and Step parameter edit/remove are covered; reversible discrete zoom and H/V viewport isolation keep musical events unchanged. | Draw/Erase and Step editing in Build #2781 / run 39062512475; PR #117 SHA 62c13b07aa17eaf5d2e7fec6e24b75d66e7b37e8, Build #3413 / run 40547525120; PR #123 SHA b11c2a4ce55a21528f056e740df108fb81d30b9b, Build #3457 / run 40578192003; PR #114 SHA 1b177ad886f87ac18e09828daa8880825ee38a3e, Build #3399 / run 40527004496. | #3608/#3609 include Grid baseline, horizontal and vertical zoom PNG/XML; smoke/native tests pass. |
| Arrangement | Viewport pan and zoom with exact inverse/reset restoration and no native event mutation; loop-brace drag/restore; event MOVE ± GRID, DUP/COPY/CUT/PASTE, Undo/Redo; empty-timeline paste cursor; Pattern placements and Project save/reopen. | Loop/MOVE acceptance Build #2781 / run 39062512475; DUP/COPY/CUT/PASTE Build #2785/#2789/#2791; empty cursor Build #2793 / run 39075574304; placement regressions #2807/#2808; real Project placement Save/Open Build #2856 / run 39162451542; PR #120 SHA 25f62d07d8e06e45847cca02f002066d10a4ac3d, Build #3458 / run 40578350114. | #3608/#3609 include Arrangement pan start/changed/restored and zoom in/out/reset evidence; full current-head emulator smoke passes. |
| Track Edit / Sample / Sampler | Sample layer tuning, gain, pan, velocity ranges, region, envelope/filter, crop, chop and waveform audition/zoom use the actual sample model; evidence snapshots are immutable per chop count. | Track Edit tuning and related sample interactions Build #2781 / run 39062512475; PR #128 SHA ade77f513404112a251ad65cbd445e33003af599, Build #3514 / run 40895016008. | #3608 is the exact verification for scroll-helper correction dea4297a54caf60daae874adcab9b5c09480f1bc, run 41226252891; #3609 exact-head run 41226976655. Both evidence archives include Gain/Pan/Tune/Velocity and CH4/CH8/CH16 PNG/XML. |
| Browser / local content | Local Content categories, real local WAV results, file-size and modified-date options, persistent preferences, picker assignment/cancellation with Pad/Layer target preservation; unsupported categories remain truthful/unavailable. | PR #56 SHA 20e33dd7b9d2462dcd816da1281b2206948d243b, Build #3239; PR #80 SHA 5fe65c952c7a6771f49478f5a026be9c0ada0f38, Build #3374 / run 40461592533; PR #111 SHA 935e59374b5ac682ec6ccca16b66b43db28f16d7, Builds #3394/#3395 / runs 40518877443 and 40518880896; PR #119 SHA e707ffd026210cef3a022d743741f5f1f631b749, Build #3424 / run 40562838515. | #3608/#3609 include local WAV results, picker assignment/cancel and Browser UI hierarchy evidence. |
| Project lifecycle / persistence | New Project cancel/discard invariance; versioned Project save/load; malformed payload rejection without mutating an already-open Project; Arrangement placement persistence through real Save/Open; Save As picker evidence. | New Project Build #3077 / run 39846737408; Project persistence Build #3073 / run 39823061552; Arrangement placement Save/Open Build #2856 / run 39162451542; persistence PR #49 SHA 4bff9cf07a90e82e433aa1caad2faba1ec5e6065, Build #3144 / run 40092822936; PR #51 SHA 96ce3f1f35653b431a6a103b86defd03722c0c8e, Build #3146 / run 40093413671. | #3608/#3609 native Project-state and versioned save/load tests passed; evidence archive contains Project picker/save/open/reopen/restart and malformed-load captures. |
| Recorder / MIDI / transport / scheduler | Per-Track recorder uses the Drum Program assigned to that Track; note mapping snapshots at note-on; invalid queued MIDI fields are rejected before Pattern mutation; native sequencer, transport, ratchet and bounded audio-trigger scheduling tests. | PR #86 SHA dda28c999993205d3950872f872b091ad5d59a31, Build #3314 / run 40343495124; PR #98 SHA 201c2b308bd7e279aac810acf76ce76a44123f8d, Build #3335 / run 40379300776. | #3608/#3609 passed MIDI decoder, Note Repeat, Step Edit, sequencer runtime, scheduler/audio-trigger, ratchet, recorder, project and edit-history test steps. |
| MPC Studio MkII software contract | Protocol/semantic maps and policy tests for MIDI decode, Note Repeat, Step Edit routing, Locate, Touch Strip, Erase, Pad Aftertouch and connection-state handling. | Hardware traceability sources are indexed in MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md and related semantic-map documents. | #3608/#3609 passed controller policy/native tests. **Physical controller acceptance is NOT RUN**; no claim is made for physical MIDI, LEDs, RGB pads, LCD, Jog Wheel or Touch Strip behavior. |
| MPC visual reference archive | Named MPC Standalone OS 3.9 visual contexts and local image/page assets to guide future UI changes. | PR #129 source branch docs/mpc-ui-visual-reference-catalog-20261029; source image inventory retained in manifest and recovery notes. | 82 VIS contexts; 1,296 local image embeds; 402 original source-image records (370 downloaded / 32 failed); 248 PDF page captures and 281 figure crops. Two later official Akai Program crops are separately typed as supplemental references. |

## Latest exact combined verification

The integrated candidate passed both final gates:

| Build | Exact SHA | Run | Result |
|---|---|---:|---|
| Android Build #3608 | dea4297a54caf60daae874adcab9b5c09480f1bc | 41226252891 | SUCCESS — 53 substantive steps, including emulator smoke and evidence upload. |
| Android Build #3609 | 727f8c3cd931171161e3fb5bc6a99c711cce9949 | 41226976655 | SUCCESS — 53 substantive steps, including emulator smoke and evidence upload. |

Exact-run artifacts:

| Build | Artifact | ID | Bytes | GitHub artifact SHA-256 |
|---|---|---:|---:|---|
| #3608 | APK artifact | 13051293765 | 10,433,069 | 5e67867de8a00523a1610de99abff32a406f2e497d39752c81174e2a5ab831a5 |
| #3608 | UI evidence ZIP | 13053086005 | 18,234,393 | 078113a3706aab9243e1b93fea5704051e84a274491c4365aecf18dd29b80137 |
| #3609 | APK artifact | 13054637251 | 10,433,069 | 10e8d34f73a2232b2360587d959927d304b12514a4be741b158abce47d019544 |
| #3609 | UI evidence ZIP | 13055390134 | 18,169,816 | 0a071ecf77e98e1364294cc2d214dd2987e0d7db8ef2f1292ae6007fb68287c1 |

Each UI evidence archive has 227 entries, including 106 PNG and 116 XML files. Both archives passed ZIP/image/XML integrity inspection. Key exact-run captures include Pad/Track Mute, Solo OFF/ON, Grid horizontal/vertical zoom, Arrangement pan/zoom, Browser local WAV assignment and picker cancellation, Track Edit Gain/Pan/Tune/Velocity, Sample Chop CH4/CH8/CH16 and Project lifecycle.

## Explicit exclusions and non-claims

- **PR #126 WAV export:** not integrated. Its source Build #3480 / run 40710623733 passed the WAV decoder step but failed later on -Werror=unused-variable; its expected AudioEngine::exportSampleToWav route is absent from the checkpoint API. The temporary partial codec addition was reverted.
- **PR #127 Browser XML assertion repair:** held after failed exact-head Build #3516.
- **PR #122 Browser empty-search retry:** held after failed exact-head run.
- **PR #116 duplicate picker-cancel path:** held after failed exact-head run; PR #119 is the accepted cancellation path.
- Unsupported or Reserved MPC functions (for example complete Channel Mixer, Sounds/expansion storefront, XYFX, 16 Levels, Pad Perform, List Edit, full metronome backend, and other incomplete routing/FX areas) are not counted as completed configurations. Keep their UI truthful and unavailable until real model/backend behavior and acceptance exist.
- Physical MPC Studio MkII validation remains **NOT RUN**.

## Operating constraints

Continue from the canonical checkpoint branch, not from stale stacked feature branches. Inspect current refs and exact-SHA Actions before acceptance claims. Preserve the original feature PRs as source records. Do not change main, merge PR #133 automatically, force-push, rebase, reset or rewrite history. For every new UI change, open the relevant local MPC OS 3.9 visual reference before implementation and record its VIS ID and evidence path.
