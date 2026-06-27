# Audio Visualisation

## Architecture overview

The audio pipeline is split into three independent layers:

```
AudioDataSource (hardware line)
  └─ AudioReader (background thread)
       └─ AudioSink.write(byte[], offset, length)
            ├─ PboAudioSink  → GL 1-D texture  → AudioWave / RadialWave (GPU waveform)
            └─ RollingAudioBuffer → float[]    → SpectrumAnalyser (CPU FFT)
```

**Key principle:** `AudioWave` and `RadialWave` are pure renderers — they do not own threads,
manage PBOs, or know about audio capture. The experiment creates and wires up the pieces.

---

## PboAudioSink — the shared GPU audio texture

`PboAudioSink` owns a persistently-mapped PBO and a `GL_RG16_SNORM` 1-D texture.
It implements `AudioSink` so `AudioReader` writes raw PCM bytes into it directly.

### Creating and disposing

```java
// Factory — must be called on the GL thread (inside init())
PboAudioSink audioSink = PboAudioSink.create(AudioWave.AUDIO_BUFFER_SIZE, ctx);
// Disposal is registered with ctx.getResourceManager() automatically — no manual dispose() needed.
```

`stereoFrames` controls the ring-buffer size. Use the `AUDIO_BUFFER_SIZE` constant from the
renderer you plan to use (`AudioWave.AUDIO_BUFFER_SIZE` or `RadialWave.AUDIO_BUFFER_SIZE`;
both are `2048`).

### Per-frame upload

Call `upload()` **exactly once per frame**, before any renderer that uses this sink:

```java
// doRender():
audioSink.upload();          // DMA: PBO → GL_RG16_SNORM texture (one call, any number of renderers)
audioWave.doRender(ctx);
radialWave.doRender(ctx);    // both see identical data
```

`upload()` calls `glTexSubImage1D` with the PBO bound to `GL_PIXEL_UNPACK_BUFFER`, giving the
GPU direct access to the mapped write buffer with no intermediate copy.

### Implements AudioSink

```java
public interface AudioSink {
    void write(byte[] data, int offset, int length);
}
```

`PboAudioSink.write()` is safe to call from the `AudioReader` background thread. Internally it
ring-wraps into the persistently-mapped `ByteBuffer`; a `volatile int head` is advanced after
each write so the GL thread can snapshot it without locking.

---

## AudioReader — the capture thread

`AudioReader` runs a single background thread that drains an `AudioDataSource` and fans out
to one or more `AudioSink` implementations.

```java
AudioReader audioReader = new AudioReader(List.of(audioSink));   // one or more sinks
Thread audioReaderThread = new Thread(audioReader, "audio-reader");
audioReaderThread.setDaemon(true);
audioReaderThread.start();
audioReader.setLine(lineAcquirer.getSelectedSource());           // start capturing
```

### Switching audio source at runtime (safe from any thread)

```java
lineAcquirer.next();
audioReader.setLine(lineAcquirer.getSelectedSource());
```

### Shutdown

```java
audioReader.setRunning(false);
audioReader.setLine(null);
audioReaderThread.join(2000);
```

---

## LineAcquirer — audio device discovery

```java
LineAcquirer lineAcquirer = new LineAcquirer();
lineAcquirer.init(ctx, LineAcquirer.IDEAL);   // IDEAL = 48 kHz, 16-bit stereo

AudioDataSource current  = lineAcquirer.getSelectedSource();
AudioDataSource next     = lineAcquirer.next();      // returns old source, advances index
AudioDataSource previous = lineAcquirer.previous();  // wraps around
```

Pass `-Dsimulate.audio=true` to prepend a `SimulatedDataSource` to the list (useful for
headless testing and CI).

---

## AudioWave — scrolling horizontal waveform renderer

`AudioWave` draws a scrolling `GL_LINE_STRIP` of 1 024 vertices displaced vertically by the
audio amplitude. It is a pure `RenderedItem` — no threads, no PBO management.

```java
// Fields:
private PboAudioSink audioSink;
private AudioReader audioReader;
private Thread audioReaderThread;
private AudioWave audioWave;
private final LineAcquirer lineAcquirer = new LineAcquirer();

// init():
audioSink = PboAudioSink.create(AudioWave.AUDIO_BUFFER_SIZE, ctx);
audioWave  = new AudioWave(audioSink);
audioWave.init(ctx);

lineAcquirer.init(ctx, LineAcquirer.IDEAL);
audioReader = new AudioReader(List.of(audioSink));
audioReaderThread = new Thread(audioReader, "audio-reader");
audioReaderThread.setDaemon(true);
audioReaderThread.start();
audioReader.setLine(lineAcquirer.getSelectedSource());

// doRender():
audioSink.upload();
audioWave.doRender(ctx);

// dispose():
audioWave.dispose();
audioReader.setRunning(false);
audioReader.setLine(null);
audioReaderThread.join(2000);
```

