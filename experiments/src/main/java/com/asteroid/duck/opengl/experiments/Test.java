package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.PassthruTextureRenderer;
import com.asteroid.duck.opengl.util.RenderContext;

import java.io.IOException;

public class Test implements Experiment {
    @Override
    public String getDescription() {
        return "Just a Test";
    }

    private PassthruTextureRenderer textureRenderer1;
    private PassthruTextureRenderer textureRenderer2;

    @Override
    public void init(RenderContext ctx) throws IOException {
        ctx.getResourceManager().getTexture("window.jpeg", "window.jpeg");
        ctx.getResourceManager().getTexture("test-card.jpeg", "test-card.jpeg");

        textureRenderer1 = new PassthruTextureRenderer("test-card.jpeg");
        textureRenderer1.init(ctx);

        textureRenderer2 = new PassthruTextureRenderer("window.jpeg");
        textureRenderer2.init(ctx);
    }

    @Override
    public void doRender(RenderContext ctx) {
        long wholeSeconds = (long) ctx.getTimer().elapsed();
        if (wholeSeconds % 2 == 0) {
            textureRenderer2.doRender(ctx);
        }
        else {
            textureRenderer1.doRender(ctx);
        }
    }

    @Override
    public void dispose() {
        textureRenderer1.dispose();
        textureRenderer2.dispose();
    }
}
