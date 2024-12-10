package com.asteroid.duck.opengl.util.resources.buffer;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.resources.Stateful;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class VertexDataBuffer extends AbstractList<Map<VertexElement, ?>> implements Stateful {
	/**
	 * This defines the order of the elements for each vertex.
	 */
	private final VertexDataStructure vertexDataStructure;
	private final AtomicBoolean initialised = new AtomicBoolean(false);
	private final AtomicBoolean active = new AtomicBoolean(false);
	private final int initialSize;
	private ByteBuffer memBuffer;

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
		if (initialised.compareAndSet(false, true)) {
			// setup the VAO and VBO
			// create a buffer of the initial size
			this.memBuffer = MemoryUtil.memAlloc(initialSize * vertexDataStructure.size());
		}
		// do nothing - already initialised
	}

	@Override
	public boolean isInitialised() {
		return initialised.get();
	}

	@Override
	public Map<VertexElement, ?> get(int index) {
		ByteBuffer readCopy = memBuffer.duplicate();
		readCopy.position(index * vertexDataStructure.size());
		Map<VertexElement, Object> result = new HashMap<>();
		for(VertexElement element : vertexDataStructure) {
			Object object = element.type().deserialize(readCopy);
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
		if(active.compareAndSet(false, true)) {
			// activate
		}
	}

	@Override
	public boolean isActive() {
		return active.get();
	}

	@Override
	public void end(RenderContext ctx) {
		active.set(false);
	}

	@Override
	public void destroy() {
		MemoryUtil.memFree(memBuffer);
	}

}
