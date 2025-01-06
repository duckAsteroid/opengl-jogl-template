package com.asteroid.duck.opengl.util.timer;

public interface Timer {

	/**
	 * The time elapsed when updated (in seconds) since the timer started.
	 *
	 * @return elapsed time in seconds
	 */
	double elapsed();

	/**
	 * Calculates the value of a wave function between -1 and 1
	 *
	 * @param frequency the frequency in hertz
	 * @param phase     the relative phase in seconds
	 * @return the wave function value at the current elapsed time
	 */
	default double waveFunction(double frequency, double phase) {
		return Math.sin(2 * Math.PI * frequency * (elapsed() + phase));
	}


	default double linearFunction(double maxY, double frequency) {
		double period = 1.0 / frequency;
		double progress = (elapsed() % period) / period;
		return progress * maxY;
	}
}
