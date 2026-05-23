package com.smartplayer.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Manages an ordered playlist of VideoFile items with current index tracking.
 * Provides navigation (next/previous), observable list for UI binding,
 * and auto-next capabilities.
 */
public class Playlist {

    private final ObservableList<VideoFile> videos = FXCollections.observableArrayList();
    private final IntegerProperty currentIndex = new SimpleIntegerProperty(-1);

    /**
     * Replaces the entire playlist with a new list of videos.
     * Resets current index to -1 (nothing playing).
     */
    public void setVideos(java.util.List<VideoFile> newVideos) {
        videos.clear();
        videos.addAll(newVideos);
        currentIndex.set(-1);
    }

    /**
     * Adds a single video to the end of the playlist.
     */
    public void addVideo(VideoFile video) {
        videos.add(video);
    }

    /**
     * Returns the currently selected video, or null if nothing is selected.
     */
    public VideoFile getCurrentVideo() {
        int idx = currentIndex.get();
        if (idx >= 0 && idx < videos.size()) {
            return videos.get(idx);
        }
        return null;
    }

    /**
     * Selects a specific video by index and returns it.
     */
    public VideoFile selectVideo(int index) {
        if (index >= 0 && index < videos.size()) {
            currentIndex.set(index);
            return videos.get(index);
        }
        return null;
    }

    /**
     * Selects a specific VideoFile object and returns its index.
     */
    public int selectVideo(VideoFile video) {
        int idx = videos.indexOf(video);
        if (idx >= 0) {
            currentIndex.set(idx);
        }
        return idx;
    }

    /**
     * Advances to the next video in the playlist.
     * Returns the next VideoFile, or null if at the end.
     */
    public VideoFile next() {
        int nextIdx = currentIndex.get() + 1;
        if (nextIdx < videos.size()) {
            currentIndex.set(nextIdx);
            return videos.get(nextIdx);
        }
        return null;
    }

    /**
     * Goes back to the previous video in the playlist.
     * Returns the previous VideoFile, or null if at the start.
     */
    public VideoFile previous() {
        int prevIdx = currentIndex.get() - 1;
        if (prevIdx >= 0) {
            currentIndex.set(prevIdx);
            return videos.get(prevIdx);
        }
        return null;
    }

    public boolean hasNext() {
        return currentIndex.get() + 1 < videos.size();
    }

    public boolean hasPrevious() {
        return currentIndex.get() - 1 >= 0;
    }

    public boolean isEmpty() {
        return videos.isEmpty();
    }

    public int size() {
        return videos.size();
    }

    // --- Observable properties for UI binding ---

    public ObservableList<VideoFile> getVideos() { return videos; }
    public IntegerProperty currentIndexProperty() { return currentIndex; }
    public int getCurrentIndex() { return currentIndex.get(); }
}
