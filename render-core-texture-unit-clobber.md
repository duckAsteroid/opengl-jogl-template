# render-core Bug: Texture Setup Operations Clobber Allocated Texture Units

**Affects:** `com.asteroid.duck.opengl:render-core:0.0.1`  
**Severity:** High — causes samplers in allocated shader programs to silently read from the wrong texture  
**Discovered in:** `cthuga-reborn` consumer project during OpenGL migration

---

## 1. Background

render-core manages OpenGL texture units through `ResourceManagerImpl`, which allocates units
1–31 on demand via `nextTextureUnit()`. Unit 0 is deliberately excluded from the pool,
functioning as an unmanaged scratch unit. `TextureUnit` is the correct abstraction: it calls
`glActiveTexture(GL_TEXTURE0 + index)` before every `glBindTexture()`, ensuring bindings land
on the right unit.

The bug is that two places in the library — `Texture.generate()` and the `FrameBuffer`
constructor — perform setup-time `glBindTexture()` calls **without first calling
`glActiveTexture()`**. They therefore silently bind to whichever unit happens to be active at
the time of the call, which is determined by whatever unrelated code ran most recently.

---

## 2. Root Cause

### 2.1 `Texture.bind()` has no concept of texture units

File: `com/asteroid/duck/opengl/util/resources/texture/Texture.java`

```java
public void bind() {
    glBindTexture(dimensions.openGlCode(), id);   // no glActiveTexture — binds to ambient active unit
}

public void unbind() {
    glBindTexture(dimensions.openGlCode(), 0);    // same — unbinds from ambient active unit
}
```

Both `generate()` overloads call `bind()` / `unbind()`:

```java
public void generate(int width, int height, long pixels) {
    this.width = width;
    this.height = height;
    // create texture
    bind();                           // ← PROBLEM: active unit is caller-determined
    glTexImage2D(...);
    wrap.openGlParamsStream()...;
    filter.openGlParamsStream()...;
    unbind();                         // ← PROBLEM: same
}

public void generate(int width, int height, ByteBuffer data) {
    this.width = width;
    this.height = height;
    bind();                           // ← PROBLEM
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glTexImage2D(...);
    wrap.openGlParamsStream()...;
    filter.openGlParamsStream()...;
    unbind();                         // ← PROBLEM
}
```

### 2.2 `FrameBuffer` constructor calls `target.bind()` without activating a safe unit

File: `com/asteroid/duck/opengl/util/resources/framebuffer/FrameBuffer.java`

```java
public FrameBuffer(Texture target) {
    this.target = target;
    fbo = glGenFramebuffers();
    bind();           // binds the FBO — no effect on texture units
    target.bind();    // ← PROBLEM: glBindTexture on ambient active unit
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, target.getId(), 0);
    glDrawBuffers(GL_COLOR_ATTACHMENT0);
    ...
    unbind();         // unbinds the FBO — no effect on texture units
}
```

### 2.3 `AudioWave.doRender()` omits `glActiveTexture` before updating its 1-D texture

File: `com/asteroid/duck/opengl/util/wave/AudioWave.java`

In `initShader()`, the audio texture is bound to unit 0:
```java
glActiveTexture(GL_TEXTURE0);
glBindTexture(GL_TEXTURE_1D, audioTextureId);
shader.uniforms().get("uAudioTex", Integer.class).set(0);
```

But in `doRender()`, the `glActiveTexture(GL_TEXTURE0)` call is missing:
```java
glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboId);
glBindTexture(GL_TEXTURE_1D, audioTextureId);   // ← PROBLEM: active unit is ambient, not unit 0
glTexSubImage1D(GL_TEXTURE_1D, 0, 0, AUDIO_BUFFER_SIZE, GL_RG, GL_SHORT, 0);
glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
```

This happens to not corrupt rendered output today (1D and 2D targets per unit are independent,
and the shader reads from unit 0 where the texture was correctly bound in `initShader()`), but
it is incorrect and fragile.

---

## 3. Concrete Failure: notifRenderer samples waveOverlayTex instead of the font atlas

This is the specific bug observed in the consumer project. It is entirely caused by the
`FrameBuffer` constructor issue (§2.2) combined with `Texture.generate()` (§2.1).

### 3.1 Init sequence and how unit 8 is clobbered

