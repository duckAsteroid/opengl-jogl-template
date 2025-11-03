package com.asteroid.duck.opengl.util.resources;

import com.asteroid.duck.opengl.util.resources.shader.ShaderLoader;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.texture.*;
import com.asteroid.duck.opengl.util.resources.texture.io.ImageLoadingOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.SortedSet;
import java.util.stream.Stream;

/**
 * Filesystem-aware resource loader and cache. This class is responsible for:
 * - resolving filesystem paths relative to a project root,
 * - delegating concrete loading to TextureFactory and ShaderLoader,
 * - caching loaded resources by logical name,
 * - allocating texture units.
 *
 * <p>This class intentionally focuses on loading/caching behavior only. It is separate
 * from the simple {@link ResourceListManager} which is a generic container of Resource instances.
 */
public class FileResourceManager implements Resource, ResourceManager {
	private static final Logger LOG = LoggerFactory.getLogger(FileResourceManager.class);

	private final Path root;
	private final ShaderLoader shaderLoader;
	private final TextureFactory textureFactory;

	// Replace heterogeneous resources map with per-type named managers
	private final NamedResourceManager<Texture> textures = new NamedResourceManager<>();
	private final NamedResourceManager<ShaderProgram> shaders = new NamedResourceManager<>();
	private final NamedResourceManager<TextureUnit> textureUnits = new NamedResourceManager<>();

	private final SortedSet<Integer> unallocatedTextureUnits = new java.util.TreeSet<>();

	/**
	 * Initialize or reset the texture-unit pool to 0..31.
	 */
	private void initTextureUnits() {
		unallocatedTextureUnits.clear();
		for (int i = 0; i < 32; i++) {
			unallocatedTextureUnits.add(i);
		}
	}

	/**
	 * Creates a FileResourceManager rooted at the provided filesystem path.
	 *
	 * @param root root directory where "glsl" and "resources/textures" are located
	 */
	public FileResourceManager(String root) {
		this.root = Paths.get(root);
		this.textureFactory = new TextureFactory(this.root.resolve("resources/textures"));
		this.shaderLoader = new ShaderLoader(this.root.resolve("glsl"));
		// populate the texture-unit pool
		initTextureUnits();
	}


	/**
	 * Returns the TextureFactory used by this manager.
	 *
	 * @return the configured {@link TextureFactory}
	 */
	public TextureFactory getTextureFactory() {
		return textureFactory;
	}

	/**
	 * Returns the {@link ShaderLoader} used by this manager.
	 *
	 * @return the configured {@link ShaderLoader}
	 */
	public ShaderLoader getShaderLoader() {
		return shaderLoader;
	}

	public Texture getTexture(String name) {
		Texture t = textures.get(name);
		if (t == null) {
			throw new IllegalArgumentException("No such resource as " + name);
		}
		return t;
	}

	public void putTexture(String name, Texture texture) {
		textures.put(name, texture);
	}

	public Texture getTexture(String name, String path) {
		return getTexture(name, path, ImageLoadingOptions.DEFAULT);
	}

	public Texture getTexture(String name, String path, ImageLoadingOptions options) {
		if (!textures.contains(name)) {
			Texture tex = null;
			if (path != null) {
				try {
					tex = textureFactory.LoadTexture(path, options);
				} catch (IOException e) {
					LOG.error("Error loading texture", e);
				}
			}
			textures.put(name, tex);
		}
		return textures.get(name);
	}

	public ImageData loadTextureData(String image, ImageLoadingOptions options) throws IOException {
		return textureFactory.loadTextureData(image, options);
	}

	public ShaderProgram getShader(String name, String vertexPath, String fragPath, String geomPath) {
		if (!shaders.contains(name)) {
			try {
				ShaderProgram shader = shaderLoader.LoadShaderProgram(vertexPath, fragPath, geomPath);
				shaders.put(name, shader);
			} catch (IOException e) {
				LOG.error("Error loading shader", e);
			}
		}
		return shaders.get(name);
	}

	public ShaderProgram getSimpleShader(String name) {
		return getShader(name, name + "/vertex.glsl", name + "/frag.glsl", null);
	}

	public TextureUnit nextTextureUnit() {
		int index = unallocatedTextureUnits.first();
		unallocatedTextureUnits.remove(index);
		TextureUnit unit = TextureUnit.index(index, this::replaceTextureUnit);
		textureUnits.put(Integer.toHexString(unit.getIndex()), unit);
		return unit;
	}

	private void replaceTextureUnit(TextureUnit textureUnit) {
		int index = textureUnit.getIndex();
		String key = Integer.toHexString(index);
		// remove mapping from the named manager and unregister from owner
		// removeByName will also stop tracking the resource via ResourceManager.remove
		textureUnits.removeByName(key);
		unallocatedTextureUnits.add(index);
	}

	public Stream<TextureUnit> textureUnits() {
		return textureUnits.entries().map(java.util.Map.Entry::getValue);
	}

	/**
	 * Destroy and clear all managed resources. After calling this method the manager will have no cached resources.
	 *
	 * Note: this replaces the former clear() method.
	 */
	@Override
	public void destroy() {
		// Each NamedResourceManager tracks its own resources; destroy them to
		// ensure proper disposal.
		textures.destroy();
		shaders.destroy();
		textureUnits.destroy();

		initTextureUnits();
	}
}
