# Blur-pass image drift

## Symptom

Over time, the whole rendered image slowly drifts — visibly up and to the right —
regardless of which tab generator (translation-table warp) is active, including
`NoDistortion` (see below), which applies zero pixel displacement. The drift is gentle,
only noticeable after tens of seconds to a few minutes, but it is a real positional walk
of on-screen content, not a rendering glitch or palette effect.

## Why `NoDistortion` doesn't fix it

`NoDistortion` (`app/src/main/java/io/github/duckasteroid/cthugha/tab/generators/NoDistortion.java`)
was added to rule out the tab-generator warp as the cause: it maps every destination
pixel straight to itself (`map_x = dstX`, `map_y = dstY`), so `TranslateTextureRenderer`
becomes a pixel-perfect passthrough copy of `displayTex` into `renderTex`. The drift is
still present with this generator selected, which proves the translate step is *not*
the source — the bug is elsewhere in the per-frame feedback loop described in
[`docs/RENDERING_PIPELINE.md`](docs/RENDERING_PIPELINE.md).

## Root cause: linear-sampling Gaussian blur in a feedback loop

The remaining candidate — and the one that fits both the direction and the gradual,
accumulating nature of the drift — is the blur pass:

```
xBlur   renderTex ──► flameTex   (horizontal Gaussian + multiplier=1.0)
yBlur   flameTex ──► displayTex  (vertical Gaussian  + multiplier=fade)
```

`BlurTextureRenderer` and `BlurKernel` (render-core, `com.asteroid.duck.opengl.util.blur`)
implement the ["Efficient Gaussian Blur with Linear Sampling"](https://www.rastergrid.com/blog/2010/09/efficient-gaussian-blur-with-linear-sampling/)
technique: instead of one texture fetch per Gaussian tap, adjacent tap pairs are
collapsed into a single fetch at a computed sub-texel offset, relying on the GPU's
bilinear filter to blend the two texels in hardware. The offset math
(`BlurKernel.getDiscreteSampleKernel()`) is symmetric — the shader always samples
`texCoords + delta` and `texCoords - delta` with the same weight — so on paper there is
no directional bias.

In practice, GPU texture units quantize the bilinear blend factor to a fixed-point
fraction (commonly 8 bits). That quantization does not round symmetrically around the
sample's true position, so it introduces a small, *consistent* bias toward one
neighbouring texel. On a single frame this is invisible. But `displayTex` — the blur
output — is also the translate step's read source *next* frame
(`displayTex → renderTex → flameTex → displayTex → …`), so the same tiny per-frame bias
is reapplied every frame and accumulates into a visible walk. A consistent bias in both
the X-axis pass and the Y-axis pass reads as diagonal motion; up-and-right is consistent
with OpenGL's texture-V convention (V increases upward).

## Why this isn't a tab-generator or app-level bug

- No pan/offset/drift logic exists anywhere in this repo's rendering code
  (`JCthugha`, `CthughaWindow`) — confirmed by inspection.
- The effect persists independent of which `TabGenerator` is selected, including the
  zero-displacement `NoDistortion` generator.
- The blur implementation lives entirely in the `render-core` library dependency
  (`com.asteroid.duck.opengl:render-core:0.0.1`), not in this repo.

## Fix implemented

This repo's `core` module *is* `render-core` (`com.asteroid.duck.opengl:render-core`), so the
fix landed directly in `BlurTextureRenderer`/`BlurKernel` rather than requiring an upstream
change.

`BlurTextureRenderer` now supports a "naive" sampling mode, selected via `setNaive(boolean)`
(and `toggleNaive()` / `OffscreenBlurTextureRenderer.setNaive(boolean)` to propagate across
X/Y stages): instead of pairing adjacent Gaussian taps into a single hardware-bilinear-filtered
`texture()` fetch, it fetches every tap individually via `texelFetch` at an exact integer texel
coordinate. This bypasses the GPU's fixed-point interpolation entirely, so there is no
quantization bias left to accumulate — at the cost of roughly double the texture reads per
fragment versus the default hardware-sampling mode. `BlurKernel.getNaiveSampleKernel()` supplies
the un-paired kernel used by this mode.

Hardware sampling remains the default (unchanged behaviour, no perf regression for renderers
that don't hit this bug in practice). Enable naive mode on any blur stage inside a feedback loop
that shows the drift.

- **Accept it as a characteristic of the visualizer**: buffer "walk" from feedback loops
  is a long-standing trait of Cthugha-style visualizers; naive mode is opt-in precisely so
  effects that find the drift tolerable or aesthetically in-keeping can keep the cheaper
  hardware-sampling default.
