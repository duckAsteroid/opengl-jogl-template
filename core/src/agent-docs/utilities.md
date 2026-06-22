# Utility Renderers

The core module includes reusable utility renderers that compose well with `RenderedItem` and
`CompositeRenderItem`. Use this table as a quick "what should I reach for?" reference.

| Class | Package | What it does | Common use | Notes / key methods |
|---|---|---|---|---|
| `BlurTextureRenderer` | `com.asteroid.duck.opengl.util.blur` | Single-pass separable Gaussian blur over one axis (X or Y). | Add horizontal or vertical blur to a texture already in `ResourceManager`. | Toggle with `setBlur(boolean)`; switch axis with `setXAxis(boolean)`; tune radius with `setKernelSize(int)` (`3..65`, odd only). |
| `OffscreenBlurTextureRenderer` | `com.asteroid.duck.opengl.util.blur` | Multi-pass blur pipeline using intermediate FBO textures. | Strong bloom/glow style blur where one pass is not enough. | Constructor accepts source texture name and pass count; `multiply(float)` controls intensity; internally alternates X/Y blur stages. |
| `BlurKernel` | `com.asteroid.duck.opengl.util.blur` | Builds Gaussian weights/offsets for blur shaders. | Precompute or inspect kernel sample distributions. | `new BlurKernel(size).getDiscreteSampleKernel()` yields linear-sampling friendly taps. |
| `PaletteRenderer` | `com.asteroid.duck.opengl.util.palette` | Indexed-color post-process: source texture values become indices into a 1D palette texture. | Palette swapping, LUT-style recoloring, retro effects. | Requires source texture + palette texture; shader uniform name is `palette`; includes helpers like `greyScale()` and `rbgTestScale()`. |
| `ColorPalette` | `com.asteroid.duck.opengl.util.palette` | Loads and registers a palette texture as a single-line image. | Bring external palette files into `ResourceManager`. | `new ColorPalette(mgr, file)` auto-registers by filename; `size()` returns entry count. |
| `StringRenderer` | `com.asteroid.duck.opengl.util.text` | Renders dynamic text using a `FontTexture`. | HUD/debug overlays, FPS counters, experiment labels. | Call `setText(...)` before first render; `setTransform(Matrix4f)` sets position/rotation/scale via model matrix; `setTextColor(Vector4f)` is render-thread safe via internal queue. |
| `Block` | `com.asteroid.duck.opengl.util.text` | Tiny text helper for line splitting. | Multi-line string preprocessing before feeding text renderers. | `Block.lines(text)` splits on `\n`. |
| `OffscreenTextureRenderer` | `com.asteroid.duck.opengl.util` | Wraps any `RenderedItem` and renders it into a target texture via FBO. | Build post-processing chains and ping-pong pipelines. | Use with `TextureFactory.createTexture(...)`; restores viewport after offscreen pass. |
| `TranslateTextureRenderer` | `com.asteroid.duck.opengl.util` | Warps a source texture through a translation-map texture. | Distortion/displacement effects (heat haze, ripple maps, UV remap effects). | Needs source + map texture names; binds map to `map` sampler and uploads `dimensions` each frame. |
| `MapTransformRenderItem` | `com.asteroid.duck.opengl.util` | Full pipeline: source renderer → offscreen → apply translate map → offscreen → screen, with per-frame feedback. | Self-referential warp effects where the warped output feeds the next frame (e.g. spinning/swirling). | Constructor: `(RenderedItem source, String mapName, String offscreenName[, TextureOptions opts])`; creates and registers the offscreen texture internally; wraps source in an `OffscreenTextureRenderer` then chains `TranslateTextureRenderer`. |
| `MultiTextureRenderer` | `com.asteroid.duck.opengl.util` | Full-screen renderer that blends/combines multiple textures in one shader. | Compositing intermediate render targets. | Binds input textures as `tex0`, `tex1`, ... and updates a time-varying `amount` uniform. |

## Text overlay with `StringRenderer`

Text vertices sit at the origin in string space. Position, rotation, and scale are all applied via
`setTransform(Matrix4f)`, which only updates the `model` uniform — the vertex buffer is never
rebuilt by a transform change.

```java
FontTexture font = new FontTextureFactory(new Font("Monospaced", Font.PLAIN, 18), true)
    .createFontTexture();
StringRenderer text = new StringRenderer(font);
text.init(ctx);
text.setText("Hello, render-core");
text.setTextColor(StandardColors.CYAN.color);

// Position only — translate to (24, 48) in screen pixels
text.setTransform(new Matrix4f().translate(24, 48, 0));

// Position + rotate 15° clockwise around the text origin
text.setTransform(new Matrix4f().translate(24, 48, 0).rotateZ((float) Math.toRadians(15)));

// Position + scale up 2× + rotate
text.setTransform(new Matrix4f().translate(24, 48, 0).rotateZ(angle).scale(2, 2, 1));

// in doRender:
text.doRender(ctx);
```

When tracking transform state across key-press or animation events, keep the components (`tx`,
`ty`, `rotation`, `scale`) as separate fields and rebuild the matrix on each change rather than
reading it back from the renderer:

```java
private float tx, ty, rotation, scale = 1f;

private void applyTransform() {
    renderer.setTransform(
        new Matrix4f().translate(tx, ty, 0).rotateZ(rotation).scale(scale, scale, 1)
    );
}
```
