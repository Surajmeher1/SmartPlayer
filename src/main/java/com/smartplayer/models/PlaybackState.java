package com.smartplayer.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores persistent playback state for resume functionality and watch history.
 * Serialized to/from JSON via Gson.
 */
public class PlaybackState {

    /** Maps video file path → last playback position in milliseconds */
    private Map<String, Long> positions = new HashMap<>();

    /** Last opened folder path */
    private String lastOpenedFolder;

    /** Recently played video paths (most recent first, max 50) */
    private List<String> recentlyPlayed = new ArrayList<>();

    /** Last known volume level (0-100) */
    private int volume = 80;

    /** Last known playback speed */
    private float playbackSpeed = 1.0f;

    private static final int MAX_RECENT = 50;

    /**
     * Saves the playback position for a video file.
     */
    public void savePosition(String filePath, long positionMs) {
        positions.put(filePath, positionMs);
    }

    /**
     * Gets the saved playback position for a video file.
     * Returns 0 if no position is saved.
     */
    public long getPosition(String filePath) {
        return positions.getOrDefault(filePath, 0L);
    }

    /**
     * Removes saved position (e.g., when video is fully watched).
     */
    public void clearPosition(String filePath) {
        positions.remove(filePath);
    }

    /**
     * Adds a video to the recently played list.
     * Moves it to the front if already present. Trims to MAX_RECENT.
     */
    public void addToRecentlyPlayed(String filePath) {
        recentlyPlayed.remove(filePath); // Remove if already present
        recentlyPlayed.add(0, filePath); // Add to front
        // Trim to max size
        if (recentlyPlayed.size() > MAX_RECENT) {
            recentlyPlayed = new ArrayList<>(recentlyPlayed.subList(0, MAX_RECENT));
        }
    }

    // --- Getters and Setters ---

    public Map<String, Long> getPositions() { return positions; }
    public void setPositions(Map<String, Long> positions) { this.positions = positions; }

    public String getLastOpenedFolder() { return lastOpenedFolder; }
    public void setLastOpenedFolder(String lastOpenedFolder) { this.lastOpenedFolder = lastOpenedFolder; }

    public List<String> getRecentlyPlayed() { return recentlyPlayed; }
    public void setRecentlyPlayed(List<String> recentlyPlayed) { this.recentlyPlayed = recentlyPlayed; }

    public int getVolume() { return volume; }
    public void setVolume(int volume) { this.volume = Math.max(0, Math.min(100, volume)); }

    public float getPlaybackSpeed() { return playbackSpeed; }
    public void setPlaybackSpeed(float playbackSpeed) { this.playbackSpeed = playbackSpeed; }
}
