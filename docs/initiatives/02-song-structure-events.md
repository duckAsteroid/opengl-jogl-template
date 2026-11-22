---
type: feature
status: done
tags: [music-analysis]
updated: 2026-11-22
docs_impact:
  - render-core/src/agent-docs/music.md
  - CLAUDE.md
---
# Song structure events

## Goal

Causal detection of breakdowns, build-ups, drops, general section changes and track changes (in gapless mixes), delivered as listener events and as current-section state, so scripts can change visual phase with the music. Scored on structured synthetic tracks in beatEval.

## Dependencies

- [1 Music state descriptors and intensity](01-music-state-descriptors-and-intensity.md) — built on the music state descriptors

## Open questions

_None._

## Acceptance criteria

- A structured synthetic track (intro → groove → breakdown → build-up → drop → groove), added to
  `SyntheticTracks` but not to the beat-eval set, produces in tests: `BREAKDOWN_START` within 3 s
  of the breakdown, `BUILDUP_START` during the riser, `DROP` within 1 s of the drop, no
  `BREAKDOWN_START` on a steady groove, and `MUSIC_START`/`SILENCE_START` around silence.
- A gapless switch between two synthetic tracks of different tempo and key reports
  `TRACK_CHANGE`.

## Design

`StructureDetector`, run inside `MusicAnalyser` on each 50 ms frame, keeps a current `Section`
and reports `MusicEvent`s to `MusicListener.onEvent` (also queued by `MusicStateSink` for the GL
thread to drain with `pollEvent()`, capped at 64, plus a `section()` in each `MusicState`).

| Section / event | Rule (all with hysteresis) |
|---|---|
| `SILENCE` / `SILENCE_START`, `MUSIC_START` | silence gate from ticket 1; `MUSIC_START` is also the best "new track" signal |
| `GROOVE` | the default: the main body of the music |
| `BREAKDOWN` / `BREAKDOWN_START` | after ≥ 8 s with kicks, kicks gone for 1.5 s *and* bass or intensity down. Music that never had kicks never breaks down |
| `BUILDUP` / `BUILDUP_START` | in a breakdown, "tension" (brightness + noisiness + busyness + intensity) has risen steadily over the last 4 s |
| `DROP` | from a breakdown or build-up, two kicks within 1.5 s with the bass back up → `GROOVE` |
| `SECTION_CHANGE` | novelty: distance between the last 4 s of descriptors (and chroma) and up to 16 s before — only since the last event — scaled by the earlier spread, crosses a threshold; ≥ 8 s apart |
| `TRACK_CHANGE` | a `SECTION_CHANGE` in a groove whose first 4 s of new material has a tempo > 4 % away (ignoring double/half) or a different key signature (gapless mixes) |

Events are reported when detected — typically 1–2 bars after the change for `SECTION_CHANGE`,
within a beat or two for `DROP`. Scripts that want transitions on the beat snap them to the next
`BeatTrackerSink.beatPhase()` wrap. Phrase alignment (8/16/32 bars) is out of scope: it needs
downbeats, which the downbeat spike showed simple cues can't find.
