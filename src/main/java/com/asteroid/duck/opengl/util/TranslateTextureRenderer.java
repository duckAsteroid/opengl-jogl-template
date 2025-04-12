package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.geom.Triangles;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.shader.vars.ShaderVariable;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import com.asteroid.duck.opengl.util.resources.texture.TextureUnit;
import org.joml.Vector2f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TranslateTextureRenderer extends AbstractPassthruRenderer {
	private static final Logger LOG = LoggerFactory.getLogger(TranslateTextureRenderer.class);

	private final String textureName;

	private final String translationTableTextureName;
	private TextureUnit translationTableTextureUnit;

	private Triangles renderedShape;

	public TranslateTextureRenderer(String textureName, String translationTableTextureName) {
		this.textureName = textureName;
		this.translationTableTextureName = translationTableTextureName;
	}

	protected ShaderProgram initShaderProgram(RenderContext ctx) throws IOException {
		// load the GLSL Shaders
		return ctx.getResourceManager().getShaderLoader().LoadSimpleShaderProgram("translate");
	}

	protected Texture initTexture(RenderContext ctx) {
		shaderProgram.use();

		// setup the map texture so we can refer to it
		Texture translationTableTexture = ctx.getResourceManager().GetTexture(translationTableTextureName);
		this.translationTableTextureUnit = ctx.getResourceManager().NextTextureUnit();
		translationTableTextureUnit.bind(translationTableTexture);
		translationTableTextureUnit.useInShader(shaderProgram, "map");

		// tell the shader our dimensions
		addVariable(ShaderVariable.vec2fVariable("dimensions", this::dimensions));

		// setup the source texture so we can refer to it
		return ctx.getResourceManager().GetTexture(textureName);
	}

	private Vector2f dimensions() {
		return new Vector2f(texture.Width, texture.Height);
	}

	@Override
	public void dispose() {
		translationTableTextureUnit.destroy();
		super.dispose();
	}
}
