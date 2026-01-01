package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.geom.Rectangle;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import com.asteroid.duck.opengl.util.resources.texture.TextureUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.glDrawElements;

/**
 * A base class for rendered items that render a texture using a quad (two triangles)
 */
public abstract class AbstractPassthruRenderer implements RenderedItem {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractPassthruRenderer.class);

	protected ShaderProgram shaderProgram = null;

	protected Texture texture;
	private TextureUnit textureUnit;
	private Rectangle renderedShape;

	@Override
	public void init(RenderContext ctx) throws IOException {
		this.shaderProgram = initShaderProgram(ctx);
		this.texture = initTexture(ctx);
		this.textureUnit = initTextureUnit(ctx);
		this.renderedShape = initBuffers(ctx);
	}

	protected abstract Texture initTexture(RenderContext ctx);

	protected abstract ShaderProgram initShaderProgram(RenderContext ctx) throws IOException;

	protected TextureUnit initTextureUnit(RenderContext ctx) {
		shaderProgram.use();
		TextureUnit textureUnit = ctx.getResourceManager().nextTextureUnit();
		textureUnit.bind(texture);
		textureUnit.useInShader(shaderProgram, "tex");
		return textureUnit;
	}

	protected Rectangle initBuffers(RenderContext ctx) throws IOException {
		Rectangle renderedShape = new Rectangle("screenPosition", "texturePosition");
		renderedShape.getVertexArrayObject().bind();
		renderedShape.getVertexBufferObject().setup(shaderProgram);
		renderedShape.getVertexArrayObject().unbind();
		if (LOG.isTraceEnabled()) {
            LOG.trace("Initialised: {}", renderedShape);
		}
		return renderedShape;
	}

	@Override
	public void doRender(RenderContext ctx) {
		shaderProgram.use();
		doRenderWithShader(ctx);
		shaderProgram.unuse();
	}

	public void doRenderWithShader(RenderContext ctx) {
		renderedShape.render(ctx);
	}

	@Override
	public void dispose() {
		renderedShape.destroy();
		shaderProgram.dispose();
		textureUnit.dispose();
	}
}
