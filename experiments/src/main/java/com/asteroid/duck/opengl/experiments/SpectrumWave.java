package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.audio.LineAcquirer;
import com.asteroid.duck.opengl.util.keys.KeyCombination;
import com.asteroid.duck.opengl.util.wave.SpectrumAnalyser;

import java.io.IOException;

public class SpectrumWave implements Experiment {

	private final SpectrumAnalyser analyser = new SpectrumAnalyser();
	private final LineAcquirer lineAcquirer = new LineAcquirer();

	@Override
	public String getDescription() {
		return "Traditional bar-chart spectrum analyser — log frequency (20 Hz–20 kHz), dB amplitude, peak hold";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		analyser.init(ctx);
		lineAcquirer.init(ctx, LineAcquirer.IDEAL);
		analyser.setLine(lineAcquirer.getSelectedSource());

		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('J'), () -> {
			lineAcquirer.next();
			analyser.setLine(lineAcquirer.getSelectedSource());
		}, "Switch to the next audio input");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('H'), () -> {
			lineAcquirer.previous();
			analyser.setLine(lineAcquirer.getSelectedSource());
		}, "Switch to the previous audio input");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('P'),
				ctx::captureNextFrame,
				"Save screenshot");
	}

	@Override
	public void doRender(RenderContext ctx) {
		analyser.doRender(ctx);
	}

	@Override
	public void dispose() {
		analyser.dispose();
	}
}
