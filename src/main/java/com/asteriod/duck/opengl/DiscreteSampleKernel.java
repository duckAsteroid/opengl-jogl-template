package com.asteriod.duck.opengl;

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
