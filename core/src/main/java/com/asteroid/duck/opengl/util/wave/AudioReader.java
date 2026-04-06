package com.asteroid.duck.opengl.util.wave;

import com.asteroid.duck.opengl.util.audio.AudioDataSource;
import com.asteroid.duck.opengl.util.resources.shader.Uniform;
import com.asteroid.duck.opengl.util.resources.shader.vars.ShaderVariable;
import com.asteroid.duck.opengl.util.stats.Stats;
import com.asteroid.duck.opengl.util.stats.StatsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import static com.asteroid.duck.opengl.util.audio.LineAcquirer.IDEAL;

class AudioReader implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(AudioReader.class);
    // 32 stereo samples (2 channels, 2 bytes per sample)
    public static final int CHUNK_SIZE = 16 * 2 * 2;
    // debug the chunk size to check we're reading in the expected sizes (except maybe the first and last reads)
    private Stats chunkSize = StatsFactory.stats("Audio: Chunk Size");
    private Stats available = StatsFactory.stats("Audio: Available");
    private byte[] audioChunk = new byte[CHUNK_SIZE];
    private final ByteBuffer gpuMapped;
    private int head = 0;
    private AudioDataSource mLine = null;
    private boolean running = true;
    private final int audioTextureByteSize;

    public AudioReader(ByteBuffer gpuMapped, int audioTextureByteSize) {
        this.gpuMapped = gpuMapped;
        this.audioTextureByteSize = audioTextureByteSize;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public int getHead() {
        return head;
    }

    public static int getChunkSize() {
        return CHUNK_SIZE;
    }

    public void run() {
        // Read in small chunks for low lag
        AudioDataSource line = null;
        try {
            while (running) {
                line = waitForLine();
                available.add(line.available());
                // read a chunk from audio line
                int read = line.read(audioChunk, 0, CHUNK_SIZE);
                chunkSize.add(read);

                // write it into the mapped buffer at the current head position
                gpuMapped.position(head);
                // will it fit in the remaining space at the end of the buffer?
                if (head + read > audioTextureByteSize) {
                    // if not, we need to wrap around - fit in what we can
                    int firstPart = audioTextureByteSize - head;
                    gpuMapped.put(audioChunk, 0, firstPart);
                    //  write the remaining part at the beginning of the buffer
                    gpuMapped.position(0);
                    gpuMapped.put(audioChunk, firstPart, read - firstPart);
                    head = read - firstPart;
                }
                else {
                    gpuMapped.put(audioChunk, 0, read);
                    // move write pointer and wrap around if needed
                    head = (head + read) % audioTextureByteSize;
                }

            }
            if (line != null) {
                line.stop();
                line.close();
            }
        }
        catch(InterruptedException | BufferOverflowException e) {
            LOG.error("Audio reader thread, stopping.", e);
        }
        System.out.println("chunk=" + chunkSize);
        System.out.println("available=" + available);
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
                        line.open(IDEAL, CHUNK_SIZE);
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
