package com.asteroid.duck.opengl.util.audio.music;

/**
 * A musical key.
 * @param tonic pitch class of the tonic, 0 = C … 11 = B
 * @param major {@code true} for major, {@code false} for minor
 */
public record Key(int tonic, boolean major) {

    private static final String[] NAMES = {"C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"};

    public Key {
        if (tonic < 0 || tonic > 11) throw new IllegalArgumentException("tonic must be 0-11: " + tonic);
    }

    /** @return e.g. {@code "A minor"} */
    public String name() {
        return NAMES[tonic] + (major ? " major" : " minor");
    }

    @Override
    public String toString() {
        return name();
    }
}
