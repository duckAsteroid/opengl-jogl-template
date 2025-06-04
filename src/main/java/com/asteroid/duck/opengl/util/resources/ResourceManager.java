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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.stream.Stream;


public class ResourceManager {
	private static final Logger LOG = LoggerFactory.getLogger(ResourceManager.class);

	private final Path root;
	private final ShaderLoader shaderLoader;
	private final TextureFactory textureFactory;

	public record ResourceLocator(Class<? extends Resource> type, String name){}

	private final HashMap<ResourceLocator, Resource> resources = new HashMap<>();
	private final SortedSet<Integer> unallocatedTextureUnits = Stream.iterate(0, i -> i + 1).limit(32).collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));

	public ResourceManager(String root) {
		this.root = Paths.get(root);
		this.textureFactory = new TextureFactory(this.root.resolve("resources/textures"));
		this.shaderLoader = new ShaderLoader(this.root.resolve("glsl"));
	}

	public TextureFactory getTextureFactory() {
		return textureFactory;
	}

	public ShaderLoader getShaderLoader() {
    return shaderLoader;
  }

	public Texture GetTexture(String name) {
		ResourceLocator locator = new ResourceLocator(Texture.class, name);
		if (!resources.containsKey(locator)) {
			throw new IllegalArgumentException("No such resource as "+name);
		}
		return (Texture) resources.get(locator);
	}

	public void PutTexture(String name, Texture texture) {
		ResourceLocator locator = new ResourceLocator(Texture.class, name);
		resources.put(locator, texture);
	}
	public Texture GetTexture(String name, String path) {
		return GetTexture(name, path, ImageLoadingOptions.DEFAULT);
	}

	public Texture GetTexture(String name, String path, ImageLoadingOptions options) {
		ResourceLocator locator = new ResourceLocator(Texture.class, name);
		if (!resources.containsKey(locator)) {
			Texture tex = null;
			if (path != null) {
				try {
					tex = textureFactory.LoadTexture(path, options);
				} catch (IOException e) {
					LOG.error("Error loading texture", e);
				}
			}
			else {
				tex = null;
			}
			resources.put(locator, tex);
		}
		return (Texture) resources.get(locator);
	}



	public ImageData LoadTextureData(String image, ImageLoadingOptions options) throws IOException {
		return textureFactory.loadTextureData(image, options);
	}


	public ShaderProgram GetShader(String name, String vertexPath, String fragPath, String geomPath) {
		ResourceLocator locator = new ResourceLocator(ShaderProgram.class, name);
		if (!resources.containsKey(locator)) {
			try {
				ShaderProgram shader = shaderLoader.LoadShaderProgram(vertexPath, fragPath, geomPath);
				resources.put(locator, shader);
			}
			catch(IOException e) {
				LOG.error("Error loading shader", e);
			}
		}
		return (ShaderProgram) resources.get(locator);
	}

	public ShaderProgram GetSimpleShader(String name) {
    return GetShader(name, name+"/vertex.glsl", name+"/frag.glsl", null);
  }

	public TextureUnit NextTextureUnit() {
		int index = unallocatedTextureUnits.first();
		unallocatedTextureUnits.remove(index);
		TextureUnit unit = TextureUnit.index(index, this::replaceTextureUnit);
		resources.put(new ResourceLocator(TextureUnit.class, Integer.toHexString(unit.getIndex())), unit);
		return unit;
	}

	private void replaceTextureUnit(TextureUnit textureUnit) {
		int index = textureUnit.getIndex();
		ResourceLocator locator = new ResourceLocator(TextureUnit.class, Integer.toHexString(index));
		resources.remove(locator);
		unallocatedTextureUnits.add(index);
	}

	public Stream<TextureUnit> TextureUnits() {
		return resources.values().stream()
						.filter(resource -> resource instanceof TextureUnit).map(resource -> (TextureUnit) resource);
	}

	public void clear() {
		Collection<Resource> clone = new ArrayList<>(resources.values());
		clone.forEach(Resource::destroy);
		resources.clear();
	}
}
