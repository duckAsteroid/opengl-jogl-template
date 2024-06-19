package com.asteriod.duck.opengl;

import java.util.stream.DoubleStream;
import java.util.Arrays;

/**
 * The concept and the maths for this class comes from the article "Efficient Gaussian Blur with Linear Sampling"
 * by RasterGrid:
 * https://www.rastergrid.com/blog/2010/09/efficient-gaussian-blur-with-linear-sampling/
 *
 * It's essentially a way to implement Gaussian blur in a two pass filter (X/Y) and using as few texture lookups as
 * possible, by using texel interpolation from GL to do some of the maths for you in hardware.
 *
 * The maths is a bit complex, but the idea is that we can use a pascal triangle to get the coefficients for the
 * kernel, and then we can eliminate the reflection and take half of the coefficients. We then do the maths to
 * ensure that the sample points (interpolation) do the right amount of texel blending for the weights and distance
 * required.
 */
public class BlurKernel {
	public final double[] offsets;
	public final double[] weights;
	public final int size;

	public BlurKernel(int size) {
		if (size <= 1) throw new IllegalArgumentException("Kernel size must be > 1");
		if (size % 2 == 0) throw new IllegalArgumentException("Kernel size must be odd");

		this.size  = size;
		double[] pascal = pascal(size); // we will discard last 2 on each end as these have little effect
		double[] coeffs = new double[pascal.length - 4];
		System.arraycopy(pascal, 2, coeffs, 0, coeffs.length);
		double sum = Arrays.stream(coeffs).sum();
		// normalise coefficients (0-1)
		double[] normalisedCoefficients = new double[coeffs.length];
		for (int i = 0; i < normalisedCoefficients.length; i++) {
			normalisedCoefficients[i] = coeffs[i] / sum;
		}
		// ok now we have normalised coefficients - lets eliminate the reflection and take half
		int center = (normalisedCoefficients.length / 2); // rounds down 3/2 = 1, 5/2 = 2 etc.
		double[] half = Arrays.copyOfRange(normalisedCoefficients, center, normalisedCoefficients.length);
		double[] linearOffsets = DoubleStream.iterate(0, o -> o + 1).limit(half.length).toArray();
		this.weights = half;
		this.offsets = linearOffsets;
	}

	public DiscreteSampleKernel getDiscreteSampleKernel() {
		double[] discreteWeights = new double[(weights.length / 2) + 1] ;
		discreteWeights[0] = weights[0];
		// 1, 3
		for (int i = 0; i < discreteWeights.length - 1; i++) {
			int t1 = 1 + (i * 2);
			int t2 = t1 + 1;
			discreteWeights[i + 1] = weights[t1] + weights[t2];
		}
		double[] discreteOffsets = new double[discreteWeights.length];
		discreteOffsets[0] = 0.0;
		for (int i = 0; i < discreteOffsets.length - 1; i++) {
			int t1 = 1 + (i * 2);
			int t2 = t1 + 1;
			discreteOffsets[i + 1] = (offsets[t1] * weights[t1] + offsets[t2] * weights[t2]) /
							discreteWeights[i + 1];
		}
		return new DiscreteSampleKernel(discreteOffsets, discreteWeights);
	}


	/**
	 * Pascal's triangle at row N (1 is first)
	 * @param n
	 * @return
	 */
	public static double[] pascal(int n) {
		double[] result = new double[n];
		int C = 1;
		for (int i = 1; i <= result.length; i++) {
			result[i - 1] = C;
			C = C * (n - i) / i;
		}
		return result;
	}
}
