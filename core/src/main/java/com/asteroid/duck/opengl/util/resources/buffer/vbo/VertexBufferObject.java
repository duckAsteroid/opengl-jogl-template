package com.asteroid.duck.opengl.util.resources.buffer.vbo;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.Resource;
import com.asteroid.duck.opengl.util.resources.buffer.BufferDrawMode;
import com.asteroid.duck.opengl.util.resources.buffer.UpdateHint;
import com.asteroid.duck.opengl.util.resources.buffer.VertexArrayObject;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

/**
 * Provides a utility for working with data in a Vertex Buffer Object (VBO).
 * Helps with reading and writing data to a native (not JVM) CPU memory area.
 * The data structure is written to the VBO/VAO and can be mapped into a shader.
 * This data can then be flushed out to the GPU as required.
 *
 * <p>A vertex data buffer stores a structure of data for each vertex (point). This data structure
 * is described using {@link VertexDataStructure} which contains 1 or more {@link VertexElement}s.</p>
 *
 * <p>As an example suppose we want to store a {@link org.joml.Vector3f} colour and a {@link org.joml.Vector2f} point
 * (x/y coords) for each vertex in a simple triangle. We could initialise the {@link VertexBufferObject} as follows:
 * <code>
 * VertexDataStructure structure = new VertexDataStructure();
 * </code>
 * </p>
 */
public class VertexBufferObject extends AbstractList<Map<VertexElement, ?>> implements Resource {

	private static final Logger log = LoggerFactory.getLogger(VertexBufferObject.class);

	private final VertexArrayObject owner;

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
	 * A GL pointer for the vertex buffer object
	 */
	private int vbo;
	/**
	 * The update hint for the buffer.
	 * This defines how the data will be used and how it should be optimised.
	 * Defaults to {@link UpdateHint#STATIC}.
	 */
	private UpdateHint updateHint = UpdateHint.STATIC;
	/**
	 * The draw mode to use when rendering the vertex data.
	 * Defaults to {@link BufferDrawMode#TRIANGLES}.
	 */
	private BufferDrawMode drawMode = BufferDrawMode.TRIANGLES;
	/**
	 * Create a vertex data buffer to store the given data structure for each of; (up to) a given
	 * number of vertices.
	 * @param structure the data structure for each vertice
	 * @param capacity the maximum number of vertices that can be stored
	 */
	public VertexBufferObject(VertexArrayObject owner, VertexDataStructure structure, int capacity) {
		Objects.requireNonNull(owner);
		this.owner = owner;
		Objects.requireNonNull(structure);
		if (capacity <= 0) {
			throw new IllegalArgumentException("Initial size must be greater than zero");
		}
		this.vertexDataStructure = structure;
		this.capacity = capacity;
	}

	public VertexArrayObject getOwner() {
		return owner;
	}

	/**
	 * Get the vertex data structure that defines the data for each vertex.
	 * @return the vertex data structure
	 */
	public VertexDataStructure getStructure() {
		return vertexDataStructure;
	}

	public UpdateHint getUpdateHint() {
		return updateHint;
	}

	public void setUpdateHint(UpdateHint updateHint) {
		this.updateHint = updateHint;
	}

	public BufferDrawMode getDrawMode() {
		return drawMode;
	}

	public void setDrawMode(BufferDrawMode drawMode) {
		this.drawMode = drawMode;
	}

	/**
	 * An iterable view of the bytes in the memory buffer.
	 * @see #duplicate(ByteBuffer)
	 */
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

