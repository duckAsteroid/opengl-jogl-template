package com.asteriod.duck.opengl.experiments;

import com.asteriod.duck.opengl.PaletteRenderer;
import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.resources.texture.ImageOptions;
import com.asteriod.duck.opengl.util.resources.texture.Texture;
import com.asteriod.duck.opengl.util.resources.texture.Type;

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
		// this is our grey scale picture
		Texture gray = ctx.getResourceManager().GetTexture("gray", "window.jpeg", ImageOptions.DEFAULT.withType(Type.GRAY));
		// this is our palette
		Texture palette = ctx.getResourceManager().GetTexture("palette", "palettes/FIRE2.MAP.png", ImageOptions.DEFAULT.withSingleLine());
		super.init(ctx);
	}
}
