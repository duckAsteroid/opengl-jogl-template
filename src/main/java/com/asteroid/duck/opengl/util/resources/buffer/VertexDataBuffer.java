package com.asteroid.duck.opengl.util.resources.buffer;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.Stateful;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
public class VertexDataBuffer extends AbstractList<Map<VertexElement, ?>> implements Stateful {
	private static final byte ZERO_BYTE = 0;
	/**
	 * This defines the order of the elements for each vertex.
	 */
	private final VertexDataStructure vertexDataStructure;
	private final int initialSize;
	private Boolean initialised = Boolean.FALSE;
	private boolean active = false;
	private ByteBuffer memBuffer = null;
	private int vao;
	private int vbo;

	public VertexDataBuffer(VertexDataStructure structure, int initialSize) {
		Objects.requireNonNull(structure);
		if (initialSize <= 0) {
			throw new IllegalArgumentException("Initial size must be greater than zero");
		}
		this.vertexDataStructure = structure;
		this.initialSize = initialSize;
	}


	@Override
	public void init(RenderContext ctx) {
		if (!initialised) {
			// set up the VAO and VBO
			vao = glGenVertexArrays();
			glBindVertexArray(vao);

			// Create a VBO and bind it
			vbo = glGenBuffers();
			glBindBuffer(GL_ARRAY_BUFFER, vbo);
			// create a buffer of the initial size
			this.memBuffer = MemoryUtil.memAlloc(initialSize * vertexDataStructure.size());
		}
		// do nothing - already initialised
	}

	@Override
	public boolean isInitialised() {
		return initialised;
	}

	public void setup(ShaderProgram shader) {
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
		ByteBuffer readCopy = memBuffer.duplicate();
		readCopy.position(index * vertexDataStructure.size());
		Map<VertexElement, Object> result = new HashMap<>();
		for(VertexElement element : vertexDataStructure) {
			Object object = element.type().deserializeRaw(readCopy);
			result.put(element, object);
		}
		return Collections.unmodifiableMap(result);
	}

	@Override
	public Map<VertexElement, ?> set(int index, Map<VertexElement, ?> vertexData) {
		ByteBuffer writeCopy = memBuffer.duplicate();
		writeCopy.position(index * vertexDataStructure.size());
		for(VertexElement element : vertexDataStructure) {
			Object object = vertexData.get(element);
			VertexElementType<?> type = element.type();
			type.serializeRaw(object, writeCopy);
		}
		return null;
	}

	public Map<VertexElement, ?> set(int index, Object ... data) {
		return set(index, createMap(data));
	}

	public Map<VertexElement, ?> createMap(Object ... data) {
		Objects.requireNonNull(data);
		if (data.length == 0) {
			return Collections.emptyMap();
		}
		Map<VertexElement, Object> result = new HashMap<>(data.length);
		for (int i = 0; i < data.length; i++) {
			VertexElement e = vertexDataStructure.getIndex(i);
			result.put(e, data[i]);
		}
		return result;
	}

	@Override
	public Map<VertexElement, ?> remove(int index) {
		Map<VertexElement, ?> current = get(index);
		ByteBuffer writeCopy = memBuffer.duplicate();
		writeCopy.position(index * vertexDataStructure.size());
		for (int i = 0; i < vertexDataStructure.size(); i++) {
			writeCopy.put(ZERO_BYTE);
		}
		return current;
	}

	@Override
	public int size() {
		return memBuffer.capacity() / vertexDataStructure.size();
	}

	@Override
	public void begin(RenderContext ctx) {
		if(!active) {
			active = true;
			// activate

		}
	}

	public void use() {
		glBindVertexArray(vao);
	}

	public void render(int start, int count) {
		use();
		glBufferData(GL_ARRAY_BUFFER, memBuffer, GL_DYNAMIC_DRAW);
		glDrawArrays(GL_TRIANGLES, start, count);
	}

	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public void end(RenderContext ctx) {
		active = false;
	}

	@Override
	public void destroy() {
		initialised = null;
		MemoryUtil.memFree(memBuffer);
		memBuffer = null;
	}


}