	/**
	 * A stream of the bytes in the memory buffer.
	 * @return a stream of bytes
	 */
	public Stream<Byte> byteStream() {
		return StreamSupport.stream(bytes().spliterator(), false);
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

	/**
	 * The number of vertices that can be stored in the buffer.
	 * The backing memory buffer divided by the size of the vertex data structure.
	 * @return the number of vertices
	 */
	@Override
	public int size() {
		return memBuffer.capacity() / vertexDataStructure.size();
	}

	/**
	 * Get the raw underlying memory that contains the vertex data as a ByteBuffer.
	 * @return the memory buffer
	 */
	public ByteBuffer memBuffer() {
		return memBuffer;
	}

	/**
	 * Initialise the vertex data buffer.
	 * This sets up the VAO and VBO, and creates the memory buffer.
	 * The memory buffer is allocated to the initial capacity of the buffer.
	 */
	public void init(RenderContext ctx) {
		owner.bind(ctx);
		// Create a VBO and bind it
		vbo = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		createBuffer();
		// initialise the buffer with the memory buffer
		glBufferData(GL_ARRAY_BUFFER, memBuffer, updateHint.openGlCode());
	}

	/**
	 * Setup the buffer to use with the given shader.
	 * Each element in the data structure is mapped to a vertex attribute
	 * pointer using the name of the element as the vertex attribute name.
	 * <p>This calls {@link #setVertexAttribPointer(int, int, int, boolean, int, long)}</p>
	 * @param shader the shader to initialise
	 */
	public void setup(ShaderProgram shader) {
		// FIXME check the shader is compatible with the vertex data structure
		for(VertexElement element : vertexDataStructure) {
			int position = shader.getAttributeLocation(element.name());
			log.debug("Setting up vertex attribute '{}' at position {} in {}", element.name(), position, shader.shortDebugName());
			setVertexAttribPointer(
							position,
							element.type().dimensions(),
							element.type().glType(),
							false,
							vertexDataStructure.size(),
							vertexDataStructure.getOffset(element));
		}
	}

	/**
	 * Does the pair of GL calls required to set up and enable a vertex attribute.
	 * Calls {@link org.lwjgl.opengl.GL20#glVertexAttribPointer(int, int, int, boolean, int, long)}
	 * and {@link org.lwjgl.opengl.GL20#glEnableVertexAttribArray(int)}
	 */
	protected void setVertexAttribPointer(int position, int size, int type, boolean normalized, int stride, long pointer) {
		glVertexAttribPointer(position, size, type, normalized, stride, pointer);
		glEnableVertexAttribArray(position);
	}

	public void createBuffer() {
		// create a memory buffer of the initial size
		this.memBuffer = MemoryUtil.memAlloc(capacity * vertexDataStructure.size());
	}


	/**
	 * Get the vertex data for the given index.
	 * @return a map of the vertex data elements and their values
	 */
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

	/**
	 * Get the value of a specific vertex data element for the given index (vertex).
	 * @param index the index of the vertex
	 * @param element the vertex data element to get
	 * @return the value of the element
	 */
	public Object getElement(int index, VertexElement element) {
		ByteBuffer readCopy = duplicate(memBuffer);
		long elementOffset = vertexDataStructure.getOffset(element);
		readCopy.position((int) ((index * vertexDataStructure.size()) + elementOffset));
		return element.type().deserializeRaw(readCopy);
	}

	/**
	 * Set the entire vertex data for the given index.
	 * This will overwrite all the existing data for the vertex.
	 * @param index the index of the vertex to set
	 * @param vertexData a map of the vertex data elements and their values
	 * @return the supplied vertex data map
	 */
	@Override
	public Map<VertexElement, ?> set(int index, Map<VertexElement, ?> vertexData) {
		vertexDataStructure.checkStructure(vertexData);
		ByteBuffer writeCopy = duplicate(memBuffer);
		writeCopy.position(index * vertexDataStructure.size());
		for(VertexElement element : vertexDataStructure) {
			Object object = vertexData.get(element);
			VertexElementType<?> type = element.type();
			type.serializeRaw(object, writeCopy);
		}
		return vertexData;
	}

	/**
	 * Set the vertex data for the given index.
	 * This will overwrite all the existing data for the vertex.
	 * The data is provided as a variable number of arguments, which are mapped to the vertex data
	 * structure by their respective structure index.
	 * @param index the index of the vertex to set
	 * @param data the data to set for the vertex
	 * @return a map of the vertex data elements and their values
	 */
	public Map<VertexElement, ?> set(int index, Object ... data) {
		return set(index, vertexDataStructure.asMap(data));
	}

	/**
	 * Set a specific vertex data element for the given index (vertex).
	 * This will ONLY overwrite the data for that element.
	 * @param index the index of the vertex to set
	 * @param element the vertex data element to set
	 * @param data the value to set for the element
	 */
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

	public void update(UpdateHint hint) {
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		if (hint == null) {
			hint = UpdateHint.STATIC;
		}
		glBufferData(GL_ARRAY_BUFFER, memBuffer, hint.openGlCode());
	}


	@Override
	public void dispose() {
		glDeleteBuffers(vbo);
		MemoryUtil.memFree(memBuffer);
		memBuffer = null;
	}
}
