package com.smartplayer.utils;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Registers global keyboard shortcuts on the application scene.
 * All shortcuts are handled on KEY_PRESSED events.
 *
 * Shortcut map:
 *   Space       → Play/Pause
 *   Left Arrow  → Skip backward 10s
 *   Right Arrow → Skip forward 10s
 *   Up Arrow    → Volume up 5%
 *   Down Arrow  → Volume down 5%
 *   F           → Toggle fullscreen
 *   M           → Toggle mute
 *   S           → Toggle subtitles
 *   N           → Next video
 *   P           → Previous video
 *   TAB         → Toggle Sidebar
 *   Escape      → Exit fullscreen (handled by JavaFX)
 */
public class KeyboardShortcuts {

    // Callback interfaces
    private Runnable onPlayPause;
    private Runnable onSkipForward;
    private Runnable onSkipBackward;
    private Runnable onVolumeUp;
    private Runnable onVolumeDown;
    private Runnable onToggleFullscreen;
    private Runnable onToggleMute;
    private Runnable onToggleSubtitles;
    private Runnable onNextVideo;
    private Runnable onPreviousVideo;
    private Runnable onToggleSidebar;

    /**
     * Registers all keyboard shortcuts on the given scene.
     * Should be called after the scene is fully initialized.
     *
     * @param scene The main application scene
     */
    public void register(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // Don't capture if a text input is focused
            if (event.getTarget() instanceof javafx.scene.control.TextInputControl) {
                return;
            }

            switch (event.getCode()) {
                case SPACE -> {
                    fire(onPlayPause);
                    event.consume();
                }
                case LEFT -> {
                    fire(onSkipBackward);
                    event.consume();
                }
                case RIGHT -> {
                    fire(onSkipForward);
                    event.consume();
                }
                case UP -> {
                    fire(onVolumeUp);
                    event.consume();
                }
                case DOWN -> {
                    fire(onVolumeDown);
                    event.consume();
                }
                case F -> {
                    fire(onToggleFullscreen);
                    event.consume();
                }
                case M -> {
                    fire(onToggleMute);
                    event.consume();
                }
                case S -> {
                    fire(onToggleSubtitles);
                    event.consume();
                }
                case N -> {
                    fire(onNextVideo);
                    event.consume();
                }
                case P -> {
                    fire(onPreviousVideo);
                    event.consume();
                }
                case TAB -> {
                    fire(onToggleSidebar);
                    event.consume();
                }
                default -> {} // No action for other keys
            }
        });
    }

    private void fire(Runnable callback) {
        if (callback != null) {
            callback.run();
        }
    }

    // --- Setter methods for callbacks ---

    public void setOnPlayPause(Runnable onPlayPause) { this.onPlayPause = onPlayPause; }
    public void setOnSkipForward(Runnable onSkipForward) { this.onSkipForward = onSkipForward; }
    public void setOnSkipBackward(Runnable onSkipBackward) { this.onSkipBackward = onSkipBackward; }
    public void setOnVolumeUp(Runnable onVolumeUp) { this.onVolumeUp = onVolumeUp; }
    public void setOnVolumeDown(Runnable onVolumeDown) { this.onVolumeDown = onVolumeDown; }
    public void setOnToggleFullscreen(Runnable onToggleFullscreen) { this.onToggleFullscreen = onToggleFullscreen; }
    public void setOnToggleMute(Runnable onToggleMute) { this.onToggleMute = onToggleMute; }
    public void setOnToggleSubtitles(Runnable onToggleSubtitles) { this.onToggleSubtitles = onToggleSubtitles; }
    public void setOnNextVideo(Runnable onNextVideo) { this.onNextVideo = onNextVideo; }
    public void setOnPreviousVideo(Runnable onPreviousVideo) { this.onPreviousVideo = onPreviousVideo; }
    public void setOnToggleSidebar(Runnable onToggleSidebar) { this.onToggleSidebar = onToggleSidebar; }
}
