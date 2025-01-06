package com.asteroid.duck.opengl.util.resources.font;

import org.joml.Matrix2f;
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

	/**
	 * The position = x,y (bottom left)
	 * z and w are the the top right
	 */
	public Vector4f extent(Matrix2f transform) {
		Vector2f position = transform == null ? position() : transform.transform(position());
		Vector2f dimension = transform == null ? dimension() : transform.transform(dimension());
		return new Vector4f(position, position.x + dimension.x, position.y + dimension.y);
	}
}
