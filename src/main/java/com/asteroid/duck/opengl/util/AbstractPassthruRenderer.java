package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import com.asteroid.duck.opengl.util.resources.texture.TextureUnit;
import org.joml.Vector2f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Takes a texture and renders it using a fullscreen quad (two triangles)
 */
public abstract class AbstractPassthruRenderer implements RenderedItem {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractPassthruRenderer.class);

	protected ShaderProgram shaderProgram = null;

	protected Texture texture;
	private TextureUnit textureUnit;
	private Triangles renderedShape;
	private final List<BiConsumer<RenderContext, ShaderProgram>> variables = new ArrayList<>();

	@Override
	public void init(RenderContext ctx) throws IOException {
		this.shaderProgram = initShaderProgram(ctx);
		this.texture = initTexture(ctx);
		this.textureUnit = initTextureUnit(ctx);
		this.renderedShape = initBuffers();
	}

	public void addVariable(BiConsumer<RenderContext, ShaderProgram> variable) {
		variables.add(variable);
	}

	public void removeVariable(BiConsumer<RenderContext, ShaderProgram> variable) {
		variables.remove(variable);
	}

	protected abstract Texture initTexture(RenderContext ctx);

	protected abstract ShaderProgram initShaderProgram(RenderContext ctx) throws IOException;

	protected TextureUnit initTextureUnit(RenderContext ctx) {
		shaderProgram.use();
		TextureUnit textureUnit = ctx.getResourceManager().NextTextureUnit();
		textureUnit.bind(texture);
		textureUnit.useInShader(shaderProgram, "tex");
		shaderProgram.setVector2f("dimensions", new Vector2f(texture.Width, texture.Height));
		for(BiConsumer<RenderContext, ShaderProgram> var : variables) {
			var.accept(ctx, shaderProgram);
		}
		return textureUnit;
	}

	protected Triangles initBuffers() {
		Triangles renderedShape = Triangles.fullscreen();
		renderedShape.setup(shaderProgram);
		return renderedShape;
	}

	@Override
	public void doRender(RenderContext ctx) {
		shaderProgram.use();
		for(BiConsumer<RenderContext, ShaderProgram> var : variables) {
			var.accept(ctx, shaderProgram);
		}
		doRenderWithShader(ctx);
		shaderProgram.unuse();
	}

	public void doRenderWithShader(RenderContext ctx) {
		renderedShape.render();
	}

	@Override
	public void dispose() {
		renderedShape.dispose();
		shaderProgram.destroy();
		textureUnit.destroy();
	}
}
