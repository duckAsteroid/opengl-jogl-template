package com.asteroid.duck.opengl.util.timer;

public class StaticTimer implements Timer {
	private final double elapsed;
	private final double now;

	public StaticTimer(double elapsed, double now) {
		this.elapsed = elapsed;
		this.now = now;
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
		return now;
	}
}
