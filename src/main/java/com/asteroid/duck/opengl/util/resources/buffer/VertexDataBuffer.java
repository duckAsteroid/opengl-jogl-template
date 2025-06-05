package com.asteroid.duck.opengl.util.resources.buffer;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.Resource;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

/**
 * Provides a utility for working with data in a vertex buffer object.
 * Helps with reading and writing data to a native (not JVM) CPU memory area.
 * The data structure is written to the VBO/VAO and can be mapped into a shader.
 * This data can then be flushed out to the GPU as required.
 */
public class VertexDataBuffer extends AbstractList<Map<VertexElement, ?>> implements Resource {
	public enum UpdateHint {
		/** The data store contents will be modified once and used many times. */
		STATIC(GL_STATIC_DRAW),
		/** The data store contents will be modified once and used at most a few times. */
		STREAM(GL_STREAM_DRAW),
		/** The data store contents will be modified repeatedly and used many times */
		DYNAMIC(GL_DYNAMIC_DRAW);

		private final int glCode;

		UpdateHint(int glCode) {
			this.glCode = glCode;
		}

		public int getGlCode() {
			return glCode;
		}
	}

	private static final byte ZERO_BYTE = 0;
	/**
	 * This defines the order of the elements for each vertex.
	 */
	private final VertexDataStructure vertexDataStructure;
	/**
	 * The maximum number of vertices that can be stored in the buffer.
	 * Each vertice will have a set of data elements associated as described by the structure.
	 */
	private final int capacity;
	/**
	 * A memory buffer that contains the raw vertex data
	 */
	private ByteBuffer memBuffer = null;
	/**
	 * A GL pointer for the vertex array object
	 */
	private int vao;
	/**
	 * A GL pointer for the vertex buffer object
	 */
	private int vbo;

	/**
	 * Create a vertex data buffer to store the given data structure for each of; (up to) a given
	 * number of vertices.
	 * @param structure the data structure for each vertice
	 * @param capacity the maximum number of vertices that can be stored
	 */
	public VertexDataBuffer(VertexDataStructure structure, int capacity) {
		Objects.requireNonNull(structure);
		if (capacity <= 0) {
			throw new IllegalArgumentException("Initial size must be greater than zero");
		}
		this.vertexDataStructure = structure;
		this.capacity = capacity;
	}

	ByteBuffer memBuffer() {
		return memBuffer;
	}

	public Iterable<Byte> bytes() {
		return () -> new Iterator<>() {
			private final ByteBuffer copy = duplicate(memBuffer);

			@Override
			public boolean hasNext() {
				return memBuffer.hasRemaining();
			}

			@Override
			public Byte next() {
				return memBuffer.get();
			}
		};
	}

	public Stream<Byte> byteStream() {
		return StreamSupport.stream(bytes().spliterator(), false);
	}

	public String byteString() {
		return byteStream()
						.map((b) -> Integer.toHexString(b & 0xFF).toUpperCase())
						.map((s) -> s.length() == 1 ? "0" + s : s)
						.collect(Collectors.joining(","));
	}

