package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.OffscreenTextureRenderer;
import com.asteroid.duck.opengl.util.PassthruTextureRenderer;
import com.asteroid.duck.opengl.util.*;
import com.asteroid.duck.opengl.util.blur.BlurTextureRenderer;
import com.asteroid.duck.opengl.util.resources.texture.*;

import java.awt.*;
import java.io.IOException;

public class TranslateExample extends TranslateTextureRenderer implements Experiment {

	public TranslateExample() {
		super("testcard", "translate");
	}

	@Override
	public String getDescription() {
		return "Just uses a translate map shader on a picture";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		// load the test card image
		Texture texture = ctx.getResourceManager().GetTexture("testcard", "test-card.jpeg", ImageOptions.DEFAULT);
		// load the translation map - it's a matrix (screen sized) of 2 * 16 bit floats
		Texture translateMap = ctx.getResourceManager().GetTexture("translate", "translate/bighalfwheel.1024x800.tab", ImageOptions.DEFAULT.withType(DataFormat.TWO_CHANNEL_16_BIT));

		super.init(ctx);
	}

	public static void stuff(CompositeRenderItem compo, RenderContext ctx) {
		Texture offscreen1 = TextureFactory.createTexture(ctx.getWindow(), false);
		ctx.getResourceManager().PutTexture("offscreen1", offscreen1);

		Texture offscreen2 = TextureFactory.createTexture(ctx.getWindow(), false);
		ctx.getResourceManager().PutTexture("offscreen2", offscreen2);

		RenderedItem pipelineA = createTranslatePipeline(ctx, "testcard", "offscreen1", "translate", "offscreen2");
		RenderedItem pipelineB = createTranslatePipeline(ctx, "testcard", "offscreen2", "translate", "offscreen1");

		FlipFlopRenderedItem flipFlop = new FlipFlopRenderedItem(pipelineA, pipelineB);
		compo.addItem(flipFlop);
	}

	public static RenderedItem createTranslatePipeline(RenderContext ctx, String periodicImageTextureName, String sourceTexture, String translateTexture, String destinationTexture) {
		CompositeRenderItem pipeline = new CompositeRenderItem();
		Texture sourceTex = ctx.getResourceManager().GetTexture(sourceTexture);

		// render the named image to the source texture every 5 seconds
		ToggledRenderItem pictureRenderer = new ToggledRenderItem(new ToggledRenderItem.Frequency(5), new OffscreenTextureRenderer(new PassthruTextureRenderer(periodicImageTextureName), sourceTex));
		pipeline.addItem(pictureRenderer);

		TranslateTextureRenderer translateRenderer = new TranslateTextureRenderer(sourceTexture, "translate");
		// make the translation fo from source to target texture
		OffscreenTextureRenderer translateTarget = new OffscreenTextureRenderer(translateRenderer, ctx.getResourceManager().GetTexture(destinationTexture));
		pipeline.addItem(translateTarget);

		// dump target texture to screen
		PassthruTextureRenderer screenRenderer = new PassthruTextureRenderer(destinationTexture);
    pipeline.addItem(screenRenderer);

		return pipeline;
	}
}
