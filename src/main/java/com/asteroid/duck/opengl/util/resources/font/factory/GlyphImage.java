package com.asteroid.duck.opengl.util.resources.font.factory;

import com.asteroid.duck.opengl.util.resources.font.GlyphData;
import com.asteroid.duck.opengl.util.resources.font.Padding;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * An individual generated glyph image with glyph location details (relative to the image)
 *
 * @param image  the BufferedImage (normally MUCH larger than the actual Glyph)
 * @param datum  the AWT Point coordinates of the character datum in the image
 * @param bounds the drawn area of the glyph in the image
 */
public record GlyphImage(BufferedImage image, Point datum, Rectangle bounds) {

	/**
	 * The offset of the datum within the bounds rectangle
	 * @return a bounding rectangle relative offset
	 */
	public Point boundsRelativeDatum() {
		return new Point(datum.x - bounds.x, datum.y - bounds.y);
	}

	/**
	 * A rectangle that encompasses the bounds AND the datum
	 *
	 * @return the bounds rect
	 */
	public Rectangle extent() {
		Rectangle extent = new Rectangle(bounds);
		extent.add(new Rectangle(datum, new Dimension(1,1)));
		return extent;
	}


	/**
	 * Render this glyph image into the font image strip
	 *
	 * @param x            the X location where the image is to be rendered
	 * @param g            the graphics 2D in the strip image (where we render to)
	 *
	 * @return the resulting GlyphData of the rendered section - allowing us to recover the glyph later
	 */
	public GlyphData renderToStrip(Padding padding, int x, Graphics2D g) {
		Rectangle destination = new Rectangle(x + padding.left(), padding.top(), bounds.width, bounds.height);
		ImageRenderer renderer = new ImageRenderer(g, image);
		renderer.drawImage(bounds, destination);
		return new GlyphData(boundsRelativeDatum(), destination);
	}

}
