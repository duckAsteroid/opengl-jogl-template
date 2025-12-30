package com.asteroid.duck.opengl.util.timer.function;

import com.asteroid.duck.opengl.util.timer.Timer;

import java.util.Objects;
/**
 * Calculates the value of a sinusoidal wave function between -1 and 1.
 * It uses a frequency in hertz (per second) and a phase offest in seconds.
 */
public class WaveFunction {
    private final Timer source;
    // the frequency in hertz
    private double frequency;
    // the relative phase in seconds
    private double phase;

    public WaveFunction(Timer source, double frequency, double phase) {
        Objects.requireNonNull(source);
        this.source = source;
        this.frequency = frequency;
        this.phase = phase;
    }

    /**
     * Uses a default 0 phase
     */
    public WaveFunction(Timer source, double frequency) {
        this(source, frequency, 0.0);
    }

    /**
     * Uses default 1Hz and 0 phase
     */
    public WaveFunction(Timer source) {
        this(source, 1.0);
    }

    public Timer getSource() {
        return source;
    }

    public double getFrequency() {
        return frequency;
    }

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }

    public double getPhase() {
        return phase;
    }

    public void setPhase(double phase) {
        this.phase = phase;
    }

    public double value() {
        return Math.sin(2 * Math.PI * frequency * (source.elapsed() + phase));
    }

    public static double value(double frequency, double phase, double elapsed) {
        return Math.sin(2 * Math.PI * frequency * (elapsed + phase));
    }
}
