package com.asteriod.duck.opengl;

import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.RenderedItem;
import com.asteriod.duck.opengl.util.Triangles;
import com.asteriod.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteriod.duck.opengl.util.resources.shader.vars.Variable;
import com.asteriod.duck.opengl.util.resources.shader.vars.VariableType;
import com.asteriod.duck.opengl.util.resources.texture.Texture;
import com.asteriod.duck.opengl.util.resources.texture.TextureUnit;
import org.joml.Vector2f;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
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
	private final String shaderName;
	private final Consumer<ShaderProgram> shaderCustomiser;

	public PassthruTextureRenderer(String name) {
		this(name, "passthru", null);
	}

	public PassthruTextureRenderer(String name, String shaderName, Consumer<ShaderProgram> shaderCustomiser) {
		this.textureName = name;
		this.shaderName = shaderName;
		this.shaderCustomiser = shaderCustomiser;
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		initShaderProgram(ctx);
		initTextures(ctx);
		initBuffers();

		if (shaderCustomiser != null) {
			shaderCustomiser.accept(shaderProgram);
		}
	}

	private void initShaderProgram(RenderContext ctx) throws IOException {
		// load the GLSL Shaders
		this.shaderProgram = ctx.getResourceManager().GetShader(shaderName, shaderName+"/vertex.glsl", shaderName+"/frag.glsl", null);
		LOG.info("Using shader program {}, id={}", shaderName, shaderProgram);
	}

	private void initTextures(RenderContext ctx) {
		shaderProgram.use();
		this.texture = ctx.getResourceManager().GetTexture(textureName);
		this.textureUnit = ctx.getResourceManager().NextTextureUnit();
		this.textureUnit.bind(texture);
		this.textureUnit.useInShader(shaderProgram, "tex");
		shaderProgram.setVector2f("dimensions", new Vector2f(texture.Width, texture.Height));
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
