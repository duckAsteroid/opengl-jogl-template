package com.asteroid.duck.opengl.util.audio.beat.eval;

import com.asteroid.duck.opengl.util.audio.beat.BeatAnalyser;

import static com.asteroid.duck.opengl.util.audio.beat.eval.BeatMetrics.WARM_UP;

/** Runs an analyser over a loaded track and scores what it reports. */
public final class Evaluator {

    /** Samples per {@link BeatAnalyser#process} call — roughly a live audio callback's worth. */
    static final int BLOCK_SIZE = 512;

    private Evaluator() {}

    /**
     * Feed the whole track through a fresh analyser, in {@value #BLOCK_SIZE}-sample blocks, and
     * score its output against the annotations.
     * @param analyser the analyser to run
     * @param group    the track's group
     * @param name     the track's name
     * @param loaded   the track's audio and annotations
     * @return the scores
     */
    public static TrackResult evaluate(Analysers.Entry analyser, String group, String name, Track.Loaded loaded) {
        AudioClip audio = loaded.audio();
        Annotations reference = loaded.annotations();
        EventRecorder recorder = new EventRecorder();
        BeatAnalyser instance = analyser.factory().create(audio.sampleRate(), recorder);
        float[] samples = audio.samples();
        for (int offset = 0; offset < samples.length; offset += BLOCK_SIZE) {
            instance.process(samples, offset, Math.min(BLOCK_SIZE, samples.length - offset));
        }

        double[] beats = recorder.beats();
        double[] refBeats = BeatMetrics.trim(reference.beats(), WARM_UP);
        double[] estBeats = BeatMetrics.trim(beats, WARM_UP);
        double beatF = Double.NaN;
        double cmlt = Double.NaN;
        double amlt = Double.NaN;
        double lockTime = Double.NaN;
        if (reference.beats().length > 0) {
            beatF = BeatMetrics.fMeasure(refBeats, estBeats, BeatMetrics.BEAT_WINDOW);
            BeatMetrics.Continuity continuity = BeatMetrics.continuity(refBeats, estBeats);
            cmlt = continuity.cmlt();
            amlt = continuity.amlt();
            lockTime = BeatMetrics.lockTime(reference.beats(), beats);
        }
        double kickF = reference.kicks().length == 0 ? Double.NaN
                : BeatMetrics.fMeasure(BeatMetrics.trim(reference.kicks(), WARM_UP),
                        BeatMetrics.trim(recorder.kicks(), WARM_UP), BeatMetrics.ONSET_WINDOW);
        double estBpm = recorder.tempo(WARM_UP);

        return new TrackResult(analyser.name(), group, name, audio.duration(),
                reference.bpm(), estBpm,
                TempoMetrics.acc1(estBpm, reference.bpm()), TempoMetrics.acc2(estBpm, reference.bpm()),
                beatF, cmlt, amlt, lockTime, kickF);
    }
}
