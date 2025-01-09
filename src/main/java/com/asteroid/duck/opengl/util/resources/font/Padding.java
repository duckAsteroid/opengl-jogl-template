package com.asteroid.duck.opengl.util.resources.font;

import java.awt.*;
import java.awt.image.BufferedImage;

public record Padding(int left, int top, int right, int bottom) {
	public Rectangle removeFrom(BufferedImage image) {
		return new Rectangle(left, top, image.getWidth() - left - right, image.getHeight() - top - bottom);
	}

	public int width() {
		return right + left;
	}

	public int height() {
		return bottom + top;
	}
}
