package com.asteroid.duck.opengl.util.resources.buffer;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Objects;

/**
 * Represents the type of a {@link VertexElement} in a {@link VertexDataStructure}.
 * @param <T> The Java type of data stored in the element
 */
public abstract class VertexElementType<T> {

	protected abstract void serialize(T obj, ByteBuffer buffer);

	public Object deserializeRaw(ByteBuffer buffer) {
		int pos = buffer.position();
		try {
			return deserialize(buffer);
		} finally {
			buffer.position(pos + size);
		}
	}

	protected abstract T deserialize(ByteBuffer buffer);

	public Object nullReplacementValue() {
		throw new IllegalArgumentException("Serialized value of "+javaType.getName()+" cannot be null");
	}

	/**
	 * How many bytes required to store one value of this type
	 * @return number of bytes
	 */
	public int size() {
		return size;
	}

	/**
	 * The expected Java type of data stored in the element
	 * @return the class of the Java type
	 */
	public Class<T> getJavaType() {
		return javaType;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		VertexElementType<?> that = (VertexElementType<?>) o;
		return size == that.size && Objects.equals(javaType, that.javaType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(javaType, size);
	}

	@Override
	public String toString() {
		return "[%s, size=%d]".formatted(javaType.getName(), size);
	}


	/// TYPED SINGLETON INSTANCES --------------------------------------------------------------------

	public static VertexElementType<Float> FLOAT = new VertexElementType<>(Float.class, 4) {

		public Float deserialize(ByteBuffer byteBuffer) {
			return byteBuffer.asFloatBuffer().get();
		}

		public void serialize(Float f, ByteBuffer target) {
			target.asFloatBuffer().put(f);
		}

		@Override
		public Object nullReplacementValue() {
			return 0f;
		}
	};

	public static VertexElementType<Vector2f> VEC_2F = new VertexElementType<>(Vector2f.class,  8) {

		public Vector2f deserialize(ByteBuffer byteBuffer) {
			FloatBuffer fb = byteBuffer.asFloatBuffer();
			return new Vector2f(fb.get(), fb.get());
		}

		public void serialize(Vector2f vec, ByteBuffer byteBuffer) {
			FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
			floatBuffer.put(vec.x).put(vec.y);
		}

		@Override
		public Object nullReplacementValue() {
			return new Vector2f(0f);
		}
	};

	public static VertexElementType<Vector3f> VEC_3F = new VertexElementType<>(Vector3f.class, 12) {

		public Vector3f deserialize(ByteBuffer byteBuffer) {
			FloatBuffer fb = byteBuffer.asFloatBuffer();
			return new Vector3f(fb.get(), fb.get(), fb.get());
		}

		public void serialize(Vector3f vec, ByteBuffer byteBuffer) {
			FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
			floatBuffer.put(vec.x).put(vec.y).put(vec.y);
		}

		@Override
		public Object nullReplacementValue() {
			return new Vector3f(0f);
		}
	};

	private final Class<T> javaType;
	private final int size;

	VertexElementType(Class<T> t, int s) {
		this.javaType = t;
		this.size = s;
	}

	public void serializeRaw(Object obj, ByteBuffer buffer) {
		int pos = buffer.position();
		try {
			if (obj == null) {
				obj = nullReplacementValue();
			}
			serialize(javaType.cast(obj), buffer);
			buffer.position(pos + size);
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("Object is not of type " + javaType.getName(), e);
		}
	}

}
