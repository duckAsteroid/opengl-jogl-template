package com.asteroid.duck.opengl.util.audio.simulated;

import com.asteroid.duck.opengl.util.stats.Stats;
import com.asteroid.duck.opengl.util.stats.StatsFactory;
import com.asteroid.duck.opengl.util.audio.AudioDataSource;
import com.asteroid.duck.opengl.util.audio.LineAcquirer;
import com.asteroid.duck.opengl.util.timer.TimeSource;
import com.asteroid.duck.opengl.util.timer.Timer;
import com.asteroid.duck.opengl.util.timer.TimerImpl;

import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import static com.asteroid.duck.opengl.util.audio.LineAcquirer.IDEAL;

public class SimulatedDataSource implements AudioDataSource {

	private final Timer timer;
	private final SampledWaveformData source;
	private AudioFormat format;
	private int limit;
	private boolean running;
	private double lastRead;

	public SimulatedDataSource(Timer timer, SampledWaveformData source) {
		this.timer = timer;
		this.source = source;
	}

	@Override
	public int available() {
		return samples(timer.elapsed()) * (bytesPerSample());
	}

	int bytesPerSample() {
		return (format.getSampleSizeInBits() / 8) * format.getChannels();
	}

	int samples(double now) {
		if (!running) return 0;
		int max = limit / bytesPerSample();
		double elapsed = now - lastRead;
		return Math.min((int) (elapsed * format.getSampleRate()), max);
	}

	@Override
	public void open(AudioFormat format, int limit) throws LineUnavailableException {
		if (!IDEAL.equals(format)) {
			throw new LineUnavailableException("Not recognised format");
		}
		this.format = IDEAL;
		this.limit = limit;
		this.lastRead = timer.elapsed();
	}

	@Override
	public int read(byte[] array, int start, int limit) {
		double now = timer.elapsed();
		double samplePeriod = 1.0 / format.getSampleRate();
		// this is the maximum number of samples we can read (respecting our buffer limit)
		int samples = samples(now);

		// this buffer is a short view onto the array calibrated to the start and limit given
		ShortBuffer buffer = ByteBuffer.wrap(array, start, limit).asShortBuffer();

		// Can the buffer take more than we can theoretically read
		if (buffer.remaining() > samples) {
			// limit it to what we can theoretically read
			buffer.limit(samples * floatsPerSample());
		}
		// read the samples into the buffer
		int samplesRead = 0;
		while (buffer.remaining() >= 2) {
			double time = now + (samplesRead * samplePeriod);
			double[] sample = source.sample(time);
			buffer.put((short)Math.round(sample[0]));
			buffer.put((short)Math.round(sample[1]));
			samplesRead++;
		}
		lastRead = now + (samplesRead * samplePeriod);
		return samplesRead * bytesPerSample();
	}

	private int floatsPerSample() {
		return format.getChannels();
	}

	@Override
	public void start() {
		this.running = true;
	}

	@Override
	public void stop() {
		this.running = false;
	}

	@Override
	public void close() {
		this.format = null;
	}

	public static void main(String[] args) throws LineUnavailableException {
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, IDEAL);
		// TODO Lets try to play our simulated audio via output to hear it...
		TimerImpl t = new TimerImpl(TimeSource.systemNanoTimeInstance());
		t.reset();
		t.setPaused(false);
		LineAcquirer laq = new LineAcquirer();
		LineAcquirer.MixerLine mixerLine = laq.allLinesMatching(info).get(0);
		SimulatedDataSource simulated = new SimulatedDataSource(t, LineAcquirer.getSampledWaveformData());
		try(Mixer mixer = mixerLine.mixer()) {
			byte[] audioBuffer = new byte[44100];
			SourceDataLine output = (SourceDataLine) mixer.getLine(info);
			System.out.println("Running on "+mixerLine);
			output.open(IDEAL);
			simulated.open(IDEAL, audioBuffer.length);
			output.start();
			simulated.start();
			Stats readStats = StatsFactory.stats("read.depth");
			while(!exitKeyPressed()) {
				t.update();

				int read = simulated.read(audioBuffer, 0, audioBuffer.length);
				readStats.add(read);
				output.write(audioBuffer, 0, read);
				System.out.print('.');
			}
			simulated.close();
			output.close();

			System.out.println(readStats);
		}
	}

	private static boolean exitKeyPressed() {
		try {
			while (System.in.available() > 0) {
				// Read the input character
				int key = System.in.read();

				// Check if the input is 'q' (or your desired exit key)
				if (key == 'q' || key == 'Q') {
					System.out.println("Exiting...");
					return true; // Exit the loop
				}
			}
			return false;
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
			return true;
		}
	}
}
