package com.asteriod.duck.opengl;

import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.RenderedItem;
import com.asteriod.duck.opengl.util.Triangles;
import com.asteriod.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteriod.duck.opengl.util.resources.texture.ImageData;
import com.asteriod.duck.opengl.util.resources.texture.Texture;
import com.asteriod.duck.opengl.util.resources.texture.TextureUnit;
import org.joml.Vector2f;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL20.glUseProgram;

/**
 * Basically an indexed colour palette implementation. RGB output colour values are looked up using
 * the palette index.
 */
public class PaletteRenderer implements RenderedItem {
	private static final Logger LOG = LoggerFactory.getLogger(PaletteRenderer.class);

	private ShaderProgram shaderProgram = null;

	private final String textureName;
	// the "indexed" texture
	private Texture texture;
	private TextureUnit textureUnit;

	// the 1D palette with an RGB value for each index
	private Texture palette;
	private TextureUnit paletteUnit;

	private Triangles renderedShape;
	private final String shaderName;

	public PaletteRenderer(String name) {
		this(name, "palette");
	}

	private PaletteRenderer(String name, String shaderName) {
		this.textureName = name;
		this.shaderName = shaderName;
	}

	public static ImageData greyScale() {
		ByteBuffer raw = BufferUtils.createByteBuffer(256 * 4);
		for (int i = 0; i < 256; i++) {
			raw.put((byte) i);
			raw.put((byte) i);
			raw.put((byte) i);
			raw.put((byte) 255); // A
		}
		raw.flip();
		return new ImageData(raw, new Dimension(256, 1));
	}

	public static ImageData rbgTestScale() {
		ByteBuffer raw = BufferUtils.createByteBuffer(256 * 4);
		int g = 128;
		int b = 255;
		for (int i = 0; i < 256; i++) {
			raw.put((byte) i); //r 0 - 255
			raw.put((byte) g); //g 128 - 255 / 0 - 127
			raw.put((byte) b); //b 255 - 0
			g += 1;
			if (g > 255) g = 0;
			b -= 1;
			raw.put((byte) 255); // A
		}
		raw.flip();
		return new ImageData(raw, new Dimension(256, 1));
	}

	// Dump a PNG with the rgbTestScale in it
	public static void main(String[] args) throws IOException {
		BufferedImage image = new BufferedImage(256, 1, BufferedImage.TYPE_INT_ARGB);
		ImageData data = rbgTestScale();
		// Iterate over the byte array, 4 bytes at a time
		ByteBuffer raw = data.buffer();
		IntBuffer intBuffer = raw.asIntBuffer();
		for (int i = 0; i < 256; i ++) {
			// Convert the RGBA bytes to an integer
			int pixel = intBuffer.get(i);

			// Set the pixel in the image
			image.setRGB(i / 4, 0, pixel);
		}
		var file = new java.io.File("test.png");
		ImageIO.write(image, "png", file);
		System.out.println(file.getAbsolutePath());
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		initShaderProgram(ctx);
		initTextures(ctx);
		initBuffers();
	}

	private void initShaderProgram(RenderContext ctx) throws IOException {
		// load the GLSL Shaders
		this.shaderProgram = ctx.getResourceManager().GetShader(shaderName, shaderName+"/vertex.glsl", shaderName+"/frag.glsl", null);
		LOG.info("Using shader program {}, id={}", shaderName, shaderProgram);
	}

	private void initTextures(RenderContext ctx) {
		shaderProgram.use();
		this.texture = ctx.getResourceManager().GetTexture(textureName);
		this.textureUnit = ctx.getResourceManager().NextTextureUnit();
		this.textureUnit.bind(texture);
		this.textureUnit.useInShader(shaderProgram, "tex");

		this.palette = ctx.getResourceManager().GetTexture("palette");
		this.paletteUnit = ctx.getResourceManager().NextTextureUnit();
		this.paletteUnit.bind(palette);
		this.paletteUnit.useInShader(shaderProgram, "palette");

	}


	private void initBuffers() {
		renderedShape = Triangles.fullscreen();
		renderedShape.setup(shaderProgram);
	}

	@Override
	public void doRender(RenderContext ctx) {
		shaderProgram.use();
		renderedShape.render();
		shaderProgram.unuse();
	}

	@Override
	public void dispose() {
		renderedShape.dispose();
	}
}
