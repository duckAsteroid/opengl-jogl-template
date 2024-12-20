package com.asteroid.duck.opengl.util.resources.font;
import java.awt.*;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.Map;

import com.asteroid.duck.opengl.util.resources.Resource;
import com.asteroid.duck.opengl.util.resources.texture.Texture;


/**
 * A wrapper around a glyph-image based font. When initialised it creates an in memory image (RGBA)
 * and renders every ASCII letter into that image. The image is then transferred to the GPU as a
 * texture. This texture can then be used in a shader to render letters on screen.
 *
 * The offsets for each letter (glyph) are also stored so we can use this to render strings in future
 *
 * Based on (but different to)
 * https://github.com/SilverTiger/lwjgl3-tutorial/blob/master/src/silvertiger/tutorial/lwjgl/text/Font.java
 */

public class FontTexture implements Resource {

	/**
	 * Contains the glyphs for each char.
	 */
	private final Map<Character, Glyph> glyphs;
	/**
	 * Contains the font texture.
	 */
	private final Texture texture;

	private final int fontHeight;

	public FontTexture(Map<Character, Glyph> glyphs, Texture texture) {
		this.glyphs = Collections.unmodifiableMap(glyphs);
		this.texture = texture;
		this.fontHeight = glyphs.values().stream().mapToInt(Glyph::height).max().orElseThrow();
	}

	/**
	 * Gets the width of the specified text.
	 *
	 * @param text The text
	 *
	 * @return Width of text
	 */
	public int getWidth(CharSequence text) {
		int width = 0;
		int lineWidth = 0;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\n') {
				/* Line end, set width to maximum from line width and stored
				 * width */
				width = Math.max(width, lineWidth);
				lineWidth = 0;
				continue;
			}
			if (c == '\r') {
				/* Carriage return, just skip it */
				continue;
			}
			Glyph g = glyphs.get(c);
			lineWidth += g.width();
		}
		width = Math.max(width, lineWidth);
		return width;
	}

	/**
	 * Gets the height of the specified text.
	 *
	 * @param text The text
	 *
	 * @return Height of text
	 */
	public int getHeight(CharSequence text) {
		int height = 0;
		int lineHeight = 0;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\n') {
				/* Line end, add line height to stored height */
				height += lineHeight;
				lineHeight = 0;
				continue;
			}
			if (c == '\r') {
				/* Carriage return, just skip it */
				continue;
			}
			Glyph g = glyphs.get(c);
			lineHeight = Math.max(lineHeight, g.height());
		}
		height += lineHeight;
		return height;
	}

	/**
	 * Compute the vertex data required to draw the text at the specified position and color.
	 *
	 * @param text     Text to draw
	 * @param x        X coordinate of the text position
	 * @param y        Y coordinate of the text position
	 * @param c        Color to use
	 * @param target   The float buffer (VBO) to write vertex data into
	 */
	public void computeVertexData(CharSequence text, float x, float y, Color c, FloatBuffer target) {
		// FIXME - create a GlyphTextureVert that would have the shader, VAO, VBO and color etc
		// it would use this texture and associated glyph data to compute the buffer when changed
		int textHeight = getHeight(text);

		float drawX = x;
		float drawY = y;
		if (textHeight > fontHeight) {
			drawY += textHeight - fontHeight;
		}

		/*texture.bind();
		renderer.begin();
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			if (ch == '\n') {
				/* Line feed, set x and y to draw at the next line /
				drawY -= fontHeight;
				drawX = x;
				continue;
			}
			if (ch == '\r') {
				/* Carriage return, just skip it /
				continue;
			}
			Glyph g = glyphs.get(ch);
			renderer.drawTextureRegion(texture, drawX, drawY, g.x, g.y, g.width, g.height, c);
			drawX += g.width();
		}
		renderer.end();
*/
	}

	/**
	 * Draw text at the specified position.
	 *
	 * @param renderer The renderer to use
	 * @param text     Text to draw
	 * @param x        X coordinate of the text position
	 * @param y        Y coordinate of the text position
	 */
	public void drawText(CharSequence text, float x, float y) {
		//drawText(renderer, text, x, y, Color.WHITE);
	}

	@Override
	public void destroy() {
		texture.destroy();
	}
}
