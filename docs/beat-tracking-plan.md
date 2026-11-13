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

### Phase 1 — onset strength + kick detection ✅
`KickDetector` (`render-core/…/util/audio/beat/`): time-domain 80–250 Hz band-pass (4th-order
Butterworth each side), band level over a 20 ms sliding window every 5 ms, onset strength = dB
rise over the last 20 ms, peak-picked at ≥ 10 dB rise with a level gate, absolute floor and
100 ms refractory period; timed at the start of the rise. Registered in the harness as `kick`.

Findings, from the harness:
- **Band:** the first cut used 40–150 Hz and scored *worse* than the baseline (kick F 0.35).
  On a loud master the sub-bass is sustained near full scale, so a kick adds ~1 dB there; and
  the kick's tail beats against the sub, so the rebound ~120 ms later reads as an onset. The
  kick's attack (pitch drop from ~150 Hz, click) is what stands out: 80–250 Hz.
- **Level window:** per-hop (5 ms) power of a 45 Hz tone swings by many dB with phase; the
  level must be measured over ≥ ~20 ms or the ripple reads as onsets.
- **Threshold:** onset strength is zero on most hops, so median + k·MAD collapses to the
  minimum rise; a fixed 10 dB minimum is what actually decides (swept 2–12 dB).
- Synthetic kick F 0.688 → **0.840** (rolling bass 0.00 → 0.99). Still weak: off-beat house
  bass (0.66 — its plucks read as kicks) and the buried kick (0.34).
- GTZAN, kicks scored as beats (a proxy — GTZAN has no kick labels): beat F 0.315 → **0.425**
  overall; disco 0.53 → 0.69, hip-hop 0.28 → 0.41, pop 0.31 → 0.49.

Broadband onset strength: `SpectralFlux` — ~43 ms Hann window (2048 @ 48 kHz, 1024 @ 22.05
kHz), ~10 ms hop, magnitudes summed into 3-per-octave bands (30 Hz–16 kHz), `ln(1 + 1000·m)`,
half-wave-rectified frame difference summed over bands. Its peaks land within ~5–9 ms of the
GTZAN beat labels once frames are timed a *quarter* window before the window's end (the steepest
part of the Hann window, where the frame-to-frame energy increase peaks — not the centre).

### Phase 2 — tempo estimation ✅
`TempoEstimator`: autocorrelation of the last 8 s of flux (mean removed) every 0.5 s; candidate
period `L` scored `acf(L) + ½·acf(2L) + ¼·acf(4L)`, times a log-normal prior (120 BPM, σ = 1
octave), exponentially smoothed across updates (hysteresis), parabolic peak interpolation.
GTZAN Acc1 **70%** / Acc2 **89%** (baseline 36% / 45%); disco 94% / 98%; synthetic 100%.
Prior swept (centre 100–130, σ 0.5–3 octaves): 120 / 1.0 is at or near the best overall; 110
/ 0.75 is ~3 points better on disco/hip-hop/pop only because hip-hop is slow.

### Phase 3 — beat phase (PLL) ✅ (snapshot API still to do)
`BeatPhaseTracker`, wired into `BeatTracker` (registered in the harness as `tracker`):
- Acquire: comb search over the last 6 s of onset strength (flux / running mean + 2 × kick
  strength) for the best phase at the estimated period.
- Track: after each predicted beat, the strongest onset within ±20% of a period (Gaussian
  weighted towards the prediction) corrects the next beat by 0.3 × error and the period by
  0.05 × error; freewheels when nothing stands out (< 1.5 × mean).
- Re-acquire: adopt a tempo estimate that moves > 4%; jump phase when another phase's comb
  scores 1.5 × better (e.g. locked to the off-beat).
- Every beat is reported at its **predicted** time, never revised — the scores are what a live
  visual would get.

Results (GTZAN + hardened synthetic):

| group | beat F | CMLt | AMLt | Acc1 | Acc2 | lock s (locked) |
|---|--:|--:|--:|--:|--:|--:|
| blues | 0.658 | 0.538 | 0.647 | 71% | 89% | 3.8 (90%) |
| classical | 0.392 | 0.267 | 0.354 | 42% | 56% | 8.3 (58%) |
| country | 0.807 | 0.586 | 0.798 | 62% | 97% | 3.6 (88%) |
| disco | **0.933** | 0.896 | 0.941 | 94% | 98% | 3.3 (96%) |
| hiphop | 0.788 | 0.699 | 0.777 | 87% | 93% | 3.7 (91%) |
| jazz | 0.580 | 0.404 | 0.585 | 58% | 87% | 4.9 (82%) |
| metal | 0.745 | 0.550 | 0.712 | 60% | 88% | 3.9 (95%) |
| pop | 0.776 | 0.670 | 0.772 | 76% | 89% | 3.5 (82%) |
| reggae | 0.848 | 0.644 | 0.836 | 68% | 95% | 3.3 (95%) |
| rock | 0.807 | 0.699 | 0.786 | 82% | 94% | 3.6 (91%) |
| synthetic | 0.977 | 0.992 | 0.992 | 100% | 100% | 3.3 (100%) |
| **all** | **0.735** | 0.599 | 0.723 | 70% | 89% | 3.6 (87%) |

