package com.asteriod.duck.opengl.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.lwjgl.opengl.GL11.GL_RGBA;


public class ResourceManager {
	private static final Logger LOG = LoggerFactory.getLogger(ResourceManager.class);

	private static ResourceManager SINGLETON = new ResourceManager("src/main/");
	private final Path root;

	public static ResourceManager instance() {
		return SINGLETON;
	}

	private ResourceManager(String root) {
		this.root = Paths.get(root);
	}

	private HashMap<String, Texture> textures = new HashMap<>();
	private HashMap<String, ShaderProgram> shaders = new HashMap<>();

	public ShaderProgram loadShaderFromFile(String name) throws IOException {
		return loadShaderFromFile(name + ".vert", name +" .frag", null);
	}

	public ShaderProgram loadShaderFromFile(String vertex, String fragment) throws IOException {
		return loadShaderFromFile(vertex, fragment, null);
	}

	public ShaderProgram loadShaderFromFile(String vertex, String fragment, String geometry) throws IOException {
		Path geomPath = (geometry == null || geometry.isBlank()) ? null : getPath("glsl/"+geometry);
		return loadShaderFromFile(getPath("glsl/"+vertex), getPath("glsl/"+fragment), geomPath);
	}

	private Path getPath(String path) {
		return root.resolve(path);
	}

	/**
	 * Loads and compiles a shader from files by path.
	 * @param vertex the vertex shader path
	 * @param fragment the fragment shader path
	 * @param geometry (optional) the geometry shader path (may be null)
	 * @return the compiled and linked shader program - ready to use
	 * @throws IOException If the files can't be accessed
	 */
	public ShaderProgram loadShaderFromFile(Path vertex, Path fragment, Path geometry) throws IOException {

		final ShaderProgram shaderProgram = ShaderProgram.compile(vertex, fragment, geometry);
		System.out.println("Loaded shader to "+shaderProgram.id());

		return shaderProgram;
	}

	public static Texture loadTextureFromFile(int width, int height, ByteBuffer textureData, boolean alpha) throws IOException {
		Texture tex = new Texture();
		if (alpha)
		{
			tex.setInternalFormat( GL_RGBA);
			tex.setImageFormat( GL_RGBA);
		}
		tex.Generate(width, height, textureData);
		return tex;
	}

	public Texture GetTexture(String name) {
		return textures.get(name);
	}

	public void clear() {
		textures.values().forEach(Texture::destroy);
		shaders.values().forEach(ShaderProgram::destroy);
		textures.clear();
		shaders.clear();
	}

	public ShaderProgram LoadShader(String vertexPath, String fragmentPath, String geometryPath, String name) {
		if (!shaders.containsKey(name)) {
			try {
				ShaderProgram program = loadShaderFromFile(vertexPath, fragmentPath, geometryPath);
				shaders.put(name, program);
			}
			catch (IOException ioe) {
				LOG.error("Unable to load shader", ioe);
			}
		}
		return shaders.get(name);
	}

	public ShaderProgram GetShader(String name) {
		return shaders.get(name);
	}

	public Texture LoadTexture(String texturePath, boolean alpha, String name) {
		if (!textures.containsKey(name)) {
			try {
				ImageData imageData = loadTextureData(texturePath, alpha);
				Texture tex = loadTextureFromFile(imageData.size().width, imageData.size().height, imageData.buffer, alpha);
				textures.put(name, tex);
			}
			catch(IOException e) {
				LOG.error("Error loading texture", e);
			}
		}
		return textures.get(name);
	}

	public record ImageData(ByteBuffer buffer, Dimension size) {}

	public ImageData loadTextureData(String texturePath, boolean alpha) throws IOException {
		try(InputStream inputStream = Files.newInputStream(getPath("resources/"+texturePath))) {
			BufferedImage image = ImageIO.read(inputStream);
			int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
			ByteBuffer buffer = ByteBuffer.allocateDirect(pixels.length * 4);
			for (int pixel : pixels) {
				// pixel is ARGB - we want RGBA or RGB
				// red
				buffer.put((byte) ((pixel >> 16) & 0xFF));
				// green
				buffer.put((byte) ((pixel >> 8) & 0xFF));
				// blue
				buffer.put((byte) ((pixel >> 0) & 0xFF));
				if (alpha) {
					buffer.put((byte)((pixel >> 24) & 0xFF));
				}
			}
			buffer.flip();

			return new ImageData(buffer, new Dimension(image.getWidth(), image.getHeight()));
		}
	}
}
