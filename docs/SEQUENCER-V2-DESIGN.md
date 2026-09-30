# Sequencer V2 Design From Build 866

This branch starts exactly from Build 866 commit 144f47ab39172c90f31fad743c47d6c206d5670f.

## Safety boundary

The 866 sampler/audio implementation is the protected baseline.

This slice must not:
- modify src/Audio/*;
- change the 4x4 software pad geometry or physical pad numbering;
- replace the global UI shell;
- introduce a new realtime audio callback path;
- make the transport UI own audio state.

The sequencer overview is a read-only visual consumer of the existing sequence transport state.

## Product model

The project uses the standalone MPC mental model:

Project -> Sequence -> Track -> Program -> Pad/Sample -> Events.

A Sequence is the musical time container. A Track is a lane inside the active Sequence. The current engine intentionally gives Drum tracks the existing sampler playback path; future track types can share the timeline without being activated prematurely.

MPC documentation describes a Sequence as a song building block containing multiple tracks, and notes that MIDI tracks exist within their Sequence. Current MPC documentation also exposes Sequence selection, Sequence-owned timing, track length, and a transport position counter.

Maschine separates idea construction from linear Song arrangement: Patterns are reusable/reference content, while Clips are unique timeline objects. Its Arranger also has explicit playhead following and quantized transitions.

Ableton Live separates non-linear Session work from the linear Arrangement and exposes a persistent Arrangement overview, loop range and Follow behavior. Push is intentionally centered on Session-style performance in standalone mode, which reinforces the value of keeping the performance surface uncluttered while making timing state continuously visible.

Akai Force combines a clip matrix with a linear Arrange workflow. Its project interchange also maps MPC sequences into clip-matrix rows and programs into tracks, confirming that Sequence/Track and performance-arrangement layers can coexist without collapsing into one overloaded view.

## UI decision

Every app page gets one thin sequence overview strip immediately below the global top bar.

The strip is a thin visual transport overview:
- bar-level movement across the full active sequence;
- loop range;
- current playhead;
- distinct playing/stopped playhead state.

Exact sequence number and bar.beat.tick readout belong to the dedicated transport context rather than being repeated inside the strip. This keeps the strip glanceable in a live performance without creating duplicate controls or duplicate textual state.

It is deliberately non-editing in the first slice. Editing the loop range remains on the dedicated SEQ page, where the existing SequenceTimelineView continues to own IN/OUT touch editing.

UX rule: the overview is global and observational. Changing page must never change what the transport means, stop sequence timing, or move the user's editing context.

This is the common "context monitor" layer: changing page does not make the user lose the temporal location of the performance.

## Sequence selection contract

The project now supports a bounded set of independent Sequences (up to the domain limit). A new Sequence starts as a clean default musical container with its own tempo, time signature, length, loop and initial DRUM track; it does not clone another Sequence's events.

Sequence selection is explicit in the SEQ page. While stopped, PREV/NEXT select a Sequence immediately. While playing, PREV/NEXT queue the target Sequence without interrupting the current one; the transition is committed only when the current Sequence crosses its loop boundary. The transport context exposes the queued target as a compact `→Sxx` indicator.

This is intentionally a small first live-performance layer. It follows the MPC pattern of selecting another Sequence during playback and having it take over at the end of the current Sequence, while keeping future quantized launch/arrangement rules separate. A destructive sequence edit (tempo, length, time signature, loop, quantize or swing) cancels the queue and stops the transport so the edit cannot create an ambiguous transition.

## Grid / Step editing slice

The first real event editor is deliberately small and musical: a 16-by-16 drum grid for the selected Track/Pattern, with sixteen time columns per page and sixteen pad lanes. A tap toggles an event at the current Sequence quantize grid. Paging is independent of Sequence bar count, so the same editor remains usable when the musical time signature changes.

The grid is an editor, not a second transport. The global playhead remains authoritative and is mirrored as a column highlight. Editing is blocked while playback is active. Before a mutation, the native layer releases the current playback session so Pattern::notes cannot be mutated underneath a live playback object that still holds a reference to that Pattern.

The first slice intentionally exposes only note presence plus the existing default velocity/probability/ratchet values. Velocity painting, per-step probability, ratchets, note duration editing and multi-page gestures remain separate future increments. This keeps the first editor fast enough for live use while preserving the existing Pattern/MidiNoteEvent data model.

The software grid follows the project's physical MPC Studio pad convention vertically: Pad 16 is the top lane and Pad 1 the bottom lane. Four-step group separators make 16-step phrasing immediately legible without adding visual chrome.

## Live Sequence Launcher

The live performance surface now includes a dedicated Sequence Launcher rather than overloading the 4x4 MAIN performance pads. It mirrors the MPC Sequence Mode idea that the 16 pads can select/trigger Sequences, while preserving the global 4x4 pad surface for sound performance.

The launcher shows sixteen Sequence targets at once and pages them in banks of sixteen, allowing the domain limit of 128 Sequences without making the first screen dense. Active and queued states are visually distinct. Tapping an inactive target while stopped selects it immediately; during playback the existing native selection contract queues the target for the current loop boundary. Tapping the active Sequence during playback uses the existing queue-clear behavior.

The launcher is intentionally separate from editing. GRID and STEP are production/edit contexts; LAUNCH is a performance context. All three retain the global transport strip so the performer does not lose temporal context when changing mode.

The explicit CANCEL QUEUE control is provided for stage ergonomics: it avoids requiring the performer to remember that tapping the active Sequence is also the queue-clear gesture.

The physical MPC Studio MkII follows the same mapping and state contract while this mode is active: Pad 1–16 addresses the visible Sequence bank. Active and queued Sequence states are mirrored to the controller LEDs. LED writes are state-cached rather than emitted on every UI timer tick, and leaving launcher mode clears the launcher LED state so the normal performance surface is not left visually contaminated.

## Realtime boundary

The strip, Grid View, Step Editor and Sequence Launcher read existing JNI sequence state. Grid/Step mutations remain stopped-state editor operations. Sequence launching uses the existing transport-aware native Sequence selection path and does not call the audio engine directly from the UI.

## References

- Akai MPC Standalone OS User Guide v3.9:
  https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf
- Akai MPC release notes:
  https://www.akaipro.com/mpc-release-notes
- Akai MPC One FAQ:
  https://support.akaipro.com/en/support/solutions/articles/69000816149-akai-pro-mpc-one-frequently-asked-questions
- Maschine+ manual, Arranging Your Project:
  https://docs.native-instruments.com/ni-tech-manuals/maschine-plus-manual/en/arranging-your-project
- Maschine+ manual, Patterns and Clips:
  https://docs.native-instruments.com/ni-tech-manuals/maschine-plus-manual/en/working-with-patterns-and-clips
- Ableton Live 12, Arrangement View:
  https://www.ableton.com/en/manual/arrangement-view/
- Akai Force User Guide:
  https://cdn.inmusicbrands.com/akai/Force/330_dsghdfgt/Force%20-%20User%20Guide%20-%20v3.3.pdf
- Akai Force 3.2.1 release notes:
  https://cdn.inmusicbrands.com/akai/Force/uywetrfg_321/Force%203.2.1%20Firmware%20Update.pdf
