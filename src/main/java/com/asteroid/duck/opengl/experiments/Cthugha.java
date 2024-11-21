package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.CompositeRenderItem;
import com.asteroid.duck.opengl.util.OffscreenTextureRenderer;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.audio.Polyline;
import com.asteroid.duck.opengl.util.palette.PaletteRenderer;
import com.asteroid.duck.opengl.util.resources.texture.*;

import java.awt.*;
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
	@Override
	public String getDescription() {
		return "An attempt to do 90% of Cthugha";
	}
	@Override
	public void init(RenderContext ctx) throws IOException {
		Rectangle screen = ctx.getWindow();
		TextureOptions red = new TextureOptions(DataFormat.GRAY, Texture.Filter.LINEAR, Texture.Wrap.REPEAT);
		// wave
		Polyline polyline = new Polyline();
		polyline.setLineWidth(1.0f);
		Texture waveTexture = TextureFactory.createTexture(screen, null, red);
		ctx.getResourceManager().PutTexture("wave", waveTexture);
		OffscreenTextureRenderer waveRenderStage = new OffscreenTextureRenderer(polyline, waveTexture);
	  addItem(waveRenderStage);

		// palette
		Texture palette = ctx.getResourceManager().GetTexture("palette", "palettes/greyscale2.png", ImageOptions.DEFAULT.withSingleLine());
		PaletteRenderer paletteRenderer = new PaletteRenderer("wave");

		addItem(paletteRenderer);
		super.init(ctx);
	}
}
