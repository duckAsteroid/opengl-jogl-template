package com.asteroid.duck.opengl.util.color;

import org.joml.Vector4f;

import java.util.function.Supplier;

public enum StandardColors implements Supplier<Vector4f> {
	WHITE(1.0f),
	BLACK(0.0f),

	RED(1.0f, 0.0f, 0.0f),
	GREEN( 0.0f,1.0f, 0.0f),
	BLUE(0.0f, 0.0f,1.0f);

	public final Vector4f color;

	StandardColors(float grey) {
		color = new Vector4f(grey, grey, grey, 1.0f);
	}

	StandardColors(float r, float g, float b) {
		this.color = new Vector4f(r,g,b,1.0f);
	}

	@Override
	public Vector4f get() {
		return color;
	}
}
