package com.asteriod.duck.opengl.experiments;

import com.asteriod.duck.opengl.*;
import com.asteriod.duck.opengl.util.CompositeRenderItem;
import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.RenderedItem;
import com.asteriod.duck.opengl.util.resources.texture.ImageOptions;
import com.asteriod.duck.opengl.util.resources.texture.Texture;

import java.awt.*;
import java.io.IOException;

import static org.lwjgl.opengl.GL11C.GL_RED;
import static org.lwjgl.opengl.GL30C.GL_R32F;

public class BlurPictureExample extends CompositeRenderItem implements Experiment {

	@Override
	public String getDescription() {
		return "Shows a simple picture blurred on screen using blur kernel implemented in shaders";
	}

	public DiscreteSampleKernel createBlurKernel() {
		return new BlurKernel(21).getDiscreteSampleKernel();
	}

	public RenderedItem createBlur(String sourceTexture, boolean x, DiscreteSampleKernel blurKernel, Texture targetTexture) {
		PassthruTextureRenderer renderer = new PassthruTextureRenderer(sourceTexture, "blur-" + (x ? "x" : "y"), shader -> {
			shader.setFloatArray("offset", blurKernel.floatOffsets());
			shader.setFloatArray("weight", blurKernel.floatWeights());
		});
		return new OffscreenTextureRenderer(renderer, targetTexture);
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		DiscreteSampleKernel blurKernel = createBlurKernel();

		ctx.getResourceManager().GetTexture("molly", "molly.jpg");
		ctx.getResourceManager().GetTexture("window", "window.jpeg");
		// a multi texture renderer alternating between two textures
		RenderedItem source = new MultiTextureRenderer("molly", "window");

		// create 2 offscreen textures
		Rectangle screen = ctx.getWindow();
		Texture[] offscreen = new Texture[3];
		for (int i = 0; i < offscreen.length ; i++) {
			offscreen[i] = Utils.createOffscreenTexture(screen, false);
			ctx.getResourceManager().PutTexture("offscreen"+i, offscreen[i]);
		}

		// wrap the multi tex to render to the offscreen texture 0
		OffscreenTextureRenderer offscreenTextureRenderer = new OffscreenTextureRenderer(source, offscreen[0]);
		// this passthrough uses the blur shader in X
		PassthruTextureRenderer blurX = new PassthruTextureRenderer("offscreen0", "blur-x", shader -> {
			shader.setFloatArray("offset", blurKernel.floatOffsets());
			shader.setFloatArray("weight", blurKernel.floatWeights());
		});
		// we wrap the blurX to render to the offscreen texture 1
		OffscreenTextureRenderer blurXStage = new OffscreenTextureRenderer(blurX , offscreen[1]);

		// a passthrough renderer (onto screen) of the "offscreen" texture 1 - which blurs Y on the way through
		PassthruTextureRenderer blurY = new PassthruTextureRenderer("offscreen1", "blur-y", shader -> {
			shader.setFloatArray("offset", blurKernel.floatOffsets());
			shader.setFloatArray("weight", blurKernel.floatWeights());
		});

		// A composite renderer to initialise and render the two paths: offscreen and onscreen
		addItems(offscreenTextureRenderer, blurXStage, blurY);
		super.init(ctx);
	}


}
