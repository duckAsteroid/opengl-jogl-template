package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.geom.Corner;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TextExperimentTest {
	@ParameterizedTest
	@MethodSource("cornerDataProvider")
	void testCorner(Corner inputCorner, Vector4f inputExtent, Vector2f expectedResult) {
		Vector2f actualResult = inputCorner.from(inputExtent);

		// Using a delta for float comparisons for robustness, this handles potential floating-point rounding errors.
		assertEquals(expectedResult.x, actualResult.x, 0.0001f);
		assertEquals(expectedResult.y, actualResult.y, 0.0001f);
	}

	static Stream<Arguments> cornerDataProvider() {
		Vector4f extent = new Vector4f(1f, 2f, 3f, 4f); // Example bounds

		return Stream.of(
						Arguments.of(Corner.BOTTOM_LEFT, extent, new Vector2f(1f, 2f)),
						Arguments.of(Corner.TOP_LEFT, extent, new Vector2f(1f, 4f)),
						Arguments.of(Corner.TOP_RIGHT, extent, new Vector2f(3f, 4f)),
						Arguments.of(Corner.BOTTOM_RIGHT, extent, new Vector2f(3f, 2f))
		);
	}
}
