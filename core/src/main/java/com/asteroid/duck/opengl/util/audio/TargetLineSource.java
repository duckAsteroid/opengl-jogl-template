package com.asteroid.duck.opengl.util.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class TargetLineSource implements AudioDataSource {
	private final String name;
	private final TargetDataLine line;
	public TargetLineSource(String name, TargetDataLine line) {
		this.name = name;
		this.line = line;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int available() {
		return line.available();
	}

	@Override
	public int read(byte[] array, int start, int limit) {
		return line.read(array, start, limit);
	}

	@Override
	public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
		line.open(format, bufferSize);
	}

	@Override
	public boolean isOpen() {
		return line.isOpen();
	}

	@Override
	public void start() {
		line.start();
	}

	@Override
	public boolean isRunning() {
		return line.isRunning();
	}

	@Override
	public void stop() {
		line.stop();
	}

	@Override
	public void close() {
		line.close();
	}
}
