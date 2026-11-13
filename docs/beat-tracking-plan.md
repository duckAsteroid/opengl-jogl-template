# Beat tracking plan

Goal: a real-time tracker that reports **tempo**, **beat phase** and **kick-drum hits** from live
audio, good enough to drive visuals on dance music. It replaces the energy-ratio
`BeatDetector`, which struggles there: it compares *normalised dB* values (so a kick 12 dB over
the bass bed is only a ~1.25× ratio), uses an 85 ms FFT window (4096 @ 48 kHz) that smears the
kick's attack, lumps the bass line in with the kick (20–250 Hz), and runs at the render frame
rate.

## Architecture

Runs on the audio thread as an `AudioSink`, independent of the display FFT and frame rate.

```
PCM ─► onset strength ─► tempo estimate ─► phase tracker ─► snapshot ─► GL thread
       (~100–200 fps)    (every ~0.5 s)    (every frame)    (lock-free)  extrapolates
            │
            └─► kick onsets (low-band flux + peak-pick)
```

Every analyser implements `BeatAnalyser` (`render-core/…/util/audio/beat/`) and reports events
to a `BeatListener`, timestamped by **samples consumed**, not the wall clock, so the same code
runs live and offline (faster than real time) and is evaluated exactly as it behaves live.

Planned public API (Phase 3):

```java
interface BeatTracker {
    double bpm();
    double beatPhase();      // 0..1 within the current beat, extrapolated to now
    long   beatCount();      // increments each beat; bar = beatCount / 4
    float  confidence();     // 0 = guessing/freewheeling, 1 = locked
    float  kickStrength();   // detected kick, decaying envelope
}
```

## Phases

### Phase 0 — evaluation harness ✅

- `beatEval` source set in `render-core` + `./gradlew :render-core:beatEval` task. Separate from
  unit tests; only the synthetic set runs in CI (as unit tests).
- Offline runner: decodes WAV/AU/AIFF via Java Sound (anything else via `ffmpeg` if on the
  `PATH`), any sample rate, feeds 512-sample blocks through a `BeatAnalyser`.
- Metrics (all scored after a 5 s warm-up, as `mir_eval` trims):
  - tempo **Acc1** (±4%) and **Acc2** (also ×2, ×½, ×3, ×⅓);
  - beat **F-measure** (±70 ms), **CMLt** / **AMLt** continuity (±17.5%);
  - **lock time** — first of 4 consecutive correct beats;
  - kick **F-measure** (±50 ms) where kick annotations exist (synthetic set only).
- Datasets: built-in synthetic set (clicks, four-on-the-floor, buried kick, rolling bass,
  off-beat bass, breakbeat, swung lo-fi hip-hop, breakdown, tempo change; mastered), plus local-only datasets (never committed).
- **Baseline**: the current `BeatDetector` as tuned in `SpectrumWave`, kicks = rising edges,
  tempo = median inter-onset interval. Later phases must beat it.

### Phase 1 — onset strength + kick detection
1024-point FFT, hop 256; multi-band log-power spectral flux (rises only). Separate 40–120 Hz
flux, peak-picked with median + k·MAD and a refractory period → kick events.
Exit: kick F on synthetic clearly above baseline.

### Phase 2 — tempo estimation
Autocorrelation of a rolling 6–8 s onset-strength window, tempo prior (centred ~120–128 BPM),
comb summing at 1×/2×/4× lags against octave errors, hysteresis before switching.
Exit: Acc1/Acc2 on GiantSteps + GTZAN, lock time.

### Phase 3 — beat phase (PLL) + `BeatTracker` API
Predict next beat, small phase/period corrections from onsets near the prediction, freewheel
through breakdowns, confidence from onset/beat alignment. Lock-free snapshot to the GL thread,
which extrapolates phase per frame with a configurable latency offset.
Exit: beat F / continuity on GTZAN-Rhythm (disco, pop, hip-hop especially) and own tracks.

### Phase 4 — wire into experiments
Beat phase in `SpectrumWave` / `RadialSpectrumWave`, debug overlay (onset curve, BPM, phase,
confidence), tune latency offset. Retire `BeatDetector` or keep it as the simple option.

### Later (optional)
- Downbeat / bar detection (phrase-aware visuals: 4/8/16/32-bar changes, build-ups).
- Small causal CNN replacing the Phase 1 onset function (ONNX Runtime); the harness decides
  whether it earns its keep. Same structure as madmom / BeatNet: network → activation,
  classic tracker → timing.

## Baseline (Phase 0)

`./gradlew :render-core:beatEval --args="--data ~/Downloads/GTZAN/genres --annotations ~/Downloads/gtzan_tempo_beat --synthetic"`
(original GTZAN `.au` release; ~70 s for all 1006 tracks):

