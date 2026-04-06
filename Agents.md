# Agents.md – opengl-jogl-template

A Java/LWJGL OpenGL framework for building and experimenting with GPU shaders and real-time
visualisations. Loosely inspired by
[tsoding/opengl-template](https://github.com/tsoding/opengl-template) but written entirely in
Java using [LWJGL 3](https://www.lwjgl.org/).

---

## Repository layout

```
opengl-jogl-template/
├── core/          # Reusable OpenGL framework library
├── experiments/   # Runnable demos / sandbox application
├── application/   # Placeholder for a future standalone application
├── docs/          # Developer notes
├── renderdoc/     # RenderDoc capture files for offline debugging
└── font-images/   # Source font bitmap assets
```

Three Gradle sub-projects are declared in `settings.gradle`:
`core`, `experiments`, and `application`.

---

## Build system

| Tool | Version / detail |
|------|-----------------|
| Gradle | Wrapper checked in (`gradlew`) |
| Java toolchain | JDK 17 |
| LWJGL | 3.3.6 (OpenGL, GLFW; linux natives by default) |
| JOML | 1.10.8 – vector/matrix maths |
| SLF4J + Logback | Logging |
| JUnit 5 + Mockito | Test infrastructure (all sub-projects) |

Build the whole project:
```bash
./gradlew build
```

Run the experiments application (default `Main` entry-point):
```bash
./gradlew :experiments:run
```

Useful experiment-specific run targets defined in `experiments/build.gradle`:

| Task | Entry-point |
|------|-------------|
| `runTextureRenderer` | `TextureRenderer` |
| `runTextureRenderer2` | `TextureRenderer2` |
| `debugRun` | `Main` with LWJGL debug agent attached |
| `graphicsCardLogger` | `GraphicsCardLogger` (reports GL capabilities) |

> **Linux / NVIDIA note** – the `run` and `debugRun` tasks inject
> `__NV_PRIME_RENDER_OFFLOAD=1` / `__GLX_VENDOR_LIBRARY_NAME=nvidia` so the discrete GPU is
> used automatically on Optimus laptops.

---

## Sub-projects

### `core` – OpenGL framework library

**Role:** A `java-library` that all other sub-projects depend on.
Contains every reusable abstraction for working with OpenGL via LWJGL.

#### Key packages

| Package | Contents |
|---------|----------|
| `util` | Top-level framework types: `GLWindow`, `RenderContext`, `RenderedItem`, `RenderedItem` composition helpers |
| `util.resources.buffer` | VAO / VBO / EBO abstractions (`VertexArrayObject`, `VertexBufferObject`, `ElementBufferObject`) |
| `util.resources.buffer.vbo` | Vertex layout model: `VertexDataStructure`, `VertexElement`, `VertexElementType` |
| `util.resources.shader` | Shader loading, linking, and uniform management (`ShaderProgram`, `Uniform`, `ShaderLoader`) |
| `util.resources.texture` | Texture creation and binding (`Texture`, `TextureFactory`, `TextureOptions`) |
| `util.resources.texture.io` | Image loading from classpath or path |
| `util.resources.framebuffer` | Off-screen `FrameBuffer` |
| `util.resources.font` | Bitmap font rendering (`FontTexture`, `FontTextureFactory`) |
| `util.resources.bound` | Generic bind/unbind exclusivity system (`Binder`, `ExclusivityGroup`) |
| `util.resources.manager` | `ResourceManager` – owns and disposes all GL resources for a window |
| `util.resources.io` | Resource loaders (`ClasspathLoader`, `PathBasedLoader`) |
| `util.audio` | Audio capture (`AudioDataSource`, `LineAcquirer`, `TargetLineSource`, `RollingFloatBuffer`) |
| `util.audio.simulated` | Synthesised audio sources for headless/test use (`Waveform`, `SimulatedDataSource`, `OscillatingStereoPositioner`) |
| `util.wave` | Real-time audio visualisation (`AudioWave`, `AudioReader`) |
| `util.geom` | Basic 2-D geometry helpers (`Rectangle`, `Triangles`, `Vertice`, etc.) |
| `util.palette` | Colour palette rendering (`ColorPalette`, `PaletteRenderer`) |
| `util.text` | On-screen text rendering utilities |
| `util.blur` | Blur post-processing pass |
| `util.color` | Colour conversion helpers |
| `util.keys` | GLFW keyboard input abstraction (`KeyRegistry`) |
| `util.timer` | Frame timing (`Timer`, `TimerImpl`, `TimeSource`) |
| `util.stats` | Lightweight statistics accumulator (`Stats`, `StatsFactory`) |
| `util.toggle` / `util.renderaction` | On/off toggles and deferred render actions |

#### Central abstractions

* **`RenderedItem`** – the lifecycle contract every renderable object implements:
  `init(RenderContext)` once before the loop, `doRender(RenderContext)` every frame,
  `dispose()` on shutdown.
* **`RenderContext`** – passed to every `RenderedItem`; provides access to `ResourceManager`,
  `Timer`, `KeyRegistry`, and window geometry.
* **`GLWindow`** – abstract GLFW window base class that owns the render loop and implements
  `RenderContext`. Concrete windows extend this.

#### Testing

Unit tests live in `core/src/test/java`. Run with:
```bash
./gradlew :core:test
```

Tests use JUnit 5 and Mockito. Because most GL calls require a live context, pure-Java logic
(serialization, structure validation, audio simulation) is the primary test target.

---

### `experiments` – Runnable demos

**Role:** An `application` sub-project that depends on `core` and provides a collection of
self-contained OpenGL experiments. Each experiment implements the `Experiment` interface (which
extends `RenderedItem`) and is selected at runtime via `ExperimentChooser`.

#### Entry-points

| Class | Purpose |
|-------|---------|
| `Main` | Primary entry-point; opens a `GLWindow` and delegates to an `Experiment` selected by `ExperimentChooser` |
| `TextureRenderer` | Standalone texture rendering demo |
| `TextureRenderer2` | Alternative texture renderer variant |

#### Experiments (`experiments/` package)

| Class | Description |
|-------|-------------|
| `SimpleTriangle` / `Triangle` | Hello-world coloured triangle |
| `SimpleTexture` | Render a texture to a quad using VBO only |
| `SimpleTextureWithEBO` | Same with an indexed EBO draw call |
| `TessellatedTexture` | Tessellated texture rendering |
| `PassthruTexture` | Full-screen passthrough shader |
| `SoundWave` | Real-time audio waveform visualiser (uses `AudioWave`) |
| `BlurPictureExample` | Gaussian blur post-process pass |
| `PalettePicture` | Palette-mapped colour rendering |
| `Cthugha` | Cthugha-style audio reactive visualisation |
| `TranslateExample` | Texture translation / scroll demo |
| `StringExperiment` / `TextExperiment` | On-screen text rendering |
| `ExperimentChooser` | Interactive keyboard-driven experiment switcher |
| `Utils` | Shared helper code for experiments |
| `Test` | Scratch / ad-hoc test harness |

#### Running a specific experiment

Pass its class simple-name as a command-line argument, e.g.:
```bash
./gradlew :experiments:run --args="SoundWave"
```
Or use one of the named Gradle tasks (`runTextureRenderer`, `debugRun`, etc.).

---

### `application` – Standalone application (placeholder)

**Role:** An empty sub-project reserved for a future polished application built on top of `core`.
The `build.gradle` is currently empty; no source files exist yet.

---

## Coding conventions

* **Java 17** with `--enable-preview` enabled for the `experiments` run tasks.
* **OpenGL thread rule** – all GL calls must be made from the render/OpenGL thread. No class in
  this codebase is thread-safe unless explicitly documented otherwise. The exception is the
  `AudioReader` producer thread, which writes to a GPU-mapped `ByteBuffer` via a carefully
  designed circular buffer; the render thread only reads the write-head position.
* **Resource lifecycle** – every `RenderedItem` / `Resource` follows
  `init → use → dispose`. GL handles are `null` until `init` is called; using them before or
  after this window is undefined behaviour.
* **Binding exclusivity** – `ExclusivityGroup<T>` is used for VAOs, EBOs, shader programs, and
  textures to prevent accidental double-binding. Always bind through the wrapper, not via raw GL
  calls.
* **`VertexDataStructure` is immutable** – offsets and sizes are pre-computed at construction
  time; do not attempt to modify a structure after creation.
* **Shader attribute names must match element names** – `VertexElement.name()` is used verbatim
  as the GLSL `in` variable name when calling `VertexBufferObject.setup(ShaderProgram)`.
* **`UpdateHint`** – set `DYNAMIC` for buffers written every frame, `STATIC` for geometry that
  never changes, and `STREAM` for buffers written once and used only a few times.
* **Logging** – SLF4J throughout; `logback-classic` on the runtime classpath. Set
  `slf4j.internal.verbosity=WARN` for clean run output.
* **Native memory** – `VertexBufferObject` allocates off-heap memory via `MemoryUtil.memAlloc`;
  `dispose()` must always be called to avoid native memory leaks.

---

## Current work-in-progress (as of 2026-02)

* Writing audio data directly into a `GL_RG16_SNORM` 1-D texture for real-time waveform
  visualisation (`AudioWave` / `AudioReader`).
* On-screen debug text overlay rendered on top of arbitrary scene content.
* Investigating affine-transform (matrix) based text placement to avoid per-frame vertex
  recalculation.

See `what-next.md` for the full running log.

