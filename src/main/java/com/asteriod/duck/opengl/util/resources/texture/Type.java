package com.asteriod.duck.opengl.util.resources.texture;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.util.Hashtable;

import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11C.GL_RGBA8;
import static org.lwjgl.opengl.GL30.*;

public enum Type implements TextureFactory.FormatHelper {
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
	GRAY(GL_RED_INTEGER, GL_R8UI, GL_UNSIGNED_BYTE, 1) {
		public BufferedImage apply(Dimension d) {
			return new BufferedImage(d.width, d.height, BufferedImage.TYPE_BYTE_GRAY);
		}
	},
	TWO_CHANNEL_16_BIT(GL_RG_INTEGER, GL_RG16UI, GL_UNSIGNED_SHORT, 4) {
		public BufferedImage apply(Dimension d) {
			throw new UnsupportedOperationException("No image for RG16UI");
		}
	};

	private final int imageFormat;
	private final int internalFormat;
	private final int bytesPerPixel;
	private final int dataType;

	Type(int imageFormat, int internalFormat, int dataType, int bytesPerPixel) {
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
		int totalPixels = data.size().width * data.size().height;
		int expectedSize = totalPixels * bytesPerPixel;
		if (expectedSize != data.buffer().remaining()) {
			throw new IllegalArgumentException("Buffer size was "+ data.buffer().remaining() + ", expected " +expectedSize);
		}
	}
}
