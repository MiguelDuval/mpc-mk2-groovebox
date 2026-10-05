# Third-Party SBOM — Initial Inventory

Status: INITIAL / NOT A RELEASE SBOM
Last reviewed: 2026-10-05
Branch: research/internal-dsp-library

> This is an engineering inventory. It does not constitute a final legal opinion or a release clearance.

## A. Currently integrated repository dependencies

| Component | Revision | License/status | Current use | Commercial status |
|---|---|---|---|---|
| JUCE | 9.0.2 | AGPLv3 / commercial JUCE licensing | native framework foundation | REVIEW — separate commercial license decision required |
| Tracktion Engine | 13b51326693e3227ddef91b224114d12af6433ce | GPLv3+ / Commercial | planned high-level audio/sequencing foundation | REVIEW — commercial Tracktion license required for proprietary distribution |
| Oboe | a81bb9f / 1.10.0 | Apache-2.0 | Android audio I/O | REVIEW — notice/verification required |
| Ableton Link | e9a2e41 / Link-4.0 | GPLv2+ / proprietary | future synchronization | REVIEW — proprietary license required for closed commercial path |

## B. Internal DSP candidates

| Component | Candidate role | License | Status | Next action |
|---|---|---|---|---|
| Airwindows | creative/character FX | MIT | CANDIDATE | select exact revision and source files |
| DaisySP core | synth + utility DSP | MIT core; LGPL modules separated upstream | CANDIDATE | pin exact revision; include MIT modules only |
| Signalsmith DSP | DSP primitives | MIT | CANDIDATE | pin exact revision and audit dependencies |
| Signalsmith Basics | limiter/reverb/chorus/etc. | MIT | CANDIDATE | audit submodules and selected classes |
| DSPFilters | filter/EQ | MIT | CANDIDATE | select exact revision and build subset |
| DSPark | modern C++20 DSP | MIT | CANDIDATE | benchmark Android build before adoption |
| sndkit | compact synthesis/DSP algorithms | MIT/Unlicense output | CANDIDATE | choose algorithm-level source subset |
| Gamma | synthesis/filtering/DSP | permissive MIT-style | CANDIDATE | component-level license and dependency audit |
| MSFA | FM/DX7-style synth engine | Apache-2.0 | CANDIDATE | select exact revision and verify source files |
| ymfm | FM/chip engine | BSD-3-Clause | CANDIDATE | evaluate against MSFA needs |
| STK | physical-model synthesis | MIT | CANDIDATE | source-selective integration review |
| sfizz | SFZ sample instrument | BSD-2-Clause; upstream archived 2026-06-21 | REVIEW candidate | pin known release/commit and test maintenance burden |
| CMSIS-DSP | ARM performance primitives | Apache-2.0 | CANDIDATE | add only where measured benefit exists |
| KISS FFT | FFT primitive | BSD-3-Clause | CANDIDATE | use only where existing FFT layer is insufficient |
| libsamplerate | high-quality resampling | BSD-2-Clause | CANDIDATE | use where engine/device conversion requires it |
| SpeexDSP | utility DSP/resampling | BSD-3-Clause | CANDIDATE | use selectively |

## C. Explicitly blocked under current product strategy

| Component family | Reason |
|---|---|
| Surge XT | GPL-3.0-or-later |
| Vital source | GPLv3; separate proprietary licensing path required for proprietary use |
| Helm | GPL-3.0 |
| Odin2 | GPL |
| Dexed | GPLv3 as complete plugin; use MSFA engine instead |
| ZynAddSubFX | GPL-2.0-or-later |
| SuperCollider | GPLv3 |
| VCV Rack | GPLv3/commercial model; not an internal proprietary dependency |
| Cardinal | GPLv3+ aggregate |
| Bespoke Synth | GPLv3 |
| MVerb | GPL-3.0 |
| Tunefish | GPL-3.0 |

## D. Required evidence before APPROVED

Every candidate needs:
- exact immutable revision;
- root and file-level license evidence;
- dependency/submodule audit;
- asset/preset audit;
- source file list actually compiled;
- CMake/build-path evidence;
- Android build result;
- realtime safety test result;
- CPU/memory measurements;
- state/preset implementation notes;
- updated THIRD_PARTY_NOTICES.md;
- this SBOM entry moved to APPROVED.

## E. Important interpretation

GREEN/YELLOW/RED in the research document is a triage classification.
CANDIDATE/REVIEW/APPROVED/BLOCKED/RETIRED in this SBOM is the integration/release state.

GREEN does not equal APPROVED.
APPROVED means the exact source that will ship has passed the repository's integration gates.