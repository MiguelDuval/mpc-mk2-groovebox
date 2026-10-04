# MPC 3.9 Screen-by-Screen UI Blueprint

## Purpose

This is the implementation blueprint beneath MPC3-UI-MIGRATION-MASTER-SPEC.md.

Each screen is specified by:

- shell regions;
- information hierarchy;
- touch targets;
- hardware entry path;
- Data Dial focus;
- bottom functions;
- backend dependencies;
- indication;
- acceptance tests.

The blueprint is intentionally implementation-oriented. It does not prescribe proprietary Akai assets.

---

## 1. Global shell blueprint

All product screens share the same horizontal composition unless the reference context explicitly requires a different full-screen editor.

### Region A — Toolbar

Persistent:

- project name;
- time counter;
- Timing Correct;
- metronome;
- automation;
- MIDI I/O/monitor status where applicable;
- transport.

Touch:
- tap field -> open focused contextual dialog;
- long press -> documented alternate state where applicable.

Hardware:
- transport buttons remain active while navigating;
- contextual controls update the same semantic state.

### Region B — Shortcut rail

Exactly five configurable high-frequency destinations.

Rules:

- icon + short label;
- current destination visually selected;
- one action per slot;
- user can reorder from Menu;
- no hidden sixth/seventh "page" rail.

### Global transport-position strip

Immediately below the persistent Toolbar, every shell context carries the same non-interactive 3dp sequence-position strip.

Rules:

- position is derived from semantic Sequence/transport state;
- it is visible on Main, Track View, Grid, Step, Browser and future workspaces;
- it never receives touch or hardware input;
- it is updated through the existing sequence state refresh path;
- it is a visual position indicator, not a new transport control.

### Region C — Compact mixer/channel strips

Context-sensitive.

Rules:

- selected track/pad is visually obvious;
- current value visible;
- detailed mixer is opened by explicit navigation;
- hide/show state is persistent.

### Region D — Workspace

The active context owns the majority of the screen.

### Region E — Function Bar

Bottom functions are determined from semantic context.

The bar must:

- expose only currently meaningful actions;
- preserve stable left-to-right ordering inside a context;
- visually distinguish primary action from inactive/unavailable action;
- mirror hardware context where possible.

---

## 1.1 Persistent left context strip

The shell's left edge is deliberately split into two persistent functional columns:

- **Shortcut rail:** exactly five configurable high-frequency destinations.
- **Compact context/channel rail:** persistent Sequence, Track, Program, Pad and Data Dial context.

The compact context rail remains visible while the workspace changes. It is a glanceable status surface and a direct-entry surface for Sequence Select, Track Select and Program Select; it is not a second workspace.

## 1.2 Compact context semantics

The persistent context strip exposes state, not duplicate editing controls:

- **Sequence:** current sequence and tempo; opens Main Sequence Select.
- **Track:** current track, type, REC-arm and mute state; opens Main Track Select.
- **Program:** current Program for a Drum Track; opens Main Program Select. For non-Drum Tracks it becomes visibly unavailable rather than presenting a false Drum-program list.
- **Pad:** selected software/hardware pad and bank.
- **Dial:** current Data Dial focus and Main subcontext.
- **Sequence overview:** thin movement/loop indicator.
- **Main Mixer Strip:** the persistent channel/context column exposes compact read-only level, pan and tuning values for the selected Drum pad. Values are read directly from the existing pad state; editing remains in Pad Mixer / sampler controls until the full MPC channel-strip command surface is implemented.
- **Selected sample identity:** the Main Track quick-sample context displays the selected layer's real project sample name when available. Document-provider display names are propagated through the existing control-thread import path; the realtime audio path does not depend on filenames.
- **Mixer visibility:** the condensed Mixer Strip can be shown or hidden without changing selected Track, Pad, Sequence or transport state.

# 2. Main Mode blueprint

## 2.1 Layout

Top:
Toolbar.

Left:
Five shortcuts + compact mixer strip.

Center:
Sequence section.

Below/center:
Track section.

Lower center:
Track/Arrangement switch and selected-track timeline/preview.

Bottom:
Function bar.

