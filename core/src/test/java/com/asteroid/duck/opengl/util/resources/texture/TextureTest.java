package com.asteroid.duck.opengl.util.resources.texture;

import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL11.*;

class TextureTest {
	private static final Logger log = LoggerFactory.getLogger(TextureTest.class);

	private static final int Width = 256; // Example width
	private static final int Height = 128; // Example height
	private static final float EPSILON = 1e-6f; // Tolerance for float comparisons

	private Texture subject;

	@Test
	void normalisationMatrix() {
		try (MockedStatic<GL11> mockedStatic = Mockito.mockStatic(GL11.class)) {
			final int ID = 1;
			mockedStatic.when(GL11::glGenTextures).thenReturn(ID);
			mockedStatic.when(() -> GL11.glBindTexture(GL11.GL_TEXTURE_2D, ID))
							.thenAnswer(this::logMethod);
			mockedStatic.when(() ->
											GL11.glTexImage2D(
															GL11.GL_TEXTURE_2D,
															0,
															GL11.GL_RGB,
															Width, Height,
															0,
															GL11.GL_RGB,
															GL_UNSIGNED_BYTE, 0))
							.thenAnswer(this::logMethod);
			mockedStatic.when(() -> GL11.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT))
							.thenAnswer(this::logMethod);

			mockedStatic.when(() -> GL11.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT))
							.thenAnswer(this::logMethod);

			mockedStatic.when(() -> GL11.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR))
							.thenAnswer(this::logMethod);

			mockedStatic.when(() -> GL11.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR))
							.thenAnswer(this::logMethod);


			// ACTUAL TEST STARTS HERE ------------------------------

			subject = new Texture();
			subject.generate(Width, Height, 0);

			Matrix3x2f normalisationMatrix = subject.getNormalisationMatrix();
			assertNotNull(normalisationMatrix);

			Scenario[] scenarios = {
							new Scenario("Top left",  new Vector2f(0,0), new Vector2f(0, 1.0f)),
							new Scenario("Top right",  new Vector2f(Width,0), new Vector2f(1.0f, 1.0f)),
							new Scenario("Bottom left",  new Vector2f(0,Height), new Vector2f(0, 0f)),
							new Scenario("Bottom right",  new Vector2f(Width,Height), new Vector2f(1.00f, 0f)),
			};

			// test some coordinates...
			for(Scenario s : scenarios) {
				Vector2f actual = new Vector2f();
				normalisationMatrix.transformPosition(s.source, actual);
				assertEquals(s.expected, actual, s.description);
			}

		}
	}

	public record Scenario(String description, Vector2f source, Vector2f expected) {

	}

	private Object logMethod(InvocationOnMock invocationOnMock) {
		log.info("{}({})", invocationOnMock.getMethod().getName(), Arrays.stream(invocationOnMock.getArguments()).map(Object::toString).collect(Collectors.joining(", ")));
		return null;
	}
}
