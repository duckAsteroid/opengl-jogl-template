# Audio Visualisation

## AudioWave — pre-built waveform renderer

`AudioWave` is a `RenderedItem` that captures real-time stereo PCM audio and draws it as a
scrolling line strip. Drop it into any experiment.

```java
// Fields:
private final AudioWave audioWave = new AudioWave();
private final LineAcquirer lineAcquirer = new LineAcquirer();

// init():
audioWave.init(ctx);
lineAcquirer.init(ctx, LineAcquirer.IDEAL);   // IDEAL = 48 kHz, 16-bit stereo
audioWave.setLine(lineAcquirer.getSelectedSource());

// doRender():
audioWave.doRender(ctx);

// dispose():
audioWave.dispose();
```

### Switching audio source at runtime (safe from any thread)

```java
lineAcquirer.next();                                    // advance to next device
audioWave.setLine(lineAcquirer.getSelectedSource());
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
audioWave.setLineWidth(4.0f);                          // GL line width in pixels
audioWave.setLineColour(StandardColors.CYAN.color);
audioWave.setAmplitudeFunction(AmplitudeFunction.constant(10f));   // flat envelope
audioWave.setAmplitudeFunction(AmplitudeFunction.ellipse(10f));    // tapers to 0 at edges
```

---

## AmplitudeFunction — custom amplitude envelopes

`AmplitudeFunction` is a `@FunctionalInterface` mapping `(vertexIndex, normalisedX) → amplitude`.
`normalisedX` is in `[-1, 1]`. The returned value is stored in each vertex's Y component and
multiplied by the normalised audio sample `[-1, 1]` in the vertex shader.

```java
// Built-in factories:
AmplitudeFunction flat    = AmplitudeFunction.constant(10f);
AmplitudeFunction tapered = AmplitudeFunction.ellipse(10f);

// Custom:
AmplitudeFunction custom = (index, x) -> 8f * (float) Math.abs(Math.cos(Math.PI * x));

audioWave.setAmplitudeFunction(custom);
```

---

## RadialWave — pre-built radial waveform renderer

`RadialWave` is a `RenderedItem` that captures real-time stereo PCM audio and draws it as a
closed circle deformed by the waveform. Each of the 1 024 vertices sits on a base circle at
radius `uRadius`; positive audio samples push the vertex outward and negative samples pull it
inward. The circle is drawn as a `GL_LINE_LOOP` and automatically corrects for non-square
viewports via an internal aspect-ratio uniform.

```java
// Fields:
private final RadialWave radialWave = new RadialWave();
private final LineAcquirer lineAcquirer = new LineAcquirer();

// init():
radialWave.init(ctx);
lineAcquirer.init(ctx, LineAcquirer.IDEAL);
radialWave.setLine(lineAcquirer.getSelectedSource());

// doRender():
radialWave.doRender(ctx);

// dispose():
radialWave.dispose();
```

### Channel modes

```java
radialWave.setChannelMode(RadialWave.CHANNEL_BLEND);  // L+R average (default)
radialWave.setChannelMode(RadialWave.CHANNEL_LEFT);
radialWave.setChannelMode(RadialWave.CHANNEL_RIGHT);
```

### Visual properties (applied on next frame, safe from any thread)

```java
radialWave.setLineWidth(3.0f);                         // GL line width in pixels
radialWave.setLineColour(StandardColors.CYAN.color);

// Base circle radius in NDC-y units (0 = centre, 1 = top/bottom screen edge)
radialWave.setRadius(0.5f);    // default

// How far a full-scale audio sample displaces the circle edge, in NDC-y units
radialWave.setAmplitude(0.3f); // default

// Move the circle centre (NDC coordinates)
radialWave.setCenter(new Vector2f(0.0f, 0.0f)); // default = screen centre
```

### Aspect-ratio correction

`RadialWave` installs a `ResizeListener` in `init()` that keeps a volatile `currentAspect`
field (width / height) in sync with the window. The value is pushed to the `uAspect` uniform
each frame in `doRender()`, so the circle stays visually round as the window is resized without
any additional work from the caller.

---

## SpectrumAnalyser — pre-built FFT spectrum renderer

`SpectrumAnalyser` is a `RenderedItem` that captures real-time PCM audio, runs a windowed FFT
each frame, and draws a classic bar-chart spectrum display. Bars use a log frequency scale
(20 Hz–20 kHz by default) and log amplitude (dB), both configurable. White horizontal
peak-hold ticks decay slowly above each bar.

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

### Construction parameters and defaults

The no-arg constructor uses sensible defaults. Pass all parameters to the full constructor:

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

`numBins` and `fftSize` are independent: more bins gives finer visual resolution (up to the
limit imposed by `fftSize/2` linear FFT bins); a larger `fftSize` gives better low-frequency
resolution at the cost of latency.

### Visual behaviour

- **Bar gradient** — each bar is shaded green at the bottom → yellow → red at the top,
  driven by the screen-space Y position of the fragment.
- **Peak hold** — the white tick for bar `i` rises instantly to the current magnitude and
  then falls at `1/60` per frame (approximately 1 second for a full-scale peak to decay to
  silence at 60 fps).
- **Texture data format** — a 1-D `GL_RG32F` texture of width `numBins` is uploaded each
  frame: R channel = current magnitude [0, 1], G channel = peak-hold level [0, 1].  Both
  bar and peak shaders sample this texture by `gl_VertexID` rather than by vertex position,
  so the geometry VBOs contain only static X coordinates and never need updating at runtime.

### Signal path (for debugging)

```
AudioReader thread
  └─ RollingFloatBuffer (CPU ring buffer, 4 × fftSize samples)
       └─ readSamples() → float[fftSize] (render thread, each frame)
            └─ FFTProcessor.process()
                 ├─ Hann window
                 ├─ FloatFFT_1D.realForward() (JTransforms)
                 └─ log-frequency bin mapping → dB normalisation → float[numBins]
                      └─ glTexSubImage1D → GL_RG32F 1-D texture
                           ├─ bar shader (GL_TRIANGLES, numBins × 6 vertices)
                           └─ peak shader (GL_LINES, numBins × 2 vertices)
```

---

## LineAcquirer — audio source discovery

```java
LineAcquirer la = new LineAcquirer();
la.init(ctx, LineAcquirer.IDEAL);

AudioDataSource current  = la.getSelectedSource();
AudioDataSource next     = la.next();      // returns old source, advances index
AudioDataSource previous = la.previous();  // wraps around

// Check simulate.audio system property:
// -Dsimulate.audio=true  → a SimulatedDataSource is prepended to the source list
// -Dsimulate.audio=false → only real hardware lines (default)
```

---

## Constraints

| Constraint | Detail |
|---|---|
| **AudioWave thread** | `AudioWave` starts a background `"audio-reader"` daemon thread in `init()`. `dispose()` stops it and joins with a 2 s timeout before releasing GPU resources. |
| **RadialWave thread** | `RadialWave` follows the same pattern as `AudioWave`: a `"radial-audio-reader"` daemon thread is started in `init()` and stopped with a 2 s join in `dispose()`. |
| **SpectrumAnalyser thread** | `SpectrumAnalyser` starts a `"spectrum-audio-reader"` daemon thread in `init()` (CPU-only `AudioReader`, no PBO). `dispose()` stops it and joins with a 2 s timeout before releasing GL resources. |
