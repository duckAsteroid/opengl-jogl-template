package com.asteroid.duck.opengl.util.audio.music;

/**
 * Maps a level in dB onto {@code [0, 1]} relative to its own recent range: the ceiling jumps up
 * to any louder value and the floor down to any quieter one, and both relax towards each other
 * at {@code relaxDbPerSecond} until they are {@code minSpanDb} apart. So a loud section reads
 * near 1 and a breakdown near 0 whatever the playback volume, and a uniformly quiet piece drifts
 * back up to mid-range.
 */
final class AutoRange {

    private final double minSpanDb;
    private final double maxSpanDb;
    private final double relaxDbPerSecond;
    private double floor = Double.NaN;
    private double ceiling = Double.NaN;

    AutoRange(double minSpanDb, double maxSpanDb, double relaxDbPerSecond) {
        this.minSpanDb = minSpanDb;
        this.maxSpanDb = maxSpanDb;
        this.relaxDbPerSecond = relaxDbPerSecond;
    }

    /**
     * @param db level in dB
     * @param dt seconds since the previous update
     * @return the level's position in the range, {@code [0, 1]}
     */
    double update(double db, double dt) {
        if (Double.isNaN(ceiling)) {
            ceiling = db;
            floor = db - minSpanDb;
        }
        double relax = relaxDbPerSecond * dt;
        ceiling = Math.max(db, Math.max(floor + minSpanDb, ceiling - relax));
        floor = Math.min(db, Math.min(ceiling - minSpanDb, floor + relax));
        floor = Math.max(floor, ceiling - maxSpanDb);
        return Math.clamp((db - floor) / (ceiling - floor), 0, 1);
    }

    void reset() {
        floor = Double.NaN;
        ceiling = Double.NaN;
    }
}
