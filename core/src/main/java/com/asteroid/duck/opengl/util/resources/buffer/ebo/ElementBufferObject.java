package com.asteroid.duck.opengl.util.resources.buffer.ebo;

import com.asteroid.duck.opengl.util.resources.AbstractBoundResource;
import com.asteroid.duck.opengl.util.resources.buffer.VertexArrayObject;
import com.asteroid.duck.opengl.util.resources.buffer.BufferDrawMode;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexBufferObject;
import org.lwjgl.BufferUtils;

import java.nio.ShortBuffer;
import java.util.Objects;
import java.util.stream.IntStream;

import static org.lwjgl.opengl.GL15.*;

/**
 * Represents a buffer of element indexes that can be used to refer to indices in an
 * {@link VertexBufferObject}
 */
public class ElementBufferObject extends AbstractBoundResource {
	private final VertexArrayObject owner;
	/**
	 * The in memory copy of the index buffer
	 */
	private ShortBuffer indexBuffer;
	/**
	 * The GL pointer to the index buffer
	 */
	private Integer ebo = null;
	/**
	 * The maximum number of indices in the buffer
	 */
	private final int capacity;

	private BufferDrawMode drawMode = BufferDrawMode.TRIANGLES;

	public static final int GL_TYPE = GL_UNSIGNED_INT;

	public ElementBufferObject(VertexArrayObject owner, int capacity) {
		Objects.requireNonNull(owner, "Vertex array object must not be null");
		this.owner = owner;
		if (capacity <= 0) {
			throw new IllegalArgumentException("Capacity must be greater than zero.");
		}
		this.capacity = capacity;
	}

	public int capacity() {
		return capacity;
	}


	public void init() {
		indexBuffer = BufferUtils.createShortBuffer(capacity);
		ebo = glGenBuffers();
		bind();
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
	}

	protected void bindImpl() throws BindingException {
		if (ebo == null) throw new BindingException("Not initialised");
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
	}

	protected void unbindImpl() throws BindingException {
		if (ebo == null) throw new BindingException("Not initialised");
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	public void update(short[] indices) {
		indexBuffer.clear();
		indexBuffer.put(indices);
		indexBuffer.flip();
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
	}

	public void update() {
		indexBuffer.flip();
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
	}

	public void update(Iterable<Short> indices) {
		indexBuffer.clear();
		for (Short index : indices) {
			indexBuffer.put(index);
		}
		indexBuffer.flip();
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
	}

	public void clear() {
		indexBuffer.clear();
	}

	public void put(int index, short[] indices) {
		indexBuffer.position(index);
		indexBuffer.put(indices);
	}

	public void put(short[] indices) {
		indexBuffer.put(indices);
	}

	public void put(short i) {
		indexBuffer.put(i);
	}

	public int size() {
		return indexBuffer.capacity();
	}

	public short get(int index) {
		return indexBuffer.get(index);
	}

	public IntStream intStream() {
		return IntStream.range(0, capacity).map(indexBuffer::get);
	}

	@Override
	public void dispose() {
		if (ebo != null) {
			glDeleteBuffers(ebo);
		}
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

	public IntStream stream() {
		if (indexBuffer == null) {
			return IntStream.empty();
		}
		final var buffer = indexBuffer.asReadOnlyBuffer().flip();
		return IntStream.generate(() -> {
			if (buffer.hasRemaining()) {
				return buffer.get();
			} else {
				throw new IndexOutOfBoundsException("Index buffer has no more elements.");
			}
		}).limit(buffer.remaining());
	}

	/**
	 * What GL data type is used for the elements in this buffer (i.e. the indices)
	 * @return the GL Data type
	 */
	public int getType() {
		return GL_UNSIGNED_SHORT;
	}
}
