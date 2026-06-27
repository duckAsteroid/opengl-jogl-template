package com.asteroid.duck.opengl.util.timer.function;

import com.asteroid.duck.opengl.util.timer.Timer;

/**
 * Represents a linear function that calculates a value based on a timer source.
 * The function uses a maximum Y value and a frequency to determine the output.
 * The output rises from 0 to max Y linearly every period.
 * In other words a saw tooth function.
 */
public class LinearFunction {
    private final Timer source;
    private double maxY;
    private double frequency;

    /**
     * Create a linear function driven by the given timer, with default maxY=0 and frequency=0.
     * Call {@link #setMaxY} and {@link #setFrequency} before using {@link #value()}.
     *
     * @param source the timer whose {@link Timer#elapsed()} drives the function
     */
    public LinearFunction(Timer source) {
        this.source = source;
    }

    /**
     * Returns the timer that drives this function's elapsed time.
     *
     * @return the source timer; never {@code null}
     */
    public Timer getSource() {
        return source;
    }

    /**
     * Returns the peak output value reached at the end of each period.
     *
     * @return the maximum Y value
     */
    public double getMaxY() {
        return maxY;
    }

    /**
     * Set the peak output value reached at the end of each period (the sawtooth ceiling).
     *
     * @param maxY the maximum Y value; negative values produce a descending sawtooth
     */
    public void setMaxY(double maxY) {
        this.maxY = maxY;
    }

    /**
     * Returns the current oscillation frequency in hertz.
     *
     * @return cycles per second
     */
    public double getFrequency() {
        return frequency;
    }

    /**
     * Set the oscillation frequency, which determines how quickly the sawtooth resets.
     *
     * @param frequency the desired frequency in hertz; must be &gt; 0
     */
    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }
    /**
     * Calculates the value of the linear function based on the timer source.
     * The value is determined by the progress within the current period,
     * scaled by the maximum Y value.
     *
     * @return The calculated value of the function.
     */
    public double value() {
        double period = 1.0 / frequency;
        double progress = (source.elapsed() % period) / period;
        return progress * maxY;
    }
}
