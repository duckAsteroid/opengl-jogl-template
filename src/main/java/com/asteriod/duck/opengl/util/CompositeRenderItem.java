package com.asteriod.duck.opengl.util;

import java.io.IOException;

public class CompositeRenderItem implements RenderedItem {

	private final RenderedItem[] items;

	public CompositeRenderItem(RenderedItem ... items) {
		this.items = items;
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		for (int i = 0; i < items.length; i++) {
			items[i].init(ctx);
		}
	}

	@Override
	public void doRender(RenderContext ctx) {
		for (int i = 0; i < items.length; i++) {
			items[i].doRender(ctx);
		}
	}

	@Override
	public void dispose() {
		for (int i = 0; i < items.length; i++) {
			items[i].dispose();
		}
	}
}
