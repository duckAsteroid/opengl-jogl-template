package com.asteroid.duck.opengl.util.audio.music;

/**
 * Receives what a {@link MusicAnalyser} finds, on the thread that calls
 * {@link MusicAnalyser#process}. Both methods default to no-ops.
 */
public interface MusicListener {

    /**
     * A new state, every {@value MusicAnalyser#FRAME_SECONDS} s.
     * @param state the current state (immutable)
     */
    default void onState(MusicState state) {}

    /**
     * A structural event. Reported before the {@link #onState} of the same frame.
     * @param event the event
     */
    default void onEvent(MusicEvent event) {}
}
