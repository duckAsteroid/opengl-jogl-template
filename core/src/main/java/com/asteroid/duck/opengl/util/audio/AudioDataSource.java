package com.asteroid.duck.opengl.util.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

/**
 * Our interface into an underlying (possibly simulated) audio data source
 */
public interface AudioDataSource {
    /**
     * A human-readable name for this source, used in log messages and UI.
     *
     * @return display name; never {@code null}
     */
	String getName();
	/**
	 * Open using a given format
	 * @param format the format
	 * @param bufferSize buffer size in bytes
	 * @throws LineUnavailableException if can't do it
	 */
	void open(AudioFormat format, int bufferSize) throws LineUnavailableException;

    /**
     * Returns {@code true} if the line has been opened via {@link #open} and not yet closed.
     * A source that is open but not {@linkplain #isRunning() running} has been paused.
     *
     * @return {@code true} if the underlying line is open
     */
	boolean isOpen();

	/**
	 * Start the data source
	 */
	void start();
	/**
	 * How many bytes are available to read
	 * @return number of bytes available
	 */
	int available();
	/**
	 * Read raw audio data into the given array
	 * @param array the array to read into
	 * @param start start index in the array, the first byte read will go here
	 * @param length maximum number of bytes to read
	 * @return number of bytes actually read
	 */
	int read(byte[] array, int start, int length);
	/**
	 * Stop the data source (can be restarted)
	 */
	void stop();

	/**
	 * Close the data source (cannot be restarted)
	 */
	void close();

    /**
     * Returns {@code true} if the source has been {@linkplain #start() started} and is actively
     * delivering audio data. A line can be open but not running if it has been stopped.
     *
     * @return {@code true} if the source is currently capturing and delivering data
     */
	boolean isRunning();
}
