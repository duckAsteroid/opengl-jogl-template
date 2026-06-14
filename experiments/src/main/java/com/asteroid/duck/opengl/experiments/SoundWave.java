package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.audio.AudioDataSource;
import com.asteroid.duck.opengl.util.audio.LineAcquirer;
import com.asteroid.duck.opengl.util.color.StandardColors;
import com.asteroid.duck.opengl.util.keys.KeyCombination;
import com.asteroid.duck.opengl.util.wave.AmplitudeFunction;
import com.asteroid.duck.opengl.util.wave.AudioWave;

import java.io.IOException;
import java.util.Random;

public class SoundWave implements Experiment {
	private static final float LINE_WIDTH_MIN  = 1.0f;
	private static final float LINE_WIDTH_MAX  = 20.0f;
	private static final float LINE_WIDTH_STEP = 1.0f;

	private static final float AMPLITUDE_MIN  = 0.1f;
	private static final float AMPLITUDE_MAX  = 100.0f;
	private static final float AMPLITUDE_STEP = 2.0f; // multiplicative: each keypress doubles or halves

	private final AudioWave audioWave = new AudioWave();
	private final LineAcquirer lineAcquirer = new LineAcquirer();
	private final Random random = new Random();
	private float lineWidth = 6.0f;
	private int channelMode = AudioWave.CHANNEL_BLEND;
	private float amplitude = 10.0f;
	private boolean useEllipse = false;

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
			lineAcquirer.next();
			audioWave.setLine(lineAcquirer.getSelectedSource());
		}, "Switch to the next audio input line");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('H'), () -> {
			lineAcquirer.previous();
			audioWave.setLine(lineAcquirer.getSelectedSource());
		}, "Switch to the previous audio input line");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('C'), () -> {
			StandardColors[] colours = StandardColors.values();
			audioWave.setLineColour(colours[random.nextInt(colours.length)].color);
		}, "Cycle to a random waveform colour");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('T'), () -> {
			lineWidth = Math.min(lineWidth + LINE_WIDTH_STEP, LINE_WIDTH_MAX);
			audioWave.setLineWidth(lineWidth);
		}, "Increase waveform line width");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('G'), () -> {
			lineWidth = Math.max(lineWidth - LINE_WIDTH_STEP, LINE_WIDTH_MIN);
			audioWave.setLineWidth(lineWidth);
		}, "Decrease waveform line width");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('M'), () -> {
			channelMode = (channelMode + 1) % 4;
			audioWave.setChannelMode(channelMode);
		}, "Cycle channel mode: blend → left → right → stereo");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('A'), () -> {
			useEllipse = !useEllipse;
			applyAmplitudeFunction();
		}, "Toggle amplitude envelope: constant ↔ ellipse");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('X'), () -> {
			amplitude = Math.min(amplitude * AMPLITUDE_STEP, AMPLITUDE_MAX);
			applyAmplitudeFunction();
		}, "Increase amplitude multiplier (×2)");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('Z'), () -> {
			amplitude = Math.max(amplitude / AMPLITUDE_STEP, AMPLITUDE_MIN);
			applyAmplitudeFunction();
		}, "Decrease amplitude multiplier (÷2)");
	}

	private void applyAmplitudeFunction() {
		audioWave.setAmplitudeFunction(useEllipse
				? AmplitudeFunction.ellipse(amplitude)
				: AmplitudeFunction.constant(amplitude));
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
