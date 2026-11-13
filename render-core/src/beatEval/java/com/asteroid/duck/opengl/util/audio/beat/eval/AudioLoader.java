package com.asteroid.duck.opengl.util.audio.beat.eval;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Decodes audio files to mono float samples. Java Sound handles WAV, AU and AIFF; anything else
 * (MP3, FLAC, ...) is decoded by {@code ffmpeg} if it is on the {@code PATH}.
 */
public final class AudioLoader {

    /** Sample rate {@code ffmpeg} decodes to. */
    static final float FFMPEG_SAMPLE_RATE = 44_100f;

    private AudioLoader() {}

    /**
     * Decode an audio file to mono, averaging its channels.
     * @param path the audio file
     * @return the decoded clip at the file's own sample rate (or {@value #FFMPEG_SAMPLE_RATE} Hz
     *         when decoded by {@code ffmpeg})
     * @throws IOException if the file can't be read or decoded
     */
    public static AudioClip load(Path path) throws IOException {
        AudioInputStream in;
        try {
            in = AudioSystem.getAudioInputStream(path.toFile());
        } catch (UnsupportedAudioFileException e) {
            return loadWithFfmpeg(path);
        }
        try (in) {
            AudioFormat source = in.getFormat();
            int channels = source.getChannels();
            AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, source.getSampleRate(),
                    16, channels, 2 * channels, source.getSampleRate(), false);
            try (AudioInputStream decoded = AudioSystem.getAudioInputStream(pcm, in)) {
                return new AudioClip(toMono(decoded.readAllBytes(), channels), source.getSampleRate());
            }
        } catch (IllegalArgumentException e) {
            throw new IOException("Can't convert " + path + " to 16-bit PCM", e);
        }
    }

    private static AudioClip loadWithFfmpeg(Path path) throws IOException {
        Process ffmpeg;
        try {
            ffmpeg = new ProcessBuilder("ffmpeg", "-v", "error", "-i", path.toString(),
                    "-f", "s16le", "-ac", "1", "-ar", String.valueOf((int) FFMPEG_SAMPLE_RATE), "-")
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (IOException e) {
            throw new IOException("Java Sound can't decode " + path + " and ffmpeg isn't available", e);
        }
        byte[] bytes;
        try (InputStream out = ffmpeg.getInputStream()) {
            bytes = out.readAllBytes();
        }
        try {
            if (ffmpeg.waitFor() != 0) {
                throw new IOException("ffmpeg failed to decode " + path);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted decoding " + path, e);
        }
        return new AudioClip(toMono(bytes, 1), FFMPEG_SAMPLE_RATE);
    }

    /** Little-endian signed 16-bit interleaved PCM → mono floats (channel average). */
    static float[] toMono(byte[] pcm, int channels) {
        int frames = pcm.length / (2 * channels);
        float[] mono = new float[frames];
        int i = 0;
        for (int f = 0; f < frames; f++) {
            int sum = 0;
            for (int c = 0; c < channels; c++, i += 2) {
                sum += (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
            }
            mono[f] = sum / (channels * 32768f);
        }
        return mono;
    }
}
