package com.asteroid.duck.opengl.util.resources.buffer;

import org.junit.jupiter.api.Test;

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
		for (int i = 0; i < 50; i++) {

		}
	}

}
