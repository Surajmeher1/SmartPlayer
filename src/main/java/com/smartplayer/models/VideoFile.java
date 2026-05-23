package com.smartplayer.models;

import java.io.File;
import java.nio.file.Path;

/**
 * Represents a single video file with metadata for sorting, playback, and display.
 * Implements Comparable for natural ordering by series name → sequence number → filename.
 */
public class VideoFile implements Comparable<VideoFile> {

    private final String filePath;
    private final String fileName;
    private String displayName;
    private String seriesName;
    private int sequenceNumber;
    private long duration; // in milliseconds
    private long lastPosition; // resume position in milliseconds
    private String subtitlePath;
    private boolean watched;

    public VideoFile(String filePath) {
        this.filePath = filePath;
        File file = new File(filePath);
        this.fileName = file.getName();
        this.displayName = stripExtension(fileName);
        this.seriesName = "";
        this.sequenceNumber = -1;
        this.duration = 0;
        this.lastPosition = 0;
        this.subtitlePath = null;
        this.watched = false;
    }

    /**
     * Strips the file extension from a filename.
     */
    private String stripExtension(String name) {
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(0, lastDot) : name;
    }

    /**
     * Checks if a companion .srt subtitle file exists alongside this video.
     * Sets the subtitlePath if found.
     */
    public void detectSubtitle() {
        Path videoPath = Path.of(filePath);
        String baseName = stripExtension(videoPath.getFileName().toString());
        Path parentDir = videoPath.getParent();
        if (parentDir != null) {
            // Check for exact match: video.srt
            File srtFile = parentDir.resolve(baseName + ".srt").toFile();
            if (srtFile.exists()) {
                this.subtitlePath = srtFile.getAbsolutePath();
                return;
            }
            // Check for case variations
            File[] files = parentDir.toFile().listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".srt") &&
                    stripExtension(name).equalsIgnoreCase(baseName));
            if (files != null && files.length > 0) {
                this.subtitlePath = files[0].getAbsolutePath();
            }
        }
    }

    @Override
    public int compareTo(VideoFile other) {
        // First compare by series name
        int seriesCompare = this.seriesName.compareToIgnoreCase(other.seriesName);
        if (seriesCompare != 0) return seriesCompare;

        // Then by sequence number
        if (this.sequenceNumber != other.sequenceNumber) {
            // Files without sequence numbers (-1) go after numbered files
            if (this.sequenceNumber == -1) return 1;
            if (other.sequenceNumber == -1) return -1;
            return Integer.compare(this.sequenceNumber, other.sequenceNumber);
        }

        // Fallback: natural string comparison on filename
        return this.fileName.compareToIgnoreCase(other.fileName);
    }

    @Override
    public String toString() {
        return displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoFile that = (VideoFile) o;
        return filePath.equals(that.filePath);
    }

    @Override
    public int hashCode() {
        return filePath.hashCode();
    }

    // --- Getters and Setters ---

    public String getFilePath() { return filePath; }
    public String getFileName() { return fileName; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getSeriesName() { return seriesName; }
    public void setSeriesName(String seriesName) { this.seriesName = seriesName; }
    public int getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    public long getLastPosition() { return lastPosition; }
    public void setLastPosition(long lastPosition) { this.lastPosition = lastPosition; }
    public String getSubtitlePath() { return subtitlePath; }
    public void setSubtitlePath(String subtitlePath) { this.subtitlePath = subtitlePath; }
    public boolean hasSubtitle() { return subtitlePath != null && !subtitlePath.isEmpty(); }
    public boolean isWatched() { return watched; }
    public void setWatched(boolean watched) { this.watched = watched; }
}
