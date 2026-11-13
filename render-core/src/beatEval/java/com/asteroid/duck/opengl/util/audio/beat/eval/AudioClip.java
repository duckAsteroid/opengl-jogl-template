package com.asteroid.duck.opengl.util.audio.beat.eval;

/**
 * A decoded mono audio clip.
 * @param samples    mono samples in {@code [-1, 1]}
 * @param sampleRate sample rate in Hz
 */
public record AudioClip(float[] samples, float sampleRate) {

    /** @return the clip length in seconds */
    public double duration() {
        return samples.length / (double) sampleRate;
    }
}
