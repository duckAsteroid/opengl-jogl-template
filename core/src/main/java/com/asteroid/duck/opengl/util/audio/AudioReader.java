package com.asteroid.duck.opengl.util.audio;

import com.asteroid.duck.opengl.util.stats.Stats;
import com.asteroid.duck.opengl.util.stats.StatsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.LineUnavailableException;
import java.util.List;

import static com.asteroid.duck.opengl.util.audio.LineAcquirer.IDEAL;

public class AudioReader implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(AudioReader.class);
    // TargetDataLine internal buffer: 128 stereo frames at 48 kHz ≈ 2.7 ms.
    public static final int LINE_BUFFER_SIZE = 128 * 2 * 2; // 512 bytes
    // Per-iteration read chunk: 16 stereo frames ≈ 0.33 ms.
    public static final int CHUNK_SIZE = 16 * 2 * 2; // 64 bytes

    private final Stats chunkSize = StatsFactory.stats("Audio: Chunk Size");
    private final Stats available = StatsFactory.stats("Audio: Available");
    private final byte[] audioChunk = new byte[CHUNK_SIZE];
    private final List<AudioSink> sinks;
    private AudioDataSource mLine = null;
    private volatile boolean running = true;

    public AudioReader(List<AudioSink> sinks) {
        this.sinks = sinks;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void run() {
        AudioDataSource line = null;
        try {
            while (running) {
                line = waitForLine();
                int avail = line.available();
                if (avail <= 0) {
                    Thread.yield();
                    continue;
                }
                available.add(avail);
                int toRead = Math.min(avail, CHUNK_SIZE);
                int read = line.read(audioChunk, 0, toRead);
                chunkSize.add(read);

                for (AudioSink sink : sinks) {
                    sink.write(audioChunk, 0, read);
                }
            }
            if (line != null) {
                line.stop();
                line.close();
            }
        } catch (InterruptedException e) {
            LOG.error("Audio reader thread, stopping.", e);
        }
        LOG.debug("Audio reader stopped. chunk={}, available={}", chunkSize, available);
    }

    protected AudioDataSource waitForLine() throws InterruptedException {
        synchronized (this) {
            while (mLine == null) {
                LOG.debug("Waiting for audio line...");
                wait();
            }
            return mLine;
        }
    }

    public void setLine(AudioDataSource line) {
        synchronized (this) {
            if (this.mLine != null) {
                this.mLine.stop();
                this.mLine.close();
            }
            if (line != null) {
                try {
                    if (!line.isOpen()) {
                        line.open(IDEAL, LINE_BUFFER_SIZE);
                    }
                    if (!line.isRunning()) {
                        line.start();
                    }
                } catch (LineUnavailableException e) {
                    LOG.error("Line unavailable: {}", line.getName(), e);
                    line = null;
                }
            }
            this.mLine = line;
            notifyAll();
        }
    }
}
