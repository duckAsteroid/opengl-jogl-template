package com.asteroid.duck.opengl.util.audio.beat.eval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationsTest {

    @TempDir
    Path dir;

    private Path write(String name, String content) throws IOException {
        Path file = dir.resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    @Test
    void readsBeatTimesFromTheFirstColumnSkippingHeadersAndComments() throws IOException {
        Path beats = write("a.beats", "# tapped\ntime,label\n1.5\t2\n0.5\t1\n2.5,3\n\n");
        assertArrayEquals(new double[] {0.5, 1.5, 2.5}, Annotations.readTimes(beats));
    }

    @Test
    void readsScientificNotationTempo() throws IOException {
        assertEquals(125.87, Annotations.readBpm(write("a.bpm", "1.258700000000000045e+02\n")), 1e-9);
    }

    @Test
    void derivesTempoFromBeatsWhenNotAnnotated() throws IOException {
        Annotations a = Annotations.load(List.of(write("a.beats", "0.5\n1.0\n1.5\n2.0\n")));
        assertEquals(120.0, a.bpm(), 1e-9);
        assertEquals(0, a.kicks().length);
    }

    @Test
    void readsJamsInBothDataLayouts() throws IOException {
        Path listLayout = write("list.jams", """
                {"annotations": [
                  {"namespace": "tempo", "data": [
                    {"time": 0, "duration": 30, "value": 64.0, "confidence": 0.3},
                    {"time": 0, "duration": 30, "value": 128.0, "confidence": 0.7}]},
                  {"namespace": "beat", "data": [
                    {"time": 1.0, "duration": 0, "value": 2, "confidence": null},
                    {"time": 0.5, "duration": 0, "value": 1, "confidence": null}]}
                ]}""");
        Annotations list = Annotations.readJams(listLayout);
        assertArrayEquals(new double[] {0.5, 1.0}, list.beats());
        assertEquals(128.0, list.bpm());

        Path columnLayout = write("column.jams", """
                {"annotations": [
                  {"namespace": "beat_position", "data": {"time": [0.25, 0.75], "value": [1, 2]}}
                ]}""");
        assertArrayEquals(new double[] {0.25, 0.75}, Annotations.readJams(columnLayout).beats());
    }

    @Test
    void pairsAudioWithDifferentlyNamedAnnotations() throws IOException {
        write("audio/blues/blues.00000.au", "");
        write("audio/blues/blues.00001.au", "");
        write("audio/rock/rock.00000.au", "");
        write("annotations/beats/gtzan_blues_00000.beats", "0.5\n1.0\n");
        write("annotations/tempo/gtzan_blues_00000.bpm", "120\n");
        write("annotations/beats/gtzan_rock_00000.beats", "0.5\n1.0\n");

        FileDataset.Scan scan = FileDataset.scan(dir.resolve("audio"), dir.resolve("annotations"));
        assertEquals(List.of("blues/blues.00000", "rock/rock.00000"), scan.tracks().stream().map(Track::id).toList());
        assertEquals(1, scan.unannotated());
    }

    @Test
    void keysNormaliseSeparatorsAndStripExtensions() {
        assertEquals("blues.00000", FileDataset.key(Path.of("blues.00000.wav.jams")));
        assertEquals("blues.00000", FileDataset.key(Path.of("Blues_00000.bpm")));
        assertEquals("track.one", FileDataset.key(Path.of("track one.mp3")));
    }
}
