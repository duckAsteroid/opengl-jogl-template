package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.resources.Resource;

import java.io.IOException;

/**
 * The interface to some resource that gets a chance to:
 * <ol>
 * 	<li>Initialise before rendering starts</li>
 * 	<li>Render during the main render loop</li>
 * </ol>
 */
public interface RenderedItem extends Resource {
	void init(RenderContext ctx) throws IOException;
	void doRender(RenderContext ctx);
}
