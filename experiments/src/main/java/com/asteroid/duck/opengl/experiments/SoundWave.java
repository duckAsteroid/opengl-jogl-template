package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.audio.AudioDataSource;
import com.asteroid.duck.opengl.util.audio.LineAcquirer;
import com.asteroid.duck.opengl.util.keys.KeyCombination;
import com.asteroid.duck.opengl.util.wave.AudioWave;

import java.io.IOException;

public class SoundWave implements Experiment {
	private final AudioWave audioWave = new  AudioWave();
	private final LineAcquirer lineAcquirer = new LineAcquirer();
	@Override
	public String getDescription() {
		return "Renders an audio wave on screen";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		audioWave.init(ctx);
		lineAcquirer.init(ctx, LineAcquirer.IDEAL);
		audioWave.setLine(lineAcquirer.getSelectedSource());
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('J'), () -> {
			AudioDataSource source = lineAcquirer.next();
			audioWave.setLine(lineAcquirer.getSelectedSource());
		}, "Start capturing audio from the next available line");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('H'), () -> {
			AudioDataSource source = lineAcquirer.next();
			audioWave.setLine(lineAcquirer.getSelectedSource());
		}, "Start capturing audio from the previous available line");
	}

	@Override
	public void doRender(RenderContext ctx) {
		audioWave.doRender(ctx);
	}

	@Override
	public void dispose() {
		audioWave.dispose();
	}
}
