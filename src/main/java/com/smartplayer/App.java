package com.smartplayer;

import com.smartplayer.models.PlaybackState;
import com.smartplayer.models.Playlist;
import com.smartplayer.models.VideoFile;
import com.smartplayer.player.MediaPlayerManager;
import com.smartplayer.player.SubtitleManager;
import com.smartplayer.scanner.FolderScanner;
import com.smartplayer.ui.*;
import com.smartplayer.utils.KeyboardShortcuts;
import com.smartplayer.utils.PersistenceManager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

/**
 * SmartPlayer — Main Application
 *
 * A modern, dark-themed desktop video player built with JavaFX and VLCJ.
 * Features recursive folder scanning, smart video sorting, auto-next playback,
 * resume functionality, subtitle support, and keyboard shortcuts.
 */
public class App extends Application {

    // Core components
    private MediaPlayerManager playerManager;
    private SubtitleManager subtitleManager;
    private PersistenceManager persistenceManager;
    private FolderScanner folderScanner;
    private Playlist playlist;
    private KeyboardShortcuts shortcuts;

    // UI
    private MainView mainView;
    private FullscreenHandler fullscreenHandler;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        // Initialize core components
        initializeComponents();

        // Build the UI
        mainView = new MainView();

        // Initialize the media player with the video surface
        playerManager.initialize(mainView.getPlayerView().getVideoImageView());

        // Bind control bar to player
        mainView.getControlBarView().bindToPlayer(playerManager, playlist);

        // Setup fullscreen handler (simplified — PlayerView manages overlay behavior)
        fullscreenHandler = new FullscreenHandler(
                stage,
                mainView,
                mainView.getPlayerView()
        );

        // Wire up callbacks
        wireCallbacks();

        // Create the scene
        Scene scene = new Scene(mainView, 1280, 720);

        // Load the dark theme CSS
        String cssPath = getClass().getResource("/com/smartplayer/styles/dark-theme.css") != null
                ? getClass().getResource("/com/smartplayer/styles/dark-theme.css").toExternalForm()
                : null;
        if (cssPath != null) {
            scene.getStylesheets().add(cssPath);
        }

        // Register keyboard shortcuts
        shortcuts.register(scene);