	public String dataString() {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < size(); i++) {
			Map<VertexElement, ?> data = get(i);
			for(VertexElement ve : vertexDataStructure) {
				Object value = data.get(ve);
				String text = "^" + ve.type().dataStringRaw(value);
				text = truncateAndPad(text, ve.type().byteSize() * 3);
				text = text.substring(0, text.length() - 2) + "^";
				result.append(text).append(' ');
			}
		}
		return result.toString();
	}

	private static String truncateAndPad(String text, int maxLength) {
		if (text.length() > maxLength) {
			return text.substring(0, maxLength);
		} else {
			return String.format("%-" + maxLength + "s", text).replace(' ', '-');
		}
	}

	public void init(RenderContext ctx) {
		// set up the VAO and VBO
		vao = glGenVertexArrays();
		glBindVertexArray(vao);

		// Create a VBO and bind it
		vbo = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		createBuffer();
		// FIXME optimise the data hint...
		glBufferData(GL_ARRAY_BUFFER, memBuffer, GL_STREAM_DRAW);
	}

	public void createBuffer() {
		// create a memory buffer of the initial size
		this.memBuffer = MemoryUtil.memAlloc(capacity * vertexDataStructure.size());
	}

	/**
	 * Setup the buffer to use with the given shader.
	 * Each element in the data structure is mapped to a vertex attribute pointer
	 * @param shader the shader to initialise
	 */
	public void setup(ShaderProgram shader) {
		// FIXME check the shader is compatible with the vertex data structure
		// FIXME check the shader is ready to be setup
		use();
		for(VertexElement element : vertexDataStructure) {
			shader.setVertexAttribPointer(
							element.name(),
							element.type().dimensions(),
							element.type().glType(),
							false,
							vertexDataStructure.size(),
							vertexDataStructure.getOffset(element));
		}
	}

	@Override
	public Map<VertexElement, ?> get(int index) {
		ByteBuffer readCopy = duplicate(memBuffer);
		readCopy.position(index * vertexDataStructure.size());
		Map<VertexElement, Object> result = new HashMap<>();
		for(VertexElement element : vertexDataStructure) {
			Object object = element.type().deserializeRaw(readCopy);
			result.put(element, object);
		}
		return Collections.unmodifiableMap(result);
	}

	public Object getElement(int index, VertexElement element) {
		ByteBuffer readCopy = duplicate(memBuffer);
		long elementOffset = vertexDataStructure.getOffset(element);
		readCopy.position((int) ((index * vertexDataStructure.size()) + elementOffset));
		return element.type().deserializeRaw(readCopy);
	}

	@Override
	public Map<VertexElement, ?> set(int index, Map<VertexElement, ?> vertexData) {
		ByteBuffer writeCopy = duplicate(memBuffer);
		writeCopy.position(index * vertexDataStructure.size());
		for(VertexElement element : vertexDataStructure) {
			Object object = vertexData.get(element);
			VertexElementType<?> type = element.type();
			type.serializeRaw(object, writeCopy);
		}
		return null;
	}

	public Map<VertexElement, ?> set(int index, Object ... data) {
		return set(index, vertexDataStructure.asMap(data));
	}

	public void setElement(int index, VertexElement element, Object data) {
		ByteBuffer writeCopy = duplicate(memBuffer);
		long elementOffset = vertexDataStructure.getOffset(element);
		writeCopy.position((int) ((index * vertexDataStructure.size()) + elementOffset));
		VertexElementType<?> type = element.type();
		type.serializeRaw(data, writeCopy);
	}

	@Override
	public Map<VertexElement, ?> remove(int index) {
		Map<VertexElement, ?> current = get(index);
		ByteBuffer writeCopy = duplicate(memBuffer);
		writeCopy.position(index * vertexDataStructure.size());
		for (int i = 0; i < vertexDataStructure.size(); i++) {
			writeCopy.put(ZERO_BYTE);
		}
		return current;
	}

	/**
	 * Like {@link ByteBuffer#duplicate()} but keeps the byte order!!
	 * @param original the byte buffer to copy
	 * @return the copy (same content, new pointers)
	 */
	private static ByteBuffer duplicate(ByteBuffer original) {
		ByteBuffer copy = original.duplicate();
		copy.order(original.order());
		return copy;
	}

	@Override
	public int size() {
		return memBuffer.capacity() / vertexDataStructure.size();
	}

	public void use() {
		glBindVertexArray(vao);
	}

	public void update(UpdateHint hint) {
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		if (hint == null) {
			hint = UpdateHint.STATIC;
		}
		glBufferData(GL_ARRAY_BUFFER, memBuffer, hint.glCode);
	}

	public void render(int start, int count) {
		glDrawArrays(GL_TRIANGLES, start, count);
	}

	@Override
	public void destroy() {
		MemoryUtil.memFree(memBuffer);
		memBuffer = null;
	}


	public VertexDataStructure getStructure() {
		return vertexDataStructure;
	}

	public String headerString() {
		return IntStream.range(0, size())
						.mapToObj((i) -> vertexDataStructure.headerString())
						.collect(Collectors.joining(" "));
	}
}
