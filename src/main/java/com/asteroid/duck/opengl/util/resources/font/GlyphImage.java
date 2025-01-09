package com.asteroid.duck.opengl.util.resources.font;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * An individual generated glyph image with glyph location details (relative to the image)
 *
 * @param image  the BufferedImage (normally MUCH larger than the actual Glyph)
 * @param x      the X coordinate of the character datum in the image
 * @param y      the Y coordinate of the character datum in the image
 * @param bounds the drawn area of the glyph (relative to the datum) in the image
 */
public record GlyphImage(BufferedImage image, int x, int y, Rectangle bounds) {
	/**
	 * The point on the image that is the datum for this glyph
	 *
	 * @return the datum point
	 */
	public Point datum() {
		return new Point(x, y);
	}

	public Rectangle imageRelativeBounds() {
		Rectangle imageBounds = new Rectangle(bounds);
		imageBounds.translate(x, y);
		return imageBounds;
	}
	/**
	 * A rectangle that encompasses the bounds AND the datum
	 *
	 * @return the extent rect
	 */
	public Rectangle extent() {
		Point datum = datum();
		Rectangle imageBounds = imageRelativeBounds();
		int x = Math.min(imageBounds.x, datum.x);
		int y = Math.min(imageBounds.y, datum.y);
		int width = Math.max(imageBounds.x + imageBounds.width, datum.x) - x;
		int height = Math.max(imageBounds.y + imageBounds.height, datum.y) - y;
		return new Rectangle(x, y, width, height);
	}


	/**
	 * Render this glyph image into the font image strip
	 *
	 * @param x the X location where the image is to be rendered
	 * @param g a graphics 2D in the strip image (where we render to)
	 * @return the width of the rendered section (so we can advance X)
	 */
	public GlyphData renderToStrip(int x, Graphics2D g) {
		Rectangle source = imageRelativeBounds();
		Point sourceFirst = topLeftOf(source);
		Point sourceSecond = bottomRightOf(source);

		//
		Rectangle destination = new Rectangle(x, 0, bounds.width, bounds.height);
		Point destinationFirst = topLeftOf(destination);
		Point destinationSecond = bottomRightOf(destination);
		// draw the image
		g.drawImage(image,
						destinationFirst.x, destinationFirst.y, destinationSecond.x, destinationSecond.y,
						sourceFirst.x, sourceFirst.y, sourceSecond.x, sourceSecond.y,
						null);

		// datum offset from destination rectangle
		Point datumOffset = new Point(-bounds.x, -bounds.y);
		return new GlyphData(datumOffset, destination);
	}

	private static Point topLeftOf(Rectangle r) {
		return new Point(r.x, r.y);
	}

	private static Point bottomRightOf(Rectangle r) {
		return new Point(r.x + r.width, r.y + r.height);
	}
}
