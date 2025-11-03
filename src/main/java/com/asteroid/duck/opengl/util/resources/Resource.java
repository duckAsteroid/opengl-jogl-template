package com.asteroid.duck.opengl.util.resources;
/**
 * Represents a graphics resource that can be destroyed to free up system resources.
 */
public interface Resource {
    /**
     * Destroys the resource, freeing any associated system resources.
     */
	void destroy();
}
