# Sampler Audio Output and Waveform Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Restore audible sampler playback for bundled and newly recorded samples, make recording assignment reliably usable, and give the shared waveform substantially more vertical working area.

**Architecture:** Keep the existing single native Oboe output/mixer path and pad/layer snapshot model. Harden output-stream lifecycle/diagnostics using the current Oboe 1.10 callback APIs, add a small testable render-path contract so silent mixing cannot regress unnoticed, and make waveform height consume the free vertical space while leaving inspector controls below it intact.

**Tech Stack:** C++20, Oboe 1.10, Android Java UI, GitHub Actions.

**Spec:** docs/PROJECT_MASTER_PROMPT.md and docs/WAVEFORM-SYSTEM.md

## Global Constraints

- Audio-first: realtime audio correctness takes priority over cosmetic polish.
- Landscape/horizontal UI is mandatory.
- Keep one shared output mixer; do not create one output stream per pad.
- Keep realtime callback free of unbounded allocation/blocking.
- Waveform remains a first-class interaction surface.

## Review Focus

- Output stream starts but callback does not deliver audible data.
- Callback delivers frames but render path produces silence because sample/region/voice state is invalid.
- Recorded audio is visible but assignment/restart loses the sample from the output snapshot.
- Waveform expansion must not push content off-screen on the short landscape target.

---

### Task 1: Make sampler output observable and robust

**Files:**
- Modify: `src/Audio/AudioEngine.cpp`
- Modify: `src/Audio/AudioEngine.h`
- Test: add a focused pure render/output-state test and wire it into `.github/workflows/android-build.yml`

**Interfaces:**
- Output callback must publish callback/frame/peak health through atomics owned by AudioEngine.
- Stream start must use the current Oboe request-start path and expose negotiated stream properties.
- Output error handling must be registered with a lifetime-safe shared callback.

- [ ] Step 1: Add the failing render/output health test first.
- [ ] Step 2: Run the focused test and observe the expected failure.
- [ ] Step 3: Implement the minimal output lifecycle/diagnostic fix and keep the existing realtime mixer.
- [ ] Step 4: Run the focused test again and confirm it passes.
- [ ] Step 5: Run the complete native test suite/build through GitHub Actions.

### Task 2: Expand waveform working area

**Files:**
- Modify: `android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MainActivity.java`

**Interfaces:**
- Sample and recorder WaveformView instances keep their current data/gesture contracts.
- Fixed-height controls remain below the waveform; only the available middle region grows.

- [ ] Step 1: Add the failing UI/layout assertion for the enlarged waveform region.
- [ ] Step 2: Run the UI check and observe the expected failure.
- [ ] Step 3: Replace the hard 96dp waveform height with weight-based flexible space and keep bottom controls anchored.
- [ ] Step 4: Run the UI check again and confirm it passes.
- [ ] Step 5: Run the complete Android build/smoke suite.

---
