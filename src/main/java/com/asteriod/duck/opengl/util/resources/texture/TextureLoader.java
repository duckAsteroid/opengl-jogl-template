package com.asteriod.duck.opengl.util.resources.texture;

import com.asteriod.duck.opengl.util.resources.impl.AbstractResourceLoader;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static org.lwjgl.opengl.GL11.GL_RGBA;

public class TextureLoader extends AbstractResourceLoader<Texture> {

	interface FormatHelper {
		BufferedImage apply(Dimension dimension);
		int internalFormat();
		int imageFormat();
		void verify(ImageData data) throws IllegalArgumentException;
	}

	public TextureLoader(Path root) {
		super(root);
	}

	public static Texture createTexture(ImageOptions options, ImageData data) throws IOException {
		Texture tex = new Texture();

		tex.setInternalFormat( options.type().internalFormat());
		tex.setImageFormat( options.type().imageFormat());

		options.type().verify(data);

		if (data.size().height == 1) {
			tex.Generate1D(data.size().width, data.buffer());
			return tex;
		}
		else {
			tex.Generate(data.size().width, data.size().height, data.buffer());
		}
		return tex;
	}

	public Texture LoadTexture(String texturePath, ImageOptions options) throws IOException {
			ImageData imageData = loadTextureData(texturePath, options);
			if (Boolean.getBoolean("texture.dumpbin")) {
				Path path = getPath(texturePath + ".bin");
				try(var channel = Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
					channel.write(imageData.buffer());
				}
				imageData.buffer().clear();
			}
			return createTexture(options, imageData);
	}

	public ImageData loadTextureData(String texturePath, ImageOptions options) throws IOException {
		try(InputStream inputStream = Files.newInputStream(getPath(texturePath))) {
			BufferedImage bufferedImage = ImageIO.read(inputStream);
			if (options.singleLine()) {
				bufferedImage = bufferedImage.getSubimage(0, 0, bufferedImage.getWidth(), 1);
			}

			var texImage = options.type().apply(new Dimension(bufferedImage.getWidth(), bufferedImage.getHeight()));

			// copy the source image into the produced image
			Graphics2D g = (Graphics2D) texImage.getGraphics();
			g.setColor(new Color(0f, 0f, 0f, 0f)); // transparent
			g.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
			if (options.flipY()) {
				g.scale(1, -1);
				g.translate(0, -bufferedImage.getHeight());
			}
			g.drawImage(bufferedImage, 0, 0, null);


			// build a byte buffer from the temporary image
			// that be used by OpenGL to produce a texture.
			byte[] data = ((DataBufferByte) texImage.getRaster().getDataBuffer())
							.getData();

			ByteBuffer imageBuffer = ByteBuffer.allocateDirect(data.length);
			imageBuffer.order(ByteOrder.nativeOrder());
			imageBuffer.put(data, 0, data.length);
			imageBuffer.flip();

			return new ImageData(imageBuffer, new Dimension(bufferedImage.getWidth(), bufferedImage.getHeight()));
		}
	}
}