`CthughaWindow.init()` runs the following sequence. The right-hand column tracks the GL active
texture unit after each call.

| # | Call | Active unit |
|---|------|------------|
| 1 | Various `Texture.generate()` calls (screenOverlayTex, pingTex, pongTex, …) | 0 (default) |
| 2 | `new FrameBuffer(pingTex)` — `target.bind()` hits unit 0 | 0 |
| 3 | `new FrameBuffer(pongTex)` — `target.bind()` hits unit 0 | 0 |
| 4 | `translateRenderer.init()` — allocates units 1, 2; `TextureUnit.activate()` sets active | **2** |
| 5 | `overlayBaker.init()` — allocates unit 3 | **3** |
| 6 | `flameRenderer.init()` — allocates unit 4 | **4** |
| 7 | `paletteDisplay.init()` — allocates units 5, 6 | **6** |
| 8 | `quoteRenderer.init()` — allocates unit 7, binds quoteFontTex to unit 7 | **7** |
| 9 | `notifRenderer.init()` — allocates unit 8, binds notifFontTex to unit 8 | **8** |
| 10 | `waveOverlayTex.generate(w, h, 0L)` — `bind()` hits unit 8, then `unbind()` hits unit 8 | **8** — notifFontTex unbound! unit 8 now = null |
| 11 | `new FrameBuffer(waveOverlayTex)` — `target.bind()` hits unit 8 | **8** — unit 8 now = waveOverlayTex! |
| 12 | `waveOverlayRenderer.init()` — allocates unit 9, binds waveOverlayTex to unit 9 | **9** |

After init completes:

| Unit | Expected binding | Actual binding |
|------|-----------------|----------------|
| 7 | quoteFontTex | quoteFontTex ✓ |
| 8 | notifFontTex | **waveOverlayTex** ✗ |
| 9 | waveOverlayTex | waveOverlayTex ✓ |

### 3.2 What the user sees

`notifRenderer`'s shader has its `tex` sampler set to unit 8 (set once in `init()` via
`textureUnit.useInShader(shader, "tex")`). At render time unit 8 holds `waveOverlayTex` — a
1280×720 RGBA render target containing the audio waveform (white line, ~85% alpha) on a fully
transparent background.

The `StringRenderer` fragment shader is:
```glsl
vec4 color = texture(tex, texCoords);
float mask = color.r;
fragColor = vec4(textColor.rgb * mask, textColor.a * color.a);
```

Sampling `waveOverlayTex` at the UV coordinates of the font glyphs:

- **Most pixels** (wave not present): `color = (0,0,0,0)` → `fragColor = (0,0,0,0)`. GL blending
  is disabled when `StringRenderer.doRender()` is called in the consumer, so this writes opaque
  black to the framebuffer → **black squares where each glyph quad sits**.
- **Pixels where the wave sweeps through**: `color = (1,1,1,0.85)` → `fragColor =
  (textColor.r, textColor.g, 0, 0.85)` (yellow for notif text) → **the waveform is visible
  inside the character quads**.

`quoteRenderer` (unit 7) is unaffected because nothing disturbs unit 7 after
`quoteRenderer.init()` binds the quote font there. Quote text renders correctly.

---

## 4. The Fix

Three changes are required, all small and confined to the files below. The invariant to enforce
is: **`glBindTexture()` for setup purposes (texture upload, FBO attachment) must always target
unit 0, because the ResourceManager never allocates unit 0 to any component.**

### 4.1 Fix `Texture.generate()` — both overloads

File: `com/asteroid/duck/opengl/util/resources/texture/Texture.java`

Add `glActiveTexture(GL_TEXTURE0)` as the first GL call in both `generate()` overloads, before
the `bind()` call. The import for `GL_TEXTURE0` is already available via
`org.lwjgl.opengl.GL13.GL_TEXTURE0`.

**Before** (long-pixel-pointer overload):
```java
public void generate(int width, int height, long pixels) {
    this.width = width;
    this.height = height;
    if (dimensions != Dimensions.TWO_DIMENSION) throw new IllegalArgumentException("...");
    // create texture
    bind();
    glTexImage2D(dimensions.openGlCode(), 0, this.internalFormat, width, height,
                 0, this.imageFormat, this.dataType, pixels);
    wrap.openGlParamsStream().forEach(param -> glTexParameteri(...));
    filter.openGlParamsStream().forEach(param -> glTexParameteri(...));
    // unbind texture
    unbind();
}
```

