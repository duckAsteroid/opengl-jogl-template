package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.OffscreenTextureRenderer;
import com.asteroid.duck.opengl.util.PassthruTextureRenderer;
import com.asteroid.duck.opengl.util.*;
import com.asteroid.duck.opengl.util.resources.texture.*;
import com.asteroid.duck.opengl.util.toggle.Frequency;
import com.asteroid.duck.opengl.util.toggle.ToggledRenderItem;

import java.io.IOException;

public class TranslateExample extends CompositeRenderItem implements Experiment {

	@Override
	public String getDescription() {
		return "Just uses a translate map shader on a picture";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		ctx.setClearScreen(true);
		double updatePeriod = 1.0 / 25.0; // in seconds
		ctx.setDesiredUpdatePeriod(updatePeriod);
		// load the test card image
		Texture texture = ctx.getResourceManager().GetTexture("testcard", "test-card.jpeg", ImageOptions.DEFAULT);
		// load the translation map - it's a matrix (screen sized) of 2 * 16 bit floats
		Texture translateMap = ctx.getResourceManager().GetTexture("translate", "translate/bighalfwheel.1024x800.tab", ImageOptions.DEFAULT.withType(DataFormat.TWO_CHANNEL_16_BIT));
		// offscreen texture
		TextureOptions opts = new TextureOptions(DataFormat.RGBA, Texture.Filter.LINEAR, Texture.Wrap.REPEAT);
		Texture offscreen = TextureFactory.createTexture(ctx.getWindow(), null, opts);
		ctx.getResourceManager().PutTexture("yabadabado", offscreen);

		// render the named image to the source texture every 5 seconds
		ToggledRenderItem pictureRenderer = new ToggledRenderItem(new Frequency(1, 0.1d), new PassthruTextureRenderer("testcard"));
		OffscreenTextureRenderer offscreenRenderer = new OffscreenTextureRenderer(pictureRenderer, offscreen);
		addItem(offscreenRenderer);

		// translate the offscreen texture
		TranslateTextureRenderer translationStage = new TranslateTextureRenderer("yabadabado", "translate");
    OffscreenTextureRenderer offscreenTrans = new OffscreenTextureRenderer(translationStage, offscreen);
    addItem(offscreenTrans);

		// render the offscreen to the screen
		PassthruTextureRenderer screenRenderer = new PassthruTextureRenderer("yabadabado");
    addItem(screenRenderer);

		super.init(ctx);
	}
}
