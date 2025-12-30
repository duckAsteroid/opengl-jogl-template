package com.asteroid.duck.opengl.util.resources.texture;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Hashtable;

import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11C.GL_RGBA8;
import static org.lwjgl.opengl.GL30.*;

/**
 * High levels formats of Texture, used during the texture loading process.
 * Defines the number of channels and the data type of those channels.
 * e.g. RGBA etc.
 */
public enum DataFormat implements TextureFactory.FormatHelper {
	/** Red, Green, Blue & Alpha - 4 bytes per pixel */
	RGBA(GL_RGBA, GL_RGBA8, GL_UNSIGNED_BYTE, 4) {
		public BufferedImage apply(Dimension d) {
			var glAlphaColorModel = new ComponentColorModel(ColorSpace
							.getInstance(ColorSpace.CS_sRGB), new int[] { 8, 8, 8, 8 },
							true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
			var raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
							d.width, d.height, 4, null);;
			return new BufferedImage(glAlphaColorModel, raster, true,
							new Hashtable<>());
		}
	},
	/** 1 channel greyscale using 32bit float per pixel */
	GRAY(GL_RED, GL_R32F, GL_FLOAT, 4) {
		public BufferedImage apply(Dimension d) {
			return new BufferedImage(d.width, d.height, BufferedImage.TYPE_BYTE_GRAY);
		}

		public ByteBuffer pixelData(BufferedImage img) {
			ByteBuffer buffer = ByteBuffer.allocateDirect(img.getWidth() * img.getHeight() * 4);
			buffer.order(ByteOrder.nativeOrder());
			for (int y = 0; y < img.getHeight(); y++) {
				for (int x = 0; x < img.getWidth(); x++) {
					float v = getLuminance(new Color(img.getRGB(x, y)));
					buffer.putFloat(v);
				}
			}
			buffer.flip();
			return buffer;
		}

		public float getLuminance(Color color) {
			// Get the RGB values
			int r = color.getRed();
			int g = color.getGreen();
			int b = color.getBlue();

			// Normalize the RGB values to the range 0-1
			float rf = r / 255f;
			float gf = g / 255f;
			float bf = b / 255f;

			// Calculate the luminance
			float luminance = 0.2126f * rf + 0.7152f * gf + 0.0722f * bf;

			return luminance;
		}

	},
	/** 2 channel RED/GREEN using 2 x 16 bit unsigned ints per pixel */
	TWO_CHANNEL_16_BIT(GL_RG_INTEGER, GL_RG16UI, GL_UNSIGNED_SHORT, 4) {
		public BufferedImage apply(Dimension d) {
			throw new UnsupportedOperationException("No image for RG16UI");
		}
	};

	private final int imageFormat;
	private final int internalFormat;
	private final int bytesPerPixel;
	private final int dataType;

	DataFormat(int imageFormat, int internalFormat, int dataType, int bytesPerPixel) {
		this.imageFormat = imageFormat;
		this.internalFormat = internalFormat;
		this.bytesPerPixel = bytesPerPixel;
		this.dataType = dataType;
	}

	@Override
	public int imageFormat() {
		return imageFormat;
	}

	@Override
	public int internalFormat() {
		return internalFormat;
	}

	@Override
	public int dataType() {
		return dataType;
	}

	@Override
	public void verify(ImageData data) throws IllegalArgumentException {
		int expectedSize = data.totalPixelCount() * bytesPerPixel;
		if (expectedSize != data.buffer().remaining()) {
			throw new IllegalArgumentException("Buffer size was "+ data.buffer().remaining() + ", expected " +expectedSize);
		}
	}

	@Override
	public ByteBuffer pixelData(BufferedImage img) {
		// build a byte buffer from the image data
		// that OpenGL can use to produce a texture.
		byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer())
						.getData();

		ByteBuffer imageBuffer = ByteBuffer.allocateDirect(data.length);
		imageBuffer.order(ByteOrder.nativeOrder());
		imageBuffer.put(data, 0, data.length);
		imageBuffer.flip();
		return imageBuffer;
	}
}
