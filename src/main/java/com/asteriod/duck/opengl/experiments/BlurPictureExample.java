package com.asteriod.duck.opengl.experiments;

import com.asteriod.duck.opengl.*;
import com.asteriod.duck.opengl.util.CompositeRenderItem;
import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.RenderedItem;
import com.asteriod.duck.opengl.util.blur.BlurKernel;
import com.asteriod.duck.opengl.BlurTextureRenderer;
import com.asteriod.duck.opengl.util.blur.DiscreteSampleKernel;
import com.asteriod.duck.opengl.util.resources.texture.DataFormat;
import com.asteriod.duck.opengl.util.resources.texture.ImageOptions;
import com.asteriod.duck.opengl.util.resources.texture.Texture;
import com.asteriod.duck.opengl.util.resources.texture.TextureFactory;

import java.awt.*;
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
		Texture gray = ctx.getResourceManager().GetTexture("window", "window.jpeg", ImageOptions.DEFAULT);
		super.init(ctx);
	}
}
