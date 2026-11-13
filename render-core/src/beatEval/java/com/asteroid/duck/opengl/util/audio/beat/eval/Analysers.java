package com.asteroid.duck.opengl.util.audio.beat.eval;

import com.asteroid.duck.opengl.util.audio.beat.BeatAnalyser;
import com.asteroid.duck.opengl.util.audio.beat.BeatListener;
import com.asteroid.duck.opengl.util.audio.beat.KickDetector;

import java.util.List;

/** The analysers the harness can evaluate, by name. Add new analysers here. */
public final class Analysers {

    /** Creates an analyser for a given sample rate, reporting to a listener. */
    @FunctionalInterface
    public interface Factory {
        /**
         * @param sampleRate sample rate of the audio it will be fed, in Hz
         * @param listener   receives its events
         * @return a fresh analyser
         */
        BeatAnalyser create(float sampleRate, BeatListener listener);
    }

    /**
     * A named analyser.
     * @param name        the name {@code --analyser} selects it by
     * @param description one line for the report
     * @param factory     creates instances
     */
    public record Entry(String name, String description, Factory factory) {}

    /** Every registered analyser. */
    public static final List<Entry> ALL = List.of(
            new Entry("baseline", "existing BeatDetector (SpectrumWave tuning), bass rising edges",
                    BaselineAnalyser::new),
            new Entry("kick", "KickDetector: time-domain 80–250 Hz energy rise, peak-picked (kicks reported as beats too)",
                    (sampleRate, listener) -> new KickDetector(sampleRate, kicksAsBeats(listener)))
    );

    /**
     * Forwards kicks and also reports each as a beat, for analysers that don't track beats
     * themselves — so their beat scores show how far kicks alone get you.
     */
    static BeatListener kicksAsBeats(BeatListener listener) {
        return new BeatListener() {
            @Override
            public void onKick(double time, float strength) {
                listener.onKick(time, strength);
                listener.onBeat(time);
            }

            @Override
            public void onTempo(double time, double bpm, float confidence) {
                listener.onTempo(time, bpm, confidence);
            }
        };
    }

    private Analysers() {}
}
