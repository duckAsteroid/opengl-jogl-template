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
	void start();

	int available();
	int read(byte[] array, int start, int limit);

	void stop();
	void close();
}
