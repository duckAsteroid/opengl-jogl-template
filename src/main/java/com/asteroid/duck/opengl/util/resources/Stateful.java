package com.asteroid.duck.opengl.util.resources;

import com.asteroid.duck.opengl.util.RenderContext;

public interface Stateful extends Resource {
	void init(RenderContext ctx);
	boolean isInitialised();

	void begin(RenderContext ctx);
	boolean isActive();
	void end(RenderContext ctx);
}
