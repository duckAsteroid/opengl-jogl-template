package com.asteriod.duck.opengl;

/**
 * Something that gets a chance to render during the main render loop
 */
public interface RenderedItem {
	void doRender(double elapsedTime);
}
