package com.asteroid.duck.opengl.util.resources.font;

import java.awt.*;

/**
 * A Font Glyph location inside a font texture strip.
 * This is defined in the normal pixel coordinate system where 0,0 is top left.
 * @param datumOffset The offset to the datum within the image bounds
 * @param bounds The bounds of the image for the glyph (in the strip texture)
 */
public record GlyphData(Point datumOffset, Rectangle bounds) {
	public int advance() {
		int advance = bounds.width;
		advance -= datumOffset.x;
		return advance;
	}
}
