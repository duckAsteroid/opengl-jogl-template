package com.asteroid.duck.opengl.util.resources.texture;

import com.asteroid.duck.opengl.util.resources.io.Loader;
import com.asteroid.duck.opengl.util.resources.texture.io.ImageLoadingOptions;
import com.asteroid.duck.opengl.util.resources.texture.io.JavaImageLoader;
import com.asteroid.duck.opengl.util.resources.texture.io.RawLoader;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

public class TextureFactory {

	private static final Set<String> IMAGE_FORMATS = Set.of("png", "jpg", "jpeg");

	interface FormatHelper {
		BufferedImage apply(Dimension dimension);
		int internalFormat();
		int imageFormat();

		int dataType();

		void verify(ImageData data) throws IllegalArgumentException;
		ByteBuffer pixelData(BufferedImage bufferedImage);
	}

	private final Loader loader;

	public TextureFactory(Loader root) {
		this.loader = root;
	}

	public static Texture createTexture(Rectangle screen, boolean is32f) {
		TextureOptions options = null;
		if (is32f) {
			options = new TextureOptions(DataFormat.GRAY, Filter.LINEAR, Wrap.REPEAT);
		}
		else {
			options = new TextureOptions(DataFormat.RGBA, Filter.LINEAR, Wrap.REPEAT);
		}
		return createTexture(screen, null, options );
	}

	public static Texture createTexture(Rectangle screen, Dimension pad, TextureOptions options) {
		if (pad == null) {
			pad = new Dimension(0, 0);
		}
		Texture offscreen = new Texture();
		if (options != null) {
			offscreen.setFilter(options.filter());
			offscreen.setWrap(options.wrap());
			offscreen.setInternalFormat(options.dataFormat().internalFormat());
			offscreen.setImageFormat(options.dataFormat().imageFormat());
			offscreen.setDataType(options.dataFormat().dataType());
		}
		offscreen.generate(screen.width + pad.width, screen.height + pad.height, 0);
		return offscreen;
	}


	public static Texture createTexture(ImageLoadingOptions options, ImageData data) {
		Texture tex = new Texture();

		tex.setInternalFormat( options.dataFormat().internalFormat());
		tex.setImageFormat( options.dataFormat().imageFormat());
		tex.setDataType( options.dataFormat().dataType());

		options.dataFormat().verify(data);

		if (data.size().height == 1) {
			tex.generate1D(data.size().width, data.buffer());
			return tex;
		}
		else {
			tex.generate(data.size().width, data.size().height, data.buffer());
		}
		return tex;
	}

	public Texture LoadTexture(String texturePath, ImageLoadingOptions options) throws IOException {
			ImageData imageData = loadTextureData(texturePath, options);
			return createTexture(options, imageData);
	}

	public ImageData loadTextureData(String texturePath, ImageLoadingOptions options) throws IOException {
		if (IMAGE_FORMATS.stream().anyMatch(texturePath::endsWith)) {
			return new JavaImageLoader(loader).load(texturePath, options);
		}
		return new RawLoader(extractDimensions(texturePath), loader).load(texturePath, options);
	}

	private Dimension extractDimensions(String texturePath) {
		String[] split = texturePath.split("\\.");
		if (split.length == 3) {
			final String dimString = split[1];
			String[] dim = dimString.split("x");
			return new Dimension(Integer.parseInt(dim[0]), Integer.parseInt(dim[1]));
		}
		throw new IllegalArgumentException("No dimensions component to file name (expected {name}.{width}x{height}.{filetype}");
	}
}
