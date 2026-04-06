package com.asteroid.duck.opengl.util.audio.simulated;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StaticStereoPositionerTest {

    @Test
    void amplitudes() {
        double[] result = StereoPositioner.amplitudes(0);
        assertArrayEquals(new double[]{0.5, 0.5}, result, 1e-6);
        result = StereoPositioner.amplitudes(StereoPositioner.POSITION_MAX_LEFT);
        assertArrayEquals(new double[]{1.0, 0.0}, result, 1e-6);
        result = StereoPositioner.amplitudes(StereoPositioner.POSITION_MAX_RIGHT);
        assertArrayEquals(new double[]{0.0, 1.0}, result, 1e-6);
    }

    @Test
    void sample() {
        var source = new MonoDataSource() {
            @Override
            public double sample(double time) {
                return 1.0;
            }
        };
        var positioner = StaticStereoPositioner.CENTER;
        var result = positioner.sample(source, 0);
        assertArrayEquals(new double[]{0.5, 0.5}, result, 1e-6);
    }
}