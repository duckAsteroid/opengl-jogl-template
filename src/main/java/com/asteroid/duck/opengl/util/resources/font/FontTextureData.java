package com.asteroid.duck.opengl.util.resources.font;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;

public record FontTextureData(Map<Character, GlyphImage> glyphImages, Map<Character, GlyphData> glyphData,
                              BufferedImage combined) {
	public Dimension combinedSize() {
		return new Dimension(combined.getWidth(), combined.getHeight());
	}
}