        // Reset overlay inactivity timer on any keyboard input
        scene.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            mainView.getPlayerView().resetInactivityTimer();
        });

        // Center play/pause icon animation on play/pause toggle
        playerManager.playingProperty().addListener((obs, wasPlaying, isPlaying) -> {
            mainView.getPlayerView().showCenterIcon(isPlaying ? "▶" : "⏸");
        });

        // Configure stage
        stage.setTitle("SmartPlayer — Intelligent Media Player");
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/app.png")));
        } catch (Exception e) {
            System.err.println("Could not load application icon.");
        }
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(500);
        stage.setFullScreenExitHint("Press F or ESC to exit fullscreen");

        // Handle window close
        stage.setOnCloseRequest(e -> shutdown());

        // Load persisted state
        loadPersistedState();

        // Show the stage
        stage.show();

        System.out.println("SmartPlayer started successfully!");
    }

    /**
     * Initializes all core (non-UI) components.
     */
    private void initializeComponents() {
        playerManager = new MediaPlayerManager();
        subtitleManager = new SubtitleManager(playerManager);
        persistenceManager = new PersistenceManager();
        folderScanner = new FolderScanner();
        playlist = new Playlist();
        shortcuts = new KeyboardShortcuts();
    }

    /**
     * Wires all UI callbacks to the core logic.
     */
    private void wireCallbacks() {
        SidebarView sidebar = mainView.getSidebarView();
        ControlBarView controls = mainView.getControlBarView();

        // --- Folder selection ---
        sidebar.setOnFolderSelected(this::onFolderSelected);
        mainView.setOnFolderDropped(this::onFolderSelected);

        // --- Video selection from playlist ---
        sidebar.setOnVideoSelected(video -> {
            int idx = playlist.selectVideo(video);
            if (idx >= 0) {
                playVideo(video);
            }
        });

        // --- Recently played ---
        sidebar.setOnRecentSelected(path -> {
            File file = new File(path);
            if (file.exists()) {
                // Find in current playlist or create single-video playlist
                VideoFile found = playlist.getVideos().stream()
                        .filter(v -> v.getFilePath().equals(path))
                        .findFirst()
                        .orElse(null);

                if (found != null) {
                    playlist.selectVideo(found);
                    playVideo(found);
                } else {
                    VideoFile vf = new VideoFile(path);
                    vf.detectSubtitle();
                    playlist.setVideos(List.of(vf));
                    playlist.selectVideo(0);
                    sidebar.updatePlaylist(playlist);
                    playVideo(vf);
                }
            }
        });

        // --- Auto-next playback ---
        playerManager.setOnFinished(() -> {
            saveCurrentPosition(); // Clear position for finished video
            VideoFile current = playlist.getCurrentVideo();
            if (current != null) {
                persistenceManager.getState().clearPosition(current.getFilePath());
                current.setWatched(true);
            }

            if (playlist.hasNext()) {
                VideoFile next = playlist.next();
                if (next != null) {
                    playVideo(next);
                }
            }
        });

        playerManager.setOnError(() -> {
            System.err.println("Playback error. Trying next video...");
            if (playlist.hasNext()) {
                VideoFile next = playlist.next();
                if (next != null) {
                    playVideo(next);
                }
            }
        });

        // --- Navigation buttons ---
        controls.getPreviousBtn().setOnAction(e -> {
            saveCurrentPosition();
            VideoFile prev = playlist.previous();
            if (prev != null) {
                playVideo(prev);
            }
        });

        controls.getNextBtn().setOnAction(e -> {
            saveCurrentPosition();
            VideoFile next = playlist.next();
            if (next != null) {
                playVideo(next);
            }
        });

        // --- Subtitle toggle ---
        controls.getSubtitleBtn().setOnAction(e -> {
            boolean enabled = subtitleManager.toggle();
            controls.getSubtitleBtn().getStyleClass().removeAll("subtitle-on", "subtitle-off");
            controls.getSubtitleBtn().getStyleClass().add(enabled ? "subtitle-on" : "subtitle-off");
        });

        // --- Fullscreen ---
        controls.getFullscreenBtn().setOnAction(e -> fullscreenHandler.toggle());

        // --- Keyboard shortcuts ---
        shortcuts.setOnPlayPause(() -> playerManager.togglePlayPause());
        shortcuts.setOnSkipForward(() -> playerManager.skip(10000));
        shortcuts.setOnSkipBackward(() -> playerManager.skip(-10000));
        shortcuts.setOnVolumeUp(() -> {
            int vol = Math.min(100, playerManager.volumeProperty().get() + 5);
            playerManager.setVolume(vol);
            controls.getVolumeSlider().setValue(vol);
        });
        shortcuts.setOnVolumeDown(() -> {
            int vol = Math.max(0, playerManager.volumeProperty().get() - 5);
            playerManager.setVolume(vol);
            controls.getVolumeSlider().setValue(vol);
        });
        shortcuts.setOnToggleFullscreen(() -> fullscreenHandler.toggle());
        shortcuts.setOnToggleMute(() -> playerManager.toggleMute());
        shortcuts.setOnToggleSubtitles(() -> {
            boolean enabled = subtitleManager.toggle();
            controls.getSubtitleBtn().getStyleClass().removeAll("subtitle-on", "subtitle-off");
            controls.getSubtitleBtn().getStyleClass().add(enabled ? "subtitle-on" : "subtitle-off");
        });
        shortcuts.setOnNextVideo(() -> {
            saveCurrentPosition();
            VideoFile next = playlist.next();
            if (next != null) playVideo(next);
        });
        shortcuts.setOnPreviousVideo(() -> {
            saveCurrentPosition();
            VideoFile prev = playlist.previous();
            if (prev != null) playVideo(prev);
        });
        shortcuts.setOnToggleSidebar(() -> mainView.toggleSidebar());
    }

    /**
     * Handles folder selection — scans for videos in background.
     */
    private void onFolderSelected(File folder) {
        persistenceManager.getState().setLastOpenedFolder(folder.getAbsolutePath());

        Task<List<VideoFile>> scanTask = folderScanner.scanAsync(folder.getAbsolutePath());

        scanTask.setOnSucceeded(e -> {
            List<VideoFile> videos = scanTask.getValue();
            playlist.setVideos(videos);
            mainView.getSidebarView().updatePlaylist(playlist);

            if (!videos.isEmpty()) {
                System.out.println("Found " + videos.size() + " videos in: " + folder.getAbsolutePath());
            }
        });

        scanTask.setOnFailed(e -> {
            System.err.println("Scan failed: " + scanTask.getException().getMessage());
        });

        Thread scanThread = new Thread(scanTask);
        scanThread.setDaemon(true);
        scanThread.start();
    }

    /**
     * Plays a specific video file with resume support.
     */
    private void playVideo(VideoFile video) {
        // Get resume position
        long resumePos = persistenceManager.getState().getPosition(video.getFilePath());

        // Play the video
        playerManager.play(video, resumePos);

        // Load subtitles
        subtitleManager.loadForVideo(video);

        // Update UI
        mainView.updateNowPlaying(video, playlist.getCurrentIndex(), playlist.size());

        // Update recently played
        persistenceManager.getState().addToRecentlyPlayed(video.getFilePath());
        mainView.getSidebarView().updateRecentlyPlayed(
                persistenceManager.getState().getRecentlyPlayed()
        );

        // Start auto-save
        persistenceManager.startAutoSave();

        // Periodically save position
        playerManager.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
            VideoFile current = playlist.getCurrentVideo();
            if (current != null) {
                persistenceManager.getState().savePosition(
                        current.getFilePath(), newVal.longValue()
                );
            }
        });

        System.out.println("Playing: " + video.getDisplayName() +
                (resumePos > 0 ? " (resuming from " + (resumePos / 1000) + "s)" : ""));
    }

    /**
     * Saves the current playback position.
     */
    private void saveCurrentPosition() {
        VideoFile current = playlist.getCurrentVideo();
        if (current != null) {
            long pos = playerManager.getCurrentTimeMs();
            if (pos > 0) {
                persistenceManager.getState().savePosition(current.getFilePath(), pos);
            }
        }
    }

    /**
     * Loads persisted state (last folder, volume, etc.) on startup.
     */
    private void loadPersistedState() {
        PlaybackState state = persistenceManager.getState();

        // Restore volume
        playerManager.setVolume(state.getVolume());
        mainView.getControlBarView().getVolumeSlider().setValue(state.getVolume());

        // Restore speed
        float speed = state.getPlaybackSpeed();
        playerManager.setRate(speed);
        mainView.getControlBarView().getSpeedCombo().setValue(speed + "x");

        // Show recently played
        mainView.getSidebarView().updateRecentlyPlayed(state.getRecentlyPlayed());

        // Restore last opened folder
        String lastFolder = state.getLastOpenedFolder();
        if (lastFolder != null && new File(lastFolder).isDirectory()) {
            onFolderSelected(new File(lastFolder));
        }
    }

    /**
     * Graceful shutdown: save state, release resources.
     */
    private void shutdown() {
        System.out.println("Shutting down SmartPlayer...");

        // Save current position
        saveCurrentPosition();

        // Save volume and speed
        persistenceManager.getState().setVolume(playerManager.volumeProperty().get());
        persistenceManager.getState().setPlaybackSpeed(playerManager.rateProperty().get());

        // Persist state
        persistenceManager.shutdown();

        // Release media player
        playerManager.dispose();

        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
