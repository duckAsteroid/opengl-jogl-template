package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.*;
import com.asteroid.duck.opengl.util.audio.Polyline;
import com.asteroid.duck.opengl.util.blur.BlurTextureRenderer;
import com.asteroid.duck.opengl.util.blur.OffscreenBlurTextureRenderer;
import com.asteroid.duck.opengl.util.keys.KeyCombination;
import com.asteroid.duck.opengl.util.keys.KeyRegistry;
import com.asteroid.duck.opengl.util.palette.PaletteRenderer;
import com.asteroid.duck.opengl.util.resources.shader.vars.ShaderVariable;
import com.asteroid.duck.opengl.util.resources.texture.*;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;

public class SoundWave extends CompositeRenderItem implements Experiment {
	public enum Blur {
		BOTH, X, Y, NONE;
	}

	private float muliplier = 0.99f;
	private Blur blurMode = Blur.BOTH;

	@Override
	public String getDescription() {
		return "Renders an audio wave on screen";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		registerKeys(ctx.getKeyRegistry());
    TextureOptions opts = new TextureOptions(DataFormat.GRAY, Texture.Filter.LINEAR, Texture.Wrap.REPEAT);
		Texture offscreen1 = TextureFactory.createTexture(ctx.getWindow(), null, opts);
		ctx.getResourceManager().PutTexture("offscreen1", offscreen1);
		Texture offscreen2 = TextureFactory.createTexture(ctx.getWindow(), null, opts);
		ctx.getResourceManager().PutTexture("offscreen2", offscreen2);

		// first stage blur to offscreen texture
		BlurTextureRenderer stage1 = new BlurTextureRenderer("offscreen1", false);
		stage1.setXAxis(true);
		stage1.addVariable(ShaderVariable.booleanVariable("blur", () -> this.blurMode == Blur.X || this.blurMode == Blur.BOTH));
		OffscreenTextureRenderer offscreenRender = new OffscreenTextureRenderer(stage1, offscreen2);
		addItem(offscreenRender);

		// seconds stage
		BlurTextureRenderer stage2 = new BlurTextureRenderer("offscreen2", false);
		stage2.setXAxis(false);
		stage2.addVariable(ShaderVariable.floatVariable("multiplier", this::multiplier));
		stage2.addVariable(ShaderVariable.booleanVariable("blur", () -> this.blurMode == Blur.Y || this.blurMode == Blur.BOTH));
		OffscreenTextureRenderer offscreenRender2 = new OffscreenTextureRenderer(stage2, offscreen1);
		addItem(offscreenRender2);

		Polyline waveform = new Polyline();
		OffscreenTextureRenderer waveRender = new OffscreenTextureRenderer(waveform, offscreen1);
		addItem(waveRender);


		SwitchableRenderItem switcher = new SwitchableRenderItem();

		PassthruTextureRenderer passThruRenderer = new PassthruTextureRenderer("offscreen1");
		switcher.addItem(passThruRenderer);

		Texture palette = ctx.getResourceManager().GetTexture("palette", "palettes/FIRE2.MAP.png", ImageOptions.DEFAULT.withSingleLine());
		PaletteRenderer paletteRenderer = new PaletteRenderer("offscreen1");
		switcher.addItem(paletteRenderer);

		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('R'), switcher::next, "Switch final render");
		addItem(switcher);

		super.init(ctx);
	}

	private void registerKeys(KeyRegistry ctx) {
		ctx.registerKeyAction(GLFW.GLFW_KEY_W, () -> multiply(1.001f), "Increase blur brightness by 1%");
		ctx.registerKeyAction(GLFW.GLFW_KEY_W, GLFW.GLFW_MOD_SHIFT, () -> multiply(1.1f), "Increase blur brightness by 10%");
		ctx.registerKeyAction(GLFW.GLFW_KEY_S, () -> multiply(0.999f), "Decrease blur brightness by 1%");
		ctx.registerKeyAction(GLFW.GLFW_KEY_S, GLFW.GLFW_MOD_SHIFT, () -> multiply(0.9f), "Decrease blur brightness by 10%");
		ctx.registerKeyAction(GLFW.GLFW_KEY_B, this::toggleBlur, "Toggle blur [Both, X, Y, None]");
	}

	private void toggleBlur() {
		switch (blurMode) {
			case BOTH:
				blurMode = Blur.X;
				break;
      case X:
        blurMode = Blur.Y;
        break;
      case Y:
        blurMode = Blur.NONE;
        break;
      case NONE:
        blurMode = Blur.BOTH;
        break;
    }
		System.out.println("blur="+blurMode);
	}

	private void multiply(float v) {
		this.muliplier *= v;
		System.out.println("multiplier="+muliplier);
	}

	public float multiplier() {
		return muliplier;
	}

}
