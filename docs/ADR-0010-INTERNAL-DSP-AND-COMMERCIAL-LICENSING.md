# ADR-0010 — Internal DSP Engines and Commercial Licensing

Status: Accepted
Date: 2026-10-05
Decision: Build internal instruments/effects from audited permissive DSP components; do not implement an external plugin host in this phase.

## Context

The product is an Android MPC Studio MkII-centered standalone-style groovebox intended to remain hardware-first and realtime-safe.

The project needs high-quality instruments and effects, but a generic VST/plugin-host architecture would introduce platform compatibility work, foreign UI lifecycles, ABI/runtime complexity, extra state/preset complexity, and a larger license/dependency surface.

Commercial distribution makes copyleft and asset licensing materially important.

## Decision

Use a native internal plugin-like architecture owned by this project.

Third-party DSP is integrated as implementation code behind application-owned adapters.

Preferred first-wave licenses:
- MIT
- BSD-2-Clause / BSD-3-Clause
- Apache-2.0
- clearly verified public-domain or CC0 code

LGPL and mixed-license projects are Yellow and require additional review.
GPL/AGPL or proprietary-only engines are Red under the current strategy.

## Rationale

This gives predictable Android integration, one realtime graph, unified MPC UI, application-owned parameters, application-owned presets/state, no arbitrary third-party plugin binaries, a smaller maintenance surface, and a finite license inventory.

It also permits use of strong engines such as Airwindows, DaisySP and MSFA without adopting their desktop plugin wrappers.

## Important precedent

A complete plugin repository and an underlying engine can have different licenses. Dexed is GPLv3 as a complete plugin while the MSFA engine it uses is separately distributed under Apache-2.0. Therefore evaluate the actual source that is compiled, not the license of a wrapper product.

## Consequences

Positive: internal effects can be designed for MPC workflows; UI/UX remains ours; engines can be optimized for ARM/Android; project state can be versioned with our schema; and compliance can be reduced to an auditable inventory.

Negative: we must maintain adapters; there is no automatic third-party plugin compatibility; each integrated engine becomes a maintenance responsibility; and licensing must be re-reviewed when source revisions change.

## Non-goals

This ADR does not establish a VST3 host, AAP host, LV2 host, CLAP host, arbitrary plugin support, compatibility with Akai proprietary plugins, or copying of third-party plugin UI.

## Review triggers

Revisit if a required engine is only available under copyleft; distribution requirements change; external plugins become a core product requirement; Android gains a materially better stable plugin ABI; or the relevant foundation licenses change.