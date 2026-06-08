# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

A Java/LWJGL 3 OpenGL framework for GPU shader experiments and real-time visualisations. Three Gradle sub-projects: `core` (library), `experiments` (runnable demos), `application` (placeholder).

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

# Run only core tests (most tests live here; GL tests require a live context)
./gradlew :core:test

# Run a single test class
./gradlew :core:test --tests "com.asteroid.duck.opengl.util.keys.KeysTest"

# Run with LWJGL debug agent (logs all GL calls to debugRun.gl.log)
./gradlew :experiments:debugRun

# Check GPU capabilities
./gradlew :experiments:graphicsCardLogger
```

The `run` and `debugRun` tasks automatically inject `__NV_PRIME_RENDER_OFFLOAD=1` / `__GLX_VENDOR_LIBRARY_NAME=nvidia` for NVIDIA Optimus laptops. Java 25 with `--enable-preview` is used. Tests require `-Xshare:off`.

## Architecture

### Core rendering lifecycle

Every renderable object implements `RenderedItem` (`core/src/main/java/…/util/RenderedItem.java`):

```
init(RenderContext)   → called once before the loop; allocate GL resources here
doRender(RenderContext) → called every frame
dispose()             → free all GL and native resources
```

`RenderContext` (`util/RenderContext.java`) is the interface passed everywhere — it gives access to `ResourceManager`, `Timer`, `KeyRegistry`, window geometry, and orthographic projection. **Never hold a direct reference to the window; always use `RenderContext`.**

`GLWindow` (`util/GLWindow.java`) is the abstract GLFW window that owns the render loop and implements `RenderContext`. Concrete windows extend it and implement `init()`, `render()`, and `registerKeys()`.

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

`AudioReader` runs a producer thread that writes into a GPU-mapped `ByteBuffer` via a circular buffer (`RollingFloatBuffer`). The render thread reads only the write-head position. This is the only intentionally concurrent code; everything else is single-threaded on the GL thread.

### GLSL shaders

Shaders live in `experiments/src/main/resources/glsl/<name>/vertex.glsl` and `frag.glsl`. They are loaded at runtime from the classpath via `ResourceManager`.

## Key conventions

- All GL calls must happen on the GL/render thread.
- GL handles are `null` until `init()` is called; using them before or after is undefined.
- SLF4J for logging; set `-Dsystem.slf4j.internal.verbosity=WARN` for clean output.
- `core` uses Google AutoService (`@AutoService`) for `Binder` implementations; run `./gradlew :core:compileJava` to regenerate the service file after adding a new `Binder`.