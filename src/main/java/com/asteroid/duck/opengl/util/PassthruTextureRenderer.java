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
	/**
	 * This customiser gives external code a chance to initialise (set parameters) on the shader
	 */
	private final Consumer<ShaderProgram> shaderCustomiser;
	private final boolean customizeOnRender;

	public PassthruTextureRenderer(String name) {
		this(name, "passthru", null, false);
	}

	public PassthruTextureRenderer(String name, String shaderName) {
		this(name, shaderName, null, false);
	}

	public PassthruTextureRenderer(String textureName, String shaderName, Consumer<ShaderProgram> shaderCustomiser) {
		this(textureName, shaderName, shaderCustomiser, false);
	}

	public PassthruTextureRenderer(String textureName, String shaderName, Consumer<ShaderProgram> shaderCustomiser, boolean customizeOnRender) {
		this.textureName = textureName;
		this.shaderName = shaderName;
		this.shaderCustomiser = shaderCustomiser;
		this.customizeOnRender = customizeOnRender;
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		super.init(ctx);
		// customise the shader if setup
		if (shaderCustomiser != null) {
			shaderCustomiser.accept(shaderProgram);
		}
	}

	protected ShaderProgram initShaderProgram(RenderContext ctx) throws IOException {
		// load the GLSL Shaders
		ShaderProgram loaded = ctx.getResourceManager().GetShader(shaderName, shaderName+"/vertex.glsl", shaderName+"/frag.glsl", null);
		LOG.info("Using shader program {}, id={}", shaderName, loaded);
		return loaded;
	}

	@Override
	protected Texture initTexture(RenderContext ctx) {
		return ctx.getResourceManager().GetTexture(textureName);
	}

	@Override
	public void doRenderWithShader(RenderContext ctx) {
		// customise the shader if setup
		if (customizeOnRender && shaderCustomiser != null) {
			shaderCustomiser.accept(shaderProgram);
		}
		super.doRenderWithShader(ctx);
	}
}
