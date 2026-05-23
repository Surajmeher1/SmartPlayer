package com.smartplayer.scanner;

import com.smartplayer.models.VideoFile;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent video sorting engine using regex pattern matching.
 * Detects common naming conventions for series, episodes, and parts,
 * then sorts videos in logical playback order.
 *
 * Supported patterns (priority order):
 *   1. S01E03         — Season/Episode
 *   2. Episode 03     — Episode keyword
 *   3. EP03           — EP shorthand
 *   4. Part 3 / Pt.3  — Part keyword
 *   5. "3 of 10"      — X of Y style
 *   6. Chapter 3      — Chapter keyword
 *   7. #03 or - 03 -  — Numbering delimiters
 *   8. Trailing digits — Fallback numeric extraction
 */
public class VideoSorter {

    // Patterns ordered by specificity (most specific first)
    private static final Pattern[] PATTERNS = {
        // S01E03, s1e3, S01.E03
        Pattern.compile("(?i)[Ss](\\d{1,2})[.\\s_-]*[Ee](\\d{1,3})"),

        // Episode 03, Episode.03, Episode_03
        Pattern.compile("(?i)(?:Episode|Ep\\.?)[\\s._-]*(\\d{1,4})"),

        // EP03, ep03 (without space)
        Pattern.compile("(?i)EP(\\d{1,4})"),

        // Part 3, Part.03, Pt 3, Pt.3
        Pattern.compile("(?i)(?:Part|Pt\\.?)[\\s._-]*(\\d{1,4})"),

        // 3 of 10
        Pattern.compile("(?i)(\\d{1,4})\\s+of\\s+\\d{1,4}"),

        // Chapter 3, Ch.3, Ch 03
        Pattern.compile("(?i)(?:Chapter|Ch\\.?)[\\s._-]*(\\d{1,4})"),

        // #03 or - 03 - or _03_
        Pattern.compile("[#\\-_]\\s*(\\d{1,4})\\s*[#\\-_]?"),

        // Trailing number: video_name_03.mp4 or video_name.03.mp4
        Pattern.compile("[\\s._-](\\d{1,4})(?:\\s*$|\\.[a-zA-Z]{2,5}$)")
    };

    /**
     * Analyzes each video file's name to extract series name and sequence number,
     * then sorts the list in proper playback order.
     *
     * @param videos List of VideoFile objects to analyze and sort in-place
     */
    public void analyzeAndSort(List<VideoFile> videos) {
        for (VideoFile video : videos) {
            analyzeFileName(video);
        }
        Collections.sort(videos);
    }

    /**
     * Parses a single video filename to extract series name and sequence number.
     * Tries each pattern in order; the first match wins.
     */
    private void analyzeFileName(VideoFile video) {
        String name = video.getDisplayName();

        for (int i = 0; i < PATTERNS.length; i++) {
            Matcher matcher = PATTERNS[i].matcher(name);
            if (matcher.find()) {
                // Extract sequence number
                if (i == 0) {
                    // Season/Episode pattern: combine season and episode into a single sortable number
                    // e.g., S02E03 → 2003 (season * 1000 + episode)
                    int season = Integer.parseInt(matcher.group(1));
                    int episode = Integer.parseInt(matcher.group(2));
                    video.setSequenceNumber(season * 1000 + episode);
                } else {
                    // All other patterns: extract the captured number group
                    int groupIndex = (i == 0) ? 2 : 1;
                    video.setSequenceNumber(Integer.parseInt(matcher.group(groupIndex)));
                }

                // Extract series name: everything before the matched pattern
                String seriesName = name.substring(0, matcher.start()).trim();
                // Clean up trailing separators
                seriesName = seriesName.replaceAll("[\\s._-]+$", "");
                if (!seriesName.isEmpty()) {
                    video.setSeriesName(seriesName);
                }
                return;
            }
        }

        // No pattern matched — try to extract any number from the filename
        Matcher numberMatcher = Pattern.compile("(\\d+)").matcher(name);
        int lastNumber = -1;
        while (numberMatcher.find()) {
            lastNumber = Integer.parseInt(numberMatcher.group(1));
        }
        if (lastNumber >= 0) {
            video.setSequenceNumber(lastNumber);
        }
    }
}
