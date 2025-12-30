package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.resources.Resource;

import java.io.IOException;

/**
 * Something that gets a chance to render during the main render loop
 */
public interface RenderedItem extends Resource {
	void init(RenderContext ctx) throws IOException;
	void doRender(RenderContext ctx);
}
