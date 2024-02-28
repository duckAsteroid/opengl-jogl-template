package com.asteriod.duck.opengl.util.resources;

import com.asteriod.duck.opengl.util.ShaderProgram;
import com.asteriod.duck.opengl.util.Texture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.function.Function;

import static org.lwjgl.opengl.GL11.GL_RGBA;


public class ResourceManager {
	private static final Logger LOG = LoggerFactory.getLogger(ResourceManager.class);

	private static ResourceManager SINGLETON = new ResourceManager("src/main/");
	private final Path root;
	private final ShaderLoader shaderLoader;
	private final TextureLoader textureLoader;


	public record ResourceLocator(Class<? extends Resource> type, String name){}

	private final HashMap<ResourceLocator, Resource> resources = new HashMap<>();

	public static ResourceManager instance() {
		return SINGLETON;
	}

	private ResourceManager(String root) {
		this.root = Paths.get(root);
		this.textureLoader = new TextureLoader(this.root.resolve("resources/textures"));
		this.shaderLoader = new ShaderLoader(this.root.resolve("glsl"));
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

	public void clear() {
		resources.values().forEach(Resource::destroy);
		resources.clear();
	}



}
