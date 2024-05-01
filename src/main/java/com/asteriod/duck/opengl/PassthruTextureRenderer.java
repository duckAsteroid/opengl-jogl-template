package com.asteriod.duck.opengl;

import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.RenderedItem;
import com.asteriod.duck.opengl.util.Triangles;
import com.asteriod.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteriod.duck.opengl.util.resources.shader.vars.Variable;
import com.asteriod.duck.opengl.util.resources.shader.vars.VariableType;
import com.asteriod.duck.opengl.util.resources.texture.Texture;
import com.asteriod.duck.opengl.util.resources.texture.TextureUnit;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F5;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.system.MemoryUtil.NULL;

public class PassthruTextureRenderer implements RenderedItem {

	private static final Logger LOG = LoggerFactory.getLogger(PassthruTextureRenderer.class);

	private ShaderProgram shaderProgram = null;

	private int vbo;
	private int vao;
	private final String textureName;
	private Texture texture;
	private TextureUnit textureUnit;
	private Triangles renderedShape;

	public PassthruTextureRenderer(String name) {
		this.textureName = name;
	}
	@Override
	public void init(RenderContext ctx) throws IOException {
		initShaderProgram(ctx);
		initTextures(ctx);
		initBuffers();
	}

	private void initShaderProgram(RenderContext ctx) throws IOException {
		// load the GLSL Shaders
		this.shaderProgram = ctx.getResourceManager().GetShader("passthru", "passthru/vertex.glsl", "passthru/frag.glsl", null);
		LOG.info("Using shader program {}", shaderProgram);
	}

	private void initTextures(RenderContext ctx) {
		shaderProgram.use();
		this.texture = ctx.getResourceManager().GetTexture(textureName);
		this.textureUnit = TextureUnit.index(3);
		this.textureUnit.bind(texture);
		this.textureUnit.useInShader(shaderProgram, "tex");
	}


	private void initBuffers() {
		renderedShape = Triangles.fullscreen();
		renderedShape.setup(shaderProgram);
	}

	@Override
	public void doRender(RenderContext ctx) {
		shaderProgram.use();
		renderedShape.render();
		glUseProgram(0);
	}

	@Override
	public void dispose() {
		renderedShape.dispose();
		shaderProgram.destroy();
	}
}
