package com.asteriod.duck.opengl.util.timer;

import org.lwjgl.glfw.GLFW;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

public class Timer {
	private boolean paused;
	private final Callable<Double> timeSource;
	private double elapsed;
	private double lastUpdate;

	public static Timer glfwGetTimeInstance() {
		return new Timer(GLFW::glfwGetTime);
	}

	public static Timer systemCurrentTimeMillisInstance() {
		return new Timer(()->(double)System.currentTimeMillis());
	}
	public Timer(Callable<Double> timeSource) {
		this.timeSource = timeSource;
		reset();
	}

	public void reset() {
		this.paused = false;
		this.lastUpdate = now();
		this.elapsed = 0;
	}

	private double now() {
		try {
			return timeSource.call();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void step(long stepSize) {
		elapsed += stepSize;
	}

	public double elapsed() {
		return elapsed;
	}

	public void update() {
		if (!paused) {
			double now = now();
			double delta = now - lastUpdate;
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
