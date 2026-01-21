package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.geom.Triangles;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.shader.vars.ShaderVariable;
import com.asteroid.duck.opengl.util.resources.shader.vars.ShaderVariables;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import com.asteroid.duck.opengl.util.resources.textureunit.TextureUnit;
import org.joml.Vector2f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A renderer that translates a texture using a translation table (a special texture).
 * A translation table is a 2D texture where each pixel's value indicates the coordinates
 * in the source texture to sample from.
 */
public class TranslateTextureRenderer extends AbstractPassthruRenderer {
	private static final Logger LOG = LoggerFactory.getLogger(TranslateTextureRenderer.class);

	private final String textureName;

	private final String translationTableTextureName;
	private TextureUnit translationTableTextureUnit;

	private final ShaderVariables variables = new ShaderVariables();

	public TranslateTextureRenderer(String textureName, String translationTableTextureName) {
		this.textureName = textureName;
		this.translationTableTextureName = translationTableTextureName;
	}

	protected ShaderProgram initShaderProgram(RenderContext ctx) throws IOException {
		// load the GLSL Shaders
		return ctx.getResourceManager().getShaderLoader().LoadSimpleShaderProgram("translate");
	}

	protected Texture initTexture(RenderContext ctx) {
		shaderProgram.use(ctx);

		// setup the map texture so we can refer to it
		Texture translationTableTexture = ctx.getResourceManager().getTexture(translationTableTextureName);
		this.translationTableTextureUnit = ctx.getResourceManager().nextTextureUnit();
		translationTableTextureUnit.bind(translationTableTexture);
		translationTableTextureUnit.useInShader(shaderProgram, "map");

		// tell the shader our dimensions
		variables.add(ShaderVariable.vec2fVariable("dimensions", this::dimensions));

		// setup the source texture so we can refer to it
		return ctx.getResourceManager().getTexture(textureName);
	}

	private Vector2f dimensions() {
		return new Vector2f(texture.getWidth(), texture.getHeight());
	}

	@Override
	public void doRenderWithShader(RenderContext ctx) {
		variables.updateForRender(ctx, shaderProgram);
		super.doRenderWithShader(ctx);
	}

	@Override
	public void dispose() {
		translationTableTextureUnit.dispose();
		super.dispose();
	}
}
