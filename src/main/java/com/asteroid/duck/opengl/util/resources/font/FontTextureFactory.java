package com.asteroid.duck.opengl.util.resources.font;

import com.asteroid.duck.opengl.util.resources.font.factory.FontTextureData;
import com.asteroid.duck.opengl.util.resources.font.factory.FontTextureDataFactory;
import com.asteroid.duck.opengl.util.resources.texture.*;
import com.asteroid.duck.opengl.util.resources.texture.io.ImageLoadingOptions;

import java.awt.*;
import java.nio.ByteBuffer;

public class FontTextureFactory {
	private final FontTextureDataFactory fontTextureDataFactory;

	public FontTextureFactory(final Font font, boolean antiAlias) {
		this.fontTextureDataFactory = new FontTextureDataFactory(font, antiAlias);
	}

	public FontTexture createFontTexture() {
		FontTextureData fontTextureData = fontTextureDataFactory.createFontTextureData();
		ByteBuffer rawBuffer = DataFormat.RGBA.pixelData(fontTextureData.combined());
		ImageData data = new ImageData(rawBuffer, fontTextureData.combinedSize());
		return new FontTexture(fontTextureData.glyphData(), TextureFactory.createTexture(ImageLoadingOptions.DEFAULT, data));
	}
}
