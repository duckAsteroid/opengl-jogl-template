package com.asteriod.duck.opengl;

import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.RenderedItem;
import com.asteriod.duck.opengl.util.resources.FrameBuffer;
import com.asteriod.duck.opengl.util.resources.texture.Texture;

import java.io.IOException;

public class TextureRenderer implements RenderedItem {
	// wrap another rendered item and render it to texture using a FBO
	private final RenderedItem renderedItem;
	private final Texture targetTexture;
	private final FrameBuffer  fbo;

	public TextureRenderer(RenderedItem renderedItem, Texture targetTexture) {
		this.renderedItem = renderedItem;
		this.targetTexture = targetTexture;
		this.fbo = new FrameBuffer(targetTexture);
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		renderedItem.init(ctx);
	}

	@Override
	public void doRender(RenderContext ctx) {
		fbo.bind();
		renderedItem.doRender(ctx);
		fbo.unbind();
	}

	@Override
	public void dispose() {
		renderedItem.dispose();
		fbo.dispose();
	}
}