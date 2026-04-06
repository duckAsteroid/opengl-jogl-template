package com.asteroid.duck.opengl.util.audio.simulated;

public interface StereoPositioner {
    double POSITION_MAX_LEFT = -1.0;
    double POSITION_CENTER = 0.0;
    double POSITION_MAX_RIGHT = 1.0;

    default double[] sample(MonoDataSource source, double time) {
        var sample = source.sample(time);
        return sample(sample, time);
    }

    double[] sample(double monoSample, double time);

    static double[] amplitudes(double stereoPosition) {
        return new double[]{
            (1.0 - stereoPosition) / 2.0,
            (1.0 + stereoPosition) / 2.0
        };
    }

    default StereoDataSource wrap(MonoDataSource source) {
        return new StereoDataSource() {
            @Override
            public double[] sample(double time) {
                double channelSample = source.sample(time);
                return StereoPositioner.this.sample(channelSample, time);
            }
        };
    }
}
