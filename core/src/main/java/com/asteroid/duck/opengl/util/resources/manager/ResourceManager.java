package com.asteroid.duck.opengl.util.resources.manager;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.Resource;
import com.asteroid.duck.opengl.util.resources.shader.ShaderLoader;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.texture.ImageData;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import com.asteroid.duck.opengl.util.resources.texture.TextureFactory;
import com.asteroid.duck.opengl.util.resources.texture.TextureUnit;
import com.asteroid.duck.opengl.util.resources.texture.io.ImageLoadingOptions;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * API surface for resource managers that provide named access to textures, shaders and texture-units.
 *
 * Implementations (for example FileResourceManager) should provide the concrete behavior for loading,
 * caching and lifetime management.
 *
 * @see RenderContext
 */
public interface ResourceManager extends Resource {
	// texture access
	Texture getTexture(String name);
	void putTexture(String name, Texture texture);
	Texture getTexture(String name, String path);
	Texture getTexture(String name, String path, ImageLoadingOptions options);
	ImageData loadTextureData(String image, ImageLoadingOptions options) throws IOException;
	TextureFactory getTextureFactory();

	// shader access
	ShaderProgram getShader(String name, String vertexPath, String fragPath, String geomPath);
	ShaderProgram getSimpleShader(String name);
	ShaderLoader getShaderLoader();

	// texture unit allocation
	TextureUnit nextTextureUnit();
	Stream<TextureUnit> textureUnits();

}
