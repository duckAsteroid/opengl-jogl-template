package com.asteroid.duck.opengl.experiments;


import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.blur.OffscreenBlurTextureRenderer;
import com.asteroid.duck.opengl.util.resources.texture.io.ImageLoadingOptions;
import com.asteroid.duck.opengl.util.resources.texture.Texture;

import java.io.IOException;

public class BlurPictureExample extends OffscreenBlurTextureRenderer implements Experiment {

	@Override
	public String getDescription() {
		return "Shows a simple picture blurred on screen using blur kernel implemented in shaders";
	}

	public BlurPictureExample() {
		super("window");
	}
	@Override
	public void init(RenderContext ctx) throws IOException {
		Texture window = ctx.getResourceManager().GetTexture("window", "test-card.jpeg", ImageLoadingOptions.DEFAULT);
		super.init(ctx);
	}
}
