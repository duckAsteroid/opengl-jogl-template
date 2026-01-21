package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.geom.Triangles;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import com.asteroid.duck.opengl.util.resources.textureunit.TextureUnit;
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
		initBuffers(ctx);
	}

	private void initShaderProgram(RenderContext ctx) throws IOException {
		// load the GLSL Shaders
		this.shaderProgram = ctx.getResourceManager().getShader("multi-tex", "multi-tex/vert.glsl", "multi-tex/frag.glsl", null);
		LOG.info("Using shader program {}", shaderProgram);
	}

	private void initTextures(RenderContext ctx) {
		this.textures = new Texture[textureNames.length];
		this.textureUnits = new TextureUnit[textureNames.length];
		shaderProgram.use(ctx);
		for (int i = 0; i < textureNames.length; i++) {
			this.textures[i] = ctx.getResourceManager().getTexture(textureNames[i]);
			this.textureUnits[i] = ctx.getResourceManager().nextTextureUnit();
			this.textureUnits[i].bind(textures[i]);
			this.textureUnits[i].useInShader(shaderProgram, "tex"+i);
		}
	}


	private void initBuffers(RenderContext ctx) throws IOException {
		renderedShape = Triangles.fullscreen();
		renderedShape.setShaderProgram(shaderProgram);
        renderedShape.init(ctx);
	}


	@Override
	public void doRender(RenderContext ctx) {

		shaderProgram.use(ctx);
		double amount = (Math.sin(Math.toRadians(ctx.getTimer().elapsed() * 100)) + 1.0 ) / 2.0;
		shaderProgram.uniforms().get("amount", Float.class).set((float) amount);

		renderedShape.doRender(ctx);
	}

	@Override
	public void dispose() {
		renderedShape.dispose();
		shaderProgram.dispose();
		for (TextureUnit textureUnit : textureUnits) {
			textureUnit.dispose();
		}
	}
}
