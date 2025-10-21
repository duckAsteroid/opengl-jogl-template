package com.asteroid.duck.opengl.util.timer;

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

    public double value() {
        double period = 1.0 / frequency;
        double progress = (source.elapsed() % period) / period;
        return progress * maxY;
    }
}
