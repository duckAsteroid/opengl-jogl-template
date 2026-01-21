package com.asteroid.duck.opengl.util.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;

/**
 * Our interface into an underlying (possibly simulated) audio data source
 */
public interface AudioDataSource {
	/**
	 * Open using a given format
	 * @param format the format
	 * @param bufferSize buffer size in bytes
	 * @throws LineUnavailableException if can't do it
	 */
	void open(AudioFormat format, int bufferSize) throws LineUnavailableException;

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
	 * @param limit maximum number of bytes to read
	 * @return number of bytes actually read
	 */
	int read(byte[] array, int start, int limit);
	/**
	 * Stop the data source (can be restarted)
	 */
	void stop();

	/**
	 * Close the data source (cannot be restarted)
	 */
	void close();
}