## 2.2 Sequence section

Fields:

- Sequence;
- BPM;
- time signature;
- length;
- loop;
- start;
- end.

Interaction:

- single tap selects field;
- double tap opens numeric/list editing where appropriate;
- Data Dial edits selected field;
- +/- perform incremental change;
- changes are semantic commands.

## 2.3 Track section

The Track region is one continuous MPC-style context. Program is a field inside the selected Track context, not a separate top-level workspace.

Fields:

- track number;
- track name;
- track type icon;
- program/instrument;
- selected pad for Drum;
- program/preset for instrument tracks;
- Track Edit entry;
- track length;
- inserts/I/O entry.

Touch:

- tap Track field -> Main Track Select subcontext;
- tap Program field -> Main Program Select subcontext;
- tap Browser -> Browser for the current loading workflow.


### Main Track quick-sample context

- The default Main Track view is the controller-first Track/Arrangement waveform surface for the selected Pad/Layer; no 4x4 software pad matrix is part of the canonical Main layout.
- A compact MPC-style track-state row sits directly below the Track canvas with **MONITOR / LENGTH / VELOCITY / LAYER** vocabulary.
- Only truthful backend state is surfaced: Monitor is currently unavailable, Length is sequence-scoped, Velocity has no track-level semantic in the current backend, and Layer is the actual selected Drum sample layer.
- Start/End handles use the existing sample-region semantic command; this is a quick-edit surface, not a replacement for full Track Edit.
- The waveform preview shows eight compact layer indicators; the selected dot follows the same selected Layer state used by the Track-state field.
- The Layer field selects the current sample layer; tapping it establishes shared SAMPLE_LAYER Data Dial focus, and the standard +/- hardware adjustment path changes the value without leaving Main.
- An empty selected pad exposes large in-canvas BROWSE / RECORD actions; a loaded pad uses one compact audition control while the waveform remains the dominant visual surface.
- Double-tapping the waveform opens the Track Edit Samples workflow; Main does not add a separate permanent SAMPLE EDIT button.

### Main Sequence field hierarchy

- Main keeps the MPC-style compact Sequence vocabulary visible around the selected sequence and BPM: **SEQ / BARS / START / END / TRANSPOSE**.
- Existing time-signature and loop state remain visible because they are backed by the current Sequence model.
- TRANSPOSE is explicitly shown as unavailable until a real domain semantic exists.

### Main visual-fidelity presentation rules

The Main surface should visually read as an MPC One / MPC3 instrument screen before any implementation detail is considered:

- Toolbar uses the graphite/status-oriented treatment and carries project identity, compact time-counter state, Timing Correct, metronome/automation state and MIDI In/Out status; transport remains hardware-first.
- The visible page title is not a separate Android-style title chip; the selected shortcut and active Main sections provide the context.
- Track identity remains one coherent header band.
- The selected Track's Program is visible directly beneath that header as a Track-owned field.
- Track-type presentation uses one persistent icon attached to Track identity; tapping it opens the Track Type selection context, where unsupported types remain explicitly unavailable.
- Shortcut and Function Bar controls use flat rectangular surfaces with clear focus/selection state rather than generic rounded Android cards.

## 2.4 Arrangement preview

Show:

- selected track;
- timeline;
- events;
- playhead;
- loop brace.

Double tap:
open Grid/appropriate editor.


### Main Track / Arrangement switch

- Main presents Track and Arrangement as sibling local views of the same selected Track/Sequence context.
- Track View is the default Main view on entry, matching the MPC workflow; switching to Arrangement changes presentation only and does not create a new navigation mode.
- Track View keeps the selected Pad/sample waveform surface; Pad selection remains on the physical controller, while Track Edit provides a bounded truthful editor for the currently implemented Drum pad semantics.
- The Track section exposes a compact pencil affordance for Track Edit, matching the MPC entry point. The same semantic destination is also opened by double-tapping the Main Track sample/waveform area.
- The Track section's compact pencil affordance and a double-tap on the Main Track sample/waveform area are the same semantic Track Edit entry gesture.
- Double-tap on the Main Arrangement overview opens Grid for a Drum Track; unsupported Track Types remain explicitly unavailable rather than being routed into a mismatched editor.

