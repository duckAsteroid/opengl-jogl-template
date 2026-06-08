package com.asteroid.duck.opengl.util.resources.buffer.vbo;

import com.asteroid.duck.opengl.util.resources.buffer.VertexArrayObject;
import com.asteroid.duck.opengl.util.resources.buffer.debug.VertexBufferVisualiser;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VertexBufferObjectTest {
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

	private VertexArrayObject vao = new VertexArrayObject();
	private VertexBufferObject subject;



	@Test
	public void testSimpleCase() {
		final int SIZE = 100;

		// float aValue, vec2f pos, vec2f texture, vec3f color
		VertexDataStructure structure = new VertexDataStructure(TEST_ELEMENTS);
		subject = vao.createVbo(structure, SIZE);

		// createBuffer() allocates native memory without requiring an active GL context
		subject.createBuffer();

		try {
			// add some data
			for (int i = 0; i < SIZE; i++) {
				Map<VertexElement, Object> vertData = structure.asMap(
								i * 1f,
								new Vector2f(i * 2f),
								new Vector2f(i * 3f),
								new Vector3f(i * 4f));
				subject.set(i, vertData);
			}

			// check the data
			for (int i = 0; i < SIZE; i++) {
				Map<VertexElement, ?> readVertData = subject.get(i);
				assertNotNull(readVertData);
				assertEquals(i * 1f, (float) readVertData.get(structure.get(A_VALUE)));
				Vector2f pos = (Vector2f) readVertData.get(structure.get(POS));
				assertEquals(new Vector2f(i * 2f), pos);
				Vector2f tex = (Vector2f) readVertData.get(structure.get(TEXTURE));
				assertEquals(new Vector2f(i * 3f), tex);
				Vector3f colorValue = (Vector3f) readVertData.get(structure.get(COLOR));
				assertEquals(new Vector3f(i * 4f), colorValue);
			}
		} finally {
			if (subject != null) {
				MemoryUtil.memFree(subject.memBuffer());
			}
		}
	}

	@Test
	public void myTextureRendererExample() {
		final Object[][] vertices = {
						// screen positions           // colors            // texture coords
						{ new Vector3f(0.5f,  0.5f, 0.0f),     new Vector3f(1.0f, 0.0f, 0.0f),    new Vector2f(1.0f, 1.0f) },  // top right
						{ new Vector3f(0.5f, -0.5f, 0.0f),     new Vector3f(0.0f, 1.0f, 0.0f),    new Vector2f(1.0f, 0.0f) },  // bottom right
						{ new Vector3f(-0.5f, -0.5f, 0.0f),    new Vector3f(0.0f, 0.0f, 1.0f),    new Vector2f(0.0f, 0.0f) },  // bottom left

						{ new Vector3f(0.5f,  0.5f, 0.0f),     new Vector3f(1.0f, 0.0f, 0.0f),    new Vector2f(1.0f, 1.0f) },  // top right
						{ new Vector3f(-0.5f, -0.5f, 0.0f),    new Vector3f(0.0f, 0.0f, 1.0f),    new Vector2f(0.0f, 0.0f) },  // bottom left
						{ new Vector3f(-0.5f,  0.5f, 0.0f),    new Vector3f(1.0f, 1.0f, 0.0f),    new Vector2f(0.0f, 1.0f) } // top left
		};

		final VertexDataStructure structure = new VertexDataStructure(
						new VertexElement(VertexElementType.VEC_3F, "aPos"),
						new VertexElement(VertexElementType.VEC_3F, "aColor"),
						new VertexElement(VertexElementType.VEC_2F, "aTexCoord")
		);

		VertexBufferObject vertexBufferObject = vao.createVbo(structure, 6);
		vertexBufferObject.createBuffer();
		for (int i = 0; i < vertices.length; i++) {
			vertexBufferObject.set(i, vertices[i]);
		}


		// old style

		float[] oldVertices = {
						// screen positions           // colors            // texture coords
						0.5f,  0.5f, 0.0f,     1.0f, 0.0f, 0.0f,    1.0f, 1.0f,  // top right
						0.5f, -0.5f, 0.0f,     0.0f, 1.0f, 0.0f,    1.0f, 0.0f,  // bottom right
						-0.5f, -0.5f, 0.0f,    0.0f, 0.0f, 1.0f,    0.0f, 0.0f,  // bottom left

						0.5f,  0.5f, 0.0f,     1.0f, 0.0f, 0.0f,    1.0f, 1.0f,  // top right
						-0.5f, -0.5f, 0.0f,    0.0f, 0.0f, 1.0f,    0.0f, 0.0f,  // bottom left
						-0.5f,  0.5f, 0.0f,    1.0f, 1.0f, 0.0f,    0.0f, 1.0f   // top left
		};
		ByteBuffer memBuffer = MemoryUtil.memAlloc(oldVertices.length * Float.BYTES);
		memBuffer.asFloatBuffer().put(oldVertices);
		//System.out.println("Original:\n"+asString(memBuffer));
		VertexBufferVisualiser visualizer = new VertexBufferVisualiser(vao);
		System.out.println(visualizer);

		MemoryUtil.memFree(memBuffer);
		MemoryUtil.memFree(vertexBufferObject.memBuffer());
	}

}
