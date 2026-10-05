# Third-Party SBOM and License Policy

Status: CANONICAL RELEASE-GATE POLICY
Purpose: make the future commercial APK auditable at source-component level.

## 1. Principle

The legal unit is the code/assets actually shipped, not the name of the GitHub repository.

For every third-party component we must know the upstream project, exact revision, source files, dependencies, generated code, presets/samples/assets, license, notices, and modifications.

## 2. Required record

| Field | Required |
|---|---|
| Component ID | yes |
| Project name | yes |
| Upstream URL | yes |
| Exact tag/commit | yes |
| SPDX identifier | yes |
| Copyright holders | yes |
| Source files included | yes |
| Dependencies | yes |
| License files/notices | yes |
| Local modifications | yes |
| Generated code | yes/no + provenance |
| Presets | yes/no + license |
| Samples/wavetables | yes/no + license |
| Artwork/fonts | yes/no + license |
| Trademark restrictions | yes/no |
| Binary/source redistribution obligations | yes |
| Commercial review status | yes |
| Reviewer/date | yes |

## 3. Status values

CANDIDATE — researched but not integrated.
REVIEW — source selected and undergoing audit.
APPROVED — exact shipped source and dependencies passed review.
BLOCKED — license, provenance, or technical issue prevents use.
RETIRED — no longer shipped.

GREEN/YELLOW/RED in the research document are triage labels, not release authorization.

## 4. Exact-revision rule

Never ship source from a floating branch or unpinned package range.

Record an exact release tag or immutable commit SHA.

When upgrading an engine, create a new SBOM review event.

## 5. Transitive dependency rule

A permissive top-level license does not automatically clear its dependencies.

Inspect git submodules, vendored folders, generated source, optional build targets, platform-specific implementations, and test libraries accidentally included in production.

Build configuration must prevent optional copyleft modules from entering the commercial artifact.

## 6. Asset rule

Code licensing is separate from presets, samples, impulse responses, wavetables, fonts, graphics, demo media and factory sound banks.

Do not copy factory content merely because source code is open.

Future factory content must be project-owned, independently licensed, or deliberately generated/original.

## 7. Required repository files

Maintain THIRD_PARTY_NOTICES.md and docs/THIRD-PARTY-SBOM.md.

A future release process should archive the exact SBOM used for the shipped APK.

## 8. Notice policy

For MIT/BSD/Apache components retain required notices and attribution. Do not alter upstream notices in a way that obscures provenance.

Copyleft components are not Approved merely because source archives are available; route them through the Yellow/Red policy and legal review.

## 9. Code-review gate

A pull request integrating third-party DSP must include exact upstream URL, exact commit/tag, license, source file list, dependency list, build path, test evidence, CPU measurement, state/preset handling, and a notice update.

No DSP code should be merged without the corresponding provenance record.

## 10. Commercial-release gate

Before public commercial release:
1. generate the final dependency inventory;
2. compare it against compiled object/library inputs;
3. verify all notices;
4. verify all assets;
5. verify no blocked source entered through optional CMake targets;
6. verify current JUCE/Tracktion/Link licensing;
7. archive the final SBOM;
8. obtain final legal review from qualified counsel/license reviewer.

## 11. Current baseline warning

The repository currently records JUCE, Tracktion Engine, Oboe and Ableton Link as third-party dependencies. Their commercial licensing remains independent of the internal DSP policy.

## 12. Component record template

Component ID:
Project:
Upstream URL:
Exact commit/tag:
SPDX:
Copyright:
Source files:
Dependencies:
Generated code:
Presets/assets:
Modifications:
Redistribution obligations:
Trademark notes:
Status:
Reviewer:
Date: