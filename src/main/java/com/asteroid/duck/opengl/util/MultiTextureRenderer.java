package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.geom.Triangles;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import com.asteroid.duck.opengl.util.resources.texture.TextureUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MultiTextureRenderer implements RenderedItem {

	private static final Logger LOG = LoggerFactory.getLogger(MultiTextureRenderer.class);

	private ShaderProgram shaderProgram = null;

	private int vbo;
	private int ibo;
	private int vao;
	private final String[] textureNames;
	private Texture[] textures;
	private TextureUnit[] textureUnits;
	private Triangles renderedShape;

	public MultiTextureRenderer(String textureName) {
		this(new String[]{textureName});
	}

	public MultiTextureRenderer(String ... textureNames) {
		this.textureNames = textureNames;
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		initShaderProgram(ctx);
		initTextures(ctx);
		initBuffers();
	}

	private void initShaderProgram(RenderContext ctx) throws IOException {
		// load the GLSL Shaders
		this.shaderProgram = ctx.getResourceManager().GetShader("multi-tex", "multi-tex/vert.glsl", "multi-tex/frag.glsl", null);
		LOG.info("Using shader program {}", shaderProgram);
	}

	private void initTextures(RenderContext ctx) {
		this.textures = new Texture[textureNames.length];
		this.textureUnits = new TextureUnit[textureNames.length];
		shaderProgram.use();
		for (int i = 0; i < textureNames.length; i++) {
			this.textures[i] = ctx.getResourceManager().GetTexture(textureNames[i]);
			this.textureUnits[i] = ctx.getResourceManager().NextTextureUnit();
			this.textureUnits[i].bind(textures[i]);
			this.textureUnits[i].useInShader(shaderProgram, "tex"+i);
		}
		shaderProgram.unuse();
	}


	private void initBuffers() {
		renderedShape = Triangles.fullscreen();
		renderedShape.setup(shaderProgram);
	}


	@Override
	public void doRender(RenderContext ctx) {

		shaderProgram.use();
		double amount = (Math.sin(Math.toRadians(ctx.getTimer().elapsed() * 100)) + 1.0 ) / 2.0;
		shaderProgram.setFloat("amount", (float) amount);

		renderedShape.render();

		shaderProgram.unuse();
	}

	@Override
	public void dispose() {
		renderedShape.dispose();
		shaderProgram.destroy();
		for (TextureUnit textureUnit : textureUnits) {
			textureUnit.destroy();
		}
	}
}
