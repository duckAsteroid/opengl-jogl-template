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
//https://github.com/jackaudio/jackaudio.github.com/wiki
//https://www.portaudio.com/
public class LineAcquirer {
  private static final Logger LOG = LoggerFactory.getLogger(LineAcquirer.class);

  private List<AudioDataSource> sources = new ArrayList<>();
  private int selectedSource = 0;

  public void init(RenderContext ctx, AudioFormat ideal)  {
    if (System.getProperty("simulate.audio", "true").equalsIgnoreCase("true")) {
      StereoDataSource audio = getSampledWaveformData();
      sources.add(new SimulatedDataSource(ctx.getTimer(), audio));
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

  public AudioDataSource getSelectedSource() {
    return sources.get(selectedSource);
  }

  public AudioDataSource next() {
    AudioDataSource current = getSelectedSource();
    selectedSource = (selectedSource + 1) % sources.size();
    return current;
  }

  public AudioDataSource previous() {
    AudioDataSource current = getSelectedSource();
    selectedSource = (selectedSource - 1 + sources.size()) % sources.size();
    return current;
  }

  public static StereoDataSource getSampledWaveformData() {
    Waveform midC = Waveform.MIDDLE_C.amplify(100);

    return OscillatingStereoPositioner.fullScale(1.0).wrap(midC);
  }

  public record MixerLine(Mixer mixer, Line.Info line) {
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

  public static Stream<MixerLine> allLinesMatching(AudioFormat format) {
    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
    return allLinesMatching(info);
  }
  public static Stream<MixerLine> allLinesMatching(DataLine.Info info) {
    return allLines().stream()
            .filter(line -> info.getLineClass().isAssignableFrom(line.line.getLineClass()))
      .filter(line -> line.mixer.isLineSupported(info));
  }

  public final static AudioFormat IDEAL = new AudioFormat( 48000f, 16, 2, true, false);

  public static void main(String[] args) {
    LineAcquirer laq = new LineAcquirer();
    laq.dump();
  }

  public void dump() {
    List<MixerLine> mixerLines = allLinesMatching(IDEAL).toList();
    mixerLines.forEach(System.out::println);
  }
}
