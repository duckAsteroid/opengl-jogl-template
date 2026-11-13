package com.asteroid.duck.opengl.util.audio.beat.eval;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Writes tracks to disk as 16-bit mono WAV plus {@code .beats}, {@code .kicks} and
 * {@code .bpm} annotation files — the format {@link FileDataset} reads back, and one Sonic
 * Visualiser can import as time-instant layers.
 */
public final class TrackExporter {

    private TrackExporter() {}

    /**
     * @param tracks the tracks to export
     * @param dir    output directory (created if missing)
     * @throws IOException if a file can't be written
     */
    public static void export(List<Track> tracks, Path dir) throws IOException {
        Files.createDirectories(dir);
        for (Track track : tracks) {
            Track.Loaded loaded = track.loader().load();
            writeWav(loaded.audio(), dir.resolve(track.name() + ".wav"));
            Annotations a = loaded.annotations();
            if (a.beats().length > 0) Files.writeString(dir.resolve(track.name() + ".beats"), times(a.beats()));
            if (a.kicks().length > 0) Files.writeString(dir.resolve(track.name() + ".kicks"), times(a.kicks()));
            if (!Double.isNaN(a.bpm())) {
                Files.writeString(dir.resolve(track.name() + ".bpm"), String.format(Locale.ROOT, "%.3f%n", a.bpm()));
            }
            System.out.println("Wrote " + dir.resolve(track.name() + ".wav"));
        }
    }

    static void writeWav(AudioClip clip, Path file) throws IOException {
        float[] samples = clip.samples();
        byte[] pcm = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            int v = Math.round(Math.clamp(samples[i], -1f, 1f) * 32767f);
            pcm[2 * i] = (byte) v;
            pcm[2 * i + 1] = (byte) (v >> 8);
        }
        AudioFormat format = new AudioFormat(clip.sampleRate(), 16, 1, true, false);
        try (AudioInputStream in = new AudioInputStream(new ByteArrayInputStream(pcm), format, samples.length)) {
            AudioSystem.write(in, AudioFileFormat.Type.WAVE, file.toFile());
        }
    }

    private static String times(double[] times) {
        return Arrays.stream(times).mapToObj(t -> String.format(Locale.ROOT, "%.4f", t))
                .collect(Collectors.joining("\n", "", "\n"));
    }
}
