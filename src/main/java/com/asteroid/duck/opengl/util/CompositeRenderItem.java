package com.asteroid.duck.opengl.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An item made up of a bunch of other items.
 * Just calls methods on each of the contained items in turn.
 */
public class CompositeRenderItem implements RenderedItem {
	private static final Logger LOG = LoggerFactory.getLogger(CompositeRenderItem.class);

	protected final List<RenderedItem> items;

	public CompositeRenderItem() {
		this.items = new ArrayList<>();
	}

	public CompositeRenderItem(RenderedItem ... items) {
		this.items = Arrays.asList(items);
	}

	public CompositeRenderItem(List<RenderedItem> items) {
		this.items = items;
	}

	public void addItem(RenderedItem item) {
		items.add(item);
	}

	public void addItems(RenderedItem... items) {
		this.items.addAll(Arrays.asList(items));
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		items.forEach(item -> {
			try {
				item.init(ctx);
			} catch (IOException e) {
				LOG.error("Error initialising item", e);
			}
		});
	}

	@Override
	public void doRender(RenderContext ctx) {
		items.forEach(item -> item.doRender(ctx));
	}

	@Override
	public void dispose() {
		items.forEach(RenderedItem::dispose);
	}
}
