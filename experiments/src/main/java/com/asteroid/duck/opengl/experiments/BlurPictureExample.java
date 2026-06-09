package com.asteroid.duck.opengl.experiments;


import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.blur.OffscreenBlurTextureRenderer;
import com.asteroid.duck.opengl.util.keys.KeyRegistry;
import com.asteroid.duck.opengl.util.resources.texture.io.ImageLoadingOptions;
import com.asteroid.duck.opengl.util.resources.texture.Texture;

import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;

public class BlurPictureExample extends OffscreenBlurTextureRenderer implements Experiment {

	@Override
	public String getDescription() {
		return "Shows a simple picture blurred on screen using blur kernel implemented in shaders";
	}

	public BlurPictureExample() {
		super("window", 15, STANDARD_TEXTURE_OPTS);
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		Texture window = ctx.getResourceManager().getTexture("window", "test-card.jpeg", ImageLoadingOptions.DEFAULT);
		super.init(ctx);
		registerKeys(ctx.getKeyRegistry());
	}

	private void registerKeys(KeyRegistry kr) {
		kr.registerKeyAction(GLFW_KEY_W, () -> multiply(1.001f), "Increase blur brightness by 1%");
		kr.registerKeyAction(GLFW_KEY_W, GLFW_MOD_SHIFT, () -> multiply(1.1f), "Increase blur brightness by 10%");
		kr.registerKeyAction(GLFW_KEY_S, () -> multiply(0.999f), "Decrease blur brightness by 1%");
		kr.registerKeyAction(GLFW_KEY_S, GLFW_MOD_SHIFT, () -> multiply(0.9f), "Decrease blur brightness by 10%");
		kr.registerKeyAction(GLFW_KEY_B, this::toggleBlur, "Toggle blur on/off");
		kr.registerKeyAction(GLFW_KEY_RIGHT_BRACKET, this::increaseKernelSize, "Increase blur kernel size");
		kr.registerKeyAction(GLFW_KEY_LEFT_BRACKET, this::decreaseKernelSize, "Decrease blur kernel size");
	}
}
