package com.asteroid.duck.opengl.util.timer;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class TimerTest {

    private Double timestamp = 0.0;

    // Minimal Clock backed by the controllable timestamp field
    private final Clock clock = () -> timestamp;

    @Test
    public void notElapsedBeforeDuration() {
        Timer timer = clock.track(Duration.ofSeconds(10));

        timestamp = 9.9;
        assertFalse(timer.hasElapsed());
    }

    @Test
    public void elapsedAtExactDuration() {
        Timer timer = clock.track(Duration.ofSeconds(10));

        timestamp = 10.0;
        assertTrue(timer.hasElapsed());
    }

    @Test
    public void elapsedAfterDuration() {
        Timer timer = clock.track(Duration.ofSeconds(10));

        timestamp = 15.0;
        assertTrue(timer.hasElapsed());
    }

    @Test
    public void elapsedReflectsTimeSinceCreation() {
        timestamp = 5.0;
        Timer timer = clock.track(Duration.ofSeconds(10));

        timestamp = 8.0;
        assertEquals(3.0, timer.elapsed(), 1e-9);
    }

    @Test
    public void remainingCountsDown() {
        Timer timer = clock.track(Duration.ofSeconds(10));

        timestamp = 4.0;
        assertEquals(6.0, timer.remaining(), 1e-9);
    }

    @Test
    public void remainingIsZeroOnceElapsed() {
        Timer timer = clock.track(Duration.ofSeconds(10));

        timestamp = 20.0;
        assertEquals(0.0, timer.remaining());
    }

    @Test
    public void progressRangeZeroToOne() {
        Timer timer = clock.track(Duration.ofSeconds(10));

        assertEquals(0.0, timer.progress(), 1e-9);

        timestamp = 5.0;
        assertEquals(0.5, timer.progress(), 1e-9);

        timestamp = 10.0;
        assertEquals(1.0, timer.progress(), 1e-9);

        timestamp = 20.0;
        assertEquals(1.0, timer.progress()); // clamped
    }

    @Test
    public void startedAtNonZeroClockOffset() {
        timestamp = 100.0;
        Timer timer = clock.track(Duration.ofSeconds(5));

        timestamp = 104.0;
        assertFalse(timer.hasElapsed());

        timestamp = 105.0;
        assertTrue(timer.hasElapsed());
    }

    @Test
    public void negativeDurationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Timer(clock, 0.0, -1.0));
    }

    @Test
    public void zeroDurationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Timer(clock, 0.0, 0.0));
    }
}
