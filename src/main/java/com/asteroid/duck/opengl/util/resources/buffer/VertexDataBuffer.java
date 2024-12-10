package com.asteroid.duck.opengl.util.resources.buffer;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.Stateful;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides a utility for working with data in a vertex buffer object.
 * Helps with reading and writing data to a native (not JVM) CPU memory area.
 * The data structure is written to the VBO/VAO and can be mapped into a shader.
 * This data can then be flushed out to the GPU as required.
 */
public class VertexDataBuffer extends AbstractList<Map<VertexElement, ?>> implements Stateful {
	/**
	 * This defines the order of the elements for each vertex.
	 */
	private final VertexDataStructure vertexDataStructure;
	private final int initialSize;
	private Boolean initialised = Boolean.FALSE;
	private boolean active = false;
	private ByteBuffer memBuffer = null;

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
			// create a buffer of the initial size
			this.memBuffer = MemoryUtil.memAlloc(initialSize * vertexDataStructure.size());
		}
		// do nothing - already initialised
	}

	@Override
	public boolean isInitialised() {
		return initialised;
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

	public ByteBuffer byteBuffer() {
		return memBuffer.duplicate();
	}
}
