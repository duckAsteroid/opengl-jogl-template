package com.asteroid.duck.opengl.util.audio.music;

import com.asteroid.duck.opengl.util.audio.beat.eval.SyntheticTracks;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static com.asteroid.duck.opengl.util.audio.music.Signals.*;
import static org.junit.jupiter.api.Assertions.*;

class MusicAnalyserTest {

    @Test
    void reportsAStateEveryFrame() {
        MusicRun run = run(tone(440, 2, 0.3));
        assertEquals(2 / MusicAnalyser.FRAME_SECONDS, run.states.size(), 1);
        assertEquals(MusicAnalyser.FRAME_SECONDS, run.states.get(1).time() - run.states.get(0).time(), 1e-3);
    }

    @Test
    void intensityFallsThroughABreakdownAndRecovers() {
        SyntheticTracks.Arranged track = SyntheticTracks.arranged128();
        MusicRun run = run(track.loaded());
        double groove = run.mean(track.groove() + 4, track.breakdown(), MusicState::intensity);
        double breakdown = run.mean(track.breakdown() + 2, track.buildup(), MusicState::intensity);
        double after = run.mean(track.drop() + 2, track.drop() + 20, MusicState::intensity);
        assertTrue(groove > 0.9, "groove " + groove);
        assertTrue(breakdown < 0.6, "breakdown " + breakdown);
        assertTrue(after > 0.85, "after the drop " + after);

        double bassGroove = run.mean(track.groove() + 4, track.breakdown(), MusicState::bass);
        double bassBreakdown = run.mean(track.breakdown() + 2, track.buildup(), MusicState::bass);
        assertTrue(bassGroove > 0.8 && bassBreakdown < 0.2, bassGroove + " / " + bassBreakdown);
    }

    @Test
    void kickRateFollowsTheKick() {
        SyntheticTracks.Arranged track = SyntheticTracks.arranged128();
        MusicRun run = run(track.loaded());
        // 128 BPM = 2.13 kicks/s = 0.85 of the full-scale rate
        double groove = run.mean(track.groove() + 4, track.breakdown(), MusicState::kickRate);
        double breakdown = run.mean(track.breakdown() + 4, track.buildup(), MusicState::kickRate);
        assertEquals(0.85, groove, 0.1);
        assertTrue(breakdown < 0.2, "breakdown " + breakdown);
    }

    @Test
    void brightnessFollowsPitch() {
        double low = run(tone(150, 3, 0.3)).at(2.5).brightness();
        double high = run(tone(3000, 3, 0.3)).at(2.5).brightness();
        assertTrue(low < 0.2, "low " + low);
        assertTrue(high > 0.7, "high " + high);
    }

    @Test
    void noiseIsNoisierThanATone() {
        double noise = run(noise(3, 0.3)).at(2.5).noisiness();
        double tone = run(tone(440, 3, 0.3)).at(2.5).noisiness();
        assertTrue(noise > 0.8, "noise " + noise);
        assertTrue(tone < 0.2, "tone " + tone);
    }

    @Test
    void bandSharesSumToOne() {
        MusicState s = run(concat(tone(80, 2, 0.3), noise(2, 0.1))).at(3.5);
        assertEquals(1, s.low() + s.mid() + s.high(), 0.02);
        assertTrue(s.high() > 0.05 && s.low() > 0.05);
    }

    @Test
    void busynessCountsOnsets() throws IOException {
        double pad = run(chords(C_MAJOR, 12)).at(11).busyness();
        double groove = run(synthetic("four-on-floor-128")).mean(10, 20, MusicState::busyness);
        assertTrue(pad < 0.1, "pad " + pad);
        assertTrue(groove > 0.3, "groove " + groove);
    }

    @Test
    void flagsSilenceAndReportsMusicStarting() {
        MusicRun run = run(concat(tone(440, 3, 0.3), silence(3), tone(440, 3, 0.3)));
        assertFalse(run.at(2).silent());
        assertTrue(run.at(5.5).silent());
        assertEquals(Section.SILENCE, run.at(5.5).section());
        assertFalse(run.at(8).silent());
        assertEquals(List.of(MusicEvent.Type.MUSIC_START, MusicEvent.Type.SILENCE_START, MusicEvent.Type.MUSIC_START),
                run.events.stream().map(MusicEvent::type).toList());
        // silence is declared 1.5 s after the sound stops
        assertEquals(4.5, run.events(MusicEvent.Type.SILENCE_START).getFirst().time(), 0.1);
    }

    @Test
    void sinkPublishesStatesAndQueuesEvents() throws IOException {
        float[] mono = synthetic("four-on-floor-128").audio().samples();
        byte[] pcm = new byte[mono.length * 4];
        for (int i = 0; i < mono.length; i++) {
            int v = Math.round(Math.clamp(mono[i], -1f, 1f) * 32767f);
            for (int c = 0; c < 2; c++) {
                pcm[4 * i + 2 * c] = (byte) v;
                pcm[4 * i + 2 * c + 1] = (byte) (v >> 8);
            }
        }
        MusicStateSink sink = new MusicStateSink(RATE);
        assertSame(MusicState.INITIAL, sink.state());
        assertNull(sink.pollEvent());
        for (int offset = 0; offset < pcm.length; offset += 4096) {
            sink.write(pcm, offset, Math.min(4096, pcm.length - offset));
        }
        MusicState s = sink.state();
        assertEquals(mono.length / RATE, s.time(), 0.1);
        assertTrue(s.intensity() > 0.8);
        assertEquals(Section.GROOVE, s.section());
        assertEquals(MusicEvent.Type.MUSIC_START, sink.pollEvent().type());
    }
}
