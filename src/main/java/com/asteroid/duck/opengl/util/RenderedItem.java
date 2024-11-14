package com.asteroid.duck.opengl.util;

import java.io.IOException;

/**
 * Something that gets a chance to render during the main render loop
 */
public interface RenderedItem {
	void init(RenderContext ctx) throws IOException;
	void doRender(RenderContext ctx);
	void dispose();
}