### Channel modes

```java
audioWave.setChannelMode(AudioWave.CHANNEL_BLEND);   // L+R average (default)
audioWave.setChannelMode(AudioWave.CHANNEL_LEFT);
audioWave.setChannelMode(AudioWave.CHANNEL_RIGHT);
audioWave.setChannelMode(AudioWave.CHANNEL_STEREO);  // two lines: L above, R below
```

### Visual properties (applied on next frame, safe from any thread)

```java
audioWave.setLineWidth(4.0f);
audioWave.setLineColour(StandardColors.CYAN.color);
audioWave.setAmplitudeFunction(AmplitudeFunction.constant(10f));   // flat envelope
audioWave.setAmplitudeFunction(AmplitudeFunction.ellipse(10f));    // tapers to 0 at edges
```

---

## RadialWave — circular radial waveform renderer

`RadialWave` draws a `GL_LINE_LOOP` of 1 024 vertices arranged around a circle, displaced
radially by the audio amplitude. Same pure-renderer pattern as `AudioWave`.

```java
// init():
audioSink  = PboAudioSink.create(RadialWave.AUDIO_BUFFER_SIZE, ctx);
radialWave = new RadialWave(audioSink);
radialWave.init(ctx);
// ... AudioReader setup as above ...

// doRender():
audioSink.upload();
radialWave.doRender(ctx);

// dispose():
radialWave.dispose();
// ... AudioReader shutdown as above ...
```

### Channel modes

```java
radialWave.setChannelMode(RadialWave.CHANNEL_BLEND);  // L+R average (default)
radialWave.setChannelMode(RadialWave.CHANNEL_LEFT);
radialWave.setChannelMode(RadialWave.CHANNEL_RIGHT);
```

### Visual properties (applied on next frame, safe from any thread)

```java
radialWave.setLineWidth(3.0f);
radialWave.setLineColour(StandardColors.CYAN.color);
radialWave.setRadius(0.5f);                               // base circle radius in NDC-y units
radialWave.setAmplitudeFunction(AmplitudeFunction.constant(1f));  // flat envelope (default)
radialWave.setAmplitudeFunction(AmplitudeFunction.ellipse(1f));   // tapers to 0 at the seam
radialWave.setCenter(new Vector2f(0f, 0f));               // NDC centre (default = screen centre)
```

`RadialWave` automatically corrects for non-square viewports via an internal aspect-ratio
uniform — the circle stays visually round as the window is resized.

---

## Sharing a PboAudioSink between multiple renderers

Because `AudioWave` and `RadialWave` accept a `PboAudioSink` in their constructors and only
hold references to `getTextureId()` and `getHead()`, any number of renderers can share a
single sink. One `upload()` call per frame is enough — both renderers sample the same texture.

```java
// Both buffer size constants are 2048, so either works:
audioSink  = PboAudioSink.create(AudioWave.AUDIO_BUFFER_SIZE, ctx);

audioWave  = new AudioWave(audioSink);
radialWave = new RadialWave(audioSink);
audioWave.init(ctx);
radialWave.init(ctx);

// One AudioReader writes into one sink:
audioReader = new AudioReader(List.of(audioSink));

// doRender() — one upload, two renders, identical audio data:
audioSink.upload();
audioWave.doRender(ctx);
radialWave.doRender(ctx);

// dispose() — both renderers, one thread shutdown:
audioWave.dispose();
radialWave.dispose();
audioReader.setRunning(false);
audioReader.setLine(null);
audioReaderThread.join(2000);
```

You can also fan the `AudioReader` into multiple sinks simultaneously (e.g. a `PboAudioSink`
for the waveform and a `RollingAudioBuffer` for the FFT analyser):

```java
audioReader = new AudioReader(List.of(pboSink, rollingBuffer));
```

---

## AmplitudeFunction — custom amplitude envelopes

`AmplitudeFunction` is a `@FunctionalInterface` mapping `(vertexIndex, normalisedX) → amplitude`.
`normalisedX` is in `[-1, 1]`. Both `AudioWave` and `RadialWave` use the returned value as a
per-vertex scale factor multiplied directly by the normalised audio sample `[-1, 1]` in the
vertex shader. A value of `1f` gives full-scale displacement on both renderers.

