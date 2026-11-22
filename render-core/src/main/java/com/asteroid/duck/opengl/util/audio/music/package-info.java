/**
 * Slow-moving musical understanding for scripting visuals: intensity and timbre descriptors,
 * key and mood, and song-structure events (breakdowns, build-ups, drops, section and track
 * changes).
 *
 * <p>{@link com.asteroid.duck.opengl.util.audio.music.MusicAnalyser} consumes mono samples on
 * one thread and reports a {@link com.asteroid.duck.opengl.util.audio.music.MusicState} every
 * {@value com.asteroid.duck.opengl.util.audio.music.MusicAnalyser#FRAME_SECONDS} s, plus
 * {@link com.asteroid.duck.opengl.util.audio.music.MusicEvent}s, to a
 * {@link com.asteroid.duck.opengl.util.audio.music.MusicListener}. Like the beat package, time
 * is the sample clock, so it runs identically live and offline.
 * {@link com.asteroid.duck.opengl.util.audio.music.MusicStateSink} wraps it for live use.</p>
 */
package com.asteroid.duck.opengl.util.audio.music;
