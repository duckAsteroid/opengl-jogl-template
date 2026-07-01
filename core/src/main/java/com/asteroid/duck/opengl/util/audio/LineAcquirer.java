package com.asteroid.duck.opengl.util.audio;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.audio.simulated.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
/**
 * Discovers and selects Java Sound {@link javax.sound.sampled.TargetDataLine}s for live audio capture.
 *
 * <p>On {@link #init}, all available mixers are scanned for lines that match the desired
 * {@link javax.sound.sampled.AudioFormat}. If the system property {@code simulate.audio=true} is
 * set, a synthetic stereo oscillator is prepended so the rest of the pipeline works without a
 * physical input device. The selected source can be cycled at runtime via {@link #next()} and
 * {@link #previous()}.</p>
 */
public class LineAcquirer {
  private static final Logger LOG = LoggerFactory.getLogger(LineAcquirer.class);

  private List<AudioDataSource> sources = new ArrayList<>();
  private int selectedSource = 0;

  /** Default constructor; sources are populated by {@link #init}. */
  public LineAcquirer() {}

  /**
   * Discover available audio sources and prepare them for use.
   *
   * <p>If {@code simulate.audio=true} a synthetic source is added first. Then all Java Sound
   * mixers supporting {@code ideal} as a capture format are opened and registered.</p>
   *
   * @param ctx   the render context (unused beyond providing access to the timer for simulation)
   * @param ideal the desired capture format; matching lines must support this format
   * @throws RuntimeException if no sources are found after scanning all mixers
   */
  public void init(RenderContext ctx, AudioFormat ideal)  {
    if (System.getProperty("simulate.audio", "false").equalsIgnoreCase("true")) {
      StereoDataSource audio = getSampledWaveformData();
      sources.add(new SimulatedDataSource(ctx.getClock(), audio));
    }
    List<MixerLine> mixerLines = allLinesMatching(ideal).toList();
    mixerLines.stream().map(MixerLine::getTargetDataLine)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(sources::add);
    if (sources.isEmpty()) {
      throw new RuntimeException("No sources found");
    }
  }

  /**
   * Returns the currently selected audio source.
   *
   * @return the active {@link AudioDataSource}
   */
  public AudioDataSource getSelectedSource() {
    return sources.get(selectedSource);
  }

  /**
   * Advance to the next source in the cycle and return the one that was active before the switch.
   *
   * @return the previously selected {@link AudioDataSource}
   */
  public AudioDataSource next() {
    AudioDataSource current = getSelectedSource();
    selectedSource = (selectedSource + 1) % sources.size();
    return current;
  }

  /**
   * Step back to the previous source in the cycle and return the one that was active before the switch.
   *
   * @return the previously selected {@link AudioDataSource}
   */
  public AudioDataSource previous() {
    AudioDataSource current = getSelectedSource();
    selectedSource = (selectedSource - 1 + sources.size()) % sources.size();
    return current;
  }

  /**
   * Create a synthetic stereo waveform source for use when no physical audio input is available.
   * Generates a middle-C tone panned slowly left and right at 1 Hz.
   *
   * @return a stereo data source suitable for passing to {@link com.asteroid.duck.opengl.util.audio.AudioReader}
   */
  public static StereoDataSource getSampledWaveformData() {
    Waveform midC = Waveform.MIDDLE_C.amplify(100);

    return OscillatingStereoPositioner.fullScale(1.0).wrap(midC);
  }

  /**
   * A pairing of a Java Sound {@link Mixer} with one of its available {@link Line.Info} descriptors.
   * Used to represent a candidate audio capture line before it has been opened.
   *
   * @param mixer the mixer that owns this line
   * @param line  descriptor for the specific line on the mixer
   */
  public record MixerLine(Mixer mixer, Line.Info line) {
    /**
     * Attempt to open this line as a {@link TargetDataLine} wrapped in a {@link TargetLineSource}.
     *
     * @return the opened source, or empty if the line is unavailable
     */
    public Optional<AudioDataSource> getTargetDataLine() {
      final String lineDesc = toString();
      try {
        return Optional.of(new TargetLineSource(lineDesc, (TargetDataLine) mixer.getLine(line)));
      } catch (LineUnavailableException e) {
          LOG.error("Line unavailable: {}", lineDesc, e);
        return Optional.empty();
      }
    }
    @NotNull
    @Override
    public String toString() {
      return mixer.getMixerInfo().getName() + ":"+ line.toString();
    }
  }

  /**
   * Enumerate all data lines across all installed Java Sound mixers.
   *
   * @return a list of every available {@link MixerLine} regardless of format support
   */
  public static List<MixerLine> allLines() {
    ArrayList<MixerLine> result = new ArrayList<>();
    Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
    for (Mixer.Info info: mixerInfos){
      Mixer m = AudioSystem.getMixer(info);
      List<Line.Info> lineInfos =
        Stream.concat(Arrays.stream(m.getTargetLineInfo()), Arrays.stream(m.getSourceLineInfo())).toList();
      for (Line.Info lineInfo: lineInfos){
        if (lineInfo instanceof DataLine.Info dli) {
          result.add(new MixerLine(m, dli));
        }
      }
    }
    return result;
  }

  /**
   * Filter all available lines to those that support the given audio format as a capture target.
   *
   * @param format the required audio format (e.g. {@link #IDEAL})
   * @return a stream of matching {@link MixerLine}s
   */
  public static Stream<MixerLine> allLinesMatching(AudioFormat format) {
    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
    return allLinesMatching(info);
  }
  /**
   * Filter all available lines to those whose class and mixer support the given {@link DataLine.Info}.
   *
   * @param info the line-info descriptor specifying class and format requirements
   * @return a stream of {@link MixerLine}s that can satisfy {@code info}
   */
  public static Stream<MixerLine> allLinesMatching(DataLine.Info info) {
    return allLines().stream()
            .filter(line -> info.getLineClass().isAssignableFrom(line.line.getLineClass()))
      .filter(line -> line.mixer.isLineSupported(info));
  }

  /** The preferred capture format: 48 kHz, 16-bit, stereo, signed, little-endian. */
  public final static AudioFormat IDEAL = new AudioFormat( 48000f, 16, 2, true, false);

  /**
   * Diagnostic entry point: prints all {@link #IDEAL}-format mixer lines to standard output.
   *
   * @param args command-line arguments (ignored)
   */
  public static void main(String[] args) {
    LineAcquirer laq = new LineAcquirer();
    laq.dump();
  }

  /**
   * Print all available {@link #IDEAL}-format mixer lines to standard output.
   * Useful for diagnosing which physical inputs the Java Sound API can see.
   */
  public void dump() {
    List<MixerLine> mixerLines = allLinesMatching(IDEAL).toList();
    mixerLines.forEach(ml -> LOG.info("{}", ml));
  }
}
