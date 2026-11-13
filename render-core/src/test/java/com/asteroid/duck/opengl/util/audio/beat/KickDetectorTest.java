package com.asteroid.duck.opengl.util.audio.beat;

import com.asteroid.duck.opengl.util.audio.beat.eval.BeatMetrics;
import com.asteroid.duck.opengl.util.audio.beat.eval.SyntheticTracks;
import com.asteroid.duck.opengl.util.audio.beat.eval.Track;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KickDetectorTest {

    private static double[] detect(float[] samples, float sampleRate) {
        List<Double> kicks = new ArrayList<>();
        KickDetector detector = new KickDetector(sampleRate, new BeatListener() {
            @Override
            public void onKick(double time, float strength) {
                assertTrue(strength > 0 && strength <= 1, "strength " + strength);
                kicks.add(time);
            }
        });
        for (int offset = 0; offset < samples.length; offset += 512) {
            detector.process(samples, offset, Math.min(512, samples.length - offset));
        }
        return kicks.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private static double kickF(String trackName) throws IOException {
        Track track = SyntheticTracks.all().stream().filter(t -> t.name().equals(trackName)).findFirst().orElseThrow();
        Track.Loaded loaded = track.loader().load();
        double[] kicks = detect(loaded.audio().samples(), loaded.audio().sampleRate());
        return BeatMetrics.fMeasure(BeatMetrics.trim(loaded.annotations().kicks(), BeatMetrics.WARM_UP),
                BeatMetrics.trim(kicks, BeatMetrics.WARM_UP), BeatMetrics.ONSET_WINDOW);
    }

    @Test
    void findsFourOnTheFloorKicks() throws IOException {
        assertTrue(kickF("four-on-floor-128") > 0.95);
    }

    @Test
    void findsKicksUnderARollingBass() throws IOException {
        // The rolling bass's own onsets share the low end; the old detector found none of these
        assertTrue(kickF("rolling-bass-124") > 0.9);
    }

    @Test
    void ignoresSilenceAndSustainedTones() {
        float sampleRate = 48_000f;
        float[] samples = new float[(int) (10 * sampleRate)];
        for (int i = samples.length / 2; i < samples.length; i++) {
            samples[i] = (float) (0.5 * Math.sin(2 * Math.PI * 110 * i / sampleRate));
        }
        // One kick at most, where the tone starts
        assertTrue(detect(samples, sampleRate).length <= 1);
    }

    @Test
    void worksAtOtherSampleRates() {
        float sampleRate = 22_050f;
        float[] samples = new float[(int) (6 * sampleRate)];
        double[] expected = {1.0, 2.0, 3.0, 4.0, 5.0};
        for (double t0 : expected) {
            int start = (int) (t0 * sampleRate);
            double phase = 0;
            for (int i = 0; i < 0.3 * sampleRate; i++) {
                double t = i / sampleRate;
                phase += 2 * Math.PI * (45 + 110 * Math.exp(-t / 0.025)) / sampleRate;
                samples[start + i] += (float) (0.8 * Math.sin(phase) * Math.exp(-t / 0.15));
            }
        }
        assertEquals(1.0, BeatMetrics.fMeasure(expected, detect(samples, sampleRate), BeatMetrics.ONSET_WINDOW));
    }
}
