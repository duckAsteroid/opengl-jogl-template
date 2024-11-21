package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import com.asteroid.duck.opengl.util.resources.texture.TextureUnit;
import org.joml.Vector2f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TranslateTextureRenderer implements RenderedItem {
	private static final Logger LOG = LoggerFactory.getLogger(TranslateTextureRenderer.class);

	private ShaderProgram translationShader;

	private final String textureName;
	private TextureUnit textureUnit;

	private final String translationTableTextureName;
	private TextureUnit translationTableTextureUnit;

	private Triangles renderedShape;

	public TranslateTextureRenderer(String textureName, String translationTableTextureName) {
		this.textureName = textureName;
		this.translationTableTextureName = translationTableTextureName;
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		initShaderProgram(ctx);
		initTextures(ctx);
		initBuffers();
	}

	private void initShaderProgram(RenderContext ctx) throws IOException {
		// load the GLSL Shaders
		this.translationShader = ctx.getResourceManager().getShaderLoader().LoadShaderProgram("translate/vertex.glsl", "translate/frag.glsl", null);
	}

	private void initTextures(RenderContext ctx) {
		translationShader.use();
		// setup the source texture so we can refer to it
		Texture texture = ctx.getResourceManager().GetTexture(textureName);
		this.textureUnit = ctx.getResourceManager().NextTextureUnit();
		this.textureUnit.bind(texture);
		this.textureUnit.useInShader(translationShader, "tex");

		// setup the map texture so we can refer to it
		Texture translationTableTexture = ctx.getResourceManager().GetTexture(translationTableTextureName);
		this.translationTableTextureUnit = ctx.getResourceManager().NextTextureUnit();
		translationTableTextureUnit.bind(translationTableTexture);
		translationTableTextureUnit.useInShader(translationShader, "map");

		// tell the shader our dimensions
		translationShader.setVector2f("dimensions", new Vector2f(texture.Width, texture.Height));
	}


	private void initBuffers() {
		renderedShape = Triangles.fullscreen();
		renderedShape.setup(translationShader);
	}

	@Override
	public void doRender(RenderContext ctx) {
		translationShader.use();
		renderedShape.render();
		translationShader.unuse();
	}

	@Override
	public void dispose() {
		renderedShape.dispose();
		translationShader.destroy();
		textureUnit.destroy();
		translationTableTextureUnit.destroy();
	}
}
