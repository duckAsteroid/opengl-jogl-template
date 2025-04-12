package com.asteroid.duck.opengl.util.geom;

import org.joml.Vector4f;

import java.util.function.Function;

enum Horizontal implements Function<Vector4f, Float> {
	LEFT,
	RIGHT;

	@Override
	public Float apply(Vector4f vector4f) {
		return switch (this) {
			case LEFT -> vector4f.x;
			case RIGHT -> vector4f.z;
		};
	}
}
