package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.audio.AudioReader;
import com.asteroid.duck.opengl.util.audio.LineAcquirer;
import com.asteroid.duck.opengl.util.audio.PboAudioSink;
import com.asteroid.duck.opengl.util.color.StandardColors;
import com.asteroid.duck.opengl.util.keys.KeyCombination;
import com.asteroid.duck.opengl.util.wave.AmplitudeFunction;
import com.asteroid.duck.opengl.util.wave.AudioWave;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class SoundWave implements Experiment {
	private static final float LINE_WIDTH_MIN  = 1.0f;
	private static final float LINE_WIDTH_MAX  = 20.0f;
	private static final float LINE_WIDTH_STEP = 1.0f;

	private static final float AMPLITUDE_MIN  = 0.1f;
	private static final float AMPLITUDE_MAX  = 100.0f;
	private static final float AMPLITUDE_STEP = 2.0f;

	private PboAudioSink audioSink;
	private AudioReader audioReader;
	private Thread audioReaderThread;
	private AudioWave audioWave;

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
		audioSink = PboAudioSink.create(AudioWave.AUDIO_BUFFER_SIZE, ctx);
		audioWave = new AudioWave(audioSink);
		audioWave.init(ctx);

		lineAcquirer.init(ctx, LineAcquirer.IDEAL);
		audioReader = new AudioReader(List.of(audioSink));
		audioReaderThread = new Thread(audioReader, "audio-reader");
		audioReaderThread.setDaemon(true);
		audioReaderThread.start();
		audioReader.setLine(lineAcquirer.getSelectedSource());

		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('J'), () -> {
			lineAcquirer.next();
			audioReader.setLine(lineAcquirer.getSelectedSource());
		}, "Switch to the next audio input line");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('H'), () -> {
			lineAcquirer.previous();
			audioReader.setLine(lineAcquirer.getSelectedSource());
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
		audioSink.upload();
		audioWave.doRender(ctx);
	}

	@Override
	public void dispose() {
		audioWave.dispose();
		audioReader.setRunning(false);
		audioReader.setLine(null);
		try {
			audioReaderThread.join(2000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
