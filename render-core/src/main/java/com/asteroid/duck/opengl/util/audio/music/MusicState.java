package com.asteroid.duck.opengl.util.audio.music;

/**
 * Slow-moving description of the music at one moment. Every value is smoothed (time constants
 * in brackets), so it can drive visuals directly. Values are in {@code [0, 1]} unless stated.
 *
 * @param time          seconds on the analyser's sample clock
 * @param levelDb       short-term (400 ms RMS) level in dBFS
 * @param silent        below −60 dBFS for the last 1.5 s
 * @param intensity     loudness relative to this music's own recent range: 1 at its loudest,
 *                      0 at its quietest (0.5 s). Quiet music isn't low intensity for long
 * @param bass          sub/bass (&lt; 150 Hz) level relative to its recent range (0.5 s)
 * @param brightness    spectral centroid, log-mapped from 200 Hz (0) to 8 kHz (1) (0.5 s)
 * @param noisiness     spectral flatness: 0 for pure tones, 1 for white noise (0.5 s)
 * @param busyness      onsets per second over the last 3 s, 8/s = 1
 * @param kickRate      kicks per second over the last 3 s, 2.5/s = 1
 * @param low           share of energy below 250 Hz (0.5 s); {@code low + mid + high = 1}
 * @param mid           share of energy from 250 Hz to 4 kHz
 * @param high          share of energy above 4 kHz
 * @param key           the estimated key, or {@code null} until there is one (~15 s average)
 * @param keyConfidence confidence in the key: correlation with its profile, reduced when
 *                      another key (often the relative major/minor) is close behind
 * @param arousal       energy/activation: intensity, busyness, brightness, kicks and tempo
 *                      combined (3 s). 0.5 is neutral
 * @param valence       a <em>weak</em> positive/negative hint from major/minor and brightness
 *                      (8 s). 0.5 is neutral, and most music stays within 0.3–0.7
 * @param section       the current section
 * @param sectionStart  sample-clock seconds when the current section began
 * @param novelty       how different the last 4 s sound from the 16 s before, in spreads
 *                      (≥ 0, unbounded; section changes fire above ~1.2)
 */
public record MusicState(double time, double levelDb, boolean silent,
                         float intensity, float bass, float brightness, float noisiness,
                         float busyness, float kickRate, float low, float mid, float high,
                         Key key, float keyConfidence, float arousal, float valence,
                         Section section, double sectionStart, float novelty) {

    /** The state before any audio has been analysed. */
    public static final MusicState INITIAL = new MusicState(0, -120, true,
            0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0.5f, 0.5f, Section.SILENCE, 0, 0);
}
