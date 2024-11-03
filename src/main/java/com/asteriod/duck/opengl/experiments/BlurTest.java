package com.asteriod.duck.opengl.experiments;

import com.asteriod.duck.opengl.PassthruTextureRenderer;
import com.asteriod.duck.opengl.util.CompositeRenderItem;
import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteriod.duck.opengl.util.resources.texture.ImageOptions;

import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;

public class BlurTest extends CompositeRenderItem implements Experiment {
	private boolean blur;
	private boolean axis;
	@Override
	public String getDescription() {
		return "Quick test at a simple single pass with blur shader";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		ctx.getResourceManager().GetTexture("window", "window.jpeg", ImageOptions.DEFAULT);
		PassthruTextureRenderer renderer = new PassthruTextureRenderer("window", "blur");
		addItem(renderer);
		ctx.registerKeyAction(GLFW_KEY_B, this::toggleBlur);
		ctx.registerKeyAction(GLFW_KEY_X, this::toggleAxis);
		renderer.addShaderCustomizer(this::blurToShader, true);
		super.init(ctx);
	}

	private void blurToShader(ShaderProgram shader) {
		shader.setBoolean("x", axis);
		shader.setBoolean("blur", blur);
	}

	public void toggleBlur() {
		blur = !blur;
		System.out.println("Blur="+blur);
	}

	public void toggleAxis() {
		axis = !axis;
		System.out.println("Axis="+( axis ? "x" : "y"));
	}

	@Override
	public void doRender(RenderContext ctx) {
		super.doRender(ctx);
	}
}
