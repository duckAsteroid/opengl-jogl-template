package com.asteroid.duck.opengl.util.timer;

/**
 * A timer that only ever holds fixed values for elapsed and now.
 * It's like a snapshot in time.
 * @param elapsed
 * @param now
 */
public record StaticTimer(double elapsed, double now) implements Timer {

    /**
     * Get the latest timestamp from the source.
     *
     * @return the timestamp
     * @throws RuntimeException if cannot get data from source
     */
    @Override
    public double now() {
        return now;
    }
}
