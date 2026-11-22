package com.asteroid.duck.opengl.util.audio.music;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.asteroid.duck.opengl.util.audio.music.Signals.*;
import static org.junit.jupiter.api.Assertions.*;

class KeyAndMoodTest {

    @Test
    void findsCMajor() {
        MusicState s = run(chords(C_MAJOR, 12)).at(10);
        assertEquals(new Key(0, true), s.key());
        assertTrue(s.keyConfidence() > 0.4, "confidence " + s.keyConfidence());
    }

    @Test
    void findsAMinor() {
        assertEquals(new Key(9, false), run(chords(A_MINOR, 12)).at(10).key());
    }

    @Test
    void findsATransposedKey() {
        assertEquals(new Key(4, true), run(chords(transpose(C_MAJOR, 4), 12)).at(10).key());
        assertEquals(new Key(2, false), run(chords(transpose(A_MINOR, 5), 12)).at(10).key());
    }

    @Test
    void namesKeys() {
        assertEquals("A minor", new Key(9, false).name());
        assertEquals("Eb major", new Key(3, true).name());
        assertThrows(IllegalArgumentException.class, () -> new Key(12, true));
    }

    @Test
    void followsAKeyChangeWithoutFlickering() {
        MusicRun run = run(concat(chords(C_MAJOR, 30), chords(transpose(C_MAJOR, 3), 40)));
        assertEquals(new Key(0, true), run.at(28).key());
        assertEquals(new Key(3, true), run.at(69).key());
        long changes = 0;
        Key previous = null;
        for (MusicState s : run.states) {
            if (s.key() != null && !s.key().equals(previous)) changes++;
            if (s.key() != null) previous = s.key();
        }
        // C major → (at most one intermediate key) → Eb major
        assertTrue(changes <= 3, changes + " key changes");
    }

    @Test
    void majorIsMorePositiveThanMinor() {
        float major = run(chords(C_MAJOR, 30)).at(29).valence();
        float minor = run(chords(A_MINOR, 30)).at(29).valence();
        assertTrue(major > 0.55, "major " + major);
        assertTrue(minor < 0.45, "minor " + minor);
    }

    @Test
    void aGrooveIsMoreArousingThanAPad() throws IOException {
        float pad = run(chords(C_MAJOR, 30)).at(29).arousal();
        float groove = run(synthetic("four-on-floor-128")).at(29).arousal();
        assertTrue(groove > pad + 0.2, "groove " + groove + ", pad " + pad);
    }

    @Test
    void keyIsUnknownUntilThereIsEnoughToGoOn() {
        MusicRun run = run(chords(C_MAJOR, 12));
        assertNull(run.at(1).key());
        assertEquals(0, run.at(1).keyConfidence());
    }
}
