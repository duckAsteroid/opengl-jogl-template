package com.asteroid.duck.opengl.util.audio.beat.eval;

import java.io.IOException;

/**
 * One evaluation track: where it is reported, and how to load its audio and annotations. Loading
 * is deferred so a dataset of hundreds of tracks is never held in memory at once.
 * @param group  report group (e.g. the GTZAN genre, or {@code synthetic})
 * @param name   track name, unique within its group
 * @param loader loads the audio and annotations on demand
 */
public record Track(String group, String name, Loader loader) {

    /** Loads a track's audio and annotations. */
    @FunctionalInterface
    public interface Loader {
        /**
         * @return the decoded audio and its annotations
         * @throws IOException if the audio or an annotation file can't be read
         */
        Loaded load() throws IOException;
    }

    /**
     * A loaded track.
     * @param audio       the decoded audio
     * @param annotations its reference annotations
     */
    public record Loaded(AudioClip audio, Annotations annotations) {}

    /** @return {@code group/name}, the string {@code --filter} matches against */
    public String id() {
        return group + "/" + name;
    }
}
