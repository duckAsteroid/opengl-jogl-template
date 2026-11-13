package com.asteroid.duck.opengl.util.audio.beat.eval;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SyntheticTracksTest {

    @Test
    void tracksAreWellFormed() throws IOException {
        for (Track track : SyntheticTracks.all()) {
            Track.Loaded loaded = track.loader().load();
            AudioClip audio = loaded.audio();
            double[] beats = loaded.annotations().beats();
            assertTrue(audio.duration() > 25, track.name() + " too short");
            assertTrue(beats.length > 40, track.name());
            for (int i = 1; i < beats.length; i++) assertTrue(beats[i] > beats[i - 1], track.name() + " beats not ascending");
            assertTrue(beats[beats.length - 1] < audio.duration());
            float peak = 0;
            for (float s : audio.samples()) peak = Math.max(peak, Math.abs(s));
            assertEquals(0.9f, peak, 1e-4f, track.name());
        }
    }

    @Test
    void tempoChangeHasNoGapAtTheJoin() throws IOException {
        Track track = SyntheticTracks.all().stream().filter(t -> t.name().startsWith("tempo-change")).findFirst().orElseThrow();
        double[] beats = track.loader().load().annotations().beats();
        for (int i = 1; i < beats.length; i++) {
            double interval = beats[i] - beats[i - 1];
            assertTrue(interval > 0.46 && interval < 0.51, "interval " + interval + " at beat " + i);
        }
    }

    @Test
    void harnessRunsEveryAnalyserOverTheSyntheticSet() {
        List<TrackResult> results = BeatEval.run(SyntheticTracks.all(), Analysers.ALL);
        assertEquals(SyntheticTracks.all().size() * Analysers.ALL.size(), results.size());
        for (TrackResult r : results) {
            assertFalse(Double.isNaN(r.beatF()), r.track() + " has no beat F");
            assertTrue(r.beatF() >= 0 && r.beatF() <= 1);
        }
        String summary = Report.summary(results, Analysers.ALL);
        assertTrue(summary.contains("| synthetic | " + SyntheticTracks.all().size() + " |"), summary);
    }
}
