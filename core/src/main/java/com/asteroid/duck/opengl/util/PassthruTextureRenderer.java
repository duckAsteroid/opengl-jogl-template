package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Takes a texture and renders it using a fullscreen quad (two triangles)
 */
public class PassthruTextureRenderer extends AbstractPassthruRenderer implements RenderedItem {

	private static final Logger LOG = LoggerFactory.getLogger(PassthruTextureRenderer.class);

	private final String shaderName;
	protected final String textureName;

	public PassthruTextureRenderer(String textureName) {
		this(textureName, "passthru");
	}

	public PassthruTextureRenderer(String textureName, String shaderName){
		this.textureName = textureName;
		this.shaderName = shaderName;
	}

	protected ShaderProgram initShaderProgram(RenderContext ctx) throws IOException {
		return ctx.getResourceManager().getSimpleShader(shaderName);
	}

	@Override
	protected Texture initTexture(RenderContext ctx) {
		return ctx.getResourceManager().getTexture(textureName);
	}

	@Override
	public void doRenderWithShader(RenderContext ctx) {
		super.doRenderWithShader(ctx);
	}
}
