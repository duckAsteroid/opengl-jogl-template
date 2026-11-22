---
type: feature
status: done
tags: [music-analysis]
updated: 2026-11-22
docs_impact:
  - render-core/src/agent-docs/music.md
  - render-core/src/agent-docs/SKILL.md
  - render-core/src/agent-docs/beat.md
  - CLAUDE.md
---
# Music state descriptors and intensity

## Goal

A `MusicStateSink` beside `BeatTrackerSink` that publishes an immutable, smoothed snapshot of slow-moving musical descriptors (intensity, brightness, noisiness, busyness, percussiveness, band balance) safe to read every frame from the GL thread, as control signals for scripting the visualiser.

## Dependencies

_None._

## Open questions

_None._

## Acceptance criteria

- `MusicStateSink` is safe to read every frame from the GL thread; publishing allocates one
  small record per 50 ms frame.
- Unit tests on generated audio: intensity drops through the `breakdown-126` breakdown and
  recovers after it; brightness orders a low tone below a high one; noisiness is higher for noise
  than for a tone; kick rate is ~2.1/s on `four-on-floor-128` and ~0 in the breakdown; silence is
  flagged.
- Agent doc `render-core/src/agent-docs/music.md` describes it.

## Design

New package `com.asteroid.duck.opengl.util.audio.music`, mirroring the beat package's split
between a single-threaded analyser on the sample clock and a sink for live use.

- **`MusicAnalyser`** — feed mono `[-1, 1]` samples, any block size. Every 50 ms it Hann-windows
  the last ~170 ms (one FFT shared with chroma, see ticket 3) and publishes a `MusicState` to a
  `MusicListener`. It runs its own `KickDetector` and `SpectralFlux` (the whole analyser costs ~1% of real time) so
  it doesn't depend on a `BeatTracker`; tempo, which only feeds arousal, is pushed in with
  `setTempo(bpm, confidence)`.
- **`MusicStateSink`** — an `AudioSink` (16-bit LE stereo → mono, like `BeatTrackerSink`) that
  runs the analyser on the audio thread and publishes the latest immutable `MusicState` through
  a volatile field. An optional `BeatTrackerSink` supplies the tempo.
- **`MusicState`** (record, all `[0, 1]` unless stated):

| Field | Meaning | How |
|---|---|---|
| `time` | Sample-clock seconds | |
| `levelDb` | Short-term level, dBFS | 400 ms RMS |
| `intensity` | Loudness relative to this music's own range | short-term dB through an auto-ranging floor/ceiling (snap out instantly, relax 1 dB/s towards a 12 dB span) |
| `bass` | Sub/bass (< 150 Hz) level, auto-ranged like intensity | |
| `brightness` | Spectral centroid, log-mapped 200 Hz–8 kHz | |
| `noisiness` | Spectral flatness 100 Hz–16 kHz, log-mapped | rises with risers / white-noise sweeps |
| `busyness` | Onsets per second, /8 | peak-picked spectral flux over 3 s |
| `kickRate` | Kicks per second, /2.5 | `KickDetector` over 3 s |
| `low`/`mid`/`high` | Energy share < 250 Hz, 250 Hz–4 kHz, > 4 kHz (sum to 1) | |
| `silent` | Below −60 dBFS for 1.5 s | same gate as `BeatTracker` |

Descriptors are smoothed with ~0.5 s exponential averages, so they are safe to drive visuals
from directly. Mood and structure fields are added by tickets 2 and 3.
