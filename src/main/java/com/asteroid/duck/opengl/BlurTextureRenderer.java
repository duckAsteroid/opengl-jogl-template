package com.asteroid.duck.opengl;

import com.asteroid.duck.opengl.util.CompositeRenderItem;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.texture.DataFormat;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import com.asteroid.duck.opengl.util.resources.texture.TextureFactory;
import com.asteroid.duck.opengl.util.resources.texture.TextureOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * A renderer that takes a texture and blurs it using a blur shader.
 * The exact process involves a single offscreen buffer to render the first shader pass (X axis).
 * This is then blurred in Y axis when rendered to the current target using a second pass.
 */
public class BlurTextureRenderer extends CompositeRenderItem {
	public static final String TEXTURE_FBO = "texture_fbo";
	public static final String SHADER_NAME = "blur";
	public static final String X_AXIS = "x";
	private final String textureName;
	private static final Logger LOG = LoggerFactory.getLogger(BlurTextureRenderer.class);
	private final TextureOptions opts;

	public BlurTextureRenderer(String textureName) {
		this(textureName, new TextureOptions(DataFormat.RGBA, Texture.Filter.LINEAR, Texture.Wrap.REPEAT));
	}

	public BlurTextureRenderer(String textureName, TextureOptions options) {
		this.textureName = textureName;
		this.opts = options;
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		ctx.getResourceManager().GetShader("blur1", "blur/vertex.glsl", "blur/frag.glsl", null);
		RenderedItem source = new PassthruTextureRenderer(textureName, "blur1", blur(true), true);
		Texture texture_fbo = TextureFactory.createTexture(ctx.getWindow(), null, opts);
		ctx.getResourceManager().PutTexture(TEXTURE_FBO, texture_fbo);
		OffscreenTextureRenderer stage1 = new OffscreenTextureRenderer(source, texture_fbo);
		PassthruTextureRenderer stage2 = new PassthruTextureRenderer(TEXTURE_FBO, SHADER_NAME, blur(false), true);
		addItems(stage1, stage2);
		super.init(ctx);
	}

	private static Consumer<ShaderProgram> blur(final boolean x) {
		return shaderProgram -> {
			shaderProgram.setBoolean(X_AXIS, x);
			shaderProgram.setBoolean("blur", true);
		};
	}
}
