package com.asteroid.duck.opengl.util.audio.beat.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Reference annotations for one track. An empty array or a {@code NaN} tempo means "not
 * annotated", and the metrics that need it are skipped for that track.
 * @param beats     beat times in seconds, ascending
 * @param kicks     kick onset times in seconds, ascending
 * @param bpm       global tempo in BPM, or {@code NaN} if unknown (or not constant)
 * @param downbeats downbeat (first beat of the bar) times in seconds, ascending
 */
public record Annotations(double[] beats, double[] kicks, double bpm, double[] downbeats) {

    /** Annotations without downbeats. */
    public Annotations(double[] beats, double[] kicks, double bpm) {
        this(beats, kicks, bpm, new double[0]);
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * Read and merge the annotation files for one track (see {@code docs/beat-tracking-plan.md}
     * for the formats). Where two files supply the same thing, the first wins. Without a tempo
     * annotation, the tempo is derived from the median beat interval.
     * @param files annotation files: {@code .beats}, {@code .beat}, {@code .kicks}, {@code .bpm},
     *              {@code .jams}
     * @return the merged annotations
     * @throws IOException if a file can't be read
     */
    public static Annotations load(List<Path> files) throws IOException {
        double[] beats = new double[0];
        double[] kicks = new double[0];
        double[] downbeats = new double[0];
        double bpm = Double.NaN;
        for (Path file : files) {
            switch (FileDataset.extension(file)) {
                case "beats", "beat" -> {
                    if (beats.length == 0) {
                        beats = readTimes(file);
                        downbeats = readDownbeats(file);
                    }
                }
                case "kicks" -> { if (kicks.length == 0) kicks = readTimes(file); }
                case "bpm" -> { if (Double.isNaN(bpm)) bpm = readBpm(file); }
                case "jams" -> {
                    Annotations jams = readJams(file);
                    if (beats.length == 0) {
                        beats = jams.beats;
                        downbeats = jams.downbeats;
                    }
                    if (Double.isNaN(bpm)) bpm = jams.bpm;
                }
                default -> { }
            }
        }
        if (Double.isNaN(bpm)) bpm = tempoFromBeats(beats);
        return new Annotations(beats, kicks, bpm, downbeats);
    }

    /**
     * Read downbeats from a beat file whose second column is the position in the bar (as in
     * GTZAN-Rhythm: {@code 1.173 1}): the times at position 1.
     * @param file the text file
     * @return downbeat times, sorted; empty if the file has no position column
     * @throws IOException if the file can't be read
     */
    static double[] readDownbeats(Path file) throws IOException {
        double[] downbeats = Files.readAllLines(file).stream()
                .map(Annotations::numbers)
                .filter(n -> n.length >= 2 && n[1] == 1)
                .mapToDouble(n -> n[0])
                .toArray();
        Arrays.sort(downbeats);
        return downbeats;
    }

    /** The leading numeric fields of a line; stops at the first non-number. */
    private static double[] numbers(String line) {
        String trimmed = line.strip();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return new double[0];
        String[] fields = trimmed.split("[\\s,;]+");
        double[] values = new double[fields.length];
        int n = 0;
        for (String field : fields) {
            try {
                values[n++] = Double.parseDouble(field);
            } catch (NumberFormatException e) {
                break;
            }
        }
        return Arrays.copyOf(values, n);
    }

    /**
     * Read event times: the first number on each line, in seconds. Lines whose first field isn't
     * a number (headers, {@code #} comments, blanks) are skipped; fields may be separated by
     * whitespace or commas.
     * @param file the text file
     * @return the times, sorted ascending
     * @throws IOException if the file can't be read
     */
    static double[] readTimes(Path file) throws IOException {
        double[] times = Files.readAllLines(file).stream()
                .map(Annotations::firstNumber)
                .filter(v -> !Double.isNaN(v))
                .mapToDouble(Double::doubleValue)
                .toArray();
        Arrays.sort(times);
        return times;
    }

    /**
     * Read a tempo: the first number in the file.
     * @param file the text file
     * @return tempo in BPM, or {@code NaN} if the file holds no number
     * @throws IOException if the file can't be read
     */
    static double readBpm(Path file) throws IOException {
        return Files.readAllLines(file).stream()
                .map(Annotations::firstNumber)
                .filter(v -> !Double.isNaN(v))
                .findFirst()
                .orElse(Double.NaN);
    }

    /**
     * Read beats ({@code beat} or {@code beat_position} namespace) and tempo ({@code tempo}
     * namespace, highest-confidence observation) from a JAMS file. Handles both the current
     * list-of-observations {@code data} layout and the older column layout.
     * @param file the JAMS file
     * @return its beats, downbeats (observations whose value is 1) and tempo; kicks are always empty
     * @throws IOException if the file can't be read or parsed
     */
    static Annotations readJams(Path file) throws IOException {
        JsonNode root = JSON.readTree(file.toFile());
        double[] beats = new double[0];
        double[] downbeats = new double[0];
        double bpm = Double.NaN;
        for (JsonNode annotation : root.path("annotations")) {
            String namespace = annotation.path("namespace").asText();
            List<double[]> observations = observations(annotation.path("data"));
            if ((namespace.equals("beat") || namespace.equals("beat_position")) && beats.length == 0) {
                beats = observations.stream().mapToDouble(o -> o[0]).filter(t -> !Double.isNaN(t)).sorted().toArray();
                downbeats = observations.stream().filter(o -> o[1] == 1).mapToDouble(o -> o[0]).sorted().toArray();
            } else if (namespace.equals("tempo") && Double.isNaN(bpm)) {
                double bestConfidence = Double.NEGATIVE_INFINITY;
                for (double[] o : observations) {
                    double confidence = Double.isNaN(o[2]) ? 1.0 : o[2];
                    if (!Double.isNaN(o[1]) && confidence > bestConfidence) {
                        bestConfidence = confidence;
                        bpm = o[1];
                    }
                }
            }
        }
        return new Annotations(beats, new double[0], bpm, downbeats);
    }

    /** JAMS observations as {@code [time, value, confidence]}; non-numeric fields become NaN. */
    private static List<double[]> observations(JsonNode data) {
        List<double[]> result = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode o : data) {
                result.add(new double[] {number(o.path("time")), number(o.path("value")), number(o.path("confidence"))});
            }
        } else if (data.isObject()) {
            JsonNode times = data.path("time");
            for (int i = 0; i < times.size(); i++) {
                result.add(new double[] {number(times.get(i)), number(data.path("value").path(i)),
                        number(data.path("confidence").path(i))});
            }
        }
        return result;
    }

    /** A JSON number, or an old-style {@code {"value": n}} wrapper; anything else is NaN. */
    private static double number(JsonNode node) {
        if (node.isNumber()) return node.asDouble();
        JsonNode value = node.path("value");
        return value.isNumber() ? value.asDouble() : Double.NaN;
    }

    private static double firstNumber(String line) {
        String trimmed = line.strip();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return Double.NaN;
        String field = trimmed.split("[\\s,;]+", 2)[0];
        try {
            return Double.parseDouble(field.toLowerCase(Locale.ROOT));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /**
     * Tempo implied by a beat sequence: 60 / median inter-beat interval.
     * @param beats beat times in seconds, ascending
     * @return tempo in BPM, or {@code NaN} with fewer than two beats
     */
    static double tempoFromBeats(double[] beats) {
        if (beats.length < 2) return Double.NaN;
        double[] intervals = new double[beats.length - 1];
        for (int i = 1; i < beats.length; i++) intervals[i - 1] = beats[i] - beats[i - 1];
        return 60.0 / Stats.median(intervals);
    }
}
