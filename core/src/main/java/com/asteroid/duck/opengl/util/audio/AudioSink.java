package com.asteroid.duck.opengl.util.audio;

public interface AudioSink {
    void write(byte[] data, int offset, int length);
}
