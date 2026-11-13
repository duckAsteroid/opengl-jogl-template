package com.asteroid.duck.opengl.util.audio.beat.eval;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Command-line entry point for the offline beat-tracking evaluation
 * ({@code ./gradlew :render-core:beatEval --args="..."}). See {@code docs/beat-tracking-plan.md}.
 */
public final class BeatEval {

    private static final String USAGE = """
            Usage: beatEval [--data DIR] [--annotations DIR] [--synthetic]
                            [--analyser NAME|all] [--filter REGEX] [--out DIR]
              --data DIR         walk DIR for audio files (wav, au, aif(f); mp3/flac/ogg via ffmpeg)
              --annotations DIR  walk DIR for annotations (.beats .kicks .bpm .jams); default: --data
              --synthetic        include the built-in synthetic tracks (the default without --data)
              --analyser NAME    analyser to run, or 'all' (default). Known: %s
              --filter REGEX     only tracks whose group/name contains a match
              --out DIR          where results.csv and summary.md go (default: build/beat-eval)
            """;

    private BeatEval() {}

    /**
     * @param args see {@link #USAGE}
     * @throws IOException if a dataset can't be scanned or the report can't be written
     */
    public static void main(String[] args) throws IOException {
        Path data = null;
        Path annotations = null;
        boolean synthetic = false;
        String analyserName = "all";
        Pattern filter = null;
        Path out = Path.of(System.getProperty("beatEval.out", "build/beat-eval"));
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--data" -> data = Path.of(expand(value(args, ++i)));
                case "--annotations" -> annotations = Path.of(expand(value(args, ++i)));
                case "--synthetic" -> synthetic = true;
                case "--analyser" -> analyserName = value(args, ++i);
                case "--filter" -> filter = Pattern.compile(value(args, ++i));
                case "--out" -> out = Path.of(expand(value(args, ++i)));
                default -> usage("Unknown option: " + args[i]);
            }
        }

        String selected = analyserName;
        List<Analysers.Entry> analysers = selected.equals("all") ? Analysers.ALL
                : Analysers.ALL.stream().filter(a -> a.name().equals(selected)).toList();
        if (analysers.isEmpty()) usage("Unknown analyser: " + analyserName);

        List<Track> tracks = new ArrayList<>();
        if (data != null) {
            FileDataset.Scan scan = FileDataset.scan(data, Objects.requireNonNullElse(annotations, data));
            System.out.printf("%s: %d annotated tracks (%d audio files without annotations skipped)%n",
                    data, scan.tracks().size(), scan.unannotated());
            tracks.addAll(scan.tracks());
        }
        if (synthetic || data == null) tracks.addAll(SyntheticTracks.all());
        if (filter != null) {
            Pattern f = filter;
            tracks.removeIf(t -> !f.matcher(t.id()).find());
        }
        if (tracks.isEmpty()) usage("No tracks to evaluate");

        List<TrackResult> results = run(tracks, analysers);

        Files.createDirectories(out);
        Files.writeString(out.resolve("results.csv"), Report.csv(results));
        String summary = Report.summary(results, analysers);
        Files.writeString(out.resolve("summary.md"), summary);
        System.out.println();
        System.out.println(summary);
        System.out.println("Wrote " + out.toAbsolutePath().resolve("results.csv") + " and summary.md");
    }

    /**
     * Load each track once and run every analyser over it, tracks in parallel. Tracks that fail
     * to load are reported and skipped.
     */
    static List<TrackResult> run(List<Track> tracks, List<Analysers.Entry> analysers) {
        AtomicInteger done = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        List<TrackResult> results = tracks.parallelStream().flatMap(track -> {
            Track.Loaded loaded;
            try {
                loaded = track.loader().load();
            } catch (IOException | RuntimeException e) {
                failed.incrementAndGet();
                System.err.println("Skipping " + track.id() + ": " + e.getMessage());
                return java.util.stream.Stream.empty();
            }
            List<TrackResult> mine = analysers.stream()
                    .map(a -> Evaluator.evaluate(a, track.group(), track.name(), loaded))
                    .toList();
            int n = done.incrementAndGet();
            if (n % 50 == 0) System.out.printf("  %d/%d tracks%n", n, tracks.size());
            return mine.stream();
        }).toList();
        if (failed.get() > 0) System.err.printf("%d tracks failed to load%n", failed.get());
        return results;
    }

    private static String value(String[] args, int i) {
        if (i >= args.length) usage("Missing value for " + args[i - 1]);
        return args[i];
    }

    private static String expand(String path) {
        return path.startsWith("~/") ? System.getProperty("user.home") + path.substring(1) : path;
    }

    private static void usage(String problem) {
        System.err.println(problem);
        System.err.printf(USAGE, Analysers.ALL.stream().map(Analysers.Entry::name).toList());
        System.exit(2);
    }
}
