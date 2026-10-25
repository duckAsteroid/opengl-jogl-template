package com.asteroid.duck.opengl.util.audio;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AudioReaderTest {

    /** A line that never has any data, counting how often the reader polls it. */
    static class EmptyLine implements AudioLine {
        final AtomicInteger polls = new AtomicInteger();

        @Override public int available() { polls.incrementAndGet(); return 0; }
        @Override public int read(byte[] array, int start, int length) { return 0; }
        @Override public void start() { }
        @Override public void stop() { }
        @Override public boolean isRunning() { return true; }
        @Override public void close() { }
    }

    static AudioDataSource sourceOf(AudioLine line) {
        return new AudioDataSource() {
            @Override public String getName() { return "test"; }
            @Override public AudioFormat[] getSupportedFormats() { return new AudioFormat[] { LineAcquirer.IDEAL }; }
            @Override public AudioLine open(AudioFormat format, int bufferSize) { return line; }
        };
    }

    /** Issue #29: an empty line must park between polls, not busy-spin on Thread.yield(). */
    @Test
    void idleReaderParksInsteadOfSpinning() throws InterruptedException {
        EmptyLine line = new EmptyLine();
        AudioReader reader = new AudioReader(List.of((chunk, offset, length) -> { }));
        Thread thread = Thread.ofPlatform().daemon().start(reader);
        reader.setLine(sourceOf(line));

        Thread.sleep(200);
        reader.setRunning(false);
        thread.join(1000);
        assertFalse(thread.isAlive(), "reader should stop once running is cleared");

        // Parking 0.5 ms allows at most ~400 polls in 200 ms; a yield loop manages millions.
        int polls = line.polls.get();
        assertTrue(polls > 0, "reader should have polled the line");
        assertTrue(polls < 1_000, "reader busy-spun: " + polls + " polls in 200 ms");
    }
}
