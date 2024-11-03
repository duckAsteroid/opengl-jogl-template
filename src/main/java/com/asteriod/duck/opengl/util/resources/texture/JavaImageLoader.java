package com.asteriod.duck.opengl.util.resources.texture;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Uses ImageIO to load BufferedImage data from disk.
 * Creates a BufferedImage of the correct raster type.
 * Draws the image onto the raster image and returns the bytes.
 */
public class JavaImageLoader implements TextureDataLoader {
	@Override
	public ImageData load(Path path, ImageOptions options) throws IOException {
		try(InputStream inputStream = Files.newInputStream(path)) {
			BufferedImage bufferedImage = ImageIO.read(inputStream);
			if (options.singleLine()) {
				bufferedImage = bufferedImage.getSubimage(0, 0, bufferedImage.getWidth(), 1);
			}

			var dims = new Dimension(bufferedImage.getWidth(), bufferedImage.getHeight());
			var texImage = options.dataFormat().apply(dims);

			// copy the source image into the produced image
			Graphics2D g = (Graphics2D) texImage.getGraphics();
			g.setColor(new Color(0f, 0f, 0f, 0f)); // transparent
			g.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
			if (options.flipY()) {
				g.scale(1, -1);
				g.translate(0, -bufferedImage.getHeight());
			}
			g.drawImage(bufferedImage, 0, 0, null);


			ByteBuffer imageBuffer = options.dataFormat().pixelData(texImage);

			return new ImageData(imageBuffer, dims);
		}
	}
}
