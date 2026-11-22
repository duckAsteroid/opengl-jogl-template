---
type: feature
status: done
tags: [music-analysis]
updated: 2026-11-22
docs_impact:
  - render-core/src/agent-docs/music.md
  - CLAUDE.md
---
# Key, mode and mood (arousal/valence)

## Goal

Estimate musical key and major/minor mode from chroma, and publish arousal (well-founded, from intensity/tempo/busyness/brightness) and valence (a weak hint, from mode and brightness) in the music state snapshot.

## Dependencies

- [1 Music state descriptors and intensity](01-music-state-descriptors-and-intensity.md) — arousal is derived from the music state descriptors

## Open questions

_None._

## Acceptance criteria

- Tests: synthetic chord loops in C major and A minor (and a transposed one) are identified
  within 10 s; a key change is followed with hysteresis (no flicker); arousal is higher for the
  `four-on-floor-128` groove than for a sparse pad; valence is higher for the major loop than the
  minor one.
- Agent doc covers key, arousal and valence, including valence's limits.

## Design

- **Chroma** — 12 pitch classes from the 170 ms FFT `MusicAnalyser` already takes (the approach
  from `spike/downbeat-chroma`'s `Chroma`, 100 Hz–4 kHz, log-magnitude, unit length).
- **Key** — chroma accumulated with a ~15 s exponential average, correlated against the 24
  rotated Krumhansl–Kessler major/minor profiles. A new key replaces the current one only after
  leading it by a margin for 4 s. `MusicState` gains `key` (a `Key` record: tonic pitch class,
  `major`, `name()` e.g. "A minor"; null until known) and `keyConfidence` (`[0, 1]`, from the best
  correlation and its lead over the runner-up).
- **Arousal** `[0, 1]`: weighted intensity, busyness, brightness, kick rate and tempo (tempo
  `(bpm − 60) / 120`, dropped and re-weighted when unknown), ~3 s smoothing. Well founded: these
  are the features arousal is known to track.
- **Valence** `[0, 1]`, 0.5 neutral: major/minor (weighted by key confidence) plus a little
  brightness, ~8 s smoothing. Documented as a weak hint, not a label.
