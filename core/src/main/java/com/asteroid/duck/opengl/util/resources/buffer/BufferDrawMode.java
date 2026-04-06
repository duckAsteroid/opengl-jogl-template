package com.asteroid.duck.opengl.util.resources.buffer;

import com.asteroid.duck.opengl.util.GLCoded;

import static org.lwjgl.opengl.GL11.*;

/**
 * OpenGL primitive topology used by draw calls such as {@code glDrawArrays} and {@code glDrawElements}.
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

    /**
     * Returns the underlying OpenGL enum value.
     *
     * @return GL primitive constant (for example {@code GL_TRIANGLES})
     */
    public int openGlCode() {
        return glCode;
    }
}
