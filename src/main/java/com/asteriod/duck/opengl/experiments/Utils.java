package com.asteriod.duck.opengl.experiments;

import com.asteriod.duck.opengl.util.resources.texture.*;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.function.BiFunction;

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


}
