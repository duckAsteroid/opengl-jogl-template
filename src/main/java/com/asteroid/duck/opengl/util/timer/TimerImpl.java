package com.asteroid.duck.opengl.util.timer;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This provides the ability to measure elapsed time in the GL application.
 * The timer can be paused and resumed to aid debugging.
 * The timer is decoupled from the time source. Two sources are supported currently:
 * {@link TimeSource#glfwGetTimeInstance()} - using Open GL time
 * {@link TimeSource#systemCurrentTimeMillisInstance()} - using {@link System#currentTimeMillis()}
 */
public class TimerImpl implements Timer {
	// records the last known time when the timer is paused
	private final AtomicReference<Double> paused = new AtomicReference<>(null);
	/**
	 * A source of timestamps in seconds since some arbitrary epoch
	 */
	private final Callable<Double> timeSource;
	/**
	 * The elapsed time accumulated so far.
	 * This can accumulate in {@link #update()} (when not paused) or by calls to {@link #step(double)}
	 */
	private double elapsed;
	/**
	 * The timestamp of the last update
	 */
	private double lastUpdate;
	private long updateCount;
	private double updateDeltaSum;

	public TimerImpl(Callable<Double> timeSource) {
		this.timeSource = timeSource;
		reset();
	}

	/**
	 * Called to update the timer with a new elapsed time calculated from {@link #now()}
	 * Returns the time since the last update (regardless of if paused)
	 */
	public double update() {
		double now = now();
		double delta = now - lastUpdate;
		lastUpdate = now;
		updateCount++;
		updateDeltaSum += delta;
		if (paused.get() == null) {
			elapsed += delta;
		}
		return delta;
	}

	public StaticTimer snapshot() {
		return new StaticTimer(elapsed(), now());
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
	 * Used to manually step forward by an amount (also in seconds)
	 * @param stepSize
	 */
	public void step(double stepSize) {
		elapsed += stepSize;
	}

	@Override
	public double elapsed() {
		return elapsed;
	}

	/**
		 * Get the latest timestamp from the source.
		 *
		 * @return the timestamp
		 * @throws RuntimeException if cannot get data from source
		 */
	public double now() {
		try {
			return timeSource.call();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public double lastUpdated() {
		return lastUpdate;
	}

	public double averageUpdatePeriod() {
		return updateDeltaSum / updateCount;
	}

	public long updateCount() {
		return updateCount;
	}

	/**
		 * Is the time paused?
		 */
	public boolean isPaused() {
		return paused.get() != null;
	}

	/**
		 * Pause or unpause the timer
		 */
	public void setPaused(boolean paused) {
		Double value = paused ? now() : null;
		this.paused.set(value);
	}

	public void togglePaused() {
		boolean newState = !isPaused();
		setPaused(newState);
	}

	@Override
	public String toString() {
		double averageDelta = updateDeltaSum / updateCount;
		return elapsed() + " ms ("+averageDelta+"s)";
	}

}
