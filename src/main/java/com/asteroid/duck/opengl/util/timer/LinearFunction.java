package com.asteroid.duck.opengl.util.timer;
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

    public LinearFunction(Timer source) {
        this.source = source;
    }

    public Timer getSource() {
        return source;
    }

    public double getMaxY() {
        return maxY;
    }

    public void setMaxY(double maxY) {
        this.maxY = maxY;
    }

    public double getFrequency() {
        return frequency;
    }

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
