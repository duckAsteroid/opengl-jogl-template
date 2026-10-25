package com.asteroid.duck.opengl.util.wave;

import com.asteroid.duck.opengl.util.RenderContext;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/** Peak-hold ballistics only — these never touch GL, so no context is needed. */
class FrequencyRendererTest {

    /** Minimal concrete renderer exposing the protected ballistics. */
    private static final class Ballistics extends FrequencyRenderer {
        Ballistics() {
            super(1);
        }

        @Override public void init(RenderContext ctx) {}
        @Override public void doRender(RenderContext ctx) {}
        @Override public void dispose() {}

        void setMagnitude(float m) {
            onSpectrum(new float[]{m});
        }

        /** Advance {@code seconds} at {@code hz} updates per second. */
        void run(int hz, double seconds) {
            for (int i = 0; i < Math.round(seconds * hz); i++) updatePeakBallistics(1.0 / hz);
        }

        float peak() {
            return peaks[0];
        }
    }

    /** A renderer with the default dynamics whose single bin has just peaked at full scale. */
    private static Ballistics peakedAtFullScale() {
        return peakedAtFullScale(new Ballistics());
    }

    /** Peaks {@code b}'s single bin at full scale, then drops the magnitude to zero. */
    private static Ballistics peakedAtFullScale(Ballistics b) {
        b.setMagnitude(1f);
        b.updatePeakBallistics(0);
        b.setMagnitude(0f);
        return b;
    }

    @Test
    void peakHoldsForDwellThenSags() {
        Ballistics b = peakedAtFullScale();

        b.run(60, 0.45);
        assertEquals(1f, b.peak(), 1e-6f, "peak should hold during the 0.5 s dwell");

        // Sagging for 1.5 s of the 3 s default fall time leaves about half
        b.run(60, 0.05 + 1.5);
        assertEquals(0.5f, b.peak(), 0.02f);
    }

    @Test
    void ballisticsAreIndependentOfFrameRate() {
        Ballistics at60  = peakedAtFullScale();
        Ballistics at144 = peakedAtFullScale();

        at60.run(60, 2.0);
        at144.run(144, 2.0);

        // 1.5 s of sag after the 0.5 s dwell, give or take a frame of dwell overshoot
        assertEquals(0.5f, at60.peak(),  0.02f);
        assertEquals(0.5f, at144.peak(), 0.02f);
    }

    @Test
    void customDynamicsApply() {
        Ballistics b = new Ballistics();
        b.setPeakDynamics(Duration.ZERO, Duration.ofSeconds(1));
        peakedAtFullScale(b);

        b.updatePeakBallistics(0.25);
        b.updatePeakBallistics(0.25);

        // Two 0.25 s steps of a 1 s full-scale fall
        assertEquals(0.5f, b.peak(), 1e-4f);
    }

    @Test
    void longGapBetweenFramesIsClamped() {
        Ballistics b = new Ballistics();
        b.setPeakDynamics(Duration.ZERO, Duration.ofSeconds(1));
        peakedAtFullScale(b);

        b.updatePeakBallistics(0);
        b.updatePeakBallistics(10.0);

        assertEquals(1f - (float) FrequencyRenderer.MAX_UPDATE_INTERVAL, b.peak(), 1e-4f);
    }

    @Test
    void perFrameDynamicsAssumeSixtyFps() {
        Ballistics b = new Ballistics();
        b.setPeakDynamicsPerFrame(0, 1f / 60f);
        peakedAtFullScale(b);

        b.run(60, 0.5);

        assertEquals(0.5f, b.peak(), 1e-4f);
    }

    @Test
    void nonPositiveFallTimeIsRejected() {
        Ballistics b = new Ballistics();
        assertThrows(IllegalArgumentException.class,
                () -> b.setPeakDynamics(Duration.ZERO, Duration.ZERO));
    }
}
