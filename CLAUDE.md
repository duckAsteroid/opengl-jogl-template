# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

A Java/LWJGL 3 OpenGL framework for GPU shader experiments and real-time visualisations. Three Gradle sub-projects: `render-core` (library), `experiments` (runnable demos), `application` (placeholder).

## Build & run commands

```bash
# Build everything
./gradlew build

# Run the experiment chooser (Swing dialog or CLI prompt)
./gradlew :experiments:run

# Run a specific experiment by title
./gradlew :experiments:run --args="SoundWave"

# Run all tests
./gradlew test

# Run only render-core tests (most tests live here; GL tests require a live context)
./gradlew :render-core:test

# Run a single test class
./gradlew :render-core:test --tests "com.asteroid.duck.opengl.util.keys.KeysTest"

# Run with LWJGL debug agent (logs all GL calls to debugRun.gl.log)
./gradlew :experiments:debugRun

# Check GPU capabilities
./gradlew :experiments:graphicsCardLogger
```

The `run` and `debugRun` tasks automatically inject `__NV_PRIME_RENDER_OFFLOAD=1` / `__GLX_VENDOR_LIBRARY_NAME=nvidia` for NVIDIA Optimus laptops. Java 25 with `--enable-preview` is used. Tests require `-Xshare:off`.

### `render-core`'s Gradle conventions

`render-core/build.gradle` applies duckAsteroid's shared build conventions (https://github.com/duckAsteroid/gradle-convention-plugin) instead of hand-rolling toolchain/publishing config:

- `duckasteroid-java` — Java toolchain, source/Javadoc jars, and the `mavenJava` publication (whose `artifactId` is simply the project name, `render-core`). Also wires up git-tag/conventional-commit versioning: `render-core`'s version comes from git tags matching `render-core/v<version>` (falls back to `v<version>` at the repo root, then `0.0.0+notag` if untagged); ordinary builds compute an `X.Y.Z-SNAPSHOT` from Conventional Commits since the last final tag. See [VERSIONING.md](https://github.com/duckAsteroid/gradle-convention-plugin/blob/main/VERSIONING.md) in the conventions repo for the full explanation.
- `duckasteroid-github-packages-publish` — opt-in; adds a publishing repository that publishes `mavenJava` to *this* repo's own GitHub Packages Maven feed (owner/repo auto-derived from the `origin` remote). No GPG signing is required for this path.
- `duckasteroid-release-flow` — opt-in; adds the `tagReleaseCandidate`/`promoteReleaseCandidate`/`changelogForReleaseCandidate`/`changelogForRelease` tasks that drive the `develop` → `release` → `main` release flow below.

`experiments` and `application` don't use these conventions. Publishing to Maven Central/OSSRH is a separate opt-in (`duckasteroid-maven-central`), not applied here.

These `duckasteroid-*` plugins are published only to GitHub Packages, never the Gradle Plugin Portal, so `settings.gradle` bootstraps plugin resolution first via the `io.github.duckasteroid.github-packages-settings` plugin pointed at the `duckAsteroid/gradle-convention-plugin` repo. Reading from GitHub Packages requires credentials even for public repos: `gpr.user`/`gpr.key` in `~/.gradle/gradle.properties`, or `GH_PACKAGES_READ_USER`/`GH_PACKAGES_READ_TOKEN`, or (in CI) `GITHUB_ACTOR`/`GITHUB_TOKEN`. See the `adopt-duckasteroid-gradle-conventions` skill if extending these conventions to other modules.

**Releasing `render-core`:** this repo follows the conventions repo's `develop` → `release` → `main` flow:
- Merging accepted work from `develop` into `release` triggers `.github/workflows/release-candidate.yml`: it mints the next `render-core/vX.Y.Z-RCn` tag (auto-incrementing `n` for the same candidate version), builds, publishes to GitHub Packages, and cuts a GitHub pre-release.
- Merging `release` into `main` triggers `.github/workflows/promote-release.yml`: it strips the `-RCn` suffix off the nearest RC tag, tags/publishes the final `render-core/vX.Y.Z`, and cuts the GitHub Release.
- Both tasks respect `-Prelease.forceVersion=X.Y.Z` as a backstop and are plain Gradle tasks runnable locally (`./gradlew :render-core:tagReleaseCandidate`, etc.), not just from CI.
- Since `render-core` is the only module using this convention today, its tag prefix (`render-core/v`) resolves unambiguously — if a second module adopts this versioning later, revisit whether the plain `v*` fallback still makes sense.

## Architecture

### Core rendering lifecycle

Every renderable object implements `RenderedItem` (`render-core/src/main/java/…/util/RenderedItem.java`):

```
init(RenderContext)   → called once before the loop; allocate GL resources here
doRender(RenderContext) → called every frame
dispose()             → free all GL and native resources
```

`RenderContext` (`util/RenderContext.java`) is the interface passed everywhere — it gives access to `ResourceManager`, `Clock`, `KeyRegistry`, window geometry, and orthographic projection. **Never hold a direct reference to the window; always use `RenderContext`.**

> **High-DPI note:** `getWindow()` returns the GLFW window size in *screen coordinates*. On high-DPI displays the GL framebuffer (viewport) is larger. `ortho()` always uses the framebuffer dimensions, so projection coordinates are always in real pixels — use `ortho()` for any pixel-space projection and avoid deriving projection from `getWindow()` dimensions.

`GLWindow` (`util/GLWindow.java`) is the abstract GLFW window that owns the render loop and implements `RenderContext`. Concrete windows extend it and implement `init()`, `render()`, and `registerKeys()`. To change GLFW init or window hints (e.g. `GLFW_PLATFORM`, which defaults to X11 on Linux), pass a `GLFWConfigurer` to the constructor — its `beforeInit()` / `beforeCreateWindow()` hooks run after GLWindow's defaults, so they override them.

### Experiment plugin system

`Experiment` extends `RenderedItem` and adds `getTitle()`, `getDescription()`, and `getPriority()`. Experiments are discovered via `ServiceLoader` — they must be listed in:

```
experiments/src/main/resources/META-INF/services/com.asteroid.duck.opengl.experiments.Experiment
```

To add a new experiment: implement `Experiment`, add the fully-qualified class name to that services file. The `ExperimentChooser` selects which experiment to run (via CLI args, system property `experiment`, Swing dialog, or last-run file).

### Resource management

`ResourceManager` (owned by `GLWindow`) is the single owner of all GL handles. Load textures, shaders, and buffers through it — it calls `dispose()` on all resources at shutdown.

**Binding exclusivity:** VAOs, EBOs, shader programs, and textures use `ExclusivityGroup<T>` wrappers (`util/resources/bound/`). Always bind through the wrapper, never raw GL calls, to prevent accidental double-binding.

**Native memory:** `VertexBufferObject` allocates off-heap via `MemoryUtil.memAlloc`. Always call `dispose()` — leaks are not GC-collected.

**`UpdateHint`:** Use `STATIC` for geometry that never changes, `DYNAMIC` for buffers written every frame, `STREAM` for once-written/few-use buffers.

### Shader ↔ vertex layout contract

`VertexElement.name()` is used verbatim as the GLSL `in` variable name when calling `VertexBufferObject.setup(ShaderProgram)`. Attribute names in Java and GLSL must match exactly. `VertexDataStructure` is immutable — offsets are pre-computed at construction.

### Audio thread safety

`AudioReader` runs a producer thread (use a **platform** thread; it parks 0.5 ms when the line is empty) that fans PCM bytes out to `AudioSink`s. This is the only intentionally concurrent code; everything else is single-threaded on the GL thread.

- `RollingAudioBuffer` (CPU ring buffer) uses a seqlock-style `claimed`/`written` counter pair so a reader never sees a torn or lapped sample; tainted samples are re-read, then held.
- `PboAudioSink` writes into a persistently-mapped PBO. `upload()` snapshots the head before issuing the (asynchronous) GPU copy and `getHead()` returns that snapshot; `AudioWave`/`RadialWave` draw the newest window *ending* at it, with `AUDIO_BUFFER_SIZE` = 4× the window as headroom for writes landing while the copy is pending. Don't draw from the oldest end of the ring — that's where the audio thread is writing.

### Transformable renderers

`Transformable` (`util/Transformable.java`) is a mix-in interface for renderers that accept a per-frame `Matrix4f` applied to all output vertex positions before rasterisation. The transform is in NDC space (origin = screen centre); the matrix is copied internally so callers may reuse it freely.

Renderers that implement `Transformable`:

| Class | Notes |
|---|---|
| `AudioWave` | Waveform strip — rotate, scale, shear |
| `RadialWave` | Radial waveform circle |
| `FrequencyRenderer` (and subclasses) | Covers `SpectrumAnalyser` and `RadialSpectrumAnalyser` |

