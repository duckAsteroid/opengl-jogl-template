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
import java.nio.ByteOrder;

import static com.asteroid.duck.opengl.util.audio.LineAcquirer.IDEAL;

class AudioReader implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(AudioReader.class);
    // 32 stereo samples (2 channels, 2 bytes per sample)
    public static final int CHUNK_SIZE = 16 * 2 * 2;
    // debug the chunk size to check we're reading in the expected sizes (except maybe the first and last reads)
    private Stats chunkSize = StatsFactory.stats("Audio: Chunk Size");
    private Stats available = StatsFactory.stats("Audio: Available");
    private byte[] audioChunk = new byte[CHUNK_SIZE];
    /** Persistently-mapped PBO buffer for GPU upload; null in CPU-only mode. */
    private final ByteBuffer gpuMapped;
    /**
     * Optional CPU-side ring buffer populated alongside the PBO.
     * Used by {@link SpectrumAnalyser} to feed the FFT pipeline without a separate audio thread.
     */
    private volatile com.asteroid.duck.opengl.util.audio.RollingFloatBuffer cpuBuffer = null;
    private volatile int head = 0;
    private AudioDataSource mLine = null;
    private volatile boolean running = true;
    private final int audioTextureByteSize;

    /**
     * @param gpuMapped            persistently-mapped PBO buffer, or {@code null} for CPU-only mode
     * @param audioTextureByteSize byte size of the PBO; ignored when {@code gpuMapped} is null
     */
    public AudioReader(ByteBuffer gpuMapped, int audioTextureByteSize) {
        this.gpuMapped = gpuMapped;
        this.audioTextureByteSize = audioTextureByteSize;
    }

    /**
     * Attach a CPU-side ring buffer that will receive every audio chunk alongside the PBO.
     * Safe to call from any thread; takes effect on the next chunk read.
     *
     * @param buffer the ring buffer to populate, or {@code null} to detach
     */
    public void setCpuBuffer(com.asteroid.duck.opengl.util.audio.RollingFloatBuffer buffer) {
        this.cpuBuffer = buffer;
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

                // write it into the GPU-mapped PBO (if one was provided)
                if (gpuMapped != null) {
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

                // also feed the optional CPU-side ring buffer (used by FFT pipeline)
                com.asteroid.duck.opengl.util.audio.RollingFloatBuffer cpu = this.cpuBuffer;
                if (cpu != null) {
                    // audio data is little-endian 16-bit stereo PCM
                    cpu.write(ByteBuffer.wrap(audioChunk, 0, read)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .asShortBuffer());
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
