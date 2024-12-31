package com.asteroid.duck.opengl.util.resources.font;

import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 * A Font Glyph
 */
public record Glyph(int width, int height, int x, int y, float advance) {
	public Vector2f position() {
		return new Vector2f(x, y);
	}
	public Vector2f dimension() {
		return new Vector2f(width, height);
	}
	public Vector4f extent() {
		return new Vector4f(x, y, x + width, y + height);
	}
}