```java
AmplitudeFunction flat    = AmplitudeFunction.constant(1f);
AmplitudeFunction tapered = AmplitudeFunction.ellipse(1f);

// Custom:
AmplitudeFunction custom = (index, x) -> 0.8f * (float) Math.abs(Math.cos(Math.PI * x));

audioWave.setAmplitudeFunction(custom);
radialWave.setAmplitudeFunction(custom);  // same API, same scale
```

---

## SpectrumAnalyser — FFT spectrum renderer

`SpectrumAnalyser` is a self-contained `RenderedItem` that manages its own `AudioReader` and
CPU ring buffer internally. It draws a log-frequency bar chart with peak-hold ticks.

```java
// Fields:
private final SpectrumAnalyser analyser = new SpectrumAnalyser();
private final LineAcquirer lineAcquirer = new LineAcquirer();

// init():
analyser.init(ctx);
lineAcquirer.init(ctx, LineAcquirer.IDEAL);
analyser.setLine(lineAcquirer.getSelectedSource());

// doRender():
analyser.doRender(ctx);

// dispose():
analyser.dispose();
```

### Audio source switching (safe from any thread)

```java
lineAcquirer.next();
analyser.setLine(lineAcquirer.getSelectedSource());
```

### Construction parameters

```java
new SpectrumAnalyser(
    numBins,     // number of visual bars — default 128
    fftSize,     // FFT window size (power of two) — default 2048
    sampleRate,  // capture rate in Hz — default 48 000
    fMin,        // lowest displayed frequency Hz — default 20
    fMax,        // highest displayed frequency Hz — default 20 000
    dBFloor,     // dB level → bar height 0 — default -80
    dBCeiling,   // dB level → bar height 1 — default 0
    gapFraction  // fraction of each bar slot used as a gap — default 0.15
);
```

### Visual behaviour

- **Bar gradient** — green at the bottom → yellow → red at the top, driven by screen-space Y.
- **Peak hold** — each white tick rises instantly and decays at ~1 full-scale unit per second.
- **Texture format** — `GL_RG32F` 1-D texture of width `numBins` uploaded each frame:
  R = current magnitude [0, 1], G = peak-hold level [0, 1].

### Signal path

```
AudioReader thread
  └─ RollingAudioBuffer (CPU ring buffer, 4 × fftSize stereo frames)
       └─ readSamples(float[], fftSize, ChannelMode.MONO_BLEND)  [render thread, each frame]
            └─ FFTProcessor.process()
                 ├─ Hann window
                 ├─ FloatFFT_1D.realForward()  (JTransforms)
                 └─ log-frequency bin mapping → dB normalisation → float[numBins]
                      └─ glTexSubImage1D → GL_RG32F 1-D texture
                           ├─ bar shader  (GL_TRIANGLES, numBins × 6 vertices)
                           └─ peak shader (GL_LINES, numBins × 2 vertices)
```

---

---

## BeatDetector — per-band onset detection

`BeatDetector` analyses the `float[] magnitudes` array produced by `FFTProcessor` each frame.
It splits the spectrum into named frequency bands, tracks a rolling energy average per band, and
publishes a `[0, 1]` beat strength that rises instantly when energy spikes above the rolling
average and then decays at a configurable rate. No extra FFT is run — it reuses work already done.

### Frequency bands

`FrequencyBand` is an immutable record `(String name, float fMin, float fMax)`.
Three presets cover the standard drum-machine layout:

| Preset constant | Name string | Hz range | Typical source |
|---|---|---|---|
| `FrequencyBand.BASS` | `"bass"` | 20–250 Hz | Kick drum, sub-bass |
| `FrequencyBand.SNARE` | `"snare"` | 250–2 000 Hz | Snare, clap, attack transients |
| `FrequencyBand.HI_HAT` | `"hihat"` | 2 000–20 000 Hz | Hi-hat, cymbals, shimmer |

`FrequencyBand.defaults()` returns `List.of(BASS, SNARE, HI_HAT)`.

Custom bands are just records — any name, any Hz range:

```java
new FrequencyBand("sub",   20f,   80f)
new FrequencyBand("kick",  80f,  200f)
new FrequencyBand("vocal", 300f, 3_000f)
```

### Signal chain (standalone)

`BeatDetector` does not own an audio pipeline. You must provide a `RollingAudioBuffer` and
`FFTProcessor`, and feed their shared `magnitudes[]` to the detector each frame:

