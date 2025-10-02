package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.PassthruTextureRenderer;
import com.asteroid.duck.opengl.util.RenderContext;

public class BasicTexture extends PassthruTextureRenderer implements Experiment {

    public BasicTexture() {
        super("molly.jpg");
    }

    @Override
    protected com.asteroid.duck.opengl.util.resources.texture.Texture initTexture(RenderContext ctx) {
        return ctx.getResourceManager().GetTexture(textureName, textureName);
    }

    @Override
    public String getDescription() {
        return "A very simple example of a texture";
    }

}
