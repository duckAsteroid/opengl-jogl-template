package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Takes a texture and renders it using a fullscreen quad (two triangles)
 */
public class PassthruTextureRenderer extends AbstractPassthruRenderer implements RenderedItem {

	private static final Logger LOG = LoggerFactory.getLogger(PassthruTextureRenderer.class);

	private final String shaderName;
	private final String textureName;

	public PassthruTextureRenderer(String textureName) {
		this(textureName, "passthru");
	}

	public PassthruTextureRenderer(String textureName, String shaderName){
		this.textureName = textureName;
		this.shaderName = shaderName;
	}

	protected ShaderProgram initShaderProgram(RenderContext ctx) throws IOException {
		// load the GLSL Shaders
		ShaderProgram loaded = ctx.getResourceManager().getShaderLoader().LoadSimpleShaderProgram(shaderName);
		LOG.info("Using shader program {}, id={}", shaderName, loaded);
		return loaded;
	}

	@Override
	protected Texture initTexture(RenderContext ctx) {
		return ctx.getResourceManager().GetTexture(textureName);
	}

	@Override
	public void doRenderWithShader(RenderContext ctx) {
		super.doRenderWithShader(ctx);
	}
}
