package com.asteriod.duck.opengl;

import java.util.concurrent.atomic.AtomicBoolean;

public class Timer {
	private boolean paused;
	private long lastUpdate;
	private long elapsed;

	public Timer() {
		reset();
	}

	public void reset() {
		this.paused = false;
		this.lastUpdate = System.currentTimeMillis();
		this.elapsed = 0;
	}

	public void step(long stepSize) {
		elapsed += stepSize;
	}

	public long elapsed() {
		return elapsed;
	}

	public void update() {
		if (!paused) {
			long now = System.currentTimeMillis();
			long delta = now - lastUpdate;
			lastUpdate = now;
			elapsed += delta;
		}
	}

	public boolean isPaused() {
		return paused;
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
	}

	public boolean togglePaused() {
		this.paused = !this.paused;
		return this.paused;
	}

	@Override
	public String toString() {
		return elapsed() + " ms";
	}
}
