package com.asteriod.duck.opengl;

import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.RenderedItem;
import com.asteriod.duck.opengl.util.Triangles;
import com.asteriod.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteriod.duck.opengl.util.resources.texture.Texture;
import com.asteriod.duck.opengl.util.resources.texture.TextureUnit;
import org.joml.Vector2f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL20.glUseProgram;

/**
 * Takes a texture and renders it using a fullscreen quad (two triangles)
 */
public class TestRenderer implements RenderedItem {

	private static final Logger LOG = LoggerFactory.getLogger(TestRenderer.class);

	private final String shaderName;
	private ShaderProgram shaderProgram = null;

	private Triangles renderedShape;
	/**
	 * This customiser gives external code a chance to initialise (set parameters) on the shader
	 */
	private final Consumer<ShaderProgram> shaderCustomiser;

	public TestRenderer() {
		this(null);
	}

	public TestRenderer(Consumer<ShaderProgram> shaderCustomiser) {
		this.shaderName = "test";
		this.shaderCustomiser = shaderCustomiser;
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		initShaderProgram(ctx);
		initBuffers();
		// customise the shader if setup
		if (shaderCustomiser != null) {
			shaderCustomiser.accept(shaderProgram);
		}
	}

	private void initShaderProgram(RenderContext ctx) throws IOException {
		// load the GLSL Shaders
		this.shaderProgram = ctx.getResourceManager().GetShader(shaderName, shaderName+"/vertex.glsl", shaderName+"/frag.glsl", null);
		LOG.info("Using shader program {}, id={}", shaderName, shaderProgram);
	}

	private void initBuffers() {
		renderedShape = Triangles.fullscreen();
		renderedShape.setup(shaderProgram);
	}

	@Override
	public void doRender(RenderContext ctx) {
		shaderProgram.use();
		renderedShape.render();
		shaderProgram.unuse();
	}

	@Override
	public void dispose() {
		renderedShape.dispose();
		shaderProgram.destroy();
	}
}
