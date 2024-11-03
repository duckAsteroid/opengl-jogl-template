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

/**
 * Takes a texture and renders it using a fullscreen quad (two triangles)
 */
public class PassthruTextureRenderer implements RenderedItem {

	private static final Logger LOG = LoggerFactory.getLogger(PassthruTextureRenderer.class);

	protected ShaderProgram shaderProgram = null;

	private final String textureName;
	private Texture texture;
	private TextureUnit textureUnit;
	private Triangles renderedShape;
	private final String shaderName;
	/**
	 * This customiser gives external code a chance to initialise (set parameters) on the shader
	 */
	private Consumer<ShaderProgram> shaderCustomiser;
	private boolean customizeOnRender;

	public PassthruTextureRenderer(String name) {
		this(name, "passthru", null, false);
	}

	public PassthruTextureRenderer(String name, String shaderName) {
		this(name, shaderName, null, false);
	}

	public PassthruTextureRenderer(String textureName, String shaderName, Consumer<ShaderProgram> shaderCustomiser) {
		this(textureName, shaderName, shaderCustomiser, false);
	}

	public PassthruTextureRenderer(String textureName, String shaderName, Consumer<ShaderProgram> shaderCustomiser, boolean customizeOnRender) {
		this.textureName = textureName;
		this.shaderName = shaderName;
		this.shaderCustomiser = shaderCustomiser;
		this.customizeOnRender = customizeOnRender;
	}

	public void addShaderCustomizer(Consumer<ShaderProgram> customizer, boolean onRender) {
		this.shaderCustomiser = customizer;
		this.customizeOnRender = onRender;
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		initShaderProgram(ctx);
		initTextures(ctx);
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
		// customise the shader if setup
		if (customizeOnRender && shaderCustomiser != null) {
			shaderCustomiser.accept(shaderProgram);
		}
		renderedShape.render();
		shaderProgram.unuse();
	}

	@Override
	public void dispose() {
		renderedShape.dispose();
		shaderProgram.destroy();
		textureUnit.destroy();
	}
}