| group | tracks | beat F | CMLt | AMLt | Acc1 | Acc2 | lock s (locked) | kick F |
|---|--:|--:|--:|--:|--:|--:|--:|--:|
| blues | 100 | 0.266 | 0.173 | 0.237 | 44% | 53% | 8.0 (26%) | – |
| classical | 100 | 0.174 | 0.045 | 0.096 | 19% | 23% | 12.8 (28%) | – |
| country | 100 | 0.394 | 0.161 | 0.413 | 31% | 56% | 5.5 (48%) | – |
| disco | 100 | 0.528 | 0.522 | 0.625 | 74% | 76% | 2.2 (67%) | – |
| hiphop | 100 | 0.283 | 0.051 | 0.212 | 25% | 29% | 11.1 (13%) | – |
| jazz | 100 | 0.381 | 0.217 | 0.289 | 32% | 46% | 5.1 (56%) | – |
| metal | 100 | 0.139 | 0.055 | 0.141 | 29% | 37% | 3.1 (13%) | – |
| pop | 100 | 0.312 | 0.137 | 0.292 | 32% | 36% | 4.1 (23%) | – |
| reggae | 99 | 0.376 | 0.177 | 0.313 | 29% | 39% | 6.6 (57%) | – |
| rock | 100 | 0.264 | 0.098 | 0.217 | 40% | 44% | 9.9 (32%) | – |
| synthetic | 7 | 0.861 | 0.797 | 0.832 | 83% | 83% | 2.2 (86%) | 0.930 |
| **all** | 1006 | 0.315 | 0.168 | 0.287 | 36% | 45% | 5.9 (37%) | 0.930 |

The first synthetic set (7 dry tracks, row above) was too easy: the baseline was perfect on
five of them. It has since been hardened — a master bus (reverb, noise, fast bus compressor,
soft clip), an un-ducked sub under a quiet kick, an off-beat house bassline, an 8th-note lead,
humanised timing and a swung 90 BPM lo-fi hip-hop track (9 tracks). Baseline on the hardened set:

| track | beat F | AMLt | Acc2 | kick F |
|---|--:|--:|--:|--:|
| clicks-120 | 1.000 | 1.000 | 100% | – |
| four-on-floor-128 | 1.000 | 1.000 | 100% | 1.000 |
| buried-kick-140 | 0.558 | 0.279 | 100% | 0.326 |
| rolling-bass-124 | 0.000 | 0.000 | – | 0.000 |
| offbeat-bass-122 | 0.991 | 0.982 | 100% | 0.844 |
| breakbeat-130 | 0.277 | 0.245 | 0% | 1.000 |
| lofi-hiphop-90 | 0.829 | 0.707 | 100% | 0.563 |
| breakdown-126 | 0.807 | 0.628 | 100% | 0.777 |
| tempo-change-120-128 | 1.000 | 1.000 | – | 0.994 |
| **mean** | 0.718 | 0.649 | 86% | 0.688 |

Pass marks are set relative to this baseline; for reference, published offline trackers
reach beat F around 0.8 on GTZAN, and online ones somewhat lower.

## Test data

None of this is committed — point the harness at a local directory.

| Dataset | Content | Annotations | Notes |
|---|---|---|---|
| Synthetic (built in) | 9 generated dance-style tracks | beats, kicks, tempo | Exact labels; covers the hard cases on purpose |
| GTZAN | 1000 × 30 s, 10 genres, 22.05 kHz mono | none on its own | No electronic genre; disco/pop/hip-hop closest. Known duplicates/mislabels/distortions; copyright unclear → local use only |
| GTZAN-Rhythm (Marchand & Peeters) | — | beats (with bar position), tempo for GTZAN | Turns GTZAN into a beat benchmark. Plain-text copy at `github.com/TempoBeatDownbeat/gtzan_tempo_beat` (`beats/gtzan_blues_00000.beats`, `tempo/gtzan_blues_00000.bpm`; 999 tracks, one reggae clip unannotated). No licence stated |
| GiantSteps Tempo | ~660 × 2 min EDM previews | global tempo only | The dance-music tempo check; audio is MP3 → needs `ffmpeg` |
| Own tracks | 5–10 dance tracks | hand-tapped beats (+ kicks if keen) | The only data that truly matches live use |

The genre-classification repo (Manishankar9977/Music-genre-classification) is only notebooks over
GTZAN; it has no audio or beat labels of its own.

### Directory conventions

`--data DIR` is walked for audio files (`.wav .au .aif .aiff .mp3 .flac .ogg`).
`--annotations DIR` (defaults to `--data`) is walked for annotation files, matched to audio by
file stem (`blues.00000.wav` ↔ `blues.00000.beats`, `blues.00000.jams` or
`blues.00000.wav.jams`). The audio file's parent directory is its group in the report (e.g.
the GTZAN genre).

| Extension | Content |
|---|---|
| `.beats` / `.beat` | one beat per line; first number on each line is the time in seconds (`#` comments, headers, CSV and Sonic Visualiser exports all fine) |
| `.kicks` | same format, kick onset times |
| `.bpm` | a single tempo in BPM |
| `.jams` | JAMS JSON; `beat`/`beat_position` and `tempo` namespaces are read |

Without a tempo annotation the reference tempo is derived from the median beat interval.

## Running

```bash
# Synthetic set only (no data needed)
./gradlew :render-core:beatEval

# A local dataset, plus the synthetic set
./gradlew :render-core:beatEval --args="--data ~/beat-data/gtzan/genres_original --annotations ~/beat-data/gtzan-rhythm --synthetic"

# Just the disco tracks
./gradlew :render-core:beatEval --args="--data ~/beat-data/gtzan/genres_original --filter disco"
```

Options: `--analyser NAME|all` (default `all`), `--filter REGEX` (matched against
`group/name`), `--out DIR` (default `render-core/build/beat-eval`). Writes `results.csv`
(per track) and `summary.md` (per group), and prints the summary.
