package com.asteroid.duck.opengl.util.text;

import com.asteroid.duck.opengl.util.resources.texture.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FontTextureFactory {
	private final java.awt.Font font;
	private final boolean antialias;
	private FontMetrics fontMetrics;

	public FontTextureFactory(java.awt.Font font, boolean antialias) {
		this.font = font;
		this.antialias = antialias;
	}

	public Stream<Character> completeCharacterSet() {
		return IntStream.range(32, 256).filter(i -> i != 127).mapToObj(i -> (char)i);
	}

	public FontTexture createFontTexture() {
		FontMetrics metrics = getFontMetrics();
		Map<Character, BufferedImage> glyphImages = new HashMap<>();
		int maxHeight = 0;
		int totalWidth = 0;
		for(Character c : completeCharacterSet().toList()) {
			BufferedImage charImage = createCharacterImage(c);
			/* If char image is null that font does not contain the char */
			if (charImage != null) {
				glyphImages.put(c, charImage);
				if (charImage.getHeight() > maxHeight) {
					maxHeight = charImage.getHeight();
				}
				totalWidth += charImage.getWidth();
			}
		}
		// now we know which characters to process
		Map<Character, Glyph> glyphData = new HashMap<>(glyphImages.size());
		Dimension imageDims = new Dimension(totalWidth, maxHeight);
		BufferedImage image = newImage(imageDims);
		Graphics2D g = image.createGraphics();
		int x = 0;
		for(Character c : glyphImages.keySet()) {
      BufferedImage charImage = glyphImages.get(c);
			glyphData.put(c, new Glyph(charImage.getWidth(), charImage.getHeight(), x, image.getHeight() - charImage.getHeight(), 0f));
      g.drawImage(charImage, x, 0, null);
      x += charImage.getWidth();
    }
		ByteBuffer rawBuffer = DataFormat.RGBA.pixelData(image);
		ImageData data = new ImageData(rawBuffer, imageDims);
		return new FontTexture(glyphData, TextureFactory.createTexture(ImageOptions.DEFAULT, data));
	}

	public BufferedImage createCharacterImage(char c) {
		FontMetrics metrics = getFontMetrics();
		/* Get char charWidth and charHeight */
		int charWidth = metrics.charWidth(c);
		int charHeight = metrics.getHeight();
		/* Check if charWidth is 0 */
		if (charWidth == 0) {
			return null;
		}

		/* Create image for holding the char */
		// this is not using the native storage raster buffer
		// as it's just going to be painted onto the actual image
		BufferedImage image = new BufferedImage(charWidth, charHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		if (antialias) {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		g.setFont(font);
		g.setPaint(java.awt.Color.WHITE);
		g.drawString(String.valueOf(c), 0, metrics.getAscent());
		g.dispose();
		return image;
	}

	private BufferedImage newImage(Dimension size) {
		return DataFormat.RGBA.apply(size);
	}
	private BufferedImage newImage(int width, int height) {
		return DataFormat.RGBA.apply(new Dimension(width, height));
	}


	private FontMetrics getFontMetrics() {
		if (fontMetrics == null) {
			BufferedImage tmp = newImage(1,1);
			Graphics2D g = tmp.createGraphics();
			if (antialias) {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			}
			g.setFont(font);
			fontMetrics = g.getFontMetrics();
			g.dispose();
		}
		return fontMetrics;
	}
}
