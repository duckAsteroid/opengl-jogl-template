package com.asteroid.duck.opengl.util.audio.music;

import com.asteroid.duck.opengl.util.audio.beat.eval.SyntheticTracks;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static com.asteroid.duck.opengl.util.audio.music.MusicEvent.Type.*;
import static com.asteroid.duck.opengl.util.audio.music.Signals.*;
import static org.junit.jupiter.api.Assertions.*;

class StructureDetectorTest {

    private static void assertOne(MusicRun run, MusicEvent.Type type, double from, double to) {
        List<MusicEvent> events = run.events(type);
        assertEquals(1, events.size(), type + ": " + run.events);
        double time = events.getFirst().time();
        assertTrue(time >= from && time <= to, type + " at " + time + ", expected " + from + "–" + to);
    }

    @Test
    void followsABreakdownBuildUpAndDrop() {
        SyntheticTracks.Arranged track = SyntheticTracks.arranged128();
        MusicRun run = run(track.loaded());

        assertOne(run, BREAKDOWN_START, track.breakdown(), track.breakdown() + 3);
        assertOne(run, BUILDUP_START, track.buildup(), track.drop());
        assertOne(run, DROP, track.drop(), track.drop() + 1);
        assertTrue(run.events(TRACK_CHANGE).isEmpty(), run.events.toString());

        assertEquals(Section.GROOVE, run.at(track.breakdown() - 1).section());
        assertEquals(Section.BREAKDOWN, run.at(track.buildup() - 1).section());
        assertEquals(Section.BUILDUP, run.at(track.drop() - 0.2).section());
        assertEquals(Section.GROOVE, run.at(track.drop() + 2).section());
        MusicState afterDrop = run.at(track.drop() + 2);
        assertEquals(run.events(DROP).getFirst().time(), afterDrop.sectionStart(), 1e-9);
    }

    @Test
    void findsTheBreakdownInBreakdown126() throws IOException {
        // 8 bars full, 8 bars breakdown with a riser over its second half, 8 bars full, at 126 BPM
        double bar = 4 * 60.0 / 126;
        double breakdown = 0.5 + 8 * bar;
        double drop = 0.5 + 16 * bar;
        MusicRun run = run(synthetic("breakdown-126"));
        assertOne(run, BREAKDOWN_START, breakdown, breakdown + 3);
        assertOne(run, BUILDUP_START, breakdown + 4 * bar, drop);
        assertOne(run, DROP, drop, drop + 1);
    }

    @Test
    void aSteadyGrooveHasNoStructureEvents() throws IOException {
        MusicRun run = run(synthetic("four-on-floor-128"));
        assertEquals(List.of(MUSIC_START), run.events.stream().map(MusicEvent::type).toList());
        assertTrue(run.states.stream().allMatch(s -> s.section() == Section.GROOVE));
    }

    @Test
    void musicWithoutAKickNeverBreaksDown() {
        // a pad, then a pause in the pad: quieter, but there was never a kick to drop out
        MusicRun run = run(concat(chords(C_MAJOR, 20), tone(220, 10, 0.02), chords(C_MAJOR, 10)));
        assertTrue(run.events(BREAKDOWN_START).isEmpty(), run.events.toString());
    }

    @Test
    void spotsATrackChangeInAGaplessMix() {
        double switchTime = SyntheticTracks.mixPoint();
        MusicRun run = run(SyntheticTracks.mixedTracks());
        assertOne(run, SECTION_CHANGE, switchTime, switchTime + 5);
        assertOne(run, TRACK_CHANGE, switchTime, switchTime + 10);
    }
}
