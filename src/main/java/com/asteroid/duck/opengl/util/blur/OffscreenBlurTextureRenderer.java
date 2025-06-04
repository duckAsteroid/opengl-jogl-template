package com.asteroid.duck.opengl.util.blur;

import com.asteroid.duck.opengl.util.*;
import com.asteroid.duck.opengl.util.keys.KeyRegistry;
import com.asteroid.duck.opengl.util.resources.shader.vars.ShaderVariable;
import com.asteroid.duck.opengl.util.resources.texture.*;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A renderer that takes a texture and blurs it using a blur shader.
 * The exact process involves a single offscreen buffer to render the first shader pass (X axis).
 * This is then blurred in Y axis when rendered to the current target using a second pass.
 */
public class OffscreenBlurTextureRenderer extends CompositeRenderItem {
	private static final Logger LOG = LoggerFactory.getLogger(OffscreenBlurTextureRenderer.class);
	public static final String TEXTURE_FBO = "texture_fbo";
	private float multiplier = 0.99f;

	private final TextureOptions opts;
	private final String sourceTextureName;
	private BlurTextureRenderer stage1;
	private BlurTextureRenderer stage2;

	public OffscreenBlurTextureRenderer(String source) {
		this(source, new TextureOptions(DataFormat.RGBA, Filter.LINEAR, Wrap.REPEAT));
	}

	public OffscreenBlurTextureRenderer(String source, TextureOptions options) {
		this.sourceTextureName = source;
		this.opts = options;
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		registerKeys(ctx.getKeyRegistry());

		// first stage blur to offscreen texture
		this.stage1 = new BlurTextureRenderer(sourceTextureName, false);
		stage1.setXAxis(true);
		stage1.addVariable(ShaderVariable.floatVariable("multiplier", this::multiplier));

		Texture offscreen = TextureFactory.createTexture(ctx.getWindow(), null, opts);
		ctx.getResourceManager().PutTexture(TEXTURE_FBO, offscreen);
		OffscreenTextureRenderer offscreenRender = new OffscreenTextureRenderer(stage1, offscreen);
		addItem(offscreenRender);

		// seconds stage
		this.stage2 = new BlurTextureRenderer(TEXTURE_FBO, false);
		stage2.setXAxis(false);
		stage2.addVariable(ShaderVariable.floatVariable("multiplier", this::multiplier));
    addItem(stage2);

		super.init(ctx);
	}

	private void registerKeys(KeyRegistry ctx) {
		ctx.registerKeyAction(GLFW.GLFW_KEY_W, () -> multiply(1.001f), "Increase blur brightness by 1%");
		ctx.registerKeyAction(GLFW.GLFW_KEY_W, GLFW.GLFW_MOD_SHIFT, () -> multiply(1.1f), "Increase blur brightness by 10%");
		ctx.registerKeyAction(GLFW.GLFW_KEY_S, () -> multiply(0.999f), "Decrease blur brightness by 1%");
		ctx.registerKeyAction(GLFW.GLFW_KEY_S, GLFW.GLFW_MOD_SHIFT, () -> multiply(0.9f), "Decrease blur brightness by 10%");
		ctx.registerKeyAction(GLFW.GLFW_KEY_B, this::toggleBlur, "Toggle blur on/off");
	}

	private float multiplier() {
		return multiplier;
	}

	private void multiply(float v) {
		multiplier *= v;
		LOG.info("multiplier={}", multiplier);
	}

	private void toggleBlur() {
    stage1.toggleBlur();
		stage2.toggleBlur();
  }
}
