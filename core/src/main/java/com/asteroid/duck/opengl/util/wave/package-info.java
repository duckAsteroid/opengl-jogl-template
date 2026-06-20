/**
 * Real-time audio visualisation renderers and supporting utilities.
 *
 * <p>All classes in this package implement
 * {@link com.asteroid.duck.opengl.util.RenderedItem} or serve as helpers that are consumed by
 * renderers. The shared audio capture infrastructure is
 * {@link com.asteroid.duck.opengl.util.wave.AudioReader}, which runs on a dedicated daemon thread
 * and writes PCM data into either a GPU-mapped PBO or a CPU-side
 * {@link com.asteroid.duck.opengl.util.audio.RollingFloatBuffer}.</p>
 *
 * <h2>Available renderers</h2>
 * <dl>
 *   <dt>{@link com.asteroid.duck.opengl.util.wave.AudioWave}</dt>
 *   <dd>Scrolling oscilloscope-style waveform drawn as a horizontal {@code GL_LINE_STRIP}.
 *       Audio is uploaded each frame via a persistently-mapped PBO to a 1-D
 *       {@code GL_RG16_SNORM} texture sampled directly by the vertex shader.</dd>
 *
 *   <dt>{@link com.asteroid.duck.opengl.util.wave.RadialWave}</dt>
 *   <dd>Oscilloscope waveform deformed around a circle. 1 024 direction vectors are stored
 *       in the VBO; the vertex shader displaces each point radially by the corresponding
 *       audio sample read from the same PBO-backed texture used by {@code AudioWave}.
 *       Aspect-ratio correction is applied automatically via a resize listener.</dd>
 *
 *   <dt>{@link com.asteroid.duck.opengl.util.wave.SpectrumAnalyser}</dt>
 *   <dd>Classic bar-chart spectrum analyser. Each frame, a Hann-windowed FFT is computed
 *       by {@link com.asteroid.duck.opengl.util.wave.FFTProcessor} and the results are
 *       uploaded to a 1-D {@code GL_RG32F} texture (R = magnitude, G = peak-hold).
 *       Two draw calls render the filled bars ({@code GL_TRIANGLES}) and white peak-hold
 *       ticks ({@code GL_LINES}). Frequency scale and amplitude scale are both logarithmic.</dd>
 * </dl>
 *
 * <h2>Supporting classes</h2>
 * <dl>
 *   <dt>{@link com.asteroid.duck.opengl.util.wave.FFTProcessor}</dt>
 *   <dd>Stateless (per instance) FFT pipeline: Hann window → JTransforms real FFT →
 *       log-frequency bin mapping → dB normalisation. All heavy objects are pre-allocated
 *       at construction; {@code process()} allocates nothing.</dd>
 *
 *   <dt>{@link com.asteroid.duck.opengl.util.wave.AmplitudeFunction}</dt>
 *   <dd>Functional interface for per-vertex amplitude envelopes used by {@code AudioWave}.</dd>
 *
 *   <dt>{@link com.asteroid.duck.opengl.util.wave.AudioReader}</dt>
 *   <dd>Internal background-thread PCM reader. Not intended for direct use; obtain an
 *       audio source via {@link com.asteroid.duck.opengl.util.audio.LineAcquirer} and pass it
 *       to the renderer's {@code setLine()} method.</dd>
 * </dl>
 */
package com.asteroid.duck.opengl.util.wave;
