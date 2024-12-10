package com.asteroid.duck.opengl.util.resources.buffer;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VertexDataBufferTest {
	public static final String A_VALUE = "aValue";
	public static final String POS = "pos";
	public static final String TEXTURE = "texture";
	public static final String COLOR = "color";

	public static final VertexElement[] TEST_ELEMENTS = new VertexElement[] {
					new VertexElement(VertexElementType.FLOAT, A_VALUE),
					new VertexElement(VertexElementType.VEC_2F, POS),
					new VertexElement(VertexElementType.VEC_2F, TEXTURE),
					new VertexElement(VertexElementType.VEC_3F, COLOR)
	};

	private VertexDataBuffer subject;

	@Test
	public void testSimpleCase() {
		// float aValue, vec2f pos, vec2f texture, vec3f color
		VertexDataStructure structure = new VertexDataStructure(TEST_ELEMENTS);
		subject = new VertexDataBuffer(structure, 100);

		// initialise
		subject.init(null);

		// add some data
		for (int i = 0; i < 2; i++) {
			Map<VertexElement, Object> vertData = structure.asMap(
							i * 1f,
							new Vector2f(i * 2f),
							new Vector2f(i * 3f),
							new Vector3f(i * 4f));
			subject.set(i, vertData);
		}

		// check the data
		for (int i = 0; i < 2; i++) {
			Map<VertexElement, ?> readVertData = subject.get(i);
			assertNotNull(readVertData);
			assertEquals(i * 1f, (float) readVertData.get(structure.get(A_VALUE)));
			Vector3f colorValue = (Vector3f) readVertData.get(structure.get(COLOR));
			assertEquals(new Vector3f(i * 4f), colorValue);
		}
	}

}
