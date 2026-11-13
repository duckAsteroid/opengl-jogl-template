package com.asteroid.duck.opengl.util.audio.beat.eval;

/**
 * One analyser's scores on one track. Any score is {@code NaN} when the track lacks the
 * annotation it needs (e.g. kick F without kick annotations).
 * @param analyser analyser name
 * @param group    track group
 * @param track    track name
 * @param duration track length in seconds
 * @param refBpm   reference tempo
 * @param estBpm   estimated tempo
 * @param acc1     tempo Acc1, 1 or 0
 * @param acc2     tempo Acc2, 1 or 0
 * @param beatF    beat F-measure
 * @param cmlt     beat continuity, correct metrical level, total
 * @param amlt     beat continuity, any metrical level, total
 * @param lockTime seconds until locked on, or {@code NaN} if it never locked (or no beats)
 * @param kickF    kick onset F-measure
 * @param downbeatF downbeat F-measure; {@code NaN} if the analyser reported no downbeats
 */
public record TrackResult(String analyser, String group, String track, double duration,
                          double refBpm, double estBpm, double acc1, double acc2,
                          double beatF, double cmlt, double amlt, double lockTime, double kickF,
                          double downbeatF) {
}
