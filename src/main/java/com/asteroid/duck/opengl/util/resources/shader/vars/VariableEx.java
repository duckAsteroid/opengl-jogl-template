package com.asteroid.duck.opengl.util.resources.shader.vars;

import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Interface between {@link VariableType} and {@link ShaderProgram}
 */
public interface VariableEx {
	int maxNameLength(int program);

	int count(int program);

	void readVariable(int program, int index, IntBuffer len, IntBuffer size, IntBuffer type, ByteBuffer value);
}
