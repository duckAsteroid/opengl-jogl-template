package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.timer.Timer;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Represents a set of triangular vertices and the vao, vbo and ibo that can be used to render them.
 */
public class Triangles {
	private final float[] vertices;
	private final short[] indices;

	private final int vao;
	private final int vbo;
	private final int ibo;
	private final int triangleCount;

	public static Triangles fullscreen() {
		// Define the vertices of the rectangle
		// TL     1      TR
		//
		// -1     0      1
		//
		// BL    -1      BR
		float[] vertices = {
						-1.0f, -1.0f, // bottom left [0]
						1.0f, -1.0f, // bottom right [1]
						1.0f, 1.0f, // top right [2]
						-1.0f, 1.0f // top left [3]
		};

		// TL     1      TR
		//            /  |
		// -1     0      1
		//    /          |
		// BL  -  -1  -  BR
		// BL, BR, TR

		// TL  -  1  -   TR
		// |          /
		// -1     0      1
		// |   /
		// BL     -1     BR
		// BL, TR, TL
		short[] indices = new short[]{
						0, 1, 2, // triangle 1
						0, 2, 3}; // triangle 2
		return new Triangles(vertices, indices);
	}

	public static Triangles singleTriangle() {
		// Define the vertices of the rectangle
		// Y
		// ^
		// TL     1      TR
		//
		// -1     0      1
		//
		// BL    -1      BR   -> X
		float[] vertices = {
						-1f, -1f, // bottom left [0]
						1f, -1f, // bottom right [1]
						0.0f, 1f // top middle [2]
		};


		short[] indices = new short[]{
						0, 1, 2}; // triangle 1
		return new Triangles(vertices, indices);
	}

	public static Triangles centralTriangle() {
		// Define the vertices of the rectangle
		// Y
		// ^
		// TL     1      TR
		//
		// -1     0      1
		//
		// BL    -1      BR   -> X
		float[] vertices = {
						-.5f, -.5f, // bottom left [0]
						.5f, -.5f, // bottom right [1]
						0.0f, .5f // top middle [2]
		};


		short[] indices = new short[]{
						0, 1, 2}; // triangle 1
		return new Triangles(vertices, indices);
	}

	public RenderedItem simpleRenderer(Vector4f color, Vector3f freq) {
		return new RenderedItem() {
			private ShaderProgram shaderProgram;
			@Override
			public void init(RenderContext ctx) throws IOException {
				shaderProgram = ctx.getResourceManager().GetShader("simple", "simple/vertex.glsl", "simple/frag.glsl", null);
				shaderProgram.use();
				Triangles.this.setup(shaderProgram);
			}

			@Override
			public void doRender(RenderContext ctx) {
				shaderProgram.use();
				Vector4f tempColor = color;
				if (freq != null) {
					Timer timer = ctx.getTimer();
					tempColor = new Vector4f();
					tempColor.x = (float)timer.waveFunction(freq.x, color.x);
					tempColor.y = (float)timer.waveFunction(freq.y, color.y);
					tempColor.z = (float)timer.waveFunction(freq.z, color.z);
				}
				shaderProgram.setVector4f("color", tempColor);
				Triangles.this.render();
				shaderProgram.unuse();
			}

			@Override
			public void dispose() {
				Triangles.this.dispose();
			}
		};
	}

	public Triangles(float[] vertices, short[] indices) {
		this.vertices = vertices;
		this.indices = indices;
		this.triangleCount = indices.length / 3;
		try(MemoryStack stack = MemoryStack.stackPush()) {
			vao = glGenVertexArrays();
			glBindVertexArray(vao);

			// Create a VBO and bind it
			vbo = glGenBuffers();
			glBindBuffer(GL_ARRAY_BUFFER, vbo);

			// Store the vertex data in the VBO
			FloatBuffer vertexBuffer = stack.floats(vertices);
			glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

			// Create an IBO and bind it
			ibo = glGenBuffers();
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);

			// Store the index data in the IBO - create two triangles
			ShortBuffer indexBuffer = stack.shorts(indices);
			glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
		}
	}

	public void setup(ShaderProgram shaderProgram) {
		// setup the vertex attribute pointer to tell GL what shape our vertices are (2 floats)
		shaderProgram.setVertexAttribPointer("position", 2, GL_FLOAT, true, 0, 0);
	}

	public int triangleCount() {
		return triangleCount;
	}

	public int vertices() {
		return triangleCount * 3;
	}

	public void render() {
		glBindVertexArray(vao);
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
		glDrawElements(GL_TRIANGLES, vertices(), GL_UNSIGNED_SHORT, 0);
	}

	public void dispose() {
		glDeleteVertexArrays(vao);
		glDeleteBuffers(vbo);
		glDeleteBuffers(ibo);
	}

	@Override
	public String toString() {
		return "Triangles{" +
						"vao=" + vao +
						", vbo=" + vbo +
						", ibo=" + ibo +
						'}';
	}
}