## 2.5 Main function bar

Initial target:

- five visual slots: New Track; SEQ + Rec Arm; − Track +; Mute; Solo;
- semantic Track − and Track + remain separate actions inside the grouped Track slot.

The Main workspace itself keeps only context-specific transitions such as Grid and Browser; high-frequency Track operations belong in this function bar.

## 2.6 Hardware

Main -> Main.

Shift + Main -> Track View.

Track/Sequence Select sets the current focus.

Data Dial edits the focused Main field.

## 2.6 Main selection subcontexts

Main remains the top-level mode while selection changes context/focus:

- Sequence Select: select the active Sequence without leaving Main;
- Track Select: select the active Track without leaving Main;
- Program Select: select the Program owned by the selected Drum Track.

Data Dial / +/- operate on the current selection focus. BACK MAIN returns to the overview; deeper edit functions open only where the backend has a truthful implementation.

## 2.7 Acceptance

A Main screen build is accepted when:

- changing track updates the Track section and preview together;
- changing sequence updates the sequence context;
- transport remains running while navigating;
- program shown equals the selected track's actual program/container;
- Data Dial focus is visible;
- five shortcuts are present;
- function bar is semantic.

---

# 3. Track View blueprint

## 3.1 Layout

Top:
Toolbar + focused Track field.

Main:
vertical list of horizontal track strips.

Each strip:

- track number/name;
- type;
- record-arm;
- mute;
- solo;
- automation status;
- compact event/timeline information.

Bottom:
function bar.

### Track View selection/status policy

Track View rows are **selectable channel strips**, not mini editor cards.

Each row shows:

- track number/name;
- type;
- current Program/instrument context;
- compact event information;
- REC-arm state;
- mute state;
- unavailable SOLO state.

The entire row selects the Track. Track mutations are performed by the shared shell Function Bar, so Track View does not duplicate REC/MUTE/Track navigation controls inside each row or add a second local action bar.

## 3.2 Functions

Primary:

- New Track;
- Rec Arm;
- Track -;
- Track +;
- Mute;
- Solo.

Shift layer:

- Duplicate Track;
- Timing Correct;
- Click;
- Track Settings.

## 3.3 Hardware

Shift + Main -> Track View.

Track Select + Data Dial -> track focus.

Pads may become direct track selection only when explicitly entered into track-select context.

---

# 4. Arrangement blueprint

## 4.1 Layout

Top:
Toolbar.

Left:
track headers.

Center:
linear timeline.

Timeline:

- time ruler;
- playhead;
- loop brace;
- events/regions;
- selection.

Bottom:
Cut, Copy, Paste, Duplicate plus context functions.

## 4.2 Interaction

- drag loop brace;
- tap track;
- double tap event/region -> editor;
- pinch/spread -> zoom;
- fit-to-view;
- six locators.

## 4.3 Live rule

Changing arrangement selection must not stop the transport.

Destructive edit actions require clear selection/confirmation semantics.

---

# 5. Grid View blueprint

## 5.1 Layout

Top:
track/sequence/time context.

Main:

- left row labels;
- grid/timeline;
- playhead.

Bottom:
tool bar / function bar.

## 5.2 Drum grid

Rows:

- pad 1..N;
- pad name;
- selected row.

Columns:

- musical time.

Cells:

- velocity/intensity;
- event presence;
- selection.

## 5.3 Melodic grid

Use piano-roll semantics:

- pitch labels;
- note events;
- duration;
- velocity/modifier lane.

## 5.4 Tools

Canonical roles:

- Draw;
- Erase;
- Select;
- Magnify/Navigation.

## 5.5 Zoom

Existing MpcSequenceZoomPolicy remains the shared policy.

Horizontal:
time density.

Vertical:
pad/pitch density.

Do not couple Grid zoom to Step page count.

## 5.6 Hardware

Shift + Main -> Grid.

Zoom -> current axis.

Data Dial -> focused event/row/value.

+/- -> focused increment.

Pads -> context-specific row/step selection.

---

