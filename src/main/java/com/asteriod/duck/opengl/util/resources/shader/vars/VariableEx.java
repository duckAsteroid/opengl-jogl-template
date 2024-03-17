package com.asteriod.duck.opengl.util.resources.shader.vars;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Interface between {@link VariableType} and {@link com.asteriod.duck.opengl.util.resources.shader.ShaderProgram}
 */
public interface VariableEx {
	int maxNameLength(int program);

	int count(int program);

	void readVariable(int program, int index, IntBuffer len, IntBuffer size, IntBuffer type, ByteBuffer value);
}
