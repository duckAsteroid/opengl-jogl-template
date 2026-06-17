package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.resources.texture.*;

import java.io.IOException;

/**
 * A composite pipeline that applies a map-based (translation table) transform to a source renderer,
 * creating a per-frame feedback loop: source → offscreen → translate(offscreen, map) → offscreen → screen.
 *
 * <p>The translate stage reads from and writes back to the same offscreen texture each frame,
 * so the warped output feeds the next frame's input — producing an evolving, self-referential effect.
 */
public class MapTransformRenderItem extends CompositeRenderItem {

    public static final TextureOptions STANDARD_OPTS = new TextureOptions(DataFormat.RGBA, Filter.LINEAR, Wrap.REPEAT);

    private final RenderedItem source;
    private final String mapTextureName;
    private final String offscreenName;
    private final TextureOptions opts;

    public MapTransformRenderItem(RenderedItem source, String mapTextureName, String offscreenName) {
        this(source, mapTextureName, offscreenName, STANDARD_OPTS);
    }

    public MapTransformRenderItem(RenderedItem source, String mapTextureName, String offscreenName, TextureOptions opts) {
        this.source = source;
        this.mapTextureName = mapTextureName;
        this.offscreenName = offscreenName;
        this.opts = opts;
    }

    @Override
    public void init(RenderContext ctx) throws IOException {
        Texture offscreen = TextureFactory.createTexture(ctx.getWindow(), null, opts);
        ctx.getResourceManager().putTexture(offscreenName, offscreen);

        add(new OffscreenTextureRenderer(source, offscreen));

        TranslateTextureRenderer translate = new TranslateTextureRenderer(offscreenName, mapTextureName);
        add(new OffscreenTextureRenderer(translate, offscreen));

        add(new PassthruTextureRenderer(offscreenName));

        super.init(ctx);
    }
}
