package com.asteriod.duck.opengl.util.blur;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * This class creates a convolution kernel for doing gaussian blurs.
 * The concept and the maths for this class comes from the article
 * <a href="https://www.rastergrid.com/blog/2010/09/efficient-gaussian-blur-with-linear-sampling/">
 *   "Efficient Gaussian Blur with Linear Sampling" by RasterGrid.</a>
 *
 * Rather than being a strict gaussian around a given point it just does a single row
 *
 * <p>It's essentially a way to implement Gaussian blur in a two pass filter (X/Y) and using as few
 * texture lookups as possible. By using texel interpolation from GL to do some of the maths for
 * you in hardware.</p>
 *
 * <p>The maths is a bit complex, but the idea is that we can use a pascal triangle to get the
 * coefficients for the gaussian kernel, and then we can eliminate the reflection and take half of
 * the coefficients. We then do the maths to ensure that the sample points (linear interpolation)
 * do the right amount of texel blending for the given weights and distance required.</p>
 */
public class BlurKernel {
	public final double[] offsets;
	public final double[] weights;
	public final int size;

	/**
	 * Create a blur kernel of a certain size.
	 * @param size the size of the kernel
	 * @throws IllegalArgumentException if the size < 1 or not odd
	 */
	public BlurKernel(int size) {
		if (size <= 1) throw new IllegalArgumentException("Kernel size must be > 1");
		if (size % 2 == 0) throw new IllegalArgumentException("Kernel size must be odd");

		this.size  = size;
		// get pascals triangle for
		double[] pascal = pascal(size);
		// sum all coefficients
		double sum = Arrays.stream(pascal).sum();
		// normalise coefficients (0-1) (against sum)
		double[] normalisedCoefficients = new double[pascal.length];
		for (int i = 0; i < normalisedCoefficients.length; i++) {
			normalisedCoefficients[i] = pascal[i] / sum;
		}
		// ok now we have normalised coefficients
		// lets eliminate the reflection and take half
		int center = (normalisedCoefficients.length / 2); // rounds down 3/2 = 1, 5/2 = 2 etc.
		this.weights = Arrays.copyOfRange(normalisedCoefficients, center, normalisedCoefficients.length);
		this.offsets = DoubleStream.iterate(0, o -> o + 1).limit(weights.length).toArray();
	}

	public DiscreteSampleKernel getDiscreteSampleKernel() {
		double[] discreteWeights = new double[(weights.length / 2) + 1] ;
		discreteWeights[0] = weights[0];

		for (int i = 0; i < discreteWeights.length - 1; i++) {
			// 1, 3, 5 ...
			int t1 = 1 + (i * 2);
			// 2, 4, 6 ...
			int t2 = t1 + 1;
			discreteWeights[i + 1] = weights[t1] + weights[t2];
		}
		double[] discreteOffsets = new double[discreteWeights.length];
		discreteOffsets[0] = 0.0;
		for (int i = 0; i < discreteOffsets.length - 1; i++) {
			int t1 = 1 + (i * 2);
			int t2 = t1 + 1;
			discreteOffsets[i + 1] = ((offsets[t1] * weights[t1]) + (offsets[t2] * weights[t2])) /
							discreteWeights[i + 1];
		}
		return new DiscreteSampleKernel(discreteOffsets, discreteWeights);
	}


	/**
	 * Pascal's triangle at row N (1 is first).
	 * <pre>
	 * Row                Values
	 * 1                     1
	 * 2                   1   1
	 * 3                 1   2   1
	 * 4               1   3   3   1
	 * 5             1   4   6   4   1
	 * etc.
	 * </pre>
	 * @param n the row of the triangle to return
	 * @return a set of values for pascals triangle at row N. length = N
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

	private static IntStream size(String[] args) {
		if (args.length == 2) {
			return IntStream.rangeClosed(parseInt(args[0]).orElse(13), parseInt(args[1]).orElse(13));
		}
		else if (args.length == 0) {
			return IntStream.of(13);
		}
		else {
			return Arrays.stream(args)
							.map(BlurKernel::parseInt)
							.filter(Optional::isPresent)
							.mapToInt(Optional::get);

		}
	}

	private static Optional<Integer> parseInt(String s) {
		try {
			return Optional.of(Integer.parseInt(s));
		} catch (NumberFormatException e) {
			System.err.println("Invalid argument. Expected an integer, got: " + s);
		}
		return Optional.empty();
	}

	public static void main(String[] args) {
		final IntStream stream = IntStream.rangeClosed(25,33).filter(i -> i % 2 != 0);
		stream.forEach(size -> {
			try {
				BlurKernel kernel = new BlurKernel(size);
				DiscreteSampleKernel dsk = kernel.getDiscreteSampleKernel();
				System.out.println("// Kernel size " + size);
				double sum = (Arrays.stream(kernel.weights).skip(1).sum() * 2.0) + kernel.weights[0];
				System.out.println("// sum of weights: "+ sum);
				System.out.println(renderAsUniform("offsets_" + size, dsk.offsets()));
				System.out.println(renderAsUniform("weights_" + size, dsk.weights()));
			}
			catch(Throwable t) {
				System.out.println("// Error at size "+size+": "+t.getMessage());
			}
		});
	}

	private static String renderAsUniform(String variableName, double[] values) {
		return Arrays.stream(values)
						.mapToObj(d -> String.format("%.14f", d))
						.collect(Collectors.joining(", ",
										"uniform float "+variableName+"["+values.length+"] = float[](",");"));
	}
}
