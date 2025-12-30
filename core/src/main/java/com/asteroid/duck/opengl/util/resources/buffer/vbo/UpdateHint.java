package com.asteroid.duck.opengl.util.resources.buffer.vbo;

import static org.lwjgl.opengl.GL15.*;

/**
 * A hint to the GL driver about how the data in the buffer will be used and updated.
 */
public enum UpdateHint {
    /**
     * The data store contents will be modified once and used many times.
     */
    STATIC(GL_STATIC_DRAW),
    /**
     * The data store contents will be modified once and used at most a few times.
     */
    STREAM(GL_STREAM_DRAW),
    /**
     * The data store contents will be modified repeatedly and used many times
     */
    DYNAMIC(GL_DYNAMIC_DRAW);

    final int glCode;

    UpdateHint(int glCode) {
        this.glCode = glCode;
    }

    public int getGlCode() {
        return glCode;
    }
}
