package com.asteroid.duck.opengl.util.audio.music;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Song structure from the per-frame descriptors: keeps the current {@link Section} and reports
 * {@link MusicEvent}s. All decisions are causal and use only the last {@value #HISTORY_SECONDS} s.
 *
 * <ul>
 *   <li><b>Breakdown:</b> after ≥ {@value #ESTABLISHED_SECONDS} s of groove with kicks, no kick
 *       for {@value #KICKLESS_SECONDS} s and the bass or intensity well below its groove level.
 *       Music that never had a kick never breaks down.</li>
 *   <li><b>Build-up:</b> in a breakdown, tension (mean of brightness, noisiness, busyness and
 *       intensity) rising by at least {@value #BUILD_SLOPE}/s over the last
 *       {@value #SLOPE_SECONDS} s, for {@value #BUILD_HOLD_SECONDS} s.</li>
 *   <li><b>Drop:</b> from a breakdown or build-up, two kicks within {@value #DROP_KICK_GAP} s with
 *       the bass back to at least half its groove level.</li>
 *   <li><b>Section change:</b> novelty — the distance between the mean descriptors of the last
 *       {@value #RECENT_SECONDS} s and those of up to {@value #HISTORY_SECONDS} s before, in units
 *       of the earlier window's spread — crossing {@value #SECTION_NOVELTY}. Only frames since
 *       the last event are compared, so one change isn't reported twice.</li>
 *   <li><b>Track change:</b> a section change in a groove whose new material, over its first
 *       {@value #RECENT_SECONDS} s (and within {@value #TRACK_CONFIRM_SECONDS} s), has a tempo
 *       more than {@value #TRACK_TEMPO_CHANGE} away from before, or a different key signature
 *       (relative major/minor count as the same). A modulation inside one track also counts.</li>
 * </ul>
 */
final class StructureDetector {

    static final double ESTABLISHED_SECONDS = 8;
    static final double KICKLESS_SECONDS = 1.5;
    static final double SLOPE_SECONDS = 4;
    static final double BUILD_SLOPE = 0.02;
    static final double BUILD_HOLD_SECONDS = 1;
    static final double DROP_KICK_GAP = 1.5;
    static final double RECENT_SECONDS = 4;
    static final double HISTORY_SECONDS = 16;
    /** Minimum earlier-window length before novelty is computed. */
    static final double MIN_HISTORY_SECONDS = 8;
    static final double SECTION_NOVELTY = 1.5;
    static final double TRACK_CONFIRM_SECONDS = 10;
    static final double TRACK_TEMPO_CHANGE = 0.04;
    /** Quiet time after any event before a {@link MusicEvent.Type#SECTION_CHANGE} may follow. */
    static final double REFRACTORY_SECONDS = 8;
    /** Floor on a descriptor's spread, so a perfectly steady one doesn't make noise look novel. */
    static final double MIN_SPREAD = 0.08;
    static final int DESCRIPTORS = 9;

    private final double frameSeconds;
    private final Consumer<MusicEvent> events;
    private final int recentFrames;
    private final int historyFrames;
    private final int slopeFrames;
    private final double grooveAlpha;

    // History ring: descriptors, chroma and tempo per frame
    private final float[][] descriptors;
    private final float[][] chroma;
    private final double[] tempo;
    private final float[] tension;
    private long frames;
    /** Frames since the last event: novelty only compares frames from the current section. */
    private long framesInSection;

    private Section section = Section.SILENCE;
    private double sectionStart;
    private double lastEvent = Double.NEGATIVE_INFINITY;
    private float novelty;

    // Kicks
    private final double[] kicks = new double[16];
    private int kickCount;
    private double groovedSeconds;
    private double grooveBass = Double.NaN;
    private double grooveIntensity = Double.NaN;
    private double risingSeconds;

    // Track change confirmation
    private double trackPeakTime = Double.NaN;
    private double tempoBefore;
    private int signatureBefore;

    // Scratch
    private final double[] meanA = new double[DESCRIPTORS];
    private final double[] meanB = new double[DESCRIPTORS];
    private final double[] chromaA = new double[12];
    private final double[] chromaB = new double[12];

    StructureDetector(double frameSeconds, Consumer<MusicEvent> events) {
        this.frameSeconds = frameSeconds;
        this.events = events;
        this.recentFrames = (int) Math.round(RECENT_SECONDS / frameSeconds);
        this.historyFrames = (int) Math.round(HISTORY_SECONDS / frameSeconds);
        this.slopeFrames = (int) Math.round(SLOPE_SECONDS / frameSeconds);
        int size = recentFrames + historyFrames;
        this.descriptors = new float[size][DESCRIPTORS];
        this.chroma = new float[size][12];
        this.tempo = new double[size];
        this.tension = new float[size];
        this.grooveAlpha = 1 - Math.exp(-frameSeconds / 4.0);
    }

    void onKick(double time) {
        kicks[kickCount++ % kicks.length] = time;
    }

    Section section() {
        return section;
    }

    double sectionStart() {
        return sectionStart;
    }

    float novelty() {
        return novelty;
    }

    /**
     * Process one frame.
     * @param f the frame's descriptors (smoothed) and raw chroma
     */
    void update(MusicAnalyser.Frame f) {
        double now = f.time;
        if (f.silent) {
            if (section != Section.SILENCE) {
                enter(Section.SILENCE, MusicEvent.Type.SILENCE_START, now);
                resetHistory();
            }
            return;
        }
        if (section == Section.SILENCE) enter(Section.GROOVE, MusicEvent.Type.MUSIC_START, now);

        int slot = (int) (frames++ % descriptors.length);
        float[] d = descriptors[slot];
        d[0] = f.intensity;
        d[1] = f.bass;
        d[2] = f.brightness;
        d[3] = f.noisiness;
        d[4] = f.busyness;
        d[5] = f.kickRate;
        d[6] = f.low;
        d[7] = f.mid;
        d[8] = f.high;
        for (int i = 0; i < 12; i++) chroma[slot][i] = (float) f.chroma[i];
        tempo[slot] = f.bpm;
        tension[slot] = (f.brightness + f.noisiness + f.busyness + f.intensity) / 4;
        framesInSection++;

        double lastKick = kick(0);
        double previousKick = kick(1);
        boolean kicking = now - previousKick <= 2;

        switch (section) {
            case GROOVE -> {
                if (kicking) {
                    groovedSeconds += frameSeconds;
                    grooveBass = Double.isNaN(grooveBass) ? f.bass : grooveBass + grooveAlpha * (f.bass - grooveBass);
                    grooveIntensity = Double.isNaN(grooveIntensity) ? f.intensity
                            : grooveIntensity + grooveAlpha * (f.intensity - grooveIntensity);
                }
                if (groovedSeconds >= ESTABLISHED_SECONDS && now - lastKick >= KICKLESS_SECONDS
                        && (f.bass < grooveBass - 0.3 || f.intensity < grooveIntensity - 0.2)) {
                    groovedSeconds = 0;
                    risingSeconds = 0;
                    enter(Section.BREAKDOWN, MusicEvent.Type.BREAKDOWN_START, now);
                }
            }
            case BREAKDOWN, BUILDUP -> {
                boolean kicksBack = now - lastKick <= 0.3 && lastKick - previousKick <= DROP_KICK_GAP;
                if (kicksBack && f.bassFast >= 0.5 * grooveBass) {
                    enter(Section.GROOVE, MusicEvent.Type.DROP, now);
                } else if (kicksSince(Math.max(now - 4, sectionStart)) >= 6) {
                    // the kick came back without the bass: just a new section
                    enter(Section.GROOVE, MusicEvent.Type.SECTION_CHANGE, now);
                } else if (section == Section.BREAKDOWN && now - sectionStart >= 2) {
                    risingSeconds = tensionSlope() >= BUILD_SLOPE ? risingSeconds + frameSeconds : 0;
                    if (risingSeconds >= BUILD_HOLD_SECONDS) enter(Section.BUILDUP, MusicEvent.Type.BUILDUP_START, now);
                }
            }
            case SILENCE -> { }
        }

        updateNovelty(now);
    }

    private void updateNovelty(double now) {
        confirmTrackChange(now);
        long available = Math.min(framesInSection, descriptors.length);
        if (available < recentFrames + MIN_HISTORY_SECONDS / frameSeconds) {
            novelty = 0;
            return;
        }
        int older = (int) (available - recentFrames);
        Arrays.fill(meanA, 0);
        Arrays.fill(meanB, 0);
        for (int age = 0; age < available; age++) {
            double[] m = age < recentFrames ? meanA : meanB;
            for (int i = 0; i < DESCRIPTORS; i++) m[i] += descriptors[slot(age)][i];
        }
        for (int i = 0; i < DESCRIPTORS; i++) {
            meanA[i] /= recentFrames;
            meanB[i] /= older;
        }
        double sum = 0;
        for (int i = 0; i < DESCRIPTORS; i++) {
            double variance = 0;
            for (int age = recentFrames; age < available; age++) {
                double v = descriptors[slot(age)][i] - meanB[i];
                variance += v * v;
            }
            double spread = Math.max(MIN_SPREAD, Math.sqrt(variance / older));
            double z = (meanA[i] - meanB[i]) / spread;
            sum += z * z;
        }
        // Harmony: 1 − cosine similarity of the mean chroma, 0.1 counting as one spread
        sumChroma(0, recentFrames, chromaA);
        sumChroma(recentFrames, (int) available, chromaB);
        double zChroma = (1 - cosine(chromaA, chromaB)) / 0.1;
        sum += zChroma * zChroma;
        novelty = (float) (Math.sqrt(sum) / 2);

        if (novelty >= SECTION_NOVELTY && now - lastEvent >= REFRACTORY_SECONDS) {
            if (section == Section.GROOVE) {
                trackPeakTime = now;
                tempoBefore = medianTempo(recentFrames, (int) available);
                signatureBefore = signature(chromaB);
            }
            emit(MusicEvent.Type.SECTION_CHANGE, now);
        }
    }

    /**
     * After a strong novelty peak, wait for the last {@value #RECENT_SECONDS} s to be all new
     * material, then compare its tempo and key signature with what came before the peak.
     */
    private void confirmTrackChange(double now) {
        if (Double.isNaN(trackPeakTime)) return;
        if (now - trackPeakTime > TRACK_CONFIRM_SECONDS || section != Section.GROOVE) {
            trackPeakTime = Double.NaN;
            return;
        }
        if (now - trackPeakTime < RECENT_SECONDS) return;
        double tempoAfter = medianTempo(0, recentFrames);
        sumChroma(0, recentFrames, chromaA);
        boolean tempoChanged = false;
        if (Double.isFinite(tempoBefore) && Double.isFinite(tempoAfter)) {
            // a jump to double or half tempo is the beat tracker changing its mind, not a new track
            double ratio = tempoAfter / tempoBefore;
            double octaves = Math.abs(Math.log(ratio) / Math.log(2));
            tempoChanged = Math.abs(ratio - 1) > TRACK_TEMPO_CHANGE
                    && Math.abs(octaves - 1) > TRACK_TEMPO_CHANGE;
        }
        boolean keyChanged = signature(chromaA) != signatureBefore;
        if (tempoChanged || keyChanged) {
            trackPeakTime = Double.NaN;
            emit(MusicEvent.Type.TRACK_CHANGE, now);
        }
    }

    private void sumChroma(int fromAge, int toAge, double[] out) {
        Arrays.fill(out, 0);
        for (int age = fromAge; age < toAge; age++) {
            for (int i = 0; i < 12; i++) out[i] += chroma[slot(age)][i];
        }
    }

    /** Key signature of a chroma vector: relative major and minor keys share one. */
    private static int signature(double[] chroma) {
        int key = KeyEstimator.best(chroma);
        return key < 12 ? key : (key - 12 + 3) % 12;
    }

    private double medianTempo(int fromAge, int toAge) {
        double[] values = new double[toAge - fromAge];
        int n = 0;
        for (int age = fromAge; age < toAge; age++) {
            double t = tempo[slot(age)];
            if (Double.isFinite(t)) values[n++] = t;
        }
        if (n == 0) return Double.NaN;
        Arrays.sort(values, 0, n);
        return values[n / 2];
    }

    /** Least-squares slope of tension over the last {@value #SLOPE_SECONDS} s, per second. */
    private double tensionSlope() {
        int n = (int) Math.min(slopeFrames, Math.min(framesInSection, tension.length));
        if (n < 4) return 0;
        double sx = 0, sy = 0, sxx = 0, sxy = 0;
        for (int age = 0; age < n; age++) {
            double x = -age * frameSeconds;
            double y = tension[slot(age)];
            sx += x;
            sy += y;
            sxx += x * x;
            sxy += x * y;
        }
        double denominator = n * sxx - sx * sx;
        return denominator == 0 ? 0 : (n * sxy - sx * sy) / denominator;
    }

    private int kicksSince(double time) {
        int count = 0;
        for (int i = 0; i < Math.min(kickCount, kicks.length); i++) if (kick(i) >= time) count++;
        return count;
    }

    /** @return the {@code n}th most recent kick time, or −∞ */
    private double kick(int n) {
        if (n >= Math.min(kickCount, kicks.length)) return Double.NEGATIVE_INFINITY;
        return kicks[Math.floorMod(kickCount - 1 - n, kicks.length)];
    }

    private int slot(int age) {
        return (int) Math.floorMod(frames - 1 - age, (long) descriptors.length);
    }

    private void enter(Section next, MusicEvent.Type type, double now) {
        section = next;
        sectionStart = now;
        emit(type, now);
    }

    private void emit(MusicEvent.Type type, double now) {
        lastEvent = now;
        if (type != MusicEvent.Type.TRACK_CHANGE) framesInSection = 0;
        events.accept(new MusicEvent(type, now, section));
    }

    private void resetHistory() {
        framesInSection = 0;
        groovedSeconds = 0;
        grooveBass = Double.NaN;
        grooveIntensity = Double.NaN;
        trackPeakTime = Double.NaN;
        novelty = 0;
        kickCount = 0;
    }

    private static double cosine(double[] a, double[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return na == 0 || nb == 0 ? 1 : dot / Math.sqrt(na * nb);
    }
}