```
AudioReader thread
  └─ RollingAudioBuffer
       └─ readSamples(float[], fftSize, ChannelMode.MONO_BLEND)   [render thread]
            └─ FFTProcessor.process(samples, magnitudes)
                 └─ BeatDetector.update(magnitudes)
                      ├─ getBeatStrength("bass")   → float [0, 1]
                      ├─ getBeatStrength("snare")  → float [0, 1]
                      └─ getBeatStrength("hihat")  → float [0, 1]
```

**Critical constraint:** `BeatDetector` must be constructed with the same `numBins`, `fMin`, and
`fMax` as the `FFTProcessor` it will receive magnitudes from. The detector pre-computes its
band-to-bin mapping from these values at construction time; a mismatch silently produces wrong
band boundaries.

### Construction

```java
// Default bands (bass / snare / hi-hat), sensible defaults for everything else:
BeatDetector beats = new BeatDetector(numBins, fMin, fMax);

// Full constructor — all parameters explicit:
BeatDetector beats = new BeatDetector(
    FrequencyBand.defaults(),   // or any List<FrequencyBand>
    numBins,                    // must match FFTProcessor (e.g. 128)
    fMin,                       // must match FFTProcessor (e.g. 20f)
    fMax,                       // must match FFTProcessor (e.g. 20_000f)
    43,                         // historyLength: frames in rolling average (~0.7 s at 60 fps)
    1.3f,                       // threshold: ratio above average before any trigger (30%)
    2.0f,                       // sensitivity: 50% above average → strength 1.0
    1f / 60f                    // decayPerFrame: full scale decays to zero in one second at 60 fps
);
```

### Wiring into an experiment

```java
// ── Fields ─────────────────────────────────────────────────────────────────────
private static final int   FFT_SIZE    = 1024;
private static final int   NUM_BINS    = 128;
private static final float SAMPLE_RATE = 48_000f;
private static final float F_MIN       = 20f;
private static final float F_MAX       = 20_000f;

private RollingAudioBuffer rollingBuffer;
private FFTProcessor       fftProcessor;
private BeatDetector       beatDetector;
private final float[]      sampleBuffer = new float[FFT_SIZE];
private final float[]      magnitudes   = new float[NUM_BINS];

private AudioReader audioReader;
private Thread      audioReaderThread;
private final LineAcquirer lineAcquirer = new LineAcquirer();

// ── init() ─────────────────────────────────────────────────────────────────────
rollingBuffer = new RollingAudioBuffer(FFT_SIZE * 4);   // 4× FFT size keeps the write head clear
fftProcessor  = new FFTProcessor(FFT_SIZE, NUM_BINS, SAMPLE_RATE, F_MIN, F_MAX, -80f, 0f);
beatDetector  = new BeatDetector(NUM_BINS, F_MIN, F_MAX);   // matches fftProcessor geometry

lineAcquirer.init(ctx, LineAcquirer.IDEAL);
audioReader = new AudioReader(List.of(rollingBuffer));
audioReaderThread = new Thread(audioReader, "beat-audio-reader");
audioReaderThread.setDaemon(true);
audioReaderThread.start();
audioReader.setLine(lineAcquirer.getSelectedSource());

// ── doRender() — called each frame on the GL thread ───────────────────────────
rollingBuffer.readSamples(sampleBuffer, FFT_SIZE, ChannelMode.MONO_BLEND);
fftProcessor.process(sampleBuffer, magnitudes);
beatDetector.update(magnitudes);

float kickStrength  = beatDetector.getBeatStrength("bass");
float snareStrength = beatDetector.getBeatStrength("snare");
float hihatStrength = beatDetector.getBeatStrength("hihat");
// → pass as shader uniforms (see below)

// ── dispose() ─────────────────────────────────────────────────────────────────
audioReader.setRunning(false);
audioReader.setLine(null);
audioReaderThread.join(2000);
```

### Querying beat strengths

```java
// By name (throws IllegalArgumentException if the name was not registered):
float kick  = beats.getBeatStrength("bass");

// By index (declaration order):
float kick  = beats.getBeatStrength(0);
float snare = beats.getBeatStrength(1);
float hihat = beats.getBeatStrength(2);

// All bands as a snapshot array (allocates; use named/indexed form in the render loop):
float[] all = beats.getBeatStrengths();   // length == getBandCount()

// Introspect the registered bands:
List<FrequencyBand> bands = beats.getBands();
int count = beats.getBandCount();
```

### Using beat strengths as shader uniforms

Beat strength values are plain floats in `[0, 1]` — pass them as uniforms after calling
`update()` each frame:

