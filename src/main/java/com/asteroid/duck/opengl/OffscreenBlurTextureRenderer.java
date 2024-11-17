package com.asteroid.duck.opengl;

import com.asteroid.duck.opengl.util.CompositeRenderItem;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.keys.KeyRegistry;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.texture.DataFormat;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import com.asteroid.duck.opengl.util.resources.texture.TextureFactory;
import com.asteroid.duck.opengl.util.resources.texture.TextureOptions;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * A renderer that takes a texture and blurs it using a blur shader.
 * The exact process involves a single offscreen buffer to render the first shader pass (X axis).
 * This is then blurred in Y axis when rendered to the current target using a second pass.
 */
public class OffscreenBlurTextureRenderer extends CompositeRenderItem {
	public static final String TEXTURE_FBO = "texture_fbo";
	public static final String SHADER_NAME = "blur";
	public static final String X_AXIS = "x";

	private static final Logger LOG = LoggerFactory.getLogger(OffscreenBlurTextureRenderer.class);
	private static float multiplier = 0.99f;

	private final TextureOptions opts;
	private final String sourceTextureName;
	private final String targetTextureName;

	public OffscreenBlurTextureRenderer(String source, String target) {
		this(source, target, new TextureOptions(DataFormat.RGBA, Texture.Filter.LINEAR, Texture.Wrap.REPEAT));
	}

	public OffscreenBlurTextureRenderer(String source, String target, TextureOptions options) {
		this.sourceTextureName = source;
		this.targetTextureName = target;
		this.opts = options;
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		ctx.getResourceManager().GetShader("blur1", "blur/vertex.glsl", "blur/frag.glsl", null);
		RenderedItem source = new PassthruTextureRenderer(sourceTextureName, "blur1", blur(true), true);
		Texture texture_fbo = TextureFactory.createTexture(ctx.getWindow(), null, opts);
		ctx.getResourceManager().PutTexture(TEXTURE_FBO, texture_fbo);
		OffscreenTextureRenderer stage1 = new OffscreenTextureRenderer(source, texture_fbo);
		Texture target = ctx.getResourceManager().GetTexture(targetTextureName);
		OffscreenTextureRenderer stage2 = new OffscreenTextureRenderer(new PassthruTextureRenderer(TEXTURE_FBO, SHADER_NAME, blur(false), true), target);
		addItems(stage1, stage2);

		registerKeys(ctx.getKeyRegistry());

		super.init(ctx);
	}

	private void registerKeys(KeyRegistry ctx) {
		ctx.registerKeyAction(GLFW.GLFW_KEY_W, () -> multiply(1.001f), "Increase blur brightness by 1%");
		ctx.registerKeyAction(GLFW.GLFW_KEY_W, GLFW.GLFW_MOD_SHIFT, () -> multiply(1.1f), "Increase blur brightness by 10%");
		ctx.registerKeyAction(GLFW.GLFW_KEY_S, () -> multiply(0.999f), "Decrease blur brightness by 1%");
		ctx.registerKeyAction(GLFW.GLFW_KEY_S, GLFW.GLFW_MOD_SHIFT, () -> multiply(0.9f), "Decrease blur brightness by 10%");

	}

	private void multiply(float v) {
		multiplier *= v;
		LOG.info("multiplier={}", multiplier);
	}

	private static Consumer<ShaderProgram> blur(final boolean x) {
		return shaderProgram -> {
			shaderProgram.setBoolean(X_AXIS, x);
			shaderProgram.setBoolean("blur", true);
			shaderProgram.setFloat("multiplier", multiplier);
		};
	}
}
