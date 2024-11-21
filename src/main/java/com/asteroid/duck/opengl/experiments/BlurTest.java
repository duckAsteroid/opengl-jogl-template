package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.blur.BlurTextureRenderer;
import com.asteroid.duck.opengl.util.resources.texture.ImageOptions;

import java.io.IOException;

public class BlurTest extends BlurTextureRenderer implements Experiment {

	public BlurTest() {
		super("window");
	}
	@Override
	public String getDescription() {
		return "Quick test at a simple single pass with blur shader";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		ctx.getResourceManager().GetTexture("window", "window.jpeg", ImageOptions.DEFAULT);
		super.init(ctx);
	}

}
