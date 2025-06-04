package com.asteroid.duck.opengl.util.resources.buffer;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.Resource;
import org.lwjgl.BufferUtils;

import java.nio.IntBuffer;
import java.util.Collection;
import java.util.Iterator;

import static org.lwjgl.opengl.GL15.*;

/**
 * Represents a buffer of indexes that can be used to refer to indices in an {@link VertexDataBuffer}
 */
public class IndexBuffer implements Resource {
	/**
	 * The in memory copy of the index buffer
	 */
	private IntBuffer indexBuffer;
	/**
	 * The GL pointer to the index buffer
	 */
	private int ibo;
	/**
	 * The maximum number of indices in the buffer
	 */
	private final int capacity;

	public static final int GL_TYPE = GL_UNSIGNED_INT;

	public IndexBuffer(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("Capacity must be greater than zero.");
		}
		this.capacity = capacity;
	}

	public int capacity() {
		return capacity;
	}

	public void init(RenderContext ctx) {
		indexBuffer = BufferUtils.createIntBuffer(capacity);
		ibo = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
	}

	public void use() {
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
	}

	public void update(int[] indices) {
		indexBuffer.clear();
		indexBuffer.put(indices);
		indexBuffer.flip();
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
	}

	public void update(Iterable<Integer> indices) {
		indexBuffer.clear();
		for (Integer index : indices) {
			indexBuffer.put(index);
		}
		indexBuffer.flip();
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
	}

	@Override
	public void destroy() {
		indexBuffer = null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("IndexBuffer{");
		sb.append("capacity=").append(capacity);
		sb.append(", indexBuffer=[");
		if (indexBuffer != null) {
			var tmp = indexBuffer.duplicate();
			while (tmp.hasRemaining()) {
				sb.append(tmp.get());
				if (tmp.hasRemaining()) {
					sb.append(", ");
				}
			}
		}
		else sb.append("null");
		sb.append("]}");
		return sb.toString();
	}
}
