package com.asteroid.duck.opengl.util.palette;

import com.asteroid.duck.opengl.util.resources.ResourceManager;
import com.asteroid.duck.opengl.util.resources.texture.ImageOptions;
import com.asteroid.duck.opengl.util.resources.texture.Texture;

import java.io.IOException;
import java.nio.file.Path;

public class ColorPalette {
	private final Texture palette;
	private final String name;

	public ColorPalette(ResourceManager mgr, String file) throws IOException {
		palette = mgr.getTextureFactory().LoadTexture(file, ImageOptions.DEFAULT.withSingleLine());
		// TODO figure out a name
		this.name = Path.of(file).getFileName().toString();
		mgr.PutTexture(name, palette);
	}

	public int size() {
		return palette.getWidth();
	}
}
