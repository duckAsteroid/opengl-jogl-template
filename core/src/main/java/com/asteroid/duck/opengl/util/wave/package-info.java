/**
 * Real-time audio visualisation renderers and supporting utilities.
 *
 * <p>All classes in this package implement
 * {@link com.asteroid.duck.opengl.util.RenderedItem} or serve as helpers that are consumed by
 * renderers. The shared audio capture infrastructure is
 * {@link com.asteroid.duck.opengl.util.audio.AudioReader}, which runs on a dedicated daemon thread
 * and writes PCM data into either a GPU-mapped PBO (via
 * {@link com.asteroid.duck.opengl.util.audio.PboAudioSink}) or a CPU-side
 * {@link com.asteroid.duck.opengl.util.audio.RollingAudioBuffer}.</p>
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
 *   <dt>{@link com.asteroid.duck.opengl.util.wave.BeatDetector}</dt>
 *   <dd>Per-frame onset detector that consumes the {@code float[] magnitudes} already produced
 *       by {@link com.asteroid.duck.opengl.util.wave.FFTProcessor} — zero extra FFT cost.
 *       Tracks energy across configurable {@link com.asteroid.duck.opengl.util.wave.FrequencyBand}s
 *       (default: bass / snare / hi-hat) and publishes a {@code [0, 1]} beat strength per band
 *       that rises instantly on onset and decays at a tunable rate.</dd>
 *
 *   <dt>{@link com.asteroid.duck.opengl.util.wave.FrequencyBand}</dt>
 *   <dd>Immutable record describing a named Hz range. Provides {@code BASS}, {@code SNARE}, and
 *       {@code HI_HAT} presets and a {@code defaults()} factory. Pass any {@code List} of these
 *       to {@link com.asteroid.duck.opengl.util.wave.BeatDetector} to configure custom bands.</dd>
 *
 *   <dt>{@link com.asteroid.duck.opengl.util.wave.AmplitudeFunction}</dt>
 *   <dd>Functional interface for per-vertex amplitude envelopes used by {@code AudioWave}.</dd>
 *
 *   <dt>{@link com.asteroid.duck.opengl.util.audio.AudioReader}</dt>
 *   <dd>Background-thread PCM reader (lives in the {@code audio} package). Create one per
 *       experiment, pass a list of {@link com.asteroid.duck.opengl.util.audio.AudioSink}s, and
 *       call {@code setLine()} with the source obtained from
 *       {@link com.asteroid.duck.opengl.util.audio.LineAcquirer}.</dd>
 * </dl>
 */
package com.asteroid.duck.opengl.util.wave;
