package com.asteriod.duck.opengl.experiments;

import com.asteriod.duck.opengl.util.resources.texture.ImageData;
import com.asteriod.duck.opengl.util.resources.texture.Texture;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.function.BiFunction;

import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_RED;
import static org.lwjgl.opengl.GL30C.GL_R32F;

public class Utils {

	public enum TestType implements BiFunction<Point, Dimension, Byte> {
		STRIPE {
			@Override
			public Byte apply(Point p, Dimension d) {
				return (byte) (p.x % 2);
			}
		},
		BANDS {
			@Override
			public Byte apply(Point p, Dimension d) {
				return (byte) (p.x % 255);
			}
		},
		HORIZONTAL {
			@Override
			public Byte apply(Point p, Dimension d) {
				double fraction = p.x / d.getWidth();
				return (byte) (fraction * 255);
			}
		},
		VERTICAL {
			@Override
			public Byte apply(Point p, Dimension d) {
				return (byte) (p.y % 256);
			}
		},
		RANDOM {
			@Override
			public Byte apply(Point p, Dimension d) {
				return (byte) (Math.random() * 256);
			}
		};
	}

	public static ImageData createTestData(Dimension size, TestType type) {
		ByteBuffer imageData = ByteBuffer.allocateDirect(size.width * size.height);
		for (int y = 0; y < size.height; y++) {
			for (int x = 0; x < size.width; x++) {
				Point p = new Point(x,y);
				imageData.put(type.apply(p,size));
			}
		}
		imageData.flip();
		return new ImageData(imageData, size);
	}


	public static Texture createOffscreenTexture(Rectangle screen, boolean is32f) {
		Texture offscreen = new Texture();
		if (is32f) {
			offscreen.setFilter(Texture.Filter.LINEAR);
			offscreen.setWrap(Texture.Wrap.CLAMP_TO_EDGE);
			offscreen.setInternalFormat(GL_R32F);
			offscreen.setImageFormat(GL_RED);
			offscreen.setDataType(GL_FLOAT);
		}
		offscreen.Generate(screen.width, screen.height, 0);
		return offscreen;
	}

}
