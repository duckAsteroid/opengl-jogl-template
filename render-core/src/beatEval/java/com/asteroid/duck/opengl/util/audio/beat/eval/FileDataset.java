package com.asteroid.duck.opengl.util.audio.beat.eval;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Finds evaluation tracks on disk: audio files under one directory, paired by file stem with
 * annotation files under another (often the same) directory. See
 * {@code docs/beat-tracking-plan.md} for the conventions.
 */
public final class FileDataset {

    static final Set<String> AUDIO_EXTENSIONS = Set.of("wav", "au", "aif", "aiff", "mp3", "flac", "ogg");
    static final Set<String> ANNOTATION_EXTENSIONS = Set.of("beats", "beat", "kicks", "bpm", "jams");

    /** Result of a scan: the paired tracks, and how many audio files had no annotations. */
    public record Scan(List<Track> tracks, int unannotated) {}

    private FileDataset() {}

    /**
     * Pair audio files with their annotations. Audio files without any annotation are skipped
     * (and counted). Each track's group is its audio file's parent directory, relative to
     * {@code audioRoot} (or {@code audioRoot}'s own name for files directly in it).
     * @param audioRoot      directory walked for audio files
     * @param annotationRoot directory walked for annotation files
     * @return the tracks, ordered by path
     * @throws IOException if a directory can't be walked
     */
    public static Scan scan(Path audioRoot, Path annotationRoot) throws IOException {
        Map<String, List<Path>> annotations = new HashMap<>();
        for (Path file : files(annotationRoot, ANNOTATION_EXTENSIONS)) {
            annotations.computeIfAbsent(key(file), k -> new ArrayList<>()).add(file);
        }
        Map<String, String> bySuffix = suffixIndex(annotations.keySet());
        List<Track> tracks = new ArrayList<>();
        int unannotated = 0;
        for (Path audio : files(audioRoot, AUDIO_EXTENSIONS)) {
            String annotationKey = annotations.containsKey(key(audio)) ? key(audio) : bySuffix.get(key(audio));
            List<Path> files = annotationKey == null ? null : annotations.get(annotationKey);
            if (files == null) {
                unannotated++;
                continue;
            }
            Path parent = audioRoot.relativize(audio.getParent());
            String group = parent.toString().isEmpty() ? String.valueOf(audioRoot.toAbsolutePath().getFileName()) : parent.toString();
            tracks.add(new Track(group, key(audio),
                    () -> new Track.Loaded(AudioLoader.load(audio), Annotations.load(files))));
        }
        return new Scan(tracks, unannotated);
    }

    private static List<Path> files(Path root, Set<String> extensions) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> extensions.contains(extension(p)))
                    .sorted()
                    .toList();
        }
    }

    /**
     * The stem audio and annotation files are paired on: the file name without its annotation
     * extension, then without an audio extension, lower-cased, with {@code _}, {@code -} and
     * spaces normalised to {@code .}. So {@code blues.00000.wav}, {@code blues.00000.beats},
     * {@code blues.00000.wav.jams} and {@code Blues_00000.bpm} all key to {@code blues.00000}.
     * @param file an audio or annotation file
     * @return its pairing key
     */
    static String key(Path file) {
        String name = file.getFileName().toString();
        if (ANNOTATION_EXTENSIONS.contains(extension(name))) name = stripExtension(name);
        if (AUDIO_EXTENSIONS.contains(extension(name))) name = stripExtension(name);
        return name.toLowerCase(Locale.ROOT).replaceAll("[_\\-\\s]+", ".");
    }

    /**
     * Maps each proper dot-suffix of the annotation keys back to its key, so annotations named
     * with a dataset prefix still pair: {@code gtzan.blues.00000} is found as
     * {@code blues.00000}. A suffix shared by two keys (e.g. {@code 00000}) is ambiguous and
     * left out.
     */
    private static Map<String, String> suffixIndex(Iterable<String> keys) {
        Map<String, String> index = new HashMap<>();
        java.util.Set<String> ambiguous = new java.util.HashSet<>();
        for (String key : keys) {
            for (int dot = key.indexOf('.'); dot >= 0; dot = key.indexOf('.', dot + 1)) {
                String suffix = key.substring(dot + 1);
                if (ambiguous.contains(suffix)) continue;
                if (index.putIfAbsent(suffix, key) != null) {
                    index.remove(suffix);
                    ambiguous.add(suffix);
                }
            }
        }
        return index;
    }

    /**
     * @param file a file
     * @return its lower-case extension without the dot, or {@code ""}
     */
    static String extension(Path file) {
        return extension(file.getFileName().toString());
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }
}
