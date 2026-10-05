# DSP Integration Gate

Status: CANONICAL ENGINEERING CHECKLIST
Applies to every internal synth/effect engine integrated into the MPC Groovebox.

## Gate 0 — Candidate

- [ ] Clear product role.
- [ ] Upstream project identified.
- [ ] Exact upstream URL recorded.
- [ ] License identified.
- [ ] Candidate tier recorded.
- [ ] No external plugin host required.

## Gate 1 — Legal/source audit

- [ ] Exact revision selected.
- [ ] Root license inspected.
- [ ] Every compiled source file inspected.
- [ ] Submodules inspected.
- [ ] Transitive dependencies inspected.
- [ ] Optional build targets inspected.
- [ ] Presets/samples/wavetables/IRs inspected.
- [ ] Copyright holders recorded.
- [ ] SPDX identifier recorded.
- [ ] Required notices captured.
- [ ] Trademark restrictions checked.
- [ ] No GPL/AGPL component enters the approved proprietary path.
- [ ] Yellow items have explicit review notes.
- [ ] SBOM entry created.

Passing this gate changes status from CANDIDATE to REVIEW, not APPROVED.

## Gate 2 — Adapter design

- [ ] Engine is behind MpcAudioNode.
- [ ] Instrument/effect role is explicit.
- [ ] Application-owned parameter IDs defined.
- [ ] Parameter ranges/defaults defined.
- [ ] Upstream IDs are not persisted directly.
- [ ] Application-owned state schema exists.
- [ ] Stable plugin/instrument type ID exists.
- [ ] State migration strategy exists.
- [ ] UI does not depend on upstream GUI code.
- [ ] Android/UI types do not enter realtime code.

## Gate 3 — Realtime safety

- [ ] No allocation in process.
- [ ] No blocking I/O in process.
- [ ] No mutex/condition wait in realtime path.
- [ ] No Android framework calls in realtime path.
- [ ] No disk/network access in realtime path.
- [ ] Buffer ownership is bounded.
- [ ] NaN/Inf and denormal behavior is understood.
- [ ] Reset is deterministic.
- [ ] Latency is measurable.
- [ ] Tail length is measurable.
- [ ] Parameter updates are realtime-safe.

## Gate 4 — Automated audio tests

- [ ] 16-frame blocks.
- [ ] 32-frame blocks.
- [ ] 64-frame blocks.
- [ ] 128-frame blocks.
- [ ] 256-frame blocks.
- [ ] 512-frame blocks.
- [ ] Supported sample rates.
- [ ] Silence input.
- [ ] Impulse input.
- [ ] Parameter min/max/default.
- [ ] Repeated automation.
- [ ] Reset after active processing.
- [ ] State save/restore.
- [ ] No NaN/Inf output.
- [ ] CPU benchmark recorded.

Instrument-specific:
- [ ] Note-on.
- [ ] Note-off.
- [ ] Velocity sweep.
- [ ] Pitch bend if supported.
- [ ] Repeated retrigger.
- [ ] Polyphony stress.
- [ ] Voice stealing.
- [ ] Sustain/modulation if supported.

## Gate 5 — Android

- [ ] NDK/CMake build succeeds.
- [ ] Debug APK builds.
- [ ] CI tests run where practical.
- [ ] Real Android device renders audio.
- [ ] Real-device CPU benchmark recorded.
- [ ] No audible instability under normal project load.
- [ ] Audio route changes do not corrupt state.
- [ ] Sample-rate changes follow engine contract.

## Gate 6 — MPC UX

- [ ] Plugin has a documented MPC context.
- [ ] Parameter page follows canonical shell.
- [ ] Parameters are touch-editable.
- [ ] Parameters are addressable through semantic hardware actions.
- [ ] Data Dial behavior documented.
- [ ] +/- behavior documented.
- [ ] Relevant pad/transport interactions preserved.
- [ ] Bypass/mute behavior explicit.
- [ ] Preset entry/exit behavior explicit.
- [ ] No foreign GUI/proprietary artwork embedded.

## Gate 7 — Release/provenance

- [ ] THIRD_PARTY_NOTICES.md updated.
- [ ] docs/THIRD-PARTY-SBOM.md updated.
- [ ] Exact source revision recorded.
- [ ] Local modifications recorded.
- [ ] Build path recorded.
- [ ] License files retained.
- [ ] Assets separately cleared.
- [ ] Commercial review status becomes APPROVED only after all gates pass.

## Gate outcome

BLOCKED — mandatory legal/realtime/build requirement fails.
REVIEW — technically viable but review items remain.
APPROVED — all gates pass.
RETIRED — previously approved engine removed from shipping build.

## First-wave target

1. one small Signalsmith Basics or Airwindows effect;
2. one DaisySP-based polyphonic synth;
3. one MSFA-based FM instrument.

Do not integrate several engines simultaneously. Prove the adapter contract with one effect first.