(Baseline overall: beat F 0.315, CMLt 0.168, AMLt 0.287.) Published *offline* trackers, which
see the whole track, reach beat F around 0.8 on GTZAN.

Parameter sweep (cached flux/kick/tempo streams per track, replaying only the phase tracker,
one parameter at a time, then combinations): every single change moved beat F by < 0.005 — the
first-guess values were already near a local optimum. Kick weight in the onset signal makes no
difference. Adopted a 6 s acquisition comb (was 4) and a 1.5× phase-switch ratio (was 1.3):
beat F 0.735 → **0.737**, AMLt 0.723 → **0.750** (fewer phase flips), disco 0.933 → **0.938**,
hip-hop AMLt 0.777 → 0.823. The remaining gap is metrical level — Acc1 70% vs Acc2 89% means
~1 track in 5 locks to double or half tempo — which needs structural work, not tuning.

Findings:
- **Frame clock drift.** The flux hop is a whole number of samples: at 22.05 kHz,
  round(220.5) = 221, i.e. 99.77 frames/s, not 100. Converting frame counts to time at the
  nominal 100/s drifted ~2.3 ms per second: beats ran ~45 ms early by mid-track (beat F 0.52),
  and tempo read 0.2% fast. At 48 kHz the hop is exact, which is why the synthetic set never
  showed it. `SpectralFlux.frameRate()` now gives the exact rate (regression test in
  `BeatTrackerTest`).
- Kicks arrive before the flux frame for their time exists; they were being dropped. They are
  now stored against their frame number.
- Residual timing against GTZAN labels: median −2 to −14 ms (disco −5 ms); synthetic +2.5 ms.
  The kick detector alone reads ~20 ms early on GTZAN vs ~6.5 ms on synthetic, so GTZAN's labels
  sit ~13 ms after physical onsets — a labelling convention, not something to tune for.

The snapshot API for the GL thread is `BeatTrackerSink` (Phase 4).

### Phase 4 — wire into experiments (SpectrumWave ✅, rest to do)
`BeatTrackerSink` (`render-core/…/util/audio/beat/`) is an `AudioSink`: add it to the
`AudioReader`'s sinks and it runs `BeatTracker` on the audio thread, publishing an immutable
snapshot (volatile) on every beat/kick/tempo event and every 100 ms. The render thread calls
`bpm()`, `confidence()`, `beatPhase()`, `beatCount()` and `kickStrength()`, extrapolated to
*now* from the snapshot's sample-clock time plus elapsed `nanoTime`; `setLatency(Duration)`
makes the visuals lead by the audio output latency.

Audio-thread cost (64-byte `AudioReader` chunks, 48 kHz): ~1% of real time, worst single write
0.32 ms against the ~2.7 ms capture line buffer — after moving the tempo autocorrelation to an
FFT (the direct sum stalled a write for ~2.4 ms every 0.5 s).

`SpectrumWave`: the **B** letter now flashes with detected kicks, and a BPM readout at the
bottom pulses on each tracked beat (dimmed by confidence). M and T still use `BeatDetector`.

To do: `RadialSpectrumWave`; a debug overlay (onset curve, phase, confidence); tune the
latency offset by eye/ear; decide whether to retire `BeatDetector`.

### Downbeats — spike, not shipped (branch `spike/downbeat-chroma`)
The harness now reads bar positions from GTZAN-Rhythm's second column (`1.173 1` = downbeat),
generates them for the synthetic set, and scores **downbeat F** (±70 ms) for any analyser that
reports `BeatListener.onDownbeat`.

Tried: per-beat-interval features fed to a causal vote — four accumulators (beat index mod 4,
decayed 0.9 per bar), leader kept with 10% hysteresis. Results on GTZAN (chance ≈ 0.25):

| features | with tracked beats: all / disco | with annotated beats: all / disco |
|---|--:|--:|
| ceiling (best bar position in hindsight) | 0.63 / 0.93 | 0.96 / 1.00 |
| onset strength, kick, 3-per-octave band change | 0.13–0.27 / 0.11–0.30 | 0.19–0.42 / 0.11–0.37 |
| bass (< 250 Hz) change | 0.26 / 0.25 (synthetic 0.83) | 0.41 / 0.30 |
| **chroma change** (12 pitch classes, 170 ms FFT) | 0.29 / **0.49** | 0.45 / 0.56 |
| chroma + 0.2 × bass change (implemented) | 0.28–0.30 / 0.46–0.49 | — |

Not shipped, because:
- Half right on disco, near chance elsewhere.
- It doesn't firm up with longer listening (disco clips looped ×3: 0.46 → 0.40) — phase jumps
  in the beat tracker shift the beat-index → bar alignment the vote relies on.
- Its confidence barely predicts correctness: downbeats reported at confidence > 0.5 were right
  40% of the time, < 0.1 26%.

Beat tracking isn't the bottleneck for dance music (disco ceiling 0.93 with tracked beats); the
features are. Next steps if revisited: anchor the vote to bar *time* rather than beat count so
phase jumps don't scramble it; beat-synchronous chroma with finer pitch resolution; or a
learned model (madmom-style RNN + DBN downbeat tracking is the reference — well above these
simple cues).

### Later (optional)
- Downbeat / bar detection (phrase-aware visuals: 4/8/16/32-bar changes, build-ups) — see the
  spike above; simple causal cues aren't good enough.
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
