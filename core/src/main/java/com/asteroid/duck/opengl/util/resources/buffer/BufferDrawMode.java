package com.asteroid.duck.opengl.util.resources.buffer;

import com.asteroid.duck.opengl.util.GLCoded;

import static org.lwjgl.opengl.GL11.*;

/**
 * Defines the drawing mode to use when rendering the vertex data.
 */
public enum BufferDrawMode implements GLCoded {
    /**
     * Interprets each vertex as a single point.
     */
    POINTS(GL_POINTS),
    /**
     * Interprets each pair of vertices as a line segment.
     */
    LINES(GL_LINES),
    LINE_STRIP(GL_LINE_STRIP),
    LINE_LOOP(GL_LINE_LOOP),
    TRIANGLES(GL_TRIANGLES),
    TRIANGLE_STRIP(GL_TRIANGLE_STRIP),
    TRIANGLE_FAN(GL_TRIANGLE_FAN);

    final int glCode;

    BufferDrawMode(int glCode) {
        this.glCode = glCode;
    }

    public int openGlCode() {
        return glCode;
    }
}
