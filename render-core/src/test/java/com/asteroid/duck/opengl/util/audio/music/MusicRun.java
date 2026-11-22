package com.asteroid.duck.opengl.util.audio.music;

import com.asteroid.duck.opengl.util.audio.beat.BeatListener;
import com.asteroid.duck.opengl.util.audio.beat.BeatTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Runs a {@link MusicAnalyser} (with a {@link BeatTracker} for tempo) over a clip, recording everything. */
final class MusicRun {

    final List<MusicState> states = new ArrayList<>();
    final List<MusicEvent> events = new ArrayList<>();

    static MusicRun of(float[] samples, float sampleRate) {
        MusicRun run = new MusicRun();
        MusicAnalyser analyser = new MusicAnalyser(sampleRate, new MusicListener() {
            @Override
            public void onState(MusicState state) {
                run.states.add(state);
            }

            @Override
            public void onEvent(MusicEvent event) {
                run.events.add(event);
            }
        });
        BeatTracker tracker = new BeatTracker(sampleRate, new BeatListener() {
            @Override
            public void onTempo(double time, double bpm, float confidence) {
                analyser.setTempo(bpm);
            }
        });
        for (int offset = 0; offset < samples.length; offset += 512) {
            int n = Math.min(512, samples.length - offset);
            tracker.process(samples, offset, n);
            analyser.process(samples, offset, n);
        }
        return run;
    }

    /** Mean of a value over the states in {@code [from, to)} seconds. */
    double mean(double from, double to, java.util.function.ToDoubleFunction<MusicState> value) {
        return states.stream().filter(s -> s.time() >= from && s.time() < to).mapToDouble(value).average().orElseThrow();
    }

    MusicState at(double time) {
        return states.stream().filter(s -> s.time() >= time).findFirst().orElseThrow();
    }

    List<MusicEvent> events(MusicEvent.Type type) {
        return events.stream().filter(e -> e.type() == type).toList();
    }

    List<MusicEvent> events(Predicate<MusicEvent> filter) {
        return events.stream().filter(filter).toList();
    }
}
