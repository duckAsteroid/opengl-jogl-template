package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.resources.manager.ResourceListManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * An item made up of a bunch of other items.
 * Just calls methods on each of the contained items in turn.
 */
public class CompositeRenderItem extends ResourceListManager<RenderedItem> implements RenderedItem {
	private static final Logger LOG = LoggerFactory.getLogger(CompositeRenderItem.class);


	public CompositeRenderItem() {
        super();
	}

	public CompositeRenderItem(RenderedItem ... items) {
		super(Arrays.asList(items));
	}

	public CompositeRenderItem(List<RenderedItem> items) {
		super(items);
	}

    protected void addItems(RenderedItem... items) {
        for(RenderedItem item : items) {
            add(item);
        }
    }

    protected List<RenderedItem> items() {
        return resources;
    }

	@Override
	public void init(final RenderContext ctx) throws IOException {
		stream().forEach(item -> {
			try {
				item.init(ctx);
			} catch (IOException e) {
				LOG.error("Error initialising item", e);
			}
		});
	}

	@Override
	public void doRender(final RenderContext ctx) {
		stream().forEach(item -> item.doRender(ctx));
	}

	@Override
	public void dispose() {
		stream().forEach(RenderedItem::dispose);
	}
}
