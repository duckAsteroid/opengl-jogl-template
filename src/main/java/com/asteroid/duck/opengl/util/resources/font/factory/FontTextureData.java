package com.asteroid.duck.opengl.util.resources.font.factory;

import com.asteroid.duck.opengl.util.resources.font.FontTextureFactory;
import com.asteroid.duck.opengl.util.resources.font.GlyphData;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * A partial result within the {@link FontTextureFactory} process.
 * @param glyphImages The AWT Glyph images that go into a {@link com.asteroid.duck.opengl.util.resources.font.FontTexture}
 * @param glyphData The metrics data about each Glyph
 * @param combined A single image combining the glyph images into a strip
 */
public record FontTextureData(Map<Character, GlyphImage> glyphImages, Map<Character, GlyphData> glyphData,
                              BufferedImage combined) {
	public Dimension combinedSize() {
		return new Dimension(combined.getWidth(), combined.getHeight());
	}
}
