package com.smartplayer.player;

import com.smartplayer.models.VideoFile;

import java.io.File;
import java.nio.file.Path;

/**
 * Manages subtitle detection and loading for video files.
 * Auto-detects .srt files matching the video filename.
 */
public class SubtitleManager {

    private final MediaPlayerManager playerManager;
    private boolean subtitlesEnabled = true;
    private String currentSubtitlePath;

    public SubtitleManager(MediaPlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    /**
     * Attempts to load subtitles for the given video file.
     * Checks for .srt files in the same directory with matching name.
     *
     * @param video The currently playing video file
     */
    public void loadForVideo(VideoFile video) {
        currentSubtitlePath = null;

        if (video.hasSubtitle()) {
            currentSubtitlePath = video.getSubtitlePath();
        } else {
            // Try to find a subtitle file
            String videoPath = video.getFilePath();
            String baseName = stripExtension(Path.of(videoPath).getFileName().toString());
            File parentDir = new File(videoPath).getParentFile();

            if (parentDir != null) {
                // Look for .srt, .sub, .ass, .ssa files
                String[] subtitleExts = {".srt", ".sub", ".ass", ".ssa", ".vtt"};
                for (String ext : subtitleExts) {
                    File subFile = new File(parentDir, baseName + ext);
                    if (subFile.exists()) {
                        currentSubtitlePath = subFile.getAbsolutePath();
                        video.setSubtitlePath(currentSubtitlePath);
                        break;
                    }
                }
            }
        }

        // Load the subtitle if found and subtitles are enabled
        if (currentSubtitlePath != null && subtitlesEnabled) {
            // Delay loading to ensure media is playing
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    playerManager.loadSubtitle(currentSubtitlePath);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    /**
     * Toggles subtitles on/off.
     * @return true if subtitles are now enabled
     */
    public boolean toggle() {
        subtitlesEnabled = !subtitlesEnabled;
        playerManager.toggleSubtitles(subtitlesEnabled);
        return subtitlesEnabled;
    }

    /**
     * Returns whether subtitles are currently enabled.
     */
    public boolean isEnabled() {
        return subtitlesEnabled;
    }

    /**
     * Returns whether the current video has subtitles available.
     */
    public boolean hasSubtitles() {
        return currentSubtitlePath != null;
    }

    private String stripExtension(String name) {
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(0, lastDot) : name;
    }
}
