package com.asteroid.duck.opengl.util.resources.font;

import java.nio.FloatBuffer;
import java.util.*;
import java.util.List;

import com.asteroid.duck.opengl.util.resources.Resource;
import com.asteroid.duck.opengl.util.resources.texture.Texture;


/**
 * A wrapper around a glyph-image based font. The glyphs (character images) are stored in a GPU
 * texture, and the various locations + sizes of each letter are described in a map of
 * {@link GlyphData} objects.
 * <p>
 * Based on (but different to)
 * <a href="https://github.com/SilverTiger/lwjgl3-tutorial/blob/master/src/silvertiger/tutorial/lwjgl/text/Font.java">this</a>
 * </p>
 */

public class FontTexture implements Resource {

	/**
	 * Contains the glyph data for each char.
	 */
	private final Map<Character, GlyphData> glyphs;
	/**
	 * Contains the complete font texture.
	 */
	private final Texture texture;
	/**
	 * Pixel height of the font (i.e. tallest glyph)
	 */
	private final int fontHeight;

	public FontTexture(Map<Character, GlyphData> glyphs, Texture texture) {
		this.glyphs = Collections.unmodifiableMap(glyphs);
		this.texture = texture;
		this.fontHeight = glyphs.values().stream()
						.map(GlyphData::bounds)
						.mapToInt(r -> r.height)
						.max().orElseThrow();
	}

	public GlyphData getGlyph(char c) {
		return glyphs.get(c);
	}

	/**
	 * Gets the width in pixels of the specified text.
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
			GlyphData g = glyphs.get(c);
			lineWidth += g.bounds().width;
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
			GlyphData g = glyphs.get(c);
			lineHeight = Math.max(lineHeight, g.bounds().height);
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
	public void computeVertexData(CharSequence text, float x, float y, Object c, FloatBuffer target) {
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


	@Override
	public void dispose() {
		texture.dispose();
	}


	public Texture getTexture() {
		return texture;
	}

	public List<GlyphData> glyphs() {
		return glyphs.values().stream().sorted(Comparator.comparingInt(value -> value.bounds().x)).toList();
	}
}
