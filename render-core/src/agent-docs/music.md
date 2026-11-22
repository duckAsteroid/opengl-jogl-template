# Music State: Intensity, Structure, Key and Mood

Package `com.asteroid.duck.opengl.util.audio.music`. Turns live audio into a few slow-moving
control signals and structural events for scripting visuals: how intense, bright, noisy and
busy the music is, whether the kick is in, its key and mood, and when it breaks down, builds up,
drops or changes section or track. Use it alongside [beat.md](beat.md)'s `BeatTrackerSink`, which
handles timing *within* the beat.

| Use | Class |
|---|---|
| Live visuals: poll the state and drain events on the render thread | `MusicStateSink` |
| Callbacks on your own thread, or offline | `MusicAnalyser` + `MusicListener` |

---

## MusicStateSink — live use (start here)

```java
private static final float SAMPLE_RATE = 48_000f;
private final BeatTrackerSink beats = new BeatTrackerSink(SAMPLE_RATE);
private final MusicStateSink music = new MusicStateSink(SAMPLE_RATE, beats); // tempo from beats
private AudioInput audioInput;

@Override
public void init(RenderContext ctx) {
    audioInput = AudioInput.start(ctx, List.of(freqProc, beats, music));
}

@Override
public void doRender(RenderContext ctx) {
    MusicState s = music.state();
    glow = s.intensity();
    hue  = s.valence();
    for (MusicEvent e; (e = music.pollEvent()) != null; ) {
        switch (e.type()) {
            case DROP -> scene.explode();
            case BREAKDOWN_START -> scene.calm();
            case TRACK_CHANGE, MUSIC_START -> scene.next();
            default -> { }
        }
    }
}
```

- `state()` is never null (`MusicState.INITIAL` before any audio). A new state is published
  every 50 ms.
- `pollEvent()` returns the oldest undelivered event, or null. The queue holds 64. If nothing
  drains it, the oldest events are dropped.
- `new MusicStateSink(rate)` without a `BeatTrackerSink` works too. Arousal then leaves tempo
  out, and track changes are spotted by key alone.
- Same input format as `BeatTrackerSink`: 16-bit little-endian stereo, mixed to mono.
- Costs about 1% of real time on the audio thread.

---

## MusicState

All values are smoothed (time constant in brackets). They are in `[0, 1]` unless stated.

| Field | Meaning |
|---|---|
| `time` | Sample-clock seconds |
| `levelDb` | 400 ms RMS level, dBFS |
| `silent` | Below −60 dBFS for the last 1.5 s |
| `intensity` | Loudness relative to the music's own recent range (0.5 s). Near 1 in the main body, low in breakdowns, whatever the playback volume. A uniformly quiet piece drifts back up |
| `bass` | < 150 Hz level, relative to its recent range (0.5 s) |
| `brightness` | Spectral centroid, log-mapped 200 Hz → 0, 8 kHz → 1 (0.5 s) |
| `noisiness` | Spectral flatness: tones ≈ 0, white noise ≈ 1. Rises with risers and sweeps (0.5 s) |
| `busyness` | Onsets per second over the last 3 s; 8/s = 1 |
| `kickRate` | Kicks per second over the last 3 s; 2.5/s = 1 (128 BPM four-on-the-floor ≈ 0.85) |
| `low` / `mid` / `high` | Energy share < 250 Hz, 250 Hz–4 kHz, > 4 kHz; they sum to 1 |
| `key` | `Key` (`tonic` 0 = C…11 = B, `major`, `name()` → `"A minor"`), or null for the first ~3 s |
| `keyConfidence` | Fit to the key profile, halved when another key (often the relative major/minor) is close |
| `arousal` | Energy: intensity, busyness, brightness, kick rate and tempo combined (3 s). 0.5 neutral |
| `valence` | **Weak** positive/negative hint: major vs minor (× key confidence) plus a little brightness (8 s). 0.5 neutral; most music stays within 0.3–0.7 |
| `section` | `SILENCE`, `GROOVE`, `BREAKDOWN` or `BUILDUP` |
| `sectionStart` | Sample-clock time the current section began |
| `novelty` | How different the last 4 s are from the rest of the current section; section changes fire at 1.5 |

- **Key** is a ~15 s average with 4 s hysteresis. It is stable, but a key change takes about
  15–25 s to show. It resets when music starts after silence.
- **Arousal is reliable; valence isn't.** Valence can't tell happy from sad lyrics or tell a
  bright minor track from a dark one. Use it for a slow palette drift, not a mood label.

---

## MusicEvent

`MusicEvent(type, time, section)`: `section` is the one in force *after* the event.

| Type | When | Typical lag |
|---|---|---|
| `MUSIC_START` | Sound after silence, or at the start. Usually a new track | immediate |
| `SILENCE_START` | Below −60 dBFS for 1.5 s | 1.5 s |
| `BREAKDOWN_START` | After ≥ 8 s of groove with a kick: no kick for 1.5 s, and bass or intensity well down | 1.5–3 s |
| `BUILDUP_START` | In a breakdown, tension (brightness + noisiness + busyness + intensity) rising steadily over 4 s | 1–5 s into the riser |
| `DROP` | From a breakdown or build-up: two kicks within 1.5 s with the bass back | < 1 s |
| `SECTION_CHANGE` | The last 4 s sound markedly different from the rest of the section | 2–4 s |
| `TRACK_CHANGE` | Follows a `SECTION_CHANGE` in a groove when the new material's tempo (> 4 %, ignoring double/half) or key signature differs | 4–10 s after the change |

- **Snap to the beat:** events arrive when detected, not on the beat. For a transition that lands
  on the beat, start it at the next `BeatTrackerSink.beatPhase()` wrap.
- A `SECTION_CHANGE` can come a second or two before a more specific event for the same change
  (e.g. just before `BREAKDOWN_START`).
- Music that never had a kick (ambient, classical) stays `GROOVE` and only gets
  `SECTION_CHANGE`s.
- A modulation inside one track can also be reported as a `TRACK_CHANGE`. Kick rolls during a
  build-up, if they come with bass, can trigger an early `DROP`.
- Phrase alignment (every 8/16/32 bars) isn't available: it would need downbeats, which aren't
  detected.

---

## MusicAnalyser — offline or on your own thread

```java
MusicAnalyser analyser = new MusicAnalyser(sampleRate, new MusicListener() {
    @Override public void onState(MusicState state) { ... }   // every 50 ms
    @Override public void onEvent(MusicEvent event) { ... }   // before that frame's onState
});
analyser.setTempo(bpm);                       // optional, whenever the tempo changes
analyser.process(samples, offset, length);    // mono, [-1, 1], any block size
```

Single-threaded and on the sample clock, like `BeatTracker`. Running a file through faster than
real time gives exactly the states and events a live run would.

Tests run it over generated tracks with known sections from the `beatEval` source set:
`SyntheticTracks.arranged128()` (intro, groove, breakdown, build-up, drop) and
`SyntheticTracks.mixedTracks()` (gapless 124 → 132 BPM, A minor → D minor). These tracks are not
in `SyntheticTracks.all()`, so beat-eval results are unaffected.
