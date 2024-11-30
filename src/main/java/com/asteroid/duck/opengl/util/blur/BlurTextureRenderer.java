package com.asteroid.duck.opengl.util.blur;

import com.asteroid.duck.opengl.util.AbstractPassthruRenderer;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.shader.vars.ShaderVariable;
import com.asteroid.duck.opengl.util.resources.texture.Texture;

import java.io.IOException;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_B;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_X;

/**
 * Renders a texture to current output with (or without) a gaussian blur in a single dimension (X/Y)
 */
public class BlurTextureRenderer extends AbstractPassthruRenderer {
	private boolean blur = true;
	// If true = X, else Y
	private boolean axis = true;
	private final String textureName;
	private final boolean registerKeys;

	public BlurTextureRenderer(String sourceTexture) {
		this(sourceTexture, true);
	}
	public BlurTextureRenderer(String sourceTexture, boolean registerKeys) {
		this.textureName = sourceTexture;
		this.registerKeys = registerKeys;
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		if (registerKeys) {
			ctx.getKeyRegistry().registerKeyAction(GLFW_KEY_B, this::toggleBlur, "Toggle blurring on/off");
			ctx.getKeyRegistry().registerKeyAction(GLFW_KEY_X, this::toggleAxis, "Toggle X/Y axis blurring");
		}
		super.init(ctx);
	}

	@Override
	protected Texture initTexture(RenderContext ctx) {
		return ctx.getResourceManager().GetTexture(textureName);
	}

	@Override
	protected ShaderProgram initShaderProgram(RenderContext ctx) throws IOException {
		addVariable(ShaderVariable.booleanVariable("blur", this::isBlur));
		addVariable(ShaderVariable.booleanVariable("x", this::isXAxis));
		return ctx.getResourceManager().getShaderLoader().LoadSimpleShaderProgram("blur");
	}

	public boolean isBlur() {
    return blur;
  }

	public void setBlur(boolean blur) {
		this.blur = blur;
	}

	public boolean isXAxis() {
    return axis;
  }

	public void setXAxis(boolean axis) {
    this.axis = axis;
  }

	public void toggleBlur() {
		blur = !blur;
		System.out.println("Blur="+blur);
	}

	public void toggleAxis() {
		axis = !axis;
		System.out.println("Axis="+( axis ? "x" : "y"));
	}
}
