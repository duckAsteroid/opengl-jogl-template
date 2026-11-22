package com.asteroid.duck.opengl.util.audio.music;

/**
 * Something changed in the music's structure. Events are reported when detected, which is after
 * the change itself: within a beat or two for a {@link Type#DROP}, typically 1–2 bars for a
 * {@link Type#SECTION_CHANGE}. To land a visual transition on the beat, snap it to the next beat
 * from a beat tracker.
 * @param type    what happened
 * @param time    seconds on the analyser's sample clock when it was detected
 * @param section the section the music is in after the event
 */
public record MusicEvent(Type type, double time, Section section) {

    /** Kinds of structural event. */
    public enum Type {
        /** Sound started after silence (or at the very start) — usually a new track. */
        MUSIC_START,
        /** The music went silent. */
        SILENCE_START,
        /** The kick dropped out and the energy fell: entered {@link Section#BREAKDOWN}. */
        BREAKDOWN_START,
        /** Tension started building in a breakdown: entered {@link Section#BUILDUP}. */
        BUILDUP_START,
        /** Kick and bass came back hard after a breakdown or build-up: back to {@link Section#GROOVE}. */
        DROP,
        /** The overall sound changed markedly (a new section) without a more specific event. */
        SECTION_CHANGE,
        /** A section change that also changed tempo or key: probably the next track in a mix. */
        TRACK_CHANGE
    }
}
