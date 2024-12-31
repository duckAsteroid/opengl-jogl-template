package com.asteroid.duck.opengl.util.audio;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.audio.simulated.CompositeWaveform;
import com.asteroid.duck.opengl.util.audio.simulated.Note;
import com.asteroid.duck.opengl.util.audio.simulated.SimulatedDataSource;
import com.asteroid.duck.opengl.util.audio.simulated.Waveform;
import com.asteroid.duck.opengl.util.timer.Timer;
import com.asteroid.duck.opengl.util.timer.TimerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

public class LineAcquirer {
  private static final Logger LOG = LoggerFactory.getLogger(LineAcquirer.class);

  public AudioDataSource acquire(RenderContext ctx, AudioFormat ideal) throws LineUnavailableException {
    if (System.getProperty("simulate.audio", "false").equalsIgnoreCase("true")) {
      CompositeWaveform audio = getSampledWaveformData();
      return new SimulatedDataSource(ctx.getTimer(), audio);
    }
    List<MixerLine> mixerLines = allLinesMatching(ideal);
    for (MixerLine mixerLine : mixerLines) {
      System.out.println(mixerLine.toString());
    }
    MixerLine mixerLine = mixerLines.get(0);
    System.out.println(mixerLine.toString());
    return mixerLine.getTargetDataLine();
  }

  public static CompositeWaveform getSampledWaveformData() {
    Waveform midC = Waveform.MIDDLE_C.amplify(100);
    CompositeWaveform audio = new CompositeWaveform(5);
    final double LENGTH = 1;
    audio.add(midC);
    // E
    //audio.add(midC.transpose(4).atStereo(-1));
    audio.add(new Note(midC.transpose(3).atStereo(-1), LENGTH, "1010"));
    // G
    //audio.add(midC.transpose(7).atStereo(+1));
    audio.add(new Note(midC.transpose(7).atStereo(+1), LENGTH, "1100"));
    // -2 octaves
    //audio.add(new Note(midC.transpose(-24).atStereo(-1.0), LENGTH, "01"));
    // +2 octaves
    //audio.add(new Note(midC.transpose(+24).atStereo(+1.0), LENGTH, "10"));
    return audio;
  }

  public record MixerLine(Mixer mixer, Line.Info line) {
    public AudioDataSource getTargetDataLine() throws LineUnavailableException {
      return new TargetLineSource((TargetDataLine) mixer.getLine(line));
    }
    @Override
    public String toString() {
      return mixer.getMixerInfo().getName() + ":"+ line.toString();
    }
  }

  public List<MixerLine> allLines() {
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

  public List<MixerLine> allLinesMatching(AudioFormat format) {
    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
    return allLinesMatching(info);
  }
  public List<MixerLine> allLinesMatching(DataLine.Info info) {
    return allLines().stream()
      .filter(line -> line.mixer.isLineSupported(info))
      .toList();
  }

  public final static AudioFormat IDEAL = new AudioFormat( 44100f, 16, 2, true, false);

  public static void main(String[] args) {
    LineAcquirer laq = new LineAcquirer();
    List<MixerLine> mixerLines = laq.allLinesMatching(IDEAL);
    mixerLines.forEach(System.out::println);
  }
}
