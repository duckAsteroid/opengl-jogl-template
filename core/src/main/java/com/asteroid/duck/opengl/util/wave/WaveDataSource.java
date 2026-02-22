package com.asteroid.duck.opengl.util.wave;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * A source of wave data
 */
public interface WaveDataSource {
    int channels();

    /**
     * @param buffer
     * @return
     */
    int read(ByteBuffer buffer);
}