**After**:
```java
public void generate(int width, int height, long pixels) {
    this.width = width;
    this.height = height;
    if (dimensions != Dimensions.TWO_DIMENSION) throw new IllegalArgumentException("...");
    glActiveTexture(GL_TEXTURE0);     // use the unmanaged scratch unit — never allocated by ResourceManager
    bind();
    glTexImage2D(dimensions.openGlCode(), 0, this.internalFormat, width, height,
                 0, this.imageFormat, this.dataType, pixels);
    wrap.openGlParamsStream().forEach(param -> glTexParameteri(...));
    filter.openGlParamsStream().forEach(param -> glTexParameteri(...));
    unbind();
}
```

**Before** (ByteBuffer overload):
```java
public void generate(int width, int height, ByteBuffer data) {
    this.width = width;
    this.height = height;
    if (dimensions != Dimensions.TWO_DIMENSION) throw new IllegalArgumentException("...");
    // create Texture
    bind();
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glTexImage2D(dimensions.openGlCode(), 0, this.internalFormat, width, height,
                 0, this.imageFormat, this.dataType, data);
    wrap.openGlParamsStream().forEach(param -> glTexParameteri(...));
    filter.openGlParamsStream().forEach(param -> glTexParameteri(...));
    // unbind texture
    unbind();
}
```

**After**:
```java
public void generate(int width, int height, ByteBuffer data) {
    this.width = width;
    this.height = height;
    if (dimensions != Dimensions.TWO_DIMENSION) throw new IllegalArgumentException("...");
    glActiveTexture(GL_TEXTURE0);     // use the unmanaged scratch unit
    bind();
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glTexImage2D(dimensions.openGlCode(), 0, this.internalFormat, width, height,
                 0, this.imageFormat, this.dataType, data);
    wrap.openGlParamsStream().forEach(param -> glTexParameteri(...));
    filter.openGlParamsStream().forEach(param -> glTexParameteri(...));
    unbind();
}
```

If `generate1D()` has the same structure (it does in the current source), apply the same fix
there too.

The required import is already available if the file imports from `GL13`; if not, add:
```java
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
```

### 4.2 Fix `FrameBuffer` constructor

File: `com/asteroid/duck/opengl/util/resources/framebuffer/FrameBuffer.java`

Add `glActiveTexture(GL_TEXTURE0)` immediately before `target.bind()` in the constructor.

**Before**:
```java
public FrameBuffer(Texture target) {
    this.target = target;
    fbo = glGenFramebuffers();
    bind();
    target.bind();
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, target.getId(), 0);
    glDrawBuffers(GL_COLOR_ATTACHMENT0);
    final int fboStatus = glCheckFramebufferStatus(GL_FRAMEBUFFER);
    if (fboStatus != GL_FRAMEBUFFER_COMPLETE) {
        int error = glGetError();
        if (fboStatus == GL_FRAMEBUFFER_UNSUPPORTED)
            throw new IllegalArgumentException("Unsupported framebuffer type " + error);
        throw new IllegalArgumentException("Error creating framebuffer " + error);
    }
    glViewport(0, 0, target.getWidth(), target.getHeight());
    System.out.println("Frame buffer ready " + fbo);
    unbind();
}
```

**After**:
```java
public FrameBuffer(Texture target) {
    this.target = target;
    fbo = glGenFramebuffers();
    bind();
    glActiveTexture(GL_TEXTURE0);     // use the unmanaged scratch unit for attachment — never allocated by ResourceManager
    target.bind();
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, target.getId(), 0);
    glDrawBuffers(GL_COLOR_ATTACHMENT0);
    final int fboStatus = glCheckFramebufferStatus(GL_FRAMEBUFFER);
    if (fboStatus != GL_FRAMEBUFFER_COMPLETE) {
        int error = glGetError();
        if (fboStatus == GL_FRAMEBUFFER_UNSUPPORTED)
            throw new IllegalArgumentException("Unsupported framebuffer type " + error);
        throw new IllegalArgumentException("Error creating framebuffer " + error);
    }
    glViewport(0, 0, target.getWidth(), target.getHeight());
    System.out.println("Frame buffer ready " + fbo);
    unbind();
}
```

