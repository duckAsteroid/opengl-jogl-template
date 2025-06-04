package com.asteroid.duck.opengl.util.resources.font;

import org.joml.Vector2f;
import org.joml.Vector4f;

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

	public Vector4f normalBounds(Vector2f imageDimensions) {
		// 1. Calculate the pixel coordinates for the sub-rectangle's corners.
		//    AWT coordinates:
		//    top-left: (bounds.x, bounds.y)
		//    bottom-right: (bounds.x + bounds.width, bounds.y + bounds.height)

		// 2. Normalize X-coordinates (s-coordinates in OpenGL)
		//    s_min corresponds to bounds.x
		//    s_max corresponds to bounds.x + bounds.width
		float s_min = bounds.x / imageDimensions.x;
		float s_max = (bounds.x + bounds.width) / imageDimensions.x;

		// 3. Normalize Y-coordinates (t-coordinates in OpenGL) and invert for OpenGL's bottom-left origin.
		//    In AWT:
		//    The top edge of the bounds is at bounds.y from the image's top.
		//    The bottom edge of the bounds is at bounds.y + bounds.height from the image's top.

		//    To get OpenGL's pixel Y (from bottom):
		//    OpenGL pixel Y for AWT's top edge = imageSize.height - bounds.y
		//    OpenGL pixel Y for AWT's bottom edge = imageSize.height - (bounds.y + bounds.height)

		//    Since t_min is the bottom of the region and t_max is the top:
		float t_min = (bounds.y + bounds.height) / imageDimensions.y;
		float t_max =  bounds.y / imageDimensions.y;

		// Create and return the Vector4f
		return new Vector4f(s_min, t_min, s_max, t_max);
	}
}
