package com.asteroid.duck.opengl.util.resources.font;

import com.asteroid.duck.opengl.util.resources.texture.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FontTextureFactory {
	private final java.awt.Font font;
	private final boolean antialias;
	private final Padding padding;
	private FontMetrics fontMetrics;
	private Path imageDumpPath;
	public boolean debugBoundary = false;

	public FontTextureFactory(java.awt.Font font, boolean antialias) {
		this.font = font;
		this.padding = padding(font.getSize());
		this.antialias = antialias;
	}

	public static Padding padding(int size) {
		int pad = Math.max(1, (5 * size) / 100);
		return new Padding(pad, pad, pad, pad);
	}

	void setImageDumpPath(Path p) {
		this.imageDumpPath = p;
	}

	public Stream<Character> completeCharacterSet() {
		return IntStream.range(32, 256).filter(i -> i != 127).mapToObj(i -> (char)i);
	}

	public FontTexture createFontTexture() {
		FontTextureData fontTextureData = createFontTextureData();
		ByteBuffer rawBuffer = DataFormat.RGBA.pixelData(fontTextureData.combined());
		ImageData data = new ImageData(rawBuffer, fontTextureData.combinedSize());
		return new FontTexture(fontTextureData.glyphData(), TextureFactory.createTexture(ImageOptions.DEFAULT, data));
	}

	public FontTextureData createFontTextureData() {
		FontMetrics metrics = getFontMetrics();
		// create a map of each character as an image containing it
		Map<Character, GlyphImage> glyphImages = new HashMap<>();
		int maxHeight = 0;
		int totalWidth = 0;
		for(Character c : completeCharacterSet().toList()) {
			GlyphImage charImage = createCharacterImage(c);
			/* If char image is null that font does not contain the char */
			if (charImage != null) {
				glyphImages.put(c, charImage);
				Rectangle rect = charImage.bounds();
				if (rect.height > maxHeight) {
					maxHeight = rect.height;
				}
				totalWidth += rect.width;
			}
		}
		// now create a single image out of the characters and glyph data
		Map<Character, GlyphData> glyphData = new HashMap<>();
		Dimension imageDims = new Dimension(totalWidth, maxHeight);
		BufferedImage image = newImage(imageDims);
		Graphics2D g = image.createGraphics();
		// OpenGL images are upside down... so correct for that
		g.scale(1, -1);
		g.translate(0, -image.getHeight());

		// now draw each character in a line
		int x = 0;
		for(Character c : glyphImages.keySet()) {
			GlyphImage gi = glyphImages.get(c);

			GlyphData gData = gi.renderToStrip(x, g);
			glyphData.put(c, gData);
			x += gData.extent().width;
    }
		// dump the image for debug
		if(imageDumpPath != null) {
			try {
				Files.createDirectories(imageDumpPath);
        ImageIO.write(image, "png", Files.newOutputStream(imageDumpPath.resolve("font.png")));
      } catch (IOException e) {
        System.err.println("Failed to dump image: " + e.getMessage());
      }
		}

		return new FontTextureData(glyphImages, glyphData, image );
	}

	/**
	 * Create a glyph image (image with data) for a given character
	 * @param c the character
	 * @return a glyph image (if the character renders) or null
	 */
	public GlyphImage createCharacterImage(char c) {
		FontMetrics metrics = getFontMetrics();
		/* Get char charWidth */
		int charWidth = metrics.charWidth(c);
		/* Check if charWidth is 0 */
		if (charWidth == 0) {
			return null;
		}

		final String charStr = String.valueOf(c);
		/* Create image for holding the char */
		// this is not using the native storage raster buffer
		// as it's just going to be painted onto the actual image
		int imageWidth = metrics.getMaxAdvance() + padding.width();
		int imageHeight = metrics.getMaxAscent() + metrics.getMaxDescent() + padding.height();
		int baseline = metrics.getMaxAscent() + padding.top();
		BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g = image.createGraphics();
		if (antialias) {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		g.setFont(font);
		g.setPaint(java.awt.Color.WHITE);
		int x = imageWidth / 2;
		int y = baseline;
		g.drawString(charStr, x, baseline);

		// find the bounding box
		Rectangle bounds = findPixelBounds(image, 1);
		if (bounds.width < 0 || bounds.height < 0) {
			// we did not have any pixel bounds (empty image - e.g. space)
			// make some up
			int width = metrics.charWidth(c);
			bounds = new Rectangle(x, y, width, 1);
		}
		bounds.translate(-x, -y);
		g.dispose();
		return new GlyphImage(image, x, y, bounds);
	}

	private Rectangle findPixelBounds(BufferedImage image, int alphaThreshold) {
		int left = image.getWidth(), right = 0, top = image.getHeight(), bottom = 0;
		for(int y = 0; y < image.getHeight(); y++) {
			for(int x = 0; x < image.getWidth(); x++) {
				int rgbaColor = image.getRGB(x, y);
				int red = (rgbaColor >> 24) & 0xFF;
				int green = (rgbaColor >> 16) & 0xFF;
				int blue = (rgbaColor >> 8) & 0xFF;
				int alpha = rgbaColor & 0xFF;

				if (alpha >= alphaThreshold) {
					left = Math.min(left, x);
					right = Math.max(right, x);
					top = Math.min(top, y);
					bottom = Math.max(bottom, y);
				}
			}
		}
		return new Rectangle(left, top, right - left + 1, bottom - top + 1);
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
