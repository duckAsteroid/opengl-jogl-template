package com.asteroid.duck.opengl.util.audio.music;

/** The kind of passage the music is in now, as {@link MusicAnalyser} judges it. */
public enum Section {
    /** Below −60 dBFS for 1.5 s; also the state before any sound. */
    SILENCE,
    /** The main body of the music — the default whenever nothing more specific applies. */
    GROOVE,
    /** The kick has dropped out (after being established) and the bass or level has fallen. */
    BREAKDOWN,
    /** In a breakdown, with tension (brightness, noise, busyness, level) rising steadily. */
    BUILDUP
}
