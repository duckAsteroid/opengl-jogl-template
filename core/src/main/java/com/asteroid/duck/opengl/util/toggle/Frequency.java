package com.asteroid.duck.opengl.util.toggle;

import com.asteroid.duck.opengl.util.RenderContext;

/**
 * A toggle that goes off every N seconds
 */
public class Frequency implements Toggle {
	// time between enabled periods (seconds)
	private final double period;
	// how long enabled period lasts
	private final double dwell;

	public Frequency(double period, double dwell) {
		this.period = period;
		this.dwell = dwell;
	}

	@Override
	public boolean isRenderEnabled(RenderContext ctx) {
		double elapsedTime = ctx.getTimer().elapsed();
		double cycleTime = elapsedTime % period;
		return cycleTime <= dwell;
	}
}
