# Resource Manager

`ResourceManager` (owned by `GLWindow`) is the single owner of all GL handles. Load textures,
shaders, and buffers through it — it calls `dispose()` on all resources at shutdown.

```java
ResourceManager rm = ctx.getResourceManager();

// Named textures — loaded once, cached by name
Texture tex = rm.getTexture("myTex", "textures/foo.png");
Texture tex2 = rm.getTexture("myTex", "textures/foo.png", ImageLoadingOptions.DEFAULT.withFlip());

// Named shaders loaded from classpath
ShaderProgram sh = rm.getShader("blur", "glsl/blur/vertex.glsl", "glsl/blur/frag.glsl", null);

// Texture units — allocated sequentially, avoids unit conflicts between components
TextureUnit unit = rm.nextTextureUnit();
unit.activate();
unit.bind(texture);
shader.uniforms().get("uTex", Integer.class).set(unit.index());

// Arbitrary lifecycle tracking — dispose() will be called at shutdown
rm.register(() -> { glDeleteBuffers(myPboId); });    // Resource is a @FunctionalInterface
```

---

## Constraints

| Constraint | Detail |
|---|---|
| **PBO persistent mapping** | A persistently-mapped PBO (`GL_MAP_PERSISTENT_BIT`) must stay mapped for the life of the object. Do not wrap it in `VertexBufferObject`; manage it with raw GL calls and register cleanup via `rm.register(...)`. |
| **ExclusivityGroup** | VAOs, EBOs, shader programs, and standard textures are bound through `ExclusivityGroup` wrappers. Always use the wrapper (`vao.bind(ctx)`, `shader.use(ctx)`) rather than raw `glBind*` calls to avoid silent state corruption. |
