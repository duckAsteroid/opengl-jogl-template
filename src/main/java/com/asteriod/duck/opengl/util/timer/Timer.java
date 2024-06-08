package com.asteriod.duck.opengl.util.timer;

import org.lwjgl.glfw.GLFW;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This provides the ability to measure elapsed time in the GL application.
 * The timer can be paused and resumed to aid debugging.
 * The timer is decoupled from the time source. Two sources are supported currently:
 * {@link #glfwGetTimeInstance()} - using Open GL time
 * {@link #systemCurrentTimeMillisInstance()} - using {@link System#currentTimeMillis()}
 */
public class Timer {
	private final AtomicBoolean paused = new AtomicBoolean(false);
	/**
	 * A source of timestamps in seconds since some arbitrary epoch
	 */
	private final Callable<Double> timeSource;
	/**
	 * The measured elapsed time - calculated in the last update
	 */
	private double elapsed;
	/**
	 * The timestamp of the last update
	 */
	private double lastUpdate;
	private long updateCount;
	private double updateDeltaSum;

	public static Timer glfwGetTimeInstance() {
		return new Timer(GLFW::glfwGetTime);
	}

	public static Timer systemCurrentTimeMillisInstance() {
		return new Timer(()->System.currentTimeMillis() / 1000.0);
	}

	public Timer(Callable<Double> timeSource) {
		this.timeSource = timeSource;
		reset();
	}

	/**
	 * Reset the timer to start counting elapsed time from {@link #now()}
	 */
	public void reset() {
		setPaused(false);
		this.lastUpdate = now();
		this.elapsed = 0;
	}

	/**
	 * Get the latest timestamp from the source.
	 * @return the timestamp
	 * @throws RuntimeException if cannot get data from source
	 */
	private double now() {
		try {
			return timeSource.call();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Used to manually step forward by an amount (also in seconds)
	 * @param stepSize
	 */
	public void step(double stepSize) {
		elapsed += stepSize;
	}

	/**
	 * The time elapsed when updated (in seconds) since the timer started
	 * @return elapsed time in seconds
	 */
	public double elapsed() {
		return elapsed;
	}

	/**
	 * Calculates the value of a wave function between -1 and 1
	 * @param frequency the frequency in hertz
	 * @param phase the relative phase in seconds
	 * @return the wave function value at the current elapsed time
	 */
	public double waveFunction(double frequency, double phase) {
		return Math.sin(2 * Math.PI * frequency * (elapsed + phase));
	}

	/**
	 * Called to update the timer with a new elapsed time calculated from {@link #now()}
	 */
	public void update() {
		if (!paused.get()) {
			double now = now();
			double delta = now - lastUpdate;
			lastUpdate = now;
			elapsed += delta;
			updateCount++;
			updateDeltaSum += delta;
		}
	}

	/**
	 * Is the time paused?
	 */
	public boolean isPaused() {
		return paused.get();
	}

	/**
	 * Pause or unpause the timer
	 */
	public void setPaused(boolean paused) {
		this.paused.set(paused);
	}

	public boolean togglePaused() {
		boolean current = paused.get();
		boolean set = false;
		while(!set) {
			set = paused.compareAndSet(current, !current);
		}
		return !current;
	}

	@Override
	public String toString() {
		double averageDelta = updateDeltaSum / updateCount;
		return elapsed() + " ms ("+averageDelta+"s)";
	}
}
