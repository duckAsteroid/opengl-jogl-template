package com.asteroid.duck.opengl.util.geom;

import org.joml.Vector4f;

import java.util.function.Function;

enum Vertical implements Function<Vector4f, Float> {
	TOP, BOTTOM;

	@Override
	public Float apply(Vector4f vector4f) {
		return switch (this) {
			case TOP -> vector4f.w;
			case BOTTOM -> vector4f.y;
		};
	}
}
