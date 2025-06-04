package com.asteroid.duck.opengl.util.resources.font.factory;

import com.asteroid.duck.opengl.util.geom.Vertice;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A helper to render rectangular areas of a source image to rectangular areas of a destination
 * Graphics context
 */
public class ImageRenderer {
	private final Graphics2D target;
	private final BufferedImage source;

	public ImageRenderer(Graphics2D target, BufferedImage source) {
		this.target = target;
		this.source = source;
	}

	public void drawImage(Rectangle src, Rectangle dst) {
		Point srcFirst = Vertice.TOP_LEFT.pointFrom(src);
		Point srcSecond = Vertice.BOTTOM_RIGHT.pointFrom(src);
		Point dstFirst = Vertice.TOP_LEFT.pointFrom(dst);
		Point dstSecond = Vertice.BOTTOM_RIGHT.pointFrom(dst);
		target.drawImage(source,
						dstFirst.x, dstFirst.y, dstSecond.x, dstSecond.y,
						srcFirst.x, srcFirst.y, srcSecond.x, srcSecond.y, null);
	}
}
