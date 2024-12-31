package com.asteroid.duck.opengl.util.resources.buffer;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class VertexElementTypeTest {

	static Stream<Arguments> elementTypeAndValueProvider() {
		return Stream.of(
						Arguments.of(VertexElementType.FLOAT, 1.23f),  // Test Float
						Arguments.of(VertexElementType.VEC_2F, new Vector2f(0.45f, 0.98f)), // Test Vec2f
						Arguments.of(VertexElementType.VEC_3F, new Vector3f(0.19f, 0.76f, 0.33f)), // Test Vec3f
						Arguments.of(VertexElementType.VEC_4F, new Vector4f(0.19f, 0.76f, 3.3f, 14.11f)) // Test Vec4f
		);
	}

	@ParameterizedTest
	@MethodSource("elementTypeAndValueProvider")
	public <T> void testRoundtrip(VertexElementType<T> subject, T testValue) {
		ByteBuffer buffer = ByteBuffer.allocate(subject.byteSize());
		assertEquals(subject.byteSize(), buffer.remaining());
		assertEquals(0, buffer.position());

		subject.serialize(testValue, buffer);
		assertEquals(0, buffer.remaining());
		assertEquals(subject.byteSize(), buffer.position());

		buffer.rewind();
		assertEquals(subject.byteSize(), buffer.remaining());
		assertEquals(0, buffer.position());

		T result = subject.deserialize(buffer);
		assertEquals(testValue, result);
		assertEquals(0, buffer.remaining());
		assertEquals(subject.byteSize(), buffer.position());
	}
}
