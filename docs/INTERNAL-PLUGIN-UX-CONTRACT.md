# Internal Plugin UX Contract

Status: CANONICAL PRODUCT CONTRACT
Applies to all future built-in instruments and effects.

## 1. Purpose

Every internal processor must behave like a native part of the MPC-style workstation, not like an embedded desktop plugin window.

## 2. Common product model

Each built-in processor has:
- application-owned type ID;
- display name;
- category;
- parameter set;
- preset/state representation;
- automation support declaration;
- latency/tail declaration;
- hardware control mapping;
- availability/capability flags.

## 3. MPC shell integration

The plugin UI appears inside the canonical MPC 3.9 shell:

Toolbar -> five shortcuts/context -> workspace -> Function Bar

The processor may own the workspace content but does not create a second top-level navigation shell.

## 4. Touch behavior

Controls must be usable on the landscape Android display.

Parameter controls should support:
- direct touch editing;
- readable current value;
- bounded range;
- clear selected/focused state;
- predictable reset/default action where appropriate.

Do not require a tiny desktop mouse-oriented control.

## 5. MPC Studio MkII behavior

Hardware is controller-first.

Where a processor is focused, the Data Dial and +/- actions should operate on an explicitly documented parameter focus.

Physical button or pad actions must enter through semantic controller commands. UI code must not inspect raw MIDI identifiers.

Note Repeat, transport and other global hardware semantics must remain available according to the active MPC context.

## 6. Parameter design

Prefer a small number of high-value macro controls over exposing every internal DSP coefficient.

Typical effect surface:
- primary amount/input;
- tone/filter;
- time/rate;
- feedback/depth;
- mix;
- output;
- bypass where appropriate.

Typical synth surface:
- oscillator/macro controls;
- filter;
- envelope;
- modulation;
- voice/polyphony where meaningful.

Advanced parameters may exist on secondary pages, but the first page must be performance-oriented.

## 7. State and automation

Parameter automation stores application-owned parameter IDs and values.

Do not persist transient pointers, object addresses or upstream library-specific GUI state.

Preset/state loading must be deterministic and version-aware.

## 8. Latency and bypass

Every effect declares whether it has measurable latency and tail.

Bypass behavior must be defined explicitly:
- hard bypass;
- wet/dry bypass;
- tail-preserving bypass;
or another documented policy.

Do not hide latency-producing processing behind a zero-latency declaration.

## 9. Presets and content

Factory presets, samples, wavetables, impulse responses and artwork are separate from source-code licensing.

Future factory content must be original/project-owned or independently cleared.

Do not import third-party factory banks merely because an engine is open source.

## 10. Branding

Use our application naming and original graphics.

Third-party copyright attribution belongs in the appropriate license/attribution surface.

Do not imply endorsement by an upstream DSP project.

## 11. Failure behavior

Unsupported or unavailable processors must show a truthful unavailable state.

A missing optional engine must not silently substitute a different instrument while keeping the old name/state.

Invalid state should fail safely and preserve the rest of the project.

## 12. Navigation entry

Every new built-in processor must document:
- MPC context that opens it;
- track/program ownership;
- selected pad/layer when relevant;
- hardware entry path;
- semantic command path;
- Function Bar actions;
- parameter focus order;
- bypass/close behavior.

## 13. UX acceptance criteria

Before calling a processor complete:
- [ ] Opens from a documented MPC context.
- [ ] Uses the canonical shell.
- [ ] Has a compact performance-oriented first page.
- [ ] Touch controls are usable in landscape.
- [ ] Data Dial focus is documented.
- [ ] +/- behavior is documented.
- [ ] State save/restore works.
- [ ] Automation mapping is stable.
- [ ] Bypass is deterministic.
- [ ] No foreign UI/assets are embedded.
- [ ] Physical-controller path is documented.
- [ ] Unsupported capabilities are truthful.