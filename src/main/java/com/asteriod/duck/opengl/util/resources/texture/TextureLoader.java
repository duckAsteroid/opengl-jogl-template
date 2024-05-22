package com.asteriod.duck.opengl.util.resources.texture;

import com.asteriod.duck.opengl.util.resources.ResourceManager;
import com.asteriod.duck.opengl.util.resources.impl.AbstractResourceLoader;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Hashtable;

import static org.lwjgl.opengl.GL11.GL_RGBA;

public class TextureLoader extends AbstractResourceLoader<Texture> {

	public TextureLoader(Path root) {
		super(root);
	}

	public static Texture createTexture(int width, int height, ByteBuffer textureData) throws IOException {
		Texture tex = new Texture();

		tex.setInternalFormat( GL_RGBA);
		tex.setImageFormat( GL_RGBA);

		tex.Generate(width, height, textureData);
		return tex;
	}

	public Texture LoadTexture(String texturePath, ResourceManager.ImageOptions options) throws IOException {
			ImageData imageData = loadTextureData(texturePath, options);
			return createTexture(imageData.size().width, imageData.size().height, imageData.buffer());
	}

	public ImageData loadTextureData(String texturePath, ResourceManager.ImageOptions options) throws IOException {
		try(InputStream inputStream = Files.newInputStream(getPath(texturePath))) {
			BufferedImage bufferedImage = ImageIO.read(inputStream);
			if (options.singleLine()) {
				bufferedImage = bufferedImage.getSubimage(0, 0, bufferedImage.getWidth(), 1);
			}
			ColorModel glAlphaColorModel = new ComponentColorModel(ColorSpace
							.getInstance(ColorSpace.CS_sRGB), new int[] { 8, 8, 8, 8 },
							true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);

			WritableRaster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
							bufferedImage.getWidth(), bufferedImage.getHeight(), 4, null);
			BufferedImage texImage = new BufferedImage(glAlphaColorModel, raster, true,
							new Hashtable<>());

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
