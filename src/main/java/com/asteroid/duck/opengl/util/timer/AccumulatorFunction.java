package com.asteroid.duck.opengl.util.timer;

import java.util.Objects;

/**
 * An accumulator continuously adds the "interval" to an accumulated value.
 * It just constantly gets bigger (while the timer is not paused).
 * The speed is just a multiplier for the interval used to accumulate.
 * Bigger speed, means bigger numbers sooner.
 */
public class AccumulatorFunction {
    private final Timer source;
    private double speed = 1.0;
    private double accumulated;
    private double lastElapsed;
    private double minSpeed = 0.01;
    private double maxSpeed = 100.0;

    public AccumulatorFunction(Timer source) {
        Objects.requireNonNull(source);
        this.source = source;
        lastElapsed = accumulated = source.elapsed();
    }

    public Timer getSource() {
        return source;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = Math.max(minSpeed, Math.min(maxSpeed, speed));
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
        if (speed > maxSpeed) maxSpeed = speed;
    }

    public double getMinSpeed() {
        return minSpeed;
    }

    public void setMinSpeed(double minSpeed) {
        this.minSpeed = minSpeed;
        if (speed < minSpeed) speed = minSpeed;
    }

    public double value() {
        double elapsed = source.elapsed();
        double delta = elapsed - lastElapsed;
        lastElapsed = elapsed;
        accumulated += delta * speed;
        return accumulated;
    }

}
