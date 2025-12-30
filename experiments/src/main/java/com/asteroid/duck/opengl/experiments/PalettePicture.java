package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.palette.PaletteRenderer;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.texture.DataFormat;
import com.asteroid.duck.opengl.util.resources.texture.io.ImageLoadingOptions;
import com.asteroid.duck.opengl.util.resources.texture.Texture;

import java.io.IOException;

public class PalettePicture extends PaletteRenderer implements Experiment {
	@Override
	public String getDescription() {
		return "Renders a picture using a palette map";
	}

	public PalettePicture() {
		super("gray");
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		// this is our one channel grey scale picture
		Texture gray = ctx.getResourceManager().getTexture("gray", "window.jpeg", ImageLoadingOptions.DEFAULT.withType(DataFormat.GRAY));
		// this is our palette
		Texture palette = ctx.getResourceManager().getTexture("palette", "palettes/FIRE2.MAP.png", ImageLoadingOptions.DEFAULT.withSingleLine());
		super.init(ctx);
	}
}
