package com.asteroid.duck.opengl.util.audio.beat;

import com.asteroid.duck.opengl.util.audio.AudioReader;
import com.asteroid.duck.opengl.util.audio.beat.eval.SyntheticTracks;
import com.asteroid.duck.opengl.util.audio.beat.eval.Track;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class BeatTrackerSinkTest {

    /** Mono floats → interleaved 16-bit little-endian stereo, as {@link AudioReader} delivers. */
    private static byte[] stereoPcm(float[] mono) {
        byte[] pcm = new byte[mono.length * 4];
        for (int i = 0; i < mono.length; i++) {
            int v = Math.round(Math.clamp(mono[i], -1f, 1f) * 32767f);
            for (int c = 0; c < 2; c++) {
                pcm[4 * i + 2 * c] = (byte) v;
                pcm[4 * i + 2 * c + 1] = (byte) (v >> 8);
            }
        }
        return pcm;
    }

    private static byte[] fourOnTheFloor() throws IOException {
        Track track = SyntheticTracks.all().stream().filter(t -> t.name().equals("four-on-floor-128")).findFirst().orElseThrow();
        return stereoPcm(track.loader().load().audio().samples());
    }

    @Test
    void tracksPcmWrittenInAudioReaderSizedChunks() throws IOException {
        byte[] pcm = fourOnTheFloor();
        BeatTrackerSink sink = new BeatTrackerSink(SyntheticTracks.SAMPLE_RATE);
        assertTrue(Double.isNaN(sink.bpm()));
        assertEquals(0, sink.beatPhase());

        for (int offset = 0; offset < pcm.length; offset += AudioReader.CHUNK_SIZE) {
            sink.write(pcm, offset, Math.min(AudioReader.CHUNK_SIZE, pcm.length - offset));
        }

        assertEquals(128, sink.bpm(), 128 * 0.02);
        assertTrue(sink.confidence() > 0);
        double phase = sink.beatPhase();
        assertTrue(phase >= 0 && phase < 1, "phase " + phase);
        // ~30 s at 128 BPM, less the few seconds it takes to lock on
        assertTrue(sink.beatCount() > 50 && sink.beatCount() < 70, "beats " + sink.beatCount());
    }

    @Test
    void writesStayWellInsideTheAudioLineBuffer() throws IOException {
        // The capture line buffer holds ~2.7 ms; a write that takes longer risks a dropout.
        byte[] pcm = fourOnTheFloor();
        BeatTrackerSink sink = new BeatTrackerSink(SyntheticTracks.SAMPLE_RATE);
        long worst = 0;
        long total = 0;
        int writes = 0;
        for (int pass = 0; pass < 2; pass++) {  // first pass warms up the JIT
            worst = 0;
            total = 0;
            writes = 0;
            for (int offset = 0; offset < pcm.length; offset += AudioReader.CHUNK_SIZE) {
                long start = System.nanoTime();
                sink.write(pcm, offset, Math.min(AudioReader.CHUNK_SIZE, pcm.length - offset));
                long took = System.nanoTime() - start;
                worst = Math.max(worst, took);
                total += took;
                writes++;
            }
        }
        double audioSeconds = pcm.length / 4.0 / SyntheticTracks.SAMPLE_RATE;
        double cpuShare = total / 1e9 / audioSeconds;
        System.out.printf("BeatTrackerSink: %d writes, mean %.1f µs, worst %.2f ms, %.1f%% of real time%n",
                writes, total / 1e3 / writes, worst / 1e6, 100 * cpuShare);
        assertTrue(cpuShare < 0.1, "uses " + cpuShare + " of real time");
    }
}