# 6. Step Sequencer blueprint

## 6.1 Layout

Top:

- sequence;
- track;
- bar;
- step division;
- timing correct.

Center:

- 16 step indicators;
- event state;
- current selected step;
- playhead.

Bottom:

- step parameters;
- edit functions.

## 6.2 Hardware

Pad 1..16:

- step selection/entry.

Step navigation:

- Bar -/+;
- Step -/+.

Data Dial:

- current Step Edit parameter.

Data Dial press:

- cycles supported parameter focus.



### 6.4 Truthful Track-Type gating

- The Step workspace exposes editor mutation only when the selected Track Type has a truthful Step backend; unsupported Track Types keep the context visible but disable Step mutation controls.
- Main Track Type presentation resolves directly from the semantic Track Type query rather than parsing display/status text.
- Persistent Program context normalizes the Program label once, so the shell never renders duplicated `PROGRAM •` prefixes.
## 6.3 Visual states

At least distinguish:

- empty;
- active;
- selected;
- playhead;
- selected + playhead;
- unavailable.

Phone and pad LED feedback must agree.

---

# 7. Track Edit blueprint

## 7.1 Entry

From Main Track section:

- tap pencil/edit;
- double tap relevant track/program area;
- hardware Mode shortcut where supported.

Track Edit geometry:

- compact **TRACK** field at the top-left;
- compact **PAD** field beside it;
- visible **Edit All Layers** action affordance at the top-right, disabled/reserved until its atomic backend command exists;
- scrollable parameter workspace;
- persistent bottom tab bar.

## 7.2 Tab bar

Target display labels:

- GLOBAL;
- SAMPLES;
- AMP ENV;
- LFO;
- MODS;
- EFFECTS.

The bottom tab bar stays outside the scrollable editor body so the primary context switch remains reachable during deep parameter editing.

## 7.3 Drum context

Selected pad:

- 16 physical performance pads remain visible conceptually;
- selected pad state;
- up to eight sample layers;
- sample region;
- level;
- pan;
- tune;
- envelope;
- filter.

Current implementation coverage:
- **Global:** Track context plus pad-global tune/level/pan.
- **Samples:** layer select, sample identity, waveform region, gain/tune/pan, velocity range and audition.
- **Envelopes:** amp ADSR and filter cutoff.
- **LFO / Modulations / Effects:** explicit RESERVED/UNAVAILABLE until the relevant domain contract exists.
- **Edit All Layers:** explicit disabled/reserved because there is no atomic multi-layer command yet.

## 7.4 Safety

Playback-critical edits that current engine disallows while running remain clearly disabled or deferred.

Never silently stop playback to make a UI operation work.

---

# 8. Sample Edit blueprint

## 8.1 Layout

Top:

- sample name;
- current pad/layer;
- time/frame context.

Center:

- large waveform;
- time ruler;
- start/end;
- loop;
- playhead.

Bottom:

- edit tool set;
- audition;
- crop;
- chop;
- assign;
- processing actions;
- zoom/navigation.

## 8.2 Touch

- finger-safe start/end handles;
- drag;
- pinch zoom;
- horizontal pan;
- tap audition.

## 8.3 Hardware

Sample Start / Sample End:
select corresponding focus.

Zoom:
horizontal/vertical focus.

Tune:
parameter context.

Sample Select:
layer/sample focus.

Touch Strip:
parameter-aware control.

---

# 9. Sampler / recording blueprint

Sampler is reached through the documented hardware/controller relationship rather than as an isolated diagnostic page.

Show:

- input source;
- armed state;
- monitor;
- threshold;
- record state;
- live waveform;
- captured duration;
- peak;
- assignment target.

Record and Monitor are independent states.

The UI must never imply that monitoring is enabled merely because recording is armed.

---

# 10. Browser blueprint

## 10.1 Layout

Left:

Places.

Center/left:

Content and category/filter controls.

Main:

result list.

Right/lower:

preview + selected item metadata + load destination.

Top:

search.

## 10.2 Primary concepts

- Places;
- Content;
- Expansions;
- Sample Assign;
- favourites 1-5;
- filters;
- audition;
- Browser Options.

