package com.asteroid.duck.opengl.util.blur;

import com.asteroid.duck.opengl.util.AbstractPassthruRenderer;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.shader.ShaderSource;
import com.asteroid.duck.opengl.util.resources.shader.vars.ShaderVariable;
import com.asteroid.duck.opengl.util.resources.shader.vars.ShaderVariables;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import org.joml.Vector2f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Renders a texture to current output with (or without) a gaussian blur in a single dimension (X/Y)
 */
public class BlurTextureRenderer extends AbstractPassthruRenderer {
	private static final Logger LOG = LoggerFactory.getLogger(BlurTextureRenderer.class);

	/** Minimum odd kernel size supported by {@link BlurKernel}. */
	public static final int MIN_KERNEL_SIZE = 3;
	/** Maximum odd kernel size; keeps the discrete tap count within MAX_KERNEL_SIZE in the shader. */
	public static final int MAX_KERNEL_SIZE = 65;

	private boolean blur = true;
	// If true = X, else Y
	private boolean axis = true;
	private final String textureName;
	private final ShaderVariables variables = new ShaderVariables();

	// language=GLSL
	private static final String VERTEX_SHADER = """
			#version 330
			in vec2 screenPosition;
			in vec2 texturePosition;
			out vec2 texCoords;

			void main() {
			    gl_Position = vec4(screenPosition, 0.0, 1.0);
			    texCoords = texturePosition;
			}
			""";

	// language=GLSL
	private static final String FRAGMENT_SHADER = """
			#version 460
			#define MAX_KERNEL_SIZE 64

			precision mediump float;

			uniform sampler2D tex;
			in vec2 texCoords;
			out vec4 fragColor;
			uniform bool x;
			uniform bool blur;
			uniform float multiplier = 0.99;
			uniform int kernelSize;
			uniform float offsets[MAX_KERNEL_SIZE];
			uniform float weights[MAX_KERNEL_SIZE];
			uniform vec2 dimensions;

			void main() {
			    fragColor = texture(tex, texCoords) * (blur ? weights[0] : 1.0);
			    if (blur) {
			        float dimension = x ? dimensions.x : dimensions.y;
			        for (int i = 1; i < kernelSize; i++) {
			            float delta = offsets[i] / dimension;
			            if (x) {
			                fragColor += texture(tex, (texCoords + vec2(delta, 0.0))) * weights[i];
			                fragColor += texture(tex, (texCoords - vec2(delta, 0.0))) * weights[i];
			            } else {
			                fragColor += texture(tex, (texCoords + vec2(0.0, delta))) * weights[i];
			                fragColor += texture(tex, (texCoords - vec2(0.0, delta))) * weights[i];
			            }
			        }
			    }
			    fragColor *= multiplier;
			}
			""";

	private int kernelSize = 29;
	private DiscreteSampleKernel cachedKernel = new BlurKernel(kernelSize).getDiscreteSampleKernel();

	public BlurTextureRenderer(String sourceTexture) {
		this.textureName = sourceTexture;
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		super.init(ctx);
	}

	@Override
	protected Texture initTexture(RenderContext ctx) {
		return ctx.getResourceManager().getTexture(textureName);
	}

	@Override
	protected ShaderProgram initShaderProgram(RenderContext ctx) throws IOException {
		addVariable(ShaderVariable.booleanVariable("blur", this::isBlur));
		addVariable(ShaderVariable.booleanVariable("x", this::isXAxis));
		addVariable(ShaderVariable.vec2fVariable("dimensions", () -> {
			Texture t = ctx.getResourceManager().getTexture(textureName);
			return new Vector2f(t.getWidth(), t.getHeight());
		}));
		addVariable(ShaderVariable.intVariable("kernelSize", ctx2 -> cachedKernel.size()));
		addVariable(ShaderVariable.floatArrayVariable("offsets", () -> cachedKernel.floatOffsets()));
		addVariable(ShaderVariable.floatArrayVariable("weights", () -> cachedKernel.floatWeights()));
		return ShaderProgram.compile(
				ShaderSource.fromClass(VERTEX_SHADER, BlurTextureRenderer.class),
				ShaderSource.fromClass(FRAGMENT_SHADER, BlurTextureRenderer.class),
				null);
	}

	public void addVariable(ShaderVariable<?> var) {
		variables.add(var);
	}

	@Override
	public void doRenderWithShader(RenderContext ctx) {
		variables.updateForRender(ctx, shaderProgram);
		super.doRenderWithShader(ctx);
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

	public int getKernelSize() {
		return kernelSize;
	}

	public void setKernelSize(int size) {
		if (size < MIN_KERNEL_SIZE) size = MIN_KERNEL_SIZE;
		if (size > MAX_KERNEL_SIZE) size = MAX_KERNEL_SIZE;
		if (size % 2 == 0) size++;
		if (size != kernelSize) {
			kernelSize = size;
			cachedKernel = new BlurKernel(kernelSize).getDiscreteSampleKernel();
			LOG.info("Blur kernel size={} ({} discrete samples)", kernelSize, cachedKernel.size());
		}
	}

	public void increaseKernelSize() {
		setKernelSize(kernelSize + 2);
	}

	public void decreaseKernelSize() {
		setKernelSize(kernelSize - 2);
	}
}
