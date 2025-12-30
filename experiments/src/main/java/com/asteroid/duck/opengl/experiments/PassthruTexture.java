package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.PassthruTextureRenderer;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.texture.Texture;

public class PassthruTexture extends PassthruTextureRenderer implements Experiment {

    public PassthruTexture() {
        super("molly.jpg");
    }

    @Override
    protected Texture initTexture(RenderContext ctx) {
        return ctx.getResourceManager().getTexture(textureName, textureName);
    }

    @Override
    public String getDescription() {
        return "Renders a texture - using PassthruTextureRenderer";
    }

}
