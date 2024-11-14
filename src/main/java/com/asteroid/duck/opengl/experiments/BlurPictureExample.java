package com.asteroid.duck.opengl.experiments;


import com.asteroid.duck.opengl.BlurTextureRenderer;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.texture.ImageOptions;
import com.asteroid.duck.opengl.util.resources.texture.Texture;

import java.io.IOException;

public class BlurPictureExample extends BlurTextureRenderer implements Experiment {

	@Override
	public String getDescription() {
		return "Shows a simple picture blurred on screen using blur kernel implemented in shaders";
	}

	public BlurPictureExample() {
		super("window");
	}
	@Override
	public void init(RenderContext ctx) throws IOException {
		Texture gray = ctx.getResourceManager().GetTexture("window", "test-card.jpeg", ImageOptions.DEFAULT);
		super.init(ctx);
	}
}