```java
// After beatDetector.update(magnitudes):
myShader.use(ctx);
myShader.uniforms().get("uKick",  Float.class).set(beatDetector.getBeatStrength("bass"));
myShader.uniforms().get("uSnare", Float.class).set(beatDetector.getBeatStrength("snare"));
myShader.uniforms().get("uHiHat", Float.class).set(beatDetector.getBeatStrength("hihat"));
```

In GLSL, use them to pulse colours, scale geometry, or mix effects:

```glsl
uniform float uKick;    // [0, 1] — bass beat strength, decayed
uniform float uSnare;
uniform float uHiHat;

// Pulse brightness on kick:
vec3 color = baseColor * (1.0 + uKick * 2.0);

// Flash white on snare:
color = mix(color, vec3(1.0), uSnare);
```

### Adding beat detection alongside SpectrumAnalyser

`SpectrumAnalyser` owns its own internal `AudioReader` and does not expose its `magnitudes[]`.
To run beat detection in the same experiment, add a second audio pipeline that feeds a
`RollingAudioBuffer` via the same `LineAcquirer`:

```java
// Both pipelines read from the same physical audio device,
// but through independent AudioReader threads:
analyser.setLine(lineAcquirer.getSelectedSource());          // SpectrumAnalyser's thread
audioReader.setLine(lineAcquirer.getSelectedSource());       // BeatDetector's thread
```

Pass `-Dsimulate.audio=true` to replace both with a `SimulatedDataSource` for headless testing.

### Tuning reference

| Parameter | Default | Effect |
|---|---|---|
| `historyLength` | 43 | Longer = slower response to new tempo; shorter = more false positives in sustained passages |
| `threshold` | 1.3 | Higher = fewer triggers; 1.0 triggers on any above-average energy |
| `sensitivity` | 2.0 | Higher = reaches full strength at a lower peak; lower = more dynamic range |
| `decayPerFrame` | 1/60 | `1/60` decays full scale in 1 s at 60 fps; larger values decay faster |

### Custom band example

```java
List<FrequencyBand> percussionBands = List.of(
    new FrequencyBand("sub",    20f,   80f),    // sub-kick thump
    new FrequencyBand("kick",   80f,  200f),    // kick body
    new FrequencyBand("snare",  200f, 600f),    // snare crack
    new FrequencyBand("hihat", 600f, 8_000f),   // closed hi-hat
    new FrequencyBand("air",  8_000f, 20_000f)  // cymbal shimmer / air
);
BeatDetector beats = new BeatDetector(
    percussionBands, NUM_BINS, F_MIN, F_MAX, 43, 1.3f, 2.0f, 1f / 60f
);
```

### Startup behaviour

For approximately the first `historyLength + ceil(1 / decayPerFrame)` frames after construction,
the rolling average is building up from zero — any non-silent audio will produce a brief burst of
high beat strengths on all bands. At the default settings this clears in about 1 second. This is
harmless for continuous visualisers; if you need to suppress it (e.g. for onset logging), ignore
values until `update()` has been called at least `historyLength * 3` times.

---

## Constraints

| Constraint | Detail |
|---|---|
| **GL thread for PboAudioSink** | `PboAudioSink.create()` and `upload()` must be called on the GL thread. `write()` is safe from the audio thread. |
| **upload() once per frame** | Call `audioSink.upload()` exactly once per frame before any renderer that shares that sink. Calling it multiple times per frame wastes a DMA transfer; skipping it leaves the texture stale. |
| **AUDIO_BUFFER_SIZE compatibility** | `AudioWave.AUDIO_BUFFER_SIZE` and `RadialWave.AUDIO_BUFFER_SIZE` are both `2048`. Pass either constant to `PboAudioSink.create()` when sharing between them. |
| **SpectrumAnalyser is self-contained** | Unlike `AudioWave` and `RadialWave`, `SpectrumAnalyser` owns its own `AudioReader` thread. Do not pass a shared `PboAudioSink` to it; use `setLine()` to give it an `AudioDataSource` directly. |
| **AudioReader thread lifecycle** | Start before calling `setLine()`; stop with `setRunning(false)` + `setLine(null)` + `join(2000)` in `dispose()`. |
| **BeatDetector geometry must match FFTProcessor** | Construct `BeatDetector` with the same `numBins`, `fMin`, and `fMax` as the `FFTProcessor` whose magnitudes it will consume. A mismatch silently produces wrong band boundaries. |
| **BeatDetector is GL-thread-free** | `BeatDetector.update()` and all getters are pure CPU arithmetic — safe to call from the render thread but do not call from the audio thread. |
