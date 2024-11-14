package com.asteroid.duck.opengl;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.Triangles;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import com.asteroid.duck.opengl.util.resources.texture.TextureUnit;
import org.joml.Vector2f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

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
