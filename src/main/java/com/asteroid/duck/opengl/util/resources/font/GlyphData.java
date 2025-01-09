package com.asteroid.duck.opengl.util.resources.font;

import org.joml.Matrix2f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;

/**
 * A Font Glyph location inside a font texture strip
 * @param datumOffset The offset to the datum from the top left of the image extent
 * @param extent The extent of the image for the glyph (in the strip)
 */
public record GlyphData(Point datumOffset, Rectangle extent) {
	public int advance() {
		int advance = extent.width;
		advance -= datumOffset.x;
		return advance;
	}
	/**
	 * The position = x,y (bottom left)
	 * z and w are the the top right
	 * Normalised using the transform matrix (e.g. in normalised in the font texture strip coords)
	 */
	public Vector4f extent(Matrix2f transform) {
		Vector2f topLeft = transform == null ? topLeft() : transform.transform(topLeft());
		Vector2f bottomRight = transform == null ? bottomRight() : transform.transform(bottomRight());
		return new Vector4f(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y);
	}

	private Vector2f topLeft() {
		return new Vector2f(extent.x, extent.y);
	}

	private Vector2f bottomRight() {
		return new Vector2f(extent.x + extent.width, extent.y + extent.height);
	}
}