Add the import if not already present:
```java
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
```

### 4.3 Fix `AudioWave.doRender()` — missing `glActiveTexture` before 1-D texture update

File: `com/asteroid/duck/opengl/util/wave/AudioWave.java`

In `doRender()`, add `glActiveTexture(GL_TEXTURE0)` before the `glBindTexture(GL_TEXTURE_1D, ...)` call. This is consistent with `initShader()` which correctly activates unit 0 before binding the same texture.

**Before** (excerpt from `doRender()`):
```java
glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboId);
glBindTexture(GL_TEXTURE_1D, audioTextureId);
glTexSubImage1D(GL_TEXTURE_1D, 0, 0, AUDIO_BUFFER_SIZE, GL_RG, GL_SHORT, 0);
glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
```

**After**:
```java
glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboId);
glActiveTexture(GL_TEXTURE0);         // audio texture is bound to unit 0 (set in initShader) — be explicit
glBindTexture(GL_TEXTURE_1D, audioTextureId);
glTexSubImage1D(GL_TEXTURE_1D, 0, 0, AUDIO_BUFFER_SIZE, GL_RG, GL_SHORT, 0);
glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
```

`GL_TEXTURE0` is already imported in `AudioWave.java` (`import static org.lwjgl.opengl.GL13.GL_TEXTURE0`).

---

## 5. Why these changes are safe

- **Unit 0 is the correct scratch unit.** `ResourceManagerImpl.initTextureUnits()` explicitly
  excludes index 0 from its pool (`for (int i = 1; i < 32; i++)`). No `nextTextureUnit()` call
  will ever return unit 0. Using unit 0 for setup is therefore guaranteed not to overwrite any
  component's sampler binding.

- **`glActiveTexture(GL_TEXTURE0)` leaves the active unit on 0 after setup.** Any component
  that subsequently calls `TextureUnit.activate()` sets the active unit explicitly to its own
  index, so no component relies on a particular ambient active unit at render time. Leaving the
  active unit at 0 after setup is harmless.

- **`glFramebufferTexture2D` uses the texture's GL ID, not the active unit.** The attachment
  call takes `target.getId()` directly. The `glActiveTexture` + `target.bind()` sequence in the
  `FrameBuffer` constructor only needs to happen so that OpenGL has a valid 2D target bound
  before `glTexImage2D` — the active unit itself is irrelevant to the attachment.

- **The 1-D audio texture on unit 0 is unaffected by 2D sampler components.** OpenGL maintains
  separate binding slots per texture target per unit. Setting unit 0's `GL_TEXTURE_1D` binding
  does not disturb the `GL_TEXTURE_2D` binding on unit 0 (used only during setup and then
  unbound via `Texture.unbind()`).

---

## 6. Verification

After applying the three fixes above, the following should hold for any consumer application:

1. **Texture creation order independence.** `RenderedItem` components may be `init()`-ed in any
   order. A `FrameBuffer` constructed between two `StringRenderer.init()` calls must not affect
   either renderer's font texture binding.

2. **Notification / quote text renders correctly.** In `cthuga-reborn`, both `notifRenderer`
   (yellow text, top-left, position `(20, 30)`) and `quoteRenderer` (white italic, centre-left,
   position `(40, h/2)`) must display readable glyphs — not black squares and not waveform
   artefacts — when their respective text is set.

3. **No regression in palette display.** The palette LUT texture (unit 6 in the current
   allocation) must remain bound correctly so that `PaletteDisplayRenderer` converts R8 palette
   indices to the correct RGBA colours.

A simple integration test would be: construct two `StringRenderer` instances, a `FrameBuffer`,
and then another `StringRenderer`, all sharing the same `RenderContext`. Call `init()` on all
of them in that order. Render a frame. Assert that each `StringRenderer` produces output
coloured by its own font atlas rather than the framebuffer's target texture.

---

## 7. Optional longer-term improvement

The three point-fixes above are minimal and correct. For a more defensive API, consider also
making `Texture.bind()` and `Texture.unbind()` package-private so only `TextureUnit` and
`Texture` itself can call them. This prevents consumer code — and future library code — from
accidentally calling `texture.bind()` without going through a `TextureUnit`. The public-facing
contract then becomes: *you bind textures through `TextureUnit`, not through `Texture` directly*.
