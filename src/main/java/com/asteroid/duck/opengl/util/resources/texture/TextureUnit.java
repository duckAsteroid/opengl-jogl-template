package com.asteroid.duck.opengl.util.resources.texture;

import com.asteroid.duck.opengl.util.resources.impl.Resource;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;

import java.util.function.Consumer;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

/**
 * Texture units are the slots textures are loaded into that can be bound into shaders
 */
public class TextureUnit implements Resource, Comparable<TextureUnit> {
	private final int index;
	private final int shaderUnit;
	private Texture boundTexture;
	private final Consumer<TextureUnit> disposalFunction;

	private TextureUnit(int index, int shaderUnit, Consumer<TextureUnit> disposalFunction) {
		this.index = index;
		this.shaderUnit = shaderUnit;
		this.disposalFunction = disposalFunction;
	}

	public static TextureUnit index(int index) {
		return index(index, null);
	}

	public static TextureUnit index(int index, Consumer<TextureUnit> disposalFunction) {
		// FIXME use GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS to restrict?
		if (index < 0 || index > 31) throw new IllegalArgumentException("Texture unit index must be between 0 and 31");
		return new TextureUnit(index, GL_TEXTURE0 + index, disposalFunction);
	}

	public void bind(Texture texture) {
		boundTexture = texture;
		glActiveTexture(shaderUnit);
		texture.Bind();
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
		if (disposalFunction != null) {
			disposalFunction.accept(this);
		}
	}

	@Override
	public int compareTo(TextureUnit other) {
		return this.index - other.index;
	}
}
