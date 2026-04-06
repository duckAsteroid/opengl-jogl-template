package com.asteroid.duck.opengl.util.audio.simulated;

public class OscillatingStereoPositioner implements StereoPositioner {

    private final double frequency;
    private final double stereoPositionA;
    private final double stereoPositionB;

    public static OscillatingStereoPositioner fullScale(double frequency) {
        return new OscillatingStereoPositioner(frequency, StereoPositioner.POSITION_MAX_LEFT, StereoPositioner.POSITION_MAX_RIGHT);
    }

    public OscillatingStereoPositioner(double frequency, double stereoPositionA, double stereoPositionB) {
        this.frequency = frequency;
        this.stereoPositionA = stereoPositionA;
        this.stereoPositionB = stereoPositionB;
    }

    public double stereoPosition(double time) {
        // Oscillates between 0 and 1
        double oscillation = (Math.sin(2 * Math.PI * frequency * time) + 1.0) / 2.0;
        return stereoPositionA * (1 - oscillation) + stereoPositionB * oscillation;
    }

    @Override
    public double[] sample(double channelSample, double time) {
        var stereoPosition = stereoPosition(time);
        var amplitudes = StereoPositioner.amplitudes(stereoPosition);
        return new double[]{
                channelSample * amplitudes[0],
                channelSample * amplitudes[1]
        };
    }


}
