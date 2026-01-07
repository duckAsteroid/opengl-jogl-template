package com.asteroid.duck.opengl.util.resources.bound;

import com.asteroid.duck.opengl.util.resources.Resource;

public interface BoundResource extends Resource {
    void bind();
    boolean isBound();
    void unbind();
}
