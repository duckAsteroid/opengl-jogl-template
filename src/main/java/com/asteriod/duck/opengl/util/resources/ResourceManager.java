package com.asteriod.duck.opengl.util.resources;

import com.asteriod.duck.opengl.util.resources.impl.Resource;
import com.asteriod.duck.opengl.util.resources.shader.ShaderLoader;
import com.asteriod.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteriod.duck.opengl.util.resources.texture.ImageData;
import com.asteriod.duck.opengl.util.resources.texture.Texture;
import com.asteriod.duck.opengl.util.resources.texture.TextureLoader;
import com.asteriod.duck.opengl.util.resources.texture.TextureUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.Stream;


public class ResourceManager {
	private static final Logger LOG = LoggerFactory.getLogger(ResourceManager.class);

	private final Path root;
	private final ShaderLoader shaderLoader;
	private final TextureLoader textureLoader;

	public record ResourceLocator(Class<? extends Resource> type, String name){}

	private final HashMap<ResourceLocator, Resource> resources = new HashMap<>();

	public ResourceManager(String root) {
		this.root = Paths.get(root);
		this.textureLoader = new TextureLoader(this.root.resolve("resources/textures"));
		this.shaderLoader = new ShaderLoader(this.root.resolve("glsl"));
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

	public Texture GetTexture(String name, String path, boolean alpha) {
		ResourceLocator locator = new ResourceLocator(Texture.class, name);
		if (!resources.containsKey(locator)) {
			Texture tex = null;
			try {
				tex = textureLoader.LoadTexture(path, alpha);
			} catch (IOException e) {
				LOG.error("Error loading texture", e);
			}
			resources.put(locator, tex);
		}
		return (Texture) resources.get(locator);
	}

	public ImageData LoadTextureData(String image, boolean b) throws IOException {
		return textureLoader.loadTextureData(image, b);
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

	public TextureUnit NextTextureUnit() {
		int index = TextureUnits().mapToInt(TextureUnit::getIndex).max().orElse(-1 ) + 1;
		TextureUnit unit = TextureUnit.index(index);
		resources.put(new ResourceLocator(TextureUnit.class, Integer.toHexString(unit.getIndex())), unit);
		return unit;
	}

	public Stream<TextureUnit> TextureUnits() {
		return resources.values().stream()
						.filter(resource -> resource instanceof TextureUnit).map(resource -> (TextureUnit) resource);
	}

	public void clear() {
		resources.values().forEach(Resource::destroy);
		resources.clear();
	}



}