All implementations are thread-safe for `setTransform` — the new matrix is applied on the GL thread at the start of the next frame.

```java
// Slow rotation with beat-driven scale pulse:
float angle = 0f;
// each frame:
angle += 0.005f;
float pulse = 1.0f + 0.15f * beats.getBeatStrength(0);
renderer.setTransform(new Matrix4f().rotateZ(angle).scale(pulse));

// Reset to identity:
renderer.setTransform(new Matrix4f());
```

All three hand the matrix to the GL thread through a `PendingValue` (`util/renderaction/`): a preallocated, lock-guarded, last-write-wins slot. `setTransform` (and `AudioWave`/`RadialWave`'s `setLineColour`) is a no-op when the value is unchanged and never allocates, so calling it every frame is fine. `setAmplitudeFunction` likewise skips the VBO rebuild for an equal function — `AmplitudeFunction.constant`/`ellipse` return value-equal records. `FrequencyRenderer` subclasses read the transform via `currentTransform()`. The remaining wave setters (line width, channel mode, radius, centre) are volatile fields (or, for the centre, another `PendingValue`) compared against the GL thread's last-applied value, so they are equally cheap; neither wave class uses `RenderActionQueue` any more.

### Screenshot and video capture

`GLWindow` registers **no** key bindings. All bindings — including screenshot and recording — are wired in `Main.registerKeys()`:

| Key | Action |
|---|---|
| `Print Screen` | `captureNextFrame()` — timestamped PNG |
| `Shift + Print Screen` | `startRecording(Duration.ofSeconds(5))` — 5-second MP4 |

`GLWindow` exposes `protected` action methods that subclasses bind to keys in `registerKeys()`:

| Method | What it does |
|---|---|
| `toggleClock()` | Pause/unpause the render clock |
| `stepClock(double seconds)` | Scrub the clock by ±N seconds (for use while paused) |
| `exit()` | Request a clean shutdown |

This separation keeps all key bindings in one place (`Main.registerKeys()`) while GLWindow owns the action logic.

**Screenshot flow:** `captureNextFrame(Path)` stores the path in `volatile pendingCapture`. After `render()` on the GL thread, `readFramebuffer()` runs `glReadPixels` → row-flip → `BufferedImage`, then a virtual thread writes the PNG via `ImageIO`.

**Video recording flow:** `startRecording(Path, Duration)` (capped at 60 s) stores a `RecordingRequest` in `volatile pendingRecording`. On the next frame the GL thread calls `beginRecordingSession()`, which opens a JCodec `AWTSequenceEncoder` (H.264/MP4, 30 fps) and spawns an encode virtual thread. Each subsequent frame calls `readFramebuffer()` and offers the image to a bounded `ArrayBlockingQueue<BufferedImage>` (capacity 30). The encode thread drains the queue and feeds frames to JCodec. When `Timer.elapsed()` passes the requested duration, the GL thread signals `stopping = true`; the encode thread drains remaining frames and calls `encoder.finish()` to write the MP4 trailer. If the window closes mid-recording, `dispose()` signals the session and joins the encode thread (up to 30 s).

**API on `RenderContext`:**
```java
ctx.startRecording(Duration.ofSeconds(30));               // timestamped file
ctx.startRecording(Path.of("out.mp4"), Duration.ofSeconds(30));
ctx.stopRecording();  // stop before duration expires
```

Timing uses `RenderContext.getClock()` (not wall-clock time) so paused or slowed renders are handled correctly.

**JCodec dependency** (`org.jcodec:jcodec:0.2.5` + `jcodec-javase:0.2.5`) is in `render-core/build.gradle`. No native binaries required.

### GLSL shaders

Shaders live in `experiments/src/main/resources/glsl/<name>/vertex.glsl` and `frag.glsl`. They are loaded at runtime from the classpath via `ResourceManager`.

## Key conventions

- All GL calls must happen on the GL/render thread.
- GL handles are `null` until `init()` is called; using them before or after is undefined.
- SLF4J for logging; set `-Dsystem.slf4j.internal.verbosity=WARN` for clean output.
- `render-core` uses Google AutoService (`@AutoService`) for `Binder` implementations; run `./gradlew :render-core:compileJava` to regenerate the service file after adding a new `Binder`.