## 10.3 Hardware

Browse -> Browser.

Shift + Browse/Save -> Save where the physical workflow defines it.

Data Dial:
result/folder selection.

Dial press:
open/confirm.

Shift + navigation:
parent/alternate function where defined.

---

# 11. Channel Mixer blueprint

## 11.1 Layout

Multiple compact strips.

Per strip:

- track name/number;
- level;
- meter;
- pan;
- solo;
- mute;
- record;
- selected state.

Additional:

- sends;
- inserts;
- I/O;
- returns;
- outputs.

Navigation:

- horizontal paging;
- focus retention.

---

# 12. Pad Mixer blueprint

Per visible pad strip:

- pad number/name;
- level;
- pan;
- routing;
- mute/solo where relevant;
- selected state.

The selected pad must remain synchronized with the physical pad.

---

# 13. Menu blueprint

## 13.1 Layout

4x4 large mode cells.

Required behavior:

- tap to enter mode;
- drag to reorder;
- left-most column becomes the high-frequency shortcut region;
- unavailable modes show reserved/unavailable status rather than dead clicks.

## 13.2 System controls

Accessible from Menu family:

- New Project;
- Save;
- Project;
- Preferences;
- system/resource information.

---

# 14. 16 Levels blueprint

Show:

- source pad/sample;
- 16 velocity positions;
- current fixed velocity;
- performance status.

Pad grid:

position 1 -> velocity step 1
...
position 16 -> velocity step 16.

The current semantic implementation already defines the velocity mapping; the migration changes presentation, not engine behavior.

---

# 15. Pad Perform blueprint

Current supported target:

- Notes;
- future Chords;
- future Scales.

Display must answer:

- what pads mean;
- current bank/range;
- current octave/range;
- active mode.

Do not expose "Pad Perform" as a hidden mode whose pad interpretation cannot be inferred from the screen.

---

# 16. Next Sequence blueprint

Show:

- current sequence;
- next/queued sequence;
- launch quantization;
- pad matrix;
- active and queued states.

Live rule:

queueing must not tear down active audio playback.

---

# 17. Project / Preferences blueprint

System views may temporarily be simpler than MPC 3.9 while the musical contexts are migrated.

However:

- navigation remains within the same shell;
- modal windows are used only for focused system tasks;
- returning from system screens restores prior musical context;
- no transport reset unless explicitly required by a future documented operation.

---

# 18. Global keyboard/controller focus model

Only one semantic focus exists for Data Dial at a time.

Focus can be:

- selector;
- numeric field;
- event;
- timeline;
- pad row;
- sample boundary;
- mixer strip;
- browser result.

Phone UI must show the focus.

Hardware feedback should mirror it where the physical device supports it.

Data Dial press:

- opens/commits selector;
- cycles editor target only where documented by our application;
- never silently changes operating mode without indication.

---

# 19. Global modal policy

Prefer in-place context transitions.

Use a modal only for:

- destructive confirmation;
- complex parameter entry;
- track/program creation;
- Browser options;
- system preferences;
- operations that truly need an isolated task.

A modal must provide:

- title;
- current context;
- explicit primary action;
- cancel/close;
- hardware route where practical.

---

# 20. UI density and touch-size rules

The display is a musical instrument surface.

Therefore:

- preserve functional controls before decoration;
- keep touch targets large enough for live use;
- do not turn compact MPC-like regions into tiny unreadable text;
- use hierarchy, not oversized labels, to show importance;
- retain immediate value visibility for high-frequency controls;
- avoid full-screen dialogs for simple one-value adjustments.

---

# 21. State rendering and performance

UI state updates must be diff-driven.

When a MIDI event or audio status changes:

1. update semantic state;
2. compute minimal UI diff;
3. update affected context;
4. update hardware feedback only if projected state changed.

Do not rebuild the complete view tree per MIDI message.

Do not make transport timing dependent on Android UI frame timing.

---

# 22. Regression matrix

Every new context must prove these baseline behaviors still work:

