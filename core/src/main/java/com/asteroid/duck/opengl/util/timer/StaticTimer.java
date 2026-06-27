package com.asteroid.duck.opengl.util.timer;

/**
 * An immutable {@link Timer} snapshot that always returns fixed values for elapsed time and the
 * current timestamp.
 *
 * <p>Useful in tests, offline rendering, or any context where real wall-clock time should be
 * decoupled from the values presented to renderers. Because the values never change, every call
 * to {@link #elapsed()} or {@link #now()} returns the same result for the lifetime of the
 * record.</p>
 *
 * @param elapsed the frozen elapsed time in seconds, returned by {@link #elapsed()}
 * @param now     the frozen current timestamp in seconds, returned by {@link #now()}
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
