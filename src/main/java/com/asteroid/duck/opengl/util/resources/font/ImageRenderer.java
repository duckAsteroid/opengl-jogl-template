package com.asteroid.duck.opengl.util.resources.font;

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

	private static Point topLeftOf(Rectangle r) {
		return new Point(r.x, r.y);
	}

	private static Point bottomRightOf(Rectangle r) {
		return new Point(r.x + r.width, r.y + r.height);
	}

	public void drawImage(Rectangle src, Rectangle dst) {
		Point srcFirst = topLeftOf(src);
		Point srcSecond = bottomRightOf(src);
		Point dstFirst = topLeftOf(dst);
		Point dstSecond = bottomRightOf(dst);
		target.drawImage(source,
						dstFirst.x, dstFirst.y, dstSecond.x, dstSecond.y,
						srcFirst.x, srcFirst.y, srcSecond.x, srcSecond.y, null);
	}
}
