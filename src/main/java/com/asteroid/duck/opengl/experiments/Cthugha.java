package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.*;
import com.asteroid.duck.opengl.util.audio.Polyline;
import com.asteroid.duck.opengl.util.blur.OffscreenBlurTextureRenderer;
import com.asteroid.duck.opengl.util.palette.PaletteRenderer;
import com.asteroid.duck.opengl.util.resources.texture.*;
import com.asteroid.duck.opengl.util.toggle.Frequency;
import com.asteroid.duck.opengl.util.toggle.ToggledRenderItem;
import org.joml.Vector4f;

import java.io.IOException;

/**
 * An attempt to do 90% of Cthugha:
 * <ul>
 *   <li>Translate</li>
 *   <li>Blur</li>
 *   <li>Wave</li>
 *   <li>Palette to screen</li>
 * </ul>
 */
public class Cthugha extends CompositeRenderItem implements Experiment {

	public static final String OFFSCREEN_TEXTURE_NAME = "yabadabado";

	@Override
	public String getDescription() {
		return "An attempt to do 90% of Cthugha";
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
		ctx.getResourceManager().PutTexture(OFFSCREEN_TEXTURE_NAME, offscreen);

		// translate the offscreen texture
		TranslateTextureRenderer translationStage = new TranslateTextureRenderer(OFFSCREEN_TEXTURE_NAME, "translate");
		OffscreenTextureRenderer offscreenTrans = new OffscreenTextureRenderer(translationStage, offscreen);
		addItem(offscreenTrans);

		// blur
		OffscreenBlurTextureRenderer blurStage = new OffscreenBlurTextureRenderer(OFFSCREEN_TEXTURE_NAME, opts);
		addItem(blurStage);

		// wave
		Polyline polyline = new Polyline();
		polyline.setLineWidth(1.0f);
		polyline.setLineColour(new Vector4f(1.0f));
		OffscreenTextureRenderer waveRenderStage = new OffscreenTextureRenderer(polyline, offscreen);
	  addItem(waveRenderStage);

		// palette
		Texture palette = ctx.getResourceManager().GetTexture("palette", "palettes/FIRE2.MAP.png", ImageOptions.DEFAULT.withSingleLine());
		PaletteRenderer paletteRenderer = new PaletteRenderer(OFFSCREEN_TEXTURE_NAME);

		addItem(paletteRenderer);
		// straight render

		super.init(ctx);
	}
}
