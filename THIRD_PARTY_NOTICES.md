# Third-Party Notices

## JUCE
https://github.com/juce-framework/JUCE
Target: 9.0.2
License: JUCE licensing / AGPLv3 options; review current upstream terms before distribution.

## Tracktion Engine
https://github.com/Tracktion/tracktion_engine
Target: develop commit 13b51326693e3227ddef91b224114d12af6433ce
License: GPL/Commercial. Appropriate Tracktion licensing is required for distributed products.

## Oboe
https://github.com/google/oboe
Target: 1.10.0 commit a81bb9f
License: Apache-2.0.

## Ableton Link
https://github.com/Ableton/link
Target: Link-4.0 commit e9a2e41
License: GPLv2+ or proprietary.

This file is informational. Perform a complete dependency/license audit before distributing binaries.


## Internal DSP research

Candidate internal DSP/instrument sources are tracked separately in:

- docs/INTERNAL-DSP-LIBRARY-RESEARCH.md
- docs/INTERNAL-DSP-LIBRARY-ARCHITECTURE.md
- docs/ADR-0010-INTERNAL-DSP-AND-COMMERCIAL-LICENSING.md
- docs/DSP-INTEGRATION-GATE.md
- docs/THIRD-PARTY-SBOM-POLICY.md
- docs/THIRD-PARTY-SBOM.md

These candidates are research/integration candidates only. They are not shipped dependencies until an exact source revision, file list, dependencies, assets and license obligations have been reviewed and the component has reached APPROVED in the SBOM.

Current first-wave licensing posture:
- MIT
- BSD-2-Clause / BSD-3-Clause
- Apache-2.0
- clearly verified public-domain / CC0 code

LGPL and mixed-license candidates remain REVIEW/YELLOW. GPL/AGPL/proprietary-only candidates remain blocked under the current commercial strategy.

Do not infer commercial clearance for the complete application from the internal DSP classification.