package com.asteriod.duck.opengl.util;

import com.asteriod.duck.opengl.util.resources.shader.ShaderProgram;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL30.*;

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
						-1.0f, -1.0f, // bottom left
						1.0f, -1.0f, // bottom right
						1.0f, 1.0f, // top right
						-1.0f, 1.0f // top left
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
		short[] indices = new short[]{0, 1, 2, 0, 2, 3};
		return new Triangles(vertices, indices);
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
		// setup the vertex attribute pointer to tell GL what shape our vertices are
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
