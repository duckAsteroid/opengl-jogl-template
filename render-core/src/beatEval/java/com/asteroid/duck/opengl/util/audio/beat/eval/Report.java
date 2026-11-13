package com.asteroid.duck.opengl.util.audio.beat.eval;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/** Formats evaluation results as a per-track CSV and a per-group Markdown summary. */
public final class Report {

    private Report() {}

    /**
     * @param results per-track results
     * @return CSV with a header row; {@code NaN} scores are empty cells
     */
    public static String csv(List<TrackResult> results) {
        StringBuilder out = new StringBuilder(
                "analyser,group,track,duration,ref_bpm,est_bpm,acc1,acc2,beat_f,cmlt,amlt,lock_time,kick_f,downbeat_f\n");
        for (TrackResult r : results) {
            out.append(String.join(",", r.analyser(), r.group(), r.track(),
                    cell(r.duration()), cell(r.refBpm()), cell(r.estBpm()), cell(r.acc1()), cell(r.acc2()),
                    cell(r.beatF()), cell(r.cmlt()), cell(r.amlt()), cell(r.lockTime()), cell(r.kickF()),
                    cell(r.downbeatF())))
                    .append('\n');
        }
        return out.toString();
    }

    /**
     * One table per analyser: a row per group plus an "all" row. Scores are means over the
     * tracks that have the needed annotation; lock time is the median over tracks that locked,
     * with the share that locked in brackets.
     * @param results  per-track results
     * @param analysers the analysers, for their descriptions
     * @return Markdown
     */
    public static String summary(List<TrackResult> results, List<Analysers.Entry> analysers) {
        StringBuilder out = new StringBuilder("# Beat tracking evaluation\n");
        Map<String, List<TrackResult>> byAnalyser = results.stream()
                .collect(Collectors.groupingBy(TrackResult::analyser, LinkedHashMap::new, Collectors.toList()));
        for (Analysers.Entry analyser : analysers) {
            List<TrackResult> mine = byAnalyser.getOrDefault(analyser.name(), List.of());
            if (mine.isEmpty()) continue;
            out.append("\n## ").append(analyser.name()).append("\n\n").append(analyser.description()).append("\n\n");
            out.append("| group | tracks | beat F | CMLt | AMLt | Acc1 | Acc2 | lock s (locked) | kick F | downbeat F |\n");
            out.append("|---|--:|--:|--:|--:|--:|--:|--:|--:|--:|\n");
            Map<String, List<TrackResult>> groups = new TreeMap<>(mine.stream()
                    .collect(Collectors.groupingBy(TrackResult::group)));
            List<Map.Entry<String, List<TrackResult>>> rows = new ArrayList<>(groups.entrySet());
            if (groups.size() > 1) rows.add(Map.entry("**all**", mine));
            for (Map.Entry<String, List<TrackResult>> row : rows) {
                List<TrackResult> rs = row.getValue();
                out.append("| ").append(row.getKey())
                        .append(" | ").append(rs.size())
                        .append(" | ").append(score(rs, TrackResult::beatF))
                        .append(" | ").append(score(rs, TrackResult::cmlt))
                        .append(" | ").append(score(rs, TrackResult::amlt))
                        .append(" | ").append(percent(rs, TrackResult::acc1))
                        .append(" | ").append(percent(rs, TrackResult::acc2))
                        .append(" | ").append(lock(rs))
                        .append(" | ").append(score(rs, TrackResult::kickF))
                        .append(" | ").append(score(rs, TrackResult::downbeatF))
                        .append(" |\n");
            }
        }
        return out.toString();
    }

    private static double[] values(List<TrackResult> rs, ToDoubleFunction<TrackResult> metric) {
        return rs.stream().mapToDouble(metric).toArray();
    }

    private static String score(List<TrackResult> rs, ToDoubleFunction<TrackResult> metric) {
        double mean = Stats.mean(values(rs, metric));
        return Double.isNaN(mean) ? "–" : String.format(Locale.ROOT, "%.3f", mean);
    }

    private static String percent(List<TrackResult> rs, ToDoubleFunction<TrackResult> metric) {
        double mean = Stats.mean(values(rs, metric));
        return Double.isNaN(mean) ? "–" : String.format(Locale.ROOT, "%.0f%%", 100 * mean);
    }

    private static String lock(List<TrackResult> rs) {
        long withBeats = rs.stream().filter(r -> !Double.isNaN(r.beatF())).count();
        double[] times = values(rs, TrackResult::lockTime);
        long locked = Stats.count(times);
        if (withBeats == 0) return "–";
        String median = locked == 0 ? "–" : String.format(Locale.ROOT, "%.1f", Stats.median(times));
        return String.format(Locale.ROOT, "%s (%.0f%%)", median, 100.0 * locked / withBeats);
    }

    private static String cell(double value) {
        return Double.isNaN(value) ? "" : String.format(Locale.ROOT, "%.4f", value);
    }
}
