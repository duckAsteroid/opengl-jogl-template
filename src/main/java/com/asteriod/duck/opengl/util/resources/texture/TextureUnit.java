package com.asteriod.duck.opengl.util.resources.texture;

import com.asteriod.duck.opengl.util.resources.impl.Resource;
import com.asteriod.duck.opengl.util.resources.shader.ShaderProgram;

import static org.lwjgl.opengl.GL13.*;

public class TextureUnit implements Resource, Comparable<TextureUnit> {
	private final int index;
	private final int shaderUnit;
	private Texture boundTexture;

	private TextureUnit(int index, int shaderUnit) {
		this.index = index;
		this.shaderUnit = shaderUnit;
	}

	private static final int[] TEXUNITS = new int[]{
					GL_TEXTURE0,
					GL_TEXTURE1,
					GL_TEXTURE2,
					GL_TEXTURE3,
					GL_TEXTURE4,
	};

	public static final TextureUnit index(int index) {
		// FIXME use GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS to restrict?
		return new TextureUnit(index, TEXUNITS[index]);
	}

	public void bind(Texture texture) {
		boundTexture = texture;
		glActiveTexture(shaderUnit);
		glBindTexture(GL_TEXTURE_2D, texture.id());
	}

	public int getIndex() {
		return index;
	}

	public Texture getBoundTexture() {
		return boundTexture;
	}

	public void useInShader(ShaderProgram program, String variable) {
		program.setInteger(variable, index);
	}

	@Override
	public void destroy() {
		// nothing to do
	}

	@Override
	public int compareTo(TextureUnit other) {
		return this.index - other.index;
	}
}
