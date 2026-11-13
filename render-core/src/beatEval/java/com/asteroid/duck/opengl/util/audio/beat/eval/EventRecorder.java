package com.asteroid.duck.opengl.util.audio.beat.eval;

import com.asteroid.duck.opengl.util.audio.beat.BeatListener;

import java.util.ArrayList;
import java.util.List;

/** Records everything a {@link com.asteroid.duck.opengl.util.audio.beat.BeatAnalyser} reports. */
final class EventRecorder implements BeatListener {

    private final List<Double> beats = new ArrayList<>();
    private final List<Double> kicks = new ArrayList<>();
    private final List<double[]> tempos = new ArrayList<>();

    @Override
    public void onBeat(double time) {
        beats.add(time);
    }

    @Override
    public void onKick(double time, float strength) {
        kicks.add(time);
    }

    @Override
    public void onTempo(double time, double bpm, float confidence) {
        tempos.add(new double[] {time, bpm});
    }

    /** @return reported beat times, ascending */
    double[] beats() {
        return sorted(beats);
    }

    /** @return reported kick times, ascending */
    double[] kicks() {
        return sorted(kicks);
    }

    /**
     * The track's overall tempo estimate: the median of the estimates reported after
     * {@code warmUp}, or of all of them if none came that late.
     * @param warmUp seconds to ignore
     * @return tempo in BPM, or {@code NaN} if none was reported
     */
    double tempo(double warmUp) {
        double[] late = tempos.stream().filter(t -> t[0] >= warmUp).mapToDouble(t -> t[1]).toArray();
        return late.length > 0 ? Stats.median(late) : Stats.median(tempos.stream().mapToDouble(t -> t[1]).toArray());
    }

    private static double[] sorted(List<Double> times) {
        return times.stream().mapToDouble(Double::doubleValue).sorted().toArray();
    }
}
