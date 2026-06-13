package com.asteroid.duck.opengl.util.wave;

/**
 * Maps a vertex position to an amplitude scale factor for the audio wave.
 *
 * <p>Applied per-vertex during VBO construction: the returned value is stored as the Y
 * component of each vertex and multiplied by the normalised audio sample in the vertex shader.
 * Use {@link #constant} for a flat envelope or {@link #ellipse} to taper the wave to zero at
 * the screen edges so it fits inside an ellipse.</p>
 */
@FunctionalInterface
public interface AmplitudeFunction {

    /**
     * Return the amplitude scale factor for a vertex.
     *
     * @param index vertex index in [0, screenWidth)
     * @param x     normalised x position in [-1, 1)
     * @return amplitude scale factor (typically > 0; 0 silences the vertex)
     */
    float amplitudeAt(int index, float x);

    /** Uniform amplitude across all vertices. */
    static AmplitudeFunction constant(float amplitude) {
        return (i, x) -> amplitude;
    }

    /**
     * Elliptical envelope: zero at the left/right edges, {@code maxAmplitude} at the centre.
     * Achieved via {@code maxAmplitude * sin(π * (x+1) / 2)}.
     */
    static AmplitudeFunction ellipse(float maxAmplitude) {
        return (i, x) -> maxAmplitude * (float) Math.sin(Math.PI * (x + 1.0) / 2.0);
    }
}
