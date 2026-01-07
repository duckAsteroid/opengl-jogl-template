package com.asteroid.duck.opengl.util.palette;

import com.asteroid.duck.opengl.util.AbstractPassthruRenderer;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.texture.io.TextureData;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import com.asteroid.duck.opengl.util.resources.textureunit.TextureUnit;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Basically an indexed colour palette implementation. RGB output colour values are looked up using
 * the palette index.
 */
public class PaletteRenderer extends AbstractPassthruRenderer {
	private static final Logger LOG = LoggerFactory.getLogger(PaletteRenderer.class);

	// indexed texture
	private final String textureName;

	// the 1D palette with an RGB value for each index
	private Texture palette;
	private TextureUnit paletteUnit;
	private final String paletteName;

	public PaletteRenderer(String name) {
		this(name, "palette");
	}

	private PaletteRenderer(String name, String paletteName) {
		this.textureName = name;
		this.paletteName = paletteName;
	}

	public static TextureData greyScale() {
		ByteBuffer raw = BufferUtils.createByteBuffer(256 * 4);
		for (int i = 0; i < 256; i++) {
			raw.put((byte) i);
			raw.put((byte) i);
			raw.put((byte) i);
			raw.put((byte) 255); // A
		}
		raw.flip();
		return new TextureData(raw, new Dimension(256, 1));
	}

	public static TextureData rbgTestScale() {
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
		return new TextureData(raw, new Dimension(256, 1));
	}

	// Dump a PNG with the rgbTestScale in it
	public static void main(String[] args) throws IOException {
		BufferedImage image = new BufferedImage(256, 1, BufferedImage.TYPE_INT_ARGB);
		TextureData data = rbgTestScale();
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


	protected ShaderProgram initShaderProgram(RenderContext ctx) throws IOException {
		// load the GLSL Shaders
		return ctx.getResourceManager().getSimpleShader("palette");
	}

	protected Texture initTexture(RenderContext ctx) {

		this.palette = ctx.getResourceManager().getTexture(paletteName);
		this.paletteUnit = ctx.getResourceManager().nextTextureUnit();
		this.paletteUnit.bind(palette);

		return ctx.getResourceManager().getTexture(textureName);
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		super.init(ctx);
		shaderProgram.use();
		this.paletteUnit.useInShader(shaderProgram, "palette");
		shaderProgram.unuse();
	}

	@Override
	public void dispose() {
		paletteUnit.dispose();
		palette.dispose();
		super.dispose();
	}
}
