package com.smartplayer.ui;

import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Manages fullscreen mode transitions.
 * Delegates auto-hide control behavior to PlayerView's overlay system.
 * In fullscreen: cursor auto-hides, tighter inactivity timeout (2s).
 * In windowed: controls auto-hide with 3s timeout, cursor stays visible.
 */
public class FullscreenHandler {

    private final Stage stage;
    private final MainView mainView;
    private final PlayerView playerView;

    private boolean isFullscreen = false;

    /**
     * @param stage      The primary stage
     * @param mainView   The root MainView layout
     * @param playerView The PlayerView that manages overlay controls
     */
    public FullscreenHandler(Stage stage, MainView mainView, PlayerView playerView) {
        this.stage = stage;
        this.mainView = mainView;
        this.playerView = playerView;
    }

    /**
     * Toggles fullscreen mode on/off.
     */
    public void toggle() {
        if (isFullscreen) {
            exitFullscreen();
        } else {
            enterFullscreen();
        }
    }

    /**
     * Enters fullscreen mode.
     * Hides sidebar and enables fullscreen overlay behavior.
     */
    private void enterFullscreen() {
        isFullscreen = true;
        stage.setFullScreen(true);

        // Hide sidebar
        if (mainView.isSidebarVisible()) {
            mainView.toggleSidebar();
        }

        // Enable fullscreen mode on PlayerView (cursor auto-hide, 2s timeout)
        playerView.setFullscreenMode(true);
    }

    /**
     * Exits fullscreen mode.
     * Restores sidebar and windowed overlay behavior.
     */
    private void exitFullscreen() {
        isFullscreen = false;
        stage.setFullScreen(false);

        // Restore sidebar
        if (!mainView.isSidebarVisible()) {
            mainView.toggleSidebar();
        }

        // Disable fullscreen mode on PlayerView (no cursor hide, 3s timeout)
        playerView.setFullscreenMode(false);

        // Ensure cursor is visible
        Scene scene = stage.getScene();
        if (scene != null) {
            scene.setCursor(Cursor.DEFAULT);
        }
    }

    public boolean isFullscreen() {
        return isFullscreen;
    }
}