- pad audition;
- transport;
- record;
- overdub;
- Note Repeat;
- Timing Correct;
- Step Edit;
- Locate;
- Erase;
- Copy/Delete;
- Undo/Redo;
- pad banks;
- touch strip;
- hardware feedback;
- LCD status.

---

# 23. Definition of "faithful"

A screen is "MPC 3.9-faithful" when:

- its information hierarchy matches the reference;
- its main controls occupy the corresponding conceptual regions;
- the same user intent can be reached through equivalent interaction steps;
- hardware and touch use the same semantic action layer;
- context/focus is visible;
- bottom functions are contextual;
- it does not invent unrelated app navigation.

Pixel-identical reproduction is not the acceptance criterion.

Behavioral and information-architecture fidelity is.


---

# 23. Main Mode — Program Select subcontext

Program Select is a Main subcontext, not a new top-level mode.

Show:

- selected Track number/name;
- Track Type;
- available Drum Programs;
- current Track Program;
- explicit current-state indication.

Touch:

- tap a Program -> assign it to the selected Drum Track.

Hardware:

- Program Select context -> Data Dial focus = PROGRAM;
- Data Dial +/- -> previous/next available Drum Program;
- Data Dial press -> open/confirm Program Select;
- non-Drum Track -> explicit unavailable status.

Semantic rule:

- assignment updates the selected Track's `programId`;
- playback resolves the Program from each Track independently;
- no global Program swap is used as a substitute for Track ownership.

Program Edit remains reserved until the backend can truthfully edit Program contents.

## 2026-10-02 Main canvas fidelity refinement

- Main Sequence now presents the high-frequency SEQ/BPM/BARS/START/END/TRANSPOSE fields as the primary interaction row; local BPM/BARS stepper buttons are removed so touch and hardware use the same Data Dial/+/- semantic path.
- Sequence Edit remains visually represented by the compact pencil affordance but is explicitly RESERVED until a real Sequence Edit contract exists.
- Main Data Dial focus is rendered as a thin red outline on the selected field, including Sequence, Track, Program and Layer contexts.
- Main Track/Arrangement is flattened into one central MPC-style work surface; the old nested Android-card appearance is no longer the target.


## 2026-10-03 Main shell persistent-context checkpoint

- The shell left edge is explicitly split into two persistent functional layers: five shortcuts and a compact context rail followed by the workspace.
- The persistent context rail carries Sequence + BPM, selected Track identity/state, selected Track Program where truthful, selected Pad, semantic Data Dial focus/subcontext, and a thin sequence movement overview.
- Mixer Strip detail visibility affects only mixer-detail controls; it must not hide or reset persistent Sequence/Track/Program/Pad/Dial context or sequence state.
- Sequence, Track and Program entries are direct semantic entry points into the existing Main selection contexts. Non-Drum Program selection remains explicitly unavailable.
- The shell geometry implementation contract is 44dp Toolbar, 48dp shortcut rail, 210dp context/channel rail and 40dp Function Bar. These are implementation geometry baselines, not claims of pixel-equivalent Akai hardware dimensions.
- The Toolbar remains reduced to the MPC information hierarchy and does not expose diagnostic AUDIO/MIDI chips as primary product controls.


### 7.5 Geometry acceptance

A Track Edit build is accepted only when the top TRACK/PAD context and bottom parameter-tab bar are structurally persistent,
the selected tab is visibly distinct, and reserved functions remain truthful rather than simulated.

### 1.1.3 MkII-first shortcut presentation

Shortcut slots remain five configurable MPC-style mode shortcuts. The visual tile uses an original mode glyph and short label so the phone surface mirrors the controller-centric mode concept without copying proprietary Akai artwork or using numbered page ordinals.


## 2026-10-03 Persistent context hierarchy fidelity checkpoint

The persistent context rail is intentionally denser than a general Android dashboard:

- each context group has a small uppercase section caption;
- value fields remain flat rectangular surfaces with compact typography;
- the active Main Data Dial focus uses the red interaction outline on the corresponding Sequence, Track, Program or Pad context field;
- the dedicated Data Dial field summarizes active focus using operator-facing labels rather than raw enum names;
- the sequence overview remains a thin non-interactive visual indicator.

