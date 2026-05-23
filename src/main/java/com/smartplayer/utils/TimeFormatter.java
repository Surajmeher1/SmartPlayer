package com.smartplayer.utils;

/**
 * Utility class for formatting time durations.
 * Converts milliseconds to human-readable HH:MM:SS or MM:SS format.
 */
public class TimeFormatter {

    /**
     * Formats milliseconds into HH:MM:SS or MM:SS string.
     * Uses HH:MM:SS format when duration >= 1 hour.
     *
     * @param millis Time in milliseconds
     * @return Formatted time string
     */
    public static String format(long millis) {
        if (millis < 0) millis = 0;

        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    /**
     * Formats remaining time (shows as negative).
     *
     * @param currentMs Current position in milliseconds
     * @param totalMs   Total duration in milliseconds
     * @return Formatted remaining time with minus prefix (e.g., "-1:23:45")
     */
    public static String formatRemaining(long currentMs, long totalMs) {
        long remaining = totalMs - currentMs;
        if (remaining < 0) remaining = 0;
        return "-" + format(remaining);
    }

    /**
     * Formats a compact time string for display in playlist items.
     * e.g., "1h 23m" or "45m"
     */
    public static String formatCompact(long millis) {
        if (millis <= 0) return "";

        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm", minutes);
        } else {
            return String.format("%ds", totalSeconds);
        }
    }
}
