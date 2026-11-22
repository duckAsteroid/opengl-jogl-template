package com.asteroid.duck.opengl.util.audio.music;

import com.asteroid.duck.opengl.util.audio.AudioSink;
import com.asteroid.duck.opengl.util.audio.beat.BeatTrackerSink;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Live music analysis: an {@link AudioSink} that runs a {@link MusicAnalyser} on the audio thread
 * and lets the render thread read the latest {@link MusicState} and drain {@link MusicEvent}s.
 *
 * <p>The state is published through a volatile field every {@value MusicAnalyser#FRAME_SECONDS}
 * s (one small record each time). Events go into a bounded queue ({@value #EVENT_CAPACITY});
 * when nobody drains it, the oldest are dropped.</p>
 *
 * <pre>{@code
 * BeatTrackerSink beats = new BeatTrackerSink(48_000f);
 * MusicStateSink music = new MusicStateSink(48_000f, beats);   // tempo from the beat tracker
 * audioInput = AudioInput.start(ctx, List.of(freqProc, beats, music));
 * // each frame:
 * MusicState s = music.state();
 * glow = s.intensity();
 * for (MusicEvent e; (e = music.pollEvent()) != null; ) {
 *     if (e.type() == MusicEvent.Type.DROP) explode();
 * }
 * }</pre>
 */
public final class MusicStateSink implements AudioSink {

    static final int EVENT_CAPACITY = 64;
    private static final int BLOCK = 512;

    private final MusicAnalyser analyser;
    private final BeatTrackerSink tempo;
    private final float[] mono = new float[BLOCK];
    private final ArrayBlockingQueue<MusicEvent> events = new ArrayBlockingQueue<>(EVENT_CAPACITY);
    private volatile MusicState state = MusicState.INITIAL;

    /**
     * Without tempo: arousal is estimated from the other descriptors only, and track changes are
     * spotted by key alone.
     * @param sampleRate sample rate of the PCM written to this sink, in Hz
     */
    public MusicStateSink(float sampleRate) {
        this(sampleRate, null);
    }

    /**
     * @param sampleRate sample rate of the PCM written to this sink, in Hz
     * @param tempo      beat tracker to read the tempo from, or {@code null}
     */
    public MusicStateSink(float sampleRate, BeatTrackerSink tempo) {
        this.tempo = tempo;
        this.analyser = new MusicAnalyser(sampleRate, new MusicListener() {
            @Override
            public void onState(MusicState s) {
                state = s;
            }

            @Override
            public void onEvent(MusicEvent event) {
                while (!events.offer(event)) events.poll();
            }
        });
    }

    /** Audio thread: decode interleaved 16-bit little-endian stereo to mono and analyse it. */
    @Override
    public void write(byte[] data, int offset, int length) {
        if (tempo != null) analyser.setTempo(tempo.bpm());
        int frames = length / 4;
        int i = offset;
        for (int done = 0; done < frames; ) {
            int n = Math.min(BLOCK, frames - done);
            for (int f = 0; f < n; f++, i += 4) {
                int left = (short) ((data[i] & 0xFF) | (data[i + 1] << 8));
                int right = (short) ((data[i + 2] & 0xFF) | (data[i + 3] << 8));
                mono[f] = (left + right) / 65536f;
            }
            analyser.process(mono, 0, n);
            done += n;
        }
    }

    /** @return the latest state; never {@code null} ({@link MusicState#INITIAL} before any audio) */
    public MusicState state() {
        return state;
    }

    /** @return the oldest undelivered event, or {@code null} if there is none */
    public MusicEvent pollEvent() {
        return events.poll();
    }
}
