package com.smartplayer.scanner;

import com.smartplayer.models.VideoFile;

import javafx.concurrent.Task;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Recursively scans a folder for video files.
 * Runs as a JavaFX Task to keep the UI responsive during scanning.
 * Auto-detects companion .srt subtitle files for each video.
 */
public class FolderScanner {

    /** Supported video file extensions (lowercase) */
    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            ".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm",
            ".m4v", ".ts", ".vob", ".3gp", ".mpg", ".mpeg"
    );

    /**
     * Creates a background Task that scans the given folder recursively
     * and returns a sorted list of VideoFile objects.
     *
     * @param folderPath The root folder to scan
     * @return A JavaFX Task producing the list of discovered video files
     */
    public Task<List<VideoFile>> scanAsync(String folderPath) {
        return new Task<>() {
            @Override
            protected List<VideoFile> call() throws Exception {
                updateMessage("Scanning folder...");
                List<VideoFile> results = scan(folderPath);
                updateMessage("Found " + results.size() + " videos");
                return results;
            }
        };
    }

    /**
     * Synchronously scans the given folder recursively for video files.
     * Detects subtitles and applies smart sorting.
     *
     * @param folderPath The root folder to scan
     * @return Sorted list of VideoFile objects
     */
    public List<VideoFile> scan(String folderPath) {
        Path root = Path.of(folderPath);
        if (!Files.isDirectory(root)) {
            return new ArrayList<>();
        }

        List<VideoFile> videos = new ArrayList<>();

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (isVideoFile(file)) {
                        VideoFile vf = new VideoFile(file.toAbsolutePath().toString());
                        vf.detectSubtitle();
                        videos.add(vf);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    // Skip inaccessible files/directories silently
                    System.err.println("Skipping inaccessible: " + file + " (" + exc.getMessage() + ")");
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // Skip hidden directories
                    if (dir.getFileName() != null && dir.getFileName().toString().startsWith(".")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Error scanning folder: " + e.getMessage());
        }

        // Apply smart sorting
        VideoSorter sorter = new VideoSorter();
        sorter.analyzeAndSort(videos);

        return videos;
    }

    /**
     * Checks if a file is a video file based on its extension.
     */
    private boolean isVideoFile(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return VIDEO_EXTENSIONS.stream().anyMatch(name::endsWith);
    }
}
