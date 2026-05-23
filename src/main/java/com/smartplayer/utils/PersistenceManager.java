package com.smartplayer.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.smartplayer.models.PlaybackState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages persistence of playback state to a JSON file.
 * Auto-saves periodically during playback and on application close.
 * State is stored in ~/.smartplayer/state.json
 */
public class PersistenceManager {

    private static final String APP_DIR = ".smartplayer";
    private static final String STATE_FILE = "state.json";

    private final Path stateFilePath;
    private final Gson gson;
    private PlaybackState state;
    private ScheduledExecutorService autoSaveExecutor;

    public PersistenceManager() {
        // Resolve state file path: ~/.smartplayer/state.json
        Path homeDir = Path.of(System.getProperty("user.home"));
        Path appDir = homeDir.resolve(APP_DIR);
        this.stateFilePath = appDir.resolve(STATE_FILE);
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        // Ensure app directory exists
        try {
            Files.createDirectories(appDir);
        } catch (IOException e) {
            System.err.println("Failed to create app directory: " + e.getMessage());
        }

        // Load existing state or create new
        this.state = load();
    }

    /**
     * Loads playback state from disk.
     * Returns a new PlaybackState if the file doesn't exist or is corrupted.
     */
    private PlaybackState load() {
        try {
            if (Files.exists(stateFilePath)) {
                String json = Files.readString(stateFilePath);
                PlaybackState loaded = gson.fromJson(json, PlaybackState.class);
                if (loaded != null) {
                    System.out.println("Loaded playback state from: " + stateFilePath);
                    return loaded;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load playback state: " + e.getMessage());
        }
        return new PlaybackState();
    }

    /**
     * Saves the current playback state to disk.
     */
    public synchronized void save() {
        try {
            String json = gson.toJson(state);
            Files.writeString(stateFilePath, json);
        } catch (IOException e) {
            System.err.println("Failed to save playback state: " + e.getMessage());
        }
    }

    /**
     * Starts auto-saving every 5 seconds.
     * Call this when playback starts.
     */
    public void startAutoSave() {
        stopAutoSave(); // Stop any existing auto-save
        autoSaveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AutoSave-Thread");
            t.setDaemon(true);
            return t;
        });
        autoSaveExecutor.scheduleAtFixedRate(this::save, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * Stops auto-saving.
     */
    public void stopAutoSave() {
        if (autoSaveExecutor != null && !autoSaveExecutor.isShutdown()) {
            autoSaveExecutor.shutdown();
            try {
                autoSaveExecutor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                autoSaveExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Returns the current playback state object.
     */
    public PlaybackState getState() {
        return state;
    }

    /**
     * Performs cleanup: stops auto-save and performs a final save.
     */
    public void shutdown() {
        stopAutoSave();
        save();
    }
}
