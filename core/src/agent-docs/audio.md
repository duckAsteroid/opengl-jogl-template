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
radialWave.setRadius(0.5f);               // base circle radius in NDC-y units
radialWave.setAmplitude(0.3f);            // displacement scale in NDC-y units
radialWave.setCenter(new Vector2f(0f, 0f)); // NDC centre (default = screen centre)
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

## AmplitudeFunction — custom amplitude envelopes for AudioWave

`AmplitudeFunction` is a `@FunctionalInterface` mapping `(vertexIndex, normalisedX) → amplitude`.
`normalisedX` is in `[-1, 1]`. The returned value is stored in each vertex's Y component and
multiplied by the normalised audio sample `[-1, 1]` in the vertex shader.

```java
AmplitudeFunction flat    = AmplitudeFunction.constant(10f);
AmplitudeFunction tapered = AmplitudeFunction.ellipse(10f);

// Custom:
AmplitudeFunction custom = (index, x) -> 8f * (float) Math.abs(Math.cos(Math.PI * x));

audioWave.setAmplitudeFunction(custom);
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

## Constraints

| Constraint | Detail |
|---|---|
| **GL thread for PboAudioSink** | `PboAudioSink.create()` and `upload()` must be called on the GL thread. `write()` is safe from the audio thread. |
| **upload() once per frame** | Call `audioSink.upload()` exactly once per frame before any renderer that shares that sink. Calling it multiple times per frame wastes a DMA transfer; skipping it leaves the texture stale. |
| **AUDIO_BUFFER_SIZE compatibility** | `AudioWave.AUDIO_BUFFER_SIZE` and `RadialWave.AUDIO_BUFFER_SIZE` are both `2048`. Pass either constant to `PboAudioSink.create()` when sharing between them. |
| **SpectrumAnalyser is self-contained** | Unlike `AudioWave` and `RadialWave`, `SpectrumAnalyser` owns its own `AudioReader` thread. Do not pass a shared `PboAudioSink` to it; use `setLine()` to give it an `AudioDataSource` directly. |
| **AudioReader thread lifecycle** | Start before calling `setLine()`; stop with `setRunning(false)` + `setLine(null)` + `join(2000)` in `dispose()`. |
