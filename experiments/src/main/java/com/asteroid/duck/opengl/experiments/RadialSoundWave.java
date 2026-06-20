package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.audio.LineAcquirer;
import com.asteroid.duck.opengl.util.color.StandardColors;
import com.asteroid.duck.opengl.util.keys.KeyCombination;
import com.asteroid.duck.opengl.util.wave.RadialWave;

import java.io.IOException;
import java.util.Random;

public class RadialSoundWave implements Experiment {
	private static final float LINE_WIDTH_MIN  = 1.0f;
	private static final float LINE_WIDTH_MAX  = 20.0f;
	private static final float LINE_WIDTH_STEP = 1.0f;

	private static final float RADIUS_MIN  = 0.05f;
	private static final float RADIUS_MAX  = 0.95f;
	private static final float RADIUS_STEP = 0.05f;

	private static final float AMPLITUDE_MIN  = 0.01f;
	private static final float AMPLITUDE_MAX  = 1.0f;
	private static final float AMPLITUDE_STEP = 1.5f; // multiplicative per keypress

	private final RadialWave radialWave = new RadialWave();
	private final LineAcquirer lineAcquirer = new LineAcquirer();
	private final Random random = new Random();
	private float lineWidth = 3.0f;
	private int channelMode = RadialWave.CHANNEL_BLEND;
	private float radius = 0.5f;
	private float amplitude = 0.3f;

	@Override
	public String getDescription() {
		return "Renders audio as a radial waveform — the waveform is displaced outward/inward around a circle";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		radialWave.init(ctx);
		lineAcquirer.init(ctx, LineAcquirer.IDEAL);
		radialWave.setLine(lineAcquirer.getSelectedSource());

		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('J'), () -> {
			lineAcquirer.next();
			radialWave.setLine(lineAcquirer.getSelectedSource());
		}, "Switch to the next audio input line");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('H'), () -> {
			lineAcquirer.previous();
			radialWave.setLine(lineAcquirer.getSelectedSource());
		}, "Switch to the previous audio input line");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('C'), () -> {
			StandardColors[] colours = StandardColors.values();
			radialWave.setLineColour(colours[random.nextInt(colours.length)].color);
		}, "Cycle to a random waveform colour");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('T'), () -> {
			lineWidth = Math.min(lineWidth + LINE_WIDTH_STEP, LINE_WIDTH_MAX);
			radialWave.setLineWidth(lineWidth);
		}, "Increase waveform line width");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('G'), () -> {
			lineWidth = Math.max(lineWidth - LINE_WIDTH_STEP, LINE_WIDTH_MIN);
			radialWave.setLineWidth(lineWidth);
		}, "Decrease waveform line width");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('M'), () -> {
			channelMode = (channelMode + 1) % 3;
			radialWave.setChannelMode(channelMode);
		}, "Cycle channel mode: blend → left → right");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('E'), () -> {
			radius = Math.min(radius + RADIUS_STEP, RADIUS_MAX);
			radialWave.setRadius(radius);
		}, "Increase circle radius");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('Q'), () -> {
			radius = Math.max(radius - RADIUS_STEP, RADIUS_MIN);
			radialWave.setRadius(radius);
		}, "Decrease circle radius");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('X'), () -> {
			amplitude = Math.min(amplitude * AMPLITUDE_STEP, AMPLITUDE_MAX);
			radialWave.setAmplitude(amplitude);
		}, "Increase amplitude multiplier");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('Z'), () -> {
			amplitude = Math.max(amplitude / AMPLITUDE_STEP, AMPLITUDE_MIN);
			radialWave.setAmplitude(amplitude);
		}, "Decrease amplitude multiplier");
	}

	@Override
	public void doRender(RenderContext ctx) {
		radialWave.doRender(ctx);
	}

	@Override
	public void dispose() {
		radialWave.dispose();
	}
}
