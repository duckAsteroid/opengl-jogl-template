package com.asteroid.duck.opengl.util.blur;

/**
 * This represents a set of sample positions and weights to use in a GLSL Texture (sampler).
 * It exploits {@link org.lwjgl.opengl.GL#GL_LINEAR}
 * @param offsets
 * @param weights
 */
public record DiscreteSampleKernel(double[] offsets, double[] weights) {
	public int size() {
		return offsets.length;
	}

	public float[] floatOffsets() {
		float[] result = new float[offsets.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = (float) offsets[i];
		}
		return result;
	}

	public float[] floatWeights() {
		float[] result = new float[weights.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = (float) weights[i];
		}
		return result;
	}
}
