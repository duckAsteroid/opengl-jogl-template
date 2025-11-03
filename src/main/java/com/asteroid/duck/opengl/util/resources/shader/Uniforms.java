package com.asteroid.duck.opengl.util.resources.shader;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.lwjgl.opengl.GL20C.glUniform1f;
import static org.lwjgl.opengl.GL20C.glUniform1fv;
import static org.lwjgl.opengl.GL20C.glUniform1i;
import static org.lwjgl.opengl.GL20C.glUniform2f;
import static org.lwjgl.opengl.GL20C.glUniform3f;
import static org.lwjgl.opengl.GL20C.glUniform4f;
import static org.lwjgl.opengl.GL20C.glUniform4fv;
import static org.lwjgl.opengl.GL20C.glGetUniformLocation;
import static org.lwjgl.opengl.GL20C.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL30C.glUniform1ui;

/**
 * An interface into the uniform values associated with a {@link ShaderProgram}
 */
public class Uniforms {
	private final ShaderProgram shaderProgram;
	private final Map<String, Integer> uniformLocationCache = new HashMap<>();

	public Uniforms(ShaderProgram shaderProgram) {
		this.shaderProgram = shaderProgram;
	}

	protected int uniformLocation(String uniformName) {
		Integer location = uniformLocationCache.computeIfAbsent(uniformName,
				name -> glGetUniformLocation(shaderProgram.id, name));
		return location;
	}

    public boolean has(String name) {
        return uniformLocation(name) >= 0;
    }

	@SuppressWarnings("unchecked")
	public <T> Uniform<T> get(String name, Class<T> type) {
		if (name == null) {
			throw new IllegalArgumentException("Uniform name cannot be null");
		}
		int loc = uniformLocation(name);
        if (loc == -1) {
            throw new NoSuchElementException("Could not find uniform with name '" + name + "' in "+shaderProgram);
        }
		if (type == Boolean.class || type == boolean.class) {
			return (Uniform<T>) new Uniform<>(name, loc, this::setBoolean);
		}
		if (type == Float.class || type == float.class) {
			return (Uniform<T>) new Uniform<>(name, loc, this::setFloat);
		}
		if (type == Integer.class || type == int.class) {
			return (Uniform<T>) new Uniform<>(name, loc, this::setInteger);
		}
		if (type == Vector2f.class) {
			return (Uniform<T>) new Uniform<>(name, loc, this::setVector2f);
		}
		if (type == Vector3f.class) {
			return (Uniform<T>) new Uniform<>(name, loc, this::setVector3f);
		}
		if (type == Vector4f.class) {
			return (Uniform<T>) new Uniform<>(name, loc, this::setVector4f);
		}
		if (type == Matrix4f.class) {
			return (Uniform<T>) new Uniform<>(name, loc, this::setMatrix4f);
		}

		throw new IllegalArgumentException("Unsupported uniform type: " + type.getName());
	}

	void setBoolean(int location, boolean value) {
		glUniform1i(location, value ? 1 : 0);
	}

	void setFloat(int location, float value)
	{
		glUniform1f(location, value);
	}

	void setFloatArray(int location, float[] value)
	{
		glUniform1fv(location, value);
	}

	void setInteger(int location, int value)
	{
		glUniform1i(location, value);
	}

	void setUnsignedInteger(int location, int value)
	{
		glUniform1ui(location, value);
	}

	void setVector2f_float(int location, float x, float y)
	{
		glUniform2f(location, x, y);
	}

	void setVector2f(int location, Vector2f value)
	{
		glUniform2f(location, value.x, value.y);
	}

	void setVector3f_float(int location, float x, float y, float z)
	{
		glUniform3f(location, x, y, z);
	}

	void setVector3f(int location, Vector3f value)
	{
		glUniform3f(location, value.x, value.y, value.z);
	}

	void setVector4f_float(int location, float x, float y, float z, float w)
	{
		glUniform4f(location, x, y, z, w);
	}

	void setVector4f(int location, Vector4f value)
	{
		try (MemoryStack stack = MemoryStack.stackPush()) {
			glUniform4fv(location, value.get(stack.mallocFloat(4)));
		}
	}

	void setMatrix4f(int location, Matrix4f matrix)
	{
		try (MemoryStack stack = MemoryStack.stackPush()) {
			FloatBuffer fb = matrix.get(stack.mallocFloat(16));
			glUniformMatrix4fv(location, false, fb);
		}
	}

	void clear() {
		uniformLocationCache.clear();
	}
}
