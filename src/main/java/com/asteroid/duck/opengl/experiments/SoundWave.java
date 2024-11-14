package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.*;
import com.asteroid.duck.opengl.util.CompositeRenderItem;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import com.asteroid.duck.opengl.util.resources.texture.TextureFactory;

import java.io.IOException;

public class SoundWave extends CompositeRenderItem implements Experiment {
	@Override
	public String getDescription() {
		return "Renders an audio wave on screen";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		Polyline line = new Polyline();

		Texture offscreen = TextureFactory.createTexture(ctx.getWindow(), true);
		ctx.getResourceManager().PutTexture("offscreen", offscreen);

		OffscreenBlurTextureRenderer blur = new OffscreenBlurTextureRenderer("offscreen", "offscreen");
		addItem(blur);

		OffscreenTextureRenderer offscreenRenderer = new OffscreenTextureRenderer(line, offscreen);
		addItem(offscreenRenderer);

		PassthruTextureRenderer render = new PassthruTextureRenderer("offscreen");
		addItem(render);

		super.init(ctx);
	}
}
