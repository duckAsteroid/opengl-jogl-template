package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.*;
import com.asteroid.duck.opengl.util.audio.Polyline;
import com.asteroid.duck.opengl.util.blur.OffscreenBlurTextureRenderer;
import com.asteroid.duck.opengl.util.keys.KeyCombination;
import com.asteroid.duck.opengl.util.palette.PaletteRenderer;
import com.asteroid.duck.opengl.util.resources.texture.*;

import java.io.IOException;

public class SoundWave extends CompositeRenderItem implements Experiment {
	@Override
	public String getDescription() {
		return "Renders an audio wave on screen";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {

		Texture offscreen = TextureFactory.createTexture(ctx.getWindow(), true);
		ctx.getResourceManager().PutTexture("offscreen", offscreen);

		Texture translateMap = ctx.getResourceManager().GetTexture("translate", "translate/bighalfwheel.1024x800.tab", ImageOptions.DEFAULT.withType(DataFormat.TWO_CHANNEL_16_BIT));
		// setup a renderer to use the translate shader
		PassthruTextureRenderer renderer = new PassthruTextureRenderer("offscreen", "translate", shaderProgram -> {
			// let the shader know about the map as a texture

		});

		OffscreenTextureRenderer tranlsateRenderer = new OffscreenTextureRenderer(renderer, offscreen);
		addItem(ToggledRenderItem.wrap(tranlsateRenderer, 'T', "Toggle Translation stage"));

		OffscreenBlurTextureRenderer blur = new OffscreenBlurTextureRenderer("offscreen");
		addItem(ToggledRenderItem.wrap(blur, 'B', "Toggle Blur stage"));


		Polyline line = new Polyline();
		OffscreenTextureRenderer offscreenRenderer = new OffscreenTextureRenderer(line, offscreen);
		addItem(offscreenRenderer);

		{
			SwitchableRenderItem switcher = new SwitchableRenderItem();
			Texture palette = ctx.getResourceManager().GetTexture("palette", "palettes/FIRE2.MAP.png", ImageOptions.DEFAULT.withSingleLine());
			PaletteRenderer paletteRenderer = new PaletteRenderer("offscreen");
			switcher.addItem(paletteRenderer);

			PassthruTextureRenderer passThruRenderer = new PassthruTextureRenderer("offscreen");
			switcher.addItem(passThruRenderer);

			ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('R'), switcher::next, "Switch final render");
			addItem(switcher);
		}
		super.init(ctx);
	}
}
