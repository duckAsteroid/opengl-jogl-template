package com.asteriod.duck.opengl;

import com.asteriod.duck.opengl.util.CompositeRenderItem;
import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.RenderedItem;
import com.asteriod.duck.opengl.util.Triangles;
import com.asteriod.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteriod.duck.opengl.util.resources.texture.DataFormat;
import com.asteriod.duck.opengl.util.resources.texture.Texture;
import com.asteriod.duck.opengl.util.resources.texture.TextureFactory;
import com.asteriod.duck.opengl.util.resources.texture.TextureOptions;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.Dimension2D;
import java.io.IOException;
import java.util.function.Consumer;


public class BlurTextureRenderer extends CompositeRenderItem {
	public static final String TEXTURE_FBO = "texture_fbo";
	public static final String SHADER_NAME = "blur";
	public static final String X_AXIS = "x";
	private final String textureName;
	private static final Logger LOG = LoggerFactory.getLogger(BlurTextureRenderer.class);

	public BlurTextureRenderer(String textureName) {
		this.textureName = textureName;
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		ctx.getResourceManager().GetShader("blur1", "blur/vertex.glsl", "blur/frag.glsl", null);
		RenderedItem source = new PassthruTextureRenderer(textureName, "blur1", blur(true), true);
		TextureOptions opts = new TextureOptions(DataFormat.RGBA, Texture.Filter.LINEAR, Texture.Wrap.REPEAT);
		Texture texture_fbo = TextureFactory.createTexture(ctx.getWindow(), false);
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
