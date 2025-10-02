package com.asteroid.duck.opengl.util.resources.buffer;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.GL_FLOAT;

/**
 * Represents the type of {@link VertexElement} in a {@link VertexDataStructure}.
 * Can go between the bytes in the underlying memory buffer and the Java type this element holds.
 * There are implementations for common vertex data types as static members:
 * <ul>
 * <li>{@link #FLOAT}</li>
 * <li>{@link #VEC_2F}</li>
 * <li>{@link #VEC_3F}</li>
 * <li>{@link #VEC_4F}</li>
 * </ul>
 * @param <T> The Java type of data stored in the element
 */
public abstract class VertexElementType<T> {
	private final Class<T> javaType;
	private final int dimensions;
	private final int glType;

	public VertexElementType(Class<T> t, int s, int glType) {
		this.javaType = t;
		this.dimensions = s;
		this.glType = glType;
	}

	public void serializeRaw(Object obj, ByteBuffer buffer) {
		try {
			if (obj == null) {
				obj = nullReplacementValue();
			}
			serialize(javaType.cast(obj), buffer);
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("Object is not of type " + javaType.getName(), e);
		}
	}

	/**
	 * The open GL type of the data in the element
	 */
	public int glType() {
		return glType;
	}

	/**
	 * How many dimensions to this element (e.g. 1 for FLOAT, 2 for Vec2 etc.)
	 */
	public int dimensions() {
		return dimensions;
	}

	public int byteSize() {
		// FIXME what to do for non FLOAT elements
		return dimensions * 4;
	}

	/**
	 * The expected Java type of data stored in the element
	 * @return the class of the Java type
	 */
	public Class<T> getJavaType() {
		return javaType;
	}

	protected abstract void serialize(T obj, ByteBuffer buffer);

	public Object deserializeRaw(ByteBuffer buffer) {
			return deserialize(buffer);
	}

	protected abstract T deserialize(ByteBuffer buffer);

	public Object nullReplacementValue() {
		throw new IllegalArgumentException("Serialized value of "+javaType.getName()+" cannot be null");
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass().equals(o.getClass())) return false;
		VertexElementType<?> that = (VertexElementType<?>) o;
		return glType == that.glType &&
						dimensions == that.dimensions &&
						Objects.equals(javaType, that.javaType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(javaType, dimensions, glType);
	}

	@Override
	public String toString() {
		return "[%s, %dx%d]".formatted(javaType.getName(), dimensions, glType);
	}

	/// TYPED SINGLETON INSTANCES --------------------------------------------------------------------

	public static VertexElementType<Float> FLOAT = new VertexElementType<>(Float.class, 1, GL_FLOAT) {

		public Float deserialize(ByteBuffer byteBuffer) {
			return byteBuffer.getFloat();
		}

		public void serialize(Float f, ByteBuffer target) {
			target.putFloat(f);
		}

		@Override
		public Object nullReplacementValue() {
			return 0f;
		}
	};

	public static VertexElementType<Vector2f> VEC_2F = new VertexElementType<>(Vector2f.class,  2, GL_FLOAT) {

		public Vector2f deserialize(ByteBuffer byteBuffer) {
			float x = byteBuffer.getFloat();
			float y = byteBuffer.getFloat();
			return new Vector2f(x, y);
		}

		public void serialize(Vector2f vec, ByteBuffer byteBuffer) {
			byteBuffer.putFloat(vec.x).putFloat(vec.y);
		}

		@Override
		public Object nullReplacementValue() {
			return new Vector2f(0f);
		}
	};

	public static VertexElementType<Vector3f> VEC_3F = new VertexElementType<>(Vector3f.class, 3, GL_FLOAT) {

		public Vector3f deserialize(ByteBuffer byteBuffer) {
			float x = byteBuffer.getFloat();
			float y = byteBuffer.getFloat();
			float z = byteBuffer.getFloat();
			return new Vector3f(x,y,z);
		}

		public void serialize(Vector3f vec, ByteBuffer byteBuffer) {
			byteBuffer.putFloat(vec.x).putFloat(vec.y).putFloat(vec.z);
		}

		@Override
		public Object nullReplacementValue() {
			return new Vector3f(0f);
		}
	};

	public static final VertexElementType<Vector4f> VEC_4F = new VertexElementType<>(Vector4f.class,4,GL_FLOAT) {
		public Vector4f deserialize(ByteBuffer byteBuffer) {
			float x = byteBuffer.getFloat();
			float y = byteBuffer.getFloat();
			float z = byteBuffer.getFloat();
			float w = byteBuffer.getFloat();
			return new Vector4f(x,y,z,w);
		}

		public void serialize(Vector4f vec, ByteBuffer byteBuffer) {
			byteBuffer.putFloat(vec.x).putFloat(vec.y).putFloat(vec.z).putFloat(vec.w);
		}

		@Override
		public Object nullReplacementValue() {
			return new Vector4f(0f);
		}
	};


}
