package com.asteroid.duck.opengl.util.resources;

public interface BoundResource extends Resource {
    void bind();
    boolean isBound();
    void unbind();
}
