package com.asteriod.duck.opengl;

import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.RenderedItem;
import com.asteriod.duck.opengl.util.Triangles;
import com.asteriod.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteriod.duck.opengl.util.resources.texture.Texture;
import com.asteriod.duck.opengl.util.resources.texture.TextureUnit;
import org.joml.Vector2f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL20.glUseProgram;

/**
 * Basically an indexed colour palette implementation. RGB output colour values are looked up using
 * the palette index.
 */
public class PaletteRenderer implements RenderedItem {
	private static final Logger LOG = LoggerFactory.getLogger(PaletteRenderer.class);

	private ShaderProgram shaderProgram = null;

	private final String textureName;

	private Texture texture;
	private TextureUnit textureUnit;

	private Texture palette;
	private TextureUnit paletteUnit;

	private Triangles renderedShape;
	private final String shaderName;

	private double rate = 50;

	public PaletteRenderer(String name) {
		this(name, "palette");
	}

	private PaletteRenderer(String name, String shaderName) {
		this.textureName = name;
		this.shaderName = shaderName;
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

		shaderProgram.setVector2f("dimensions", new Vector2f(texture.Width, texture.Height));
	}


	private void initBuffers() {
		renderedShape = Triangles.fullscreen();
		renderedShape.setup(shaderProgram);
	}

	@Override
	public void doRender(RenderContext ctx) {
		shaderProgram.use();
		int offset = (int) (ctx.getTimer().elapsed() * rate);
		shaderProgram.setInteger("offset", offset);
		renderedShape.render();
		glUseProgram(0);
	}

	@Override
	public void dispose() {
		renderedShape.dispose();
		shaderProgram.destroy();
		textureUnit.destroy();
	}
}
