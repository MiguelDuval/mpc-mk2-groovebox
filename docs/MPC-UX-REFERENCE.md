# MPC Standalone UX Reference

Primary official reference:
https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf

Additional official references:
https://support.akaipro.com/en/support/solutions/articles/69000838263-akai-pro-mpc-series-standalone-mode-sampler-overview
https://support.akaipro.com/en/support/solutions/articles/69000857771-mpc3-faq

Target:
compact standalone MPC-style workflow, not MPC Desktop.

Important screens/contexts:

- Main
- Browser
- Sampler
- Sample Edit
- Grid
- Step Sequencer
- Track Edit
- Channel Mixer
- Pad Mixer
- 16 Levels
- Pad Perform
- Q-Link
- Next Sequence
- XYFX
- Project

The visual implementation must be original. Reproduce documented interaction patterns and information architecture without copying proprietary artwork or firmware resources.


## Mandatory orientation

The application uses a **horizontal / landscape display orientation** as a hard product requirement.

All primary UI layouts, screen compositions and interaction surfaces must be designed for a landscape canvas. Portrait orientation must not become the default or an alternate application mode.

This matches the intended standalone-workstation presentation and the horizontal display relationship of hardware such as the MPC One.

### Controller indication invariant

The MPC Studio MkII is the primary physical surface. Its buttons are not assumed to be self-explanatory: every latched or contextual action must expose its current state on the controller and on the Android screen.

The phone-side controller strip is persistent and answers four questions without navigation:
1. What MkII context is active?
2. What does Data Dial / +/- currently edit?
3. Which pad bank is active?
4. Is the context primary, alternate/Shift, active, or unavailable?

The controller side mirrors the same state using the MkII's available button LEDs, pad RGB, Touch Strip segments and LCD. A transient action-result message is never the sole indication of a latched context.

This is a production/live-performance invariant: a performer must be able to recover the current control state at a glance after looking away from the screen.

## Permanent migration rule — 2026-10-02

This document is now a compact pointer to the permanent migration program.

For detailed screen-by-screen architecture, source evidence, migration order and protected-layer rules, read:

- docs/MPC3-UI-MIGRATION-MASTER-SPEC.md
- docs/MPC3-REFERENCE-INDEX.md
- docs/UI-MIGRATION-SAFE-CHANGE-CONTRACT.md

The target is **MPC Standalone OS 3.9 information architecture and interaction behavior**, not a color/shape imitation.

The canonical shell is:

Toolbar → Five Shortcuts → Compact Mixer/Channel context → Main Workspace → Contextual Function Bar

Menu is the operating-system launcher.

The current seven-page rail, current diagnostic pages and Android Document Picker presentation are migration scaffolding and must not be treated as the final product architecture.

The existing sampler, sequencer, MIDI and feedback layers are preserved beneath this UI migration.