These rules are presentation-only and must not create duplicate domain or transport ownership.

## 2026-10-03 Compact context sizing checkpoint

The persistent context panel is content-sized rather than fixed-height. This keeps Sequence/Track/Program/Pad/Data Dial information tightly grouped and prevents empty space from appearing when Mixer Strip details are hidden.

## 2026-10-03 Main section framing fidelity checkpoint

Main is rendered as two visually coherent framed workspaces beneath the shared shell:

- **Sequence section:** one flat panel containing Sequence/BPM/SEQ/Time Signature/header controls and the BARS/START/END/TRANSPOSE/Loop row;
- **Track / Arrangement section:** one flat panel containing the selected Track identity, Track Type, Track Edit entry, Track/Arrangement context and the active performance/timeline workspace;
- no separate Arrangement-edit control is rendered in the header because the Arrangement workspace itself is the navigation surface.

The selected parameter continues to use the red focus outline; section framing itself does not become an additional semantic focus state.

## 2026-10-04 Main Toolbar project-entry fidelity increment

The persistent Toolbar keeps project identity, a compact adjacent Browser/project affordance, BAR/BEAT/TICK position, Timing Correct, metronome/automation state and MIDI In/Out status. The In/Out cells are status/monitor affordances; transport remains hardware-first. The Browser affordance opens the existing Browser semantic context and does not introduce a second navigation model.

## 2026-10-04 Main Track type affordance fidelity checkpoint

The Main Track header now reserves the full six-choice Program Type affordance documented for MPC Main:

- Drum;
- Keygroup;
- Plugin;
- MIDI;
- Clip;
- CV.

Only Drum is enabled because it is the only Track Type with a truthful backend in the current build. The complete six-icon presentation remains visible so the Main screen follows the reference hierarchy without inventing unsupported behavior.

## 2026-10-03 Main shell geometry fidelity checkpoint

The Main presentation now uses explicit MPC One geometry constants instead of scattered legacy spacing:

- 4dp Main outer gutter and 2dp section gap;
- 40dp primary Main field/header and Track-state bands;
- 36dp compact Sequence metrics;
- zero-radius Main surfaces, preserving the flat rectangular MPC chrome;
- compact 4dp insets for the shell shortcut/context rails.

This is presentation-only. MpcUiState, MpcNavigationController, native sequencing/audio semantics, the five-slot Function Bar and the Track/Arrangement sibling model are unchanged.

- Main interactive controls now use a dedicated flat Main action helper; the global Android-style rounded button default is no longer inherited by Main controls.

- Main Toolbar fidelity checkpoint: fixed Menu/Project zones, a flexible BAR/BEAT/TICK transport cluster, fixed TC/METRO/AUTO controls and fixed MIDI In/Out status cells keep the MPC3 hierarchy stable across screen widths.


## 2026-10-04 Pad Mixer fidelity checkpoint

The Pad Mixer now follows the persistent MPC workspace model instead of the legacy four-strip diagnostic layout:

- 16 Drum Pad strips are represented in one horizontally scrollable mixer workspace, with eight compact strips visible at the target density.
- Each strip keeps Pad selection, sample identity, Level, Pan and Tune in one local context.
- Level uses a vertical fader surface; Pad selection is the primary red semantic selection.
- Data Dial focus is explicit and visible: LEVEL -> PAN -> TUNE, with Data Dial press cycling the three focused controls.
- Data Dial deltas dispatch to the existing native pad Level/Pan/Tuning setters; no realtime callback, sampler, sequencer or MIDI decoder code is involved.
- Channel Mixer remains RESERVED until truthful track-strip mixer semantics exist.

Browser chrome now also uses the same flat MPC selection language as the migrated shell: rectangular surfaces, red selected state, and zero-radius framing.


## 2026-10-04 Toolbar / Browser / Menu fidelity checkpoint

- Toolbar chrome is graphite/status-oriented; red is reserved for semantic selection/accent surfaces.
- Browser uses the MPC hierarchy `Places / Content / Expansions`, official file-type filter vocabulary, and keeps Sample Assign/Audition/Load on the shell Function Bar.
- Browser target Pad/Layer and current sample are compact read-only context state; the workspace no longer reserves a duplicate 190dp target command panel.
- Menu remains a 4×4 launcher. System commands are exposed through the shell Function Bar rather than a nested second footer.
- Pad Mixer is a 16-pad workspace with eight visible compact strips and explicit Data Dial Level/Pan/Tune focus.


### 2026-10-04 Main shell fidelity correction

The Main screen is now being aligned to the actual MPC 3.9 composition rather than a generic “MPC-like” shell. The visible left region is the five-shortcut rail followed by XL Mixer Strips; the old persistent Sequence/Track/Program/Data Dial rail is hidden migration scaffolding only.

The Track/Pad strip choice remains in the lower-right of the Main Track/Arrangement section, matching the documented MPC interaction placement. Main Toolbar presentation remains graphite in Main; red is reserved for semantic selection/accent surfaces.


## 2026-10-04 XL Channel Strip fidelity checkpoint

Main's visible left-side composition is now treated as a real MPC 3.9 XL Channel Strip rather than a generic Android mixer card:

`five shortcuts -> XL Channel Strips -> Main workspace -> Function Bar`.

The strip layout follows the documented relationship:
- selected Track -> Main Output on the right;
- selected Drum Pad -> corresponding Track on the right;
- top visibility affordance controls expanded/collapsed strip detail;
- LVL remains the implemented strip view while FX/SEND/I/O are reserved;
- Track strips expose Mute/Solo/Automation/Record according to documented availability;
- Pad strips do not invent Track-only controls;
- the strip uses a dense vertical meter/fader and pan control rather than large Android-style value cards.

The strip is presentation-first. Semantic ownership remains in `MpcUiState`, `MpcNavigationController`, MainActivity/native state, and the protected audio/MIDI layers.

Source references:
- MPC Standalone OS User Guide v3.9, Mixer Strips section, pp. 135-136.
- Akai Professional, MPC3 FAQ: XL Channel Strip / Full-Color Track and Pad Mixer / One-to-One Track Workflow.

Remaining fidelity gap:
- dynamic right-strip switching for SEND/return context;
- FX insert surface;
- SEND knob surface;
- I/O surface;
- final physical MkII verification.


## 2026-10-04 Main Track Type UI correction

Fidelity correction: the Main Track header now renders one Track Type icon, matching the documented MPC workflow. The previous six-icon cluster has been removed from the persistent header. Six Track Types remain available conceptually through Track Type selection, but only the currently selected type is shown in the Main header.

Interaction contract:
- tap Track Type icon -> Track Type Select context;
- Data Dial focus -> TRACK_TYPE;
- current type is shown by the icon;
- unsupported backend types remain unavailable rather than being visually presented as independently selectable Main buttons.

This supersedes the earlier six-icon-cluster description.


## 2026-10-04 Main Track identity geometry correction

The Track identity band now places the single Track Type icon immediately before the Track number/name block, keeping the icon spatially attached to Track identity as described by the MPC 3.9 workflow. The Track Type icon is the only persistent type affordance; its selection surface opens the Track Type context. This replaces the earlier six-icon header interpretation.


## 2026-10-04 Main Track controller-first layout correction

Canonical Main Track composition is now controller-first: no on-screen 4x4 pad matrix is rendered inside Main. Pads are selected on the MPC Studio MkII, and the phone display uses the resulting selected Pad/Layer state in the waveform surface. This keeps the Main screen structurally faithful to MPC 3.x instead of adding Android-specific performance controls.


## 2026-10-04 XL Channel Strip top-control fidelity

The visible XL Channel Strip region keeps its show/hide control compact and icon-only. A textual "MIXER" header is not part of the strip identity and must not consume the strip's vertical workspace budget. The top control remains the single visibility affordance for the XL strip region.

## 2026-10-04 Shortcut Rail selection indicator fidelity

The five shortcut cells remain dark graphite in both selected and unselected states. The selected shortcut is identified by a narrow red edge indicator rendered by the same deterministic icon drawable used for the rail pictogram. This is presentation-only and does not change navigation or hardware routing.

