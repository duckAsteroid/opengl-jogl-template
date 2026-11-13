package com.asteroid.duck.opengl.util.audio.beat.eval;

import com.asteroid.duck.opengl.util.audio.beat.BeatAnalyser;
import com.asteroid.duck.opengl.util.audio.beat.BeatListener;

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
                    BaselineAnalyser::new)
    );

    private Analysers() {}
}
