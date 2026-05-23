package com.smartplayer.ui;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

/**
 * The player area displaying the VLCJ video surface via an ImageView.
 * Also serves as the overlay host for floating controls, gradients,
 * and center play/pause icon — providing YouTube/VLC-style auto-hide behavior.
 */
public class PlayerView extends StackPane {

    private final ImageView videoImageView;
    private final javafx.scene.control.ProgressIndicator loadingIndicator;

    // Overlay layers
    private Pane bottomGradient;
    private Pane topGradient;
    private VBox bottomOverlayContainer;
    private HBox topOverlayContainer;
    private Label centerPlayIcon;

    // Animation
    private FadeTransition fadeInTransition;
    private FadeTransition fadeOutTransition;
    private FadeTransition centerIconFadeOut;
    private ScaleTransition centerIconScale;
    private PauseTransition inactivityTimer;
    private PauseTransition centerIconDelay;

    // State
    private boolean controlsVisible = true;
    private boolean isHoveringControls = false;
    private boolean isFullscreenMode = false;
    private boolean cursorAutoHide = false;

    // Group node that holds all overlay elements for unified fade
    private StackPane overlayGroup;

    public PlayerView() {
        getStyleClass().add("player-area");

        // Force StackPane to allow shrinking down to 0
        setMinSize(0, 0);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Create the ImageView for VLCJ video rendering
        videoImageView = new ImageView();
        videoImageView.setPreserveRatio(true);
        videoImageView.setSmooth(true);
        videoImageView.setCache(true);

        // Bind ImageView size to the StackPane size
        videoImageView.fitWidthProperty().bind(widthProperty());
        videoImageView.fitHeightProperty().bind(heightProperty());

        // Center the video
        StackPane.setAlignment(videoImageView, Pos.CENTER);

        // Add clipping to prevent overflow during aggressive resizing
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        // Add a loading indicator
        loadingIndicator = new javafx.scene.control.ProgressIndicator();
        loadingIndicator.setMaxSize(50, 50);
        loadingIndicator.setVisible(false);
        StackPane.setAlignment(loadingIndicator, Pos.CENTER);

        // Center play/pause icon (hidden by default)
        centerPlayIcon = new Label("▶");
        centerPlayIcon.getStyleClass().add("center-play-icon");
        centerPlayIcon.setOpacity(0);
        centerPlayIcon.setMouseTransparent(true);
        StackPane.setAlignment(centerPlayIcon, Pos.CENTER);

        getChildren().addAll(videoImageView, loadingIndicator, centerPlayIcon);

        // Setup inactivity timer
        setupInactivityTimer();

        // Setup mouse tracking
        setupMouseTracking();
    }

    /**
     * Injects overlay controls (control bar + now-playing bar) into this StackPane.
     * Call this after construction, once the control bar and now-playing bar are created.
     */
    public void setupOverlayControls(ControlBarView controlBarView, HBox nowPlayingBar) {
        // --- Bottom gradient (cinematic) ---
        bottomGradient = new Pane();
        bottomGradient.getStyleClass().add("bottom-gradient");
        bottomGradient.setMouseTransparent(true);
        bottomGradient.setMaxHeight(180);
        bottomGradient.setPrefHeight(180);
        StackPane.setAlignment(bottomGradient, Pos.BOTTOM_CENTER);

        // --- Top gradient (cinematic) ---
        topGradient = new Pane();
        topGradient.getStyleClass().add("top-gradient");
        topGradient.setMouseTransparent(true);
        topGradient.setMaxHeight(100);
        topGradient.setPrefHeight(100);
        StackPane.setAlignment(topGradient, Pos.TOP_CENTER);

        // --- Bottom overlay container (holds the control bar) ---
        bottomOverlayContainer = new VBox();
        bottomOverlayContainer.getStyleClass().add("control-overlay-container");
        bottomOverlayContainer.getChildren().add(controlBarView);
        bottomOverlayContainer.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(bottomOverlayContainer, Pos.BOTTOM_CENTER);

        // Prevent auto-hide while hovering the control bar
        bottomOverlayContainer.setOnMouseEntered(e -> {
            isHoveringControls = true;
            inactivityTimer.stop();
        });
        bottomOverlayContainer.setOnMouseExited(e -> {
            isHoveringControls = false;
            resetInactivityTimer();
        });

        // Any interaction on the control bar resets the timer
        controlBarView.setOnInteraction(this::resetInactivityTimer);

        // --- Top overlay container (holds the now-playing bar) ---
        // Re-style the now-playing bar for overlay mode
        nowPlayingBar.getStyleClass().remove("now-playing-bar");
        nowPlayingBar.getStyleClass().add("now-playing-overlay");

        topOverlayContainer = nowPlayingBar;
        topOverlayContainer.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(topOverlayContainer, Pos.TOP_CENTER);

        // Prevent auto-hide while hovering the now-playing bar
        topOverlayContainer.setOnMouseEntered(e -> {
            isHoveringControls = true;
            inactivityTimer.stop();
        });
        topOverlayContainer.setOnMouseExited(e -> {
            isHoveringControls = false;
            resetInactivityTimer();
        });

        // --- Overlay group for unified fade ---
        overlayGroup = new StackPane();
        overlayGroup.setPickOnBounds(false);
        overlayGroup.getChildren().addAll(bottomGradient, topGradient, bottomOverlayContainer, topOverlayContainer);

        // Re-apply alignments inside overlay group
        StackPane.setAlignment(bottomGradient, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(topGradient, Pos.TOP_CENTER);
        StackPane.setAlignment(bottomOverlayContainer, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(topOverlayContainer, Pos.TOP_CENTER);

        // Make gradients fill width
        bottomGradient.maxWidthProperty().bind(widthProperty());
        topGradient.maxWidthProperty().bind(widthProperty());

        // Add overlay group to the StackPane (above video, below center icon)
        // Order: videoImageView, loadingIndicator, centerPlayIcon already added
        // Insert overlay group before the center play icon
        int centerIconIndex = getChildren().indexOf(centerPlayIcon);
        getChildren().add(centerIconIndex, overlayGroup);

        // Setup fade animations
        setupFadeAnimations();

        // Start the inactivity timer
        resetInactivityTimer();
    }

    /**
     * Shows the center play/pause icon briefly with a scale+fade animation.
     */
    public void showCenterIcon(String icon) {
        centerPlayIcon.setText(icon);

        // Stop any running animation
        if (centerIconScale != null) centerIconScale.stop();
        if (centerIconFadeOut != null) centerIconFadeOut.stop();
        if (centerIconDelay != null) centerIconDelay.stop();

        // Reset state
        centerPlayIcon.setOpacity(0.85);
        centerPlayIcon.setScaleX(0.5);
        centerPlayIcon.setScaleY(0.5);

        // Scale up
        centerIconScale = new ScaleTransition(Duration.millis(350), centerPlayIcon);
        centerIconScale.setFromX(0.5);
        centerIconScale.setFromY(0.5);
        centerIconScale.setToX(1.3);
        centerIconScale.setToY(1.3);
        centerIconScale.setInterpolator(Interpolator.EASE_OUT);

        // Fade out after a brief hold
        centerIconFadeOut = new FadeTransition(Duration.millis(500), centerPlayIcon);
        centerIconFadeOut.setFromValue(0.85);
        centerIconFadeOut.setToValue(0.0);
        centerIconFadeOut.setInterpolator(Interpolator.EASE_IN);

        // Hold briefly, then fade out
        centerIconDelay = new PauseTransition(Duration.millis(150));

        SequentialTransition seq = new SequentialTransition(
            new ParallelTransition(centerIconScale),
            centerIconDelay,
            centerIconFadeOut
        );
        seq.play();
    }

    /**
     * Shows or hides the loading indicator.
     */
    public void setLoading(boolean isLoading) {
        loadingIndicator.setVisible(isLoading);
    }

    /**
     * Returns the ImageView used as the VLCJ video surface.
     */
    public ImageView getVideoImageView() {
        return videoImageView;
    }

    /**
     * Sets fullscreen mode — enables cursor auto-hide and tighter timers.
     */
    public void setFullscreenMode(boolean fullscreen) {
        this.isFullscreenMode = fullscreen;
        this.cursorAutoHide = fullscreen;

        if (fullscreen) {
            // Tighter timeout in fullscreen
            inactivityTimer.setDuration(Duration.seconds(2));
        } else {
            // Normal timeout in windowed mode
            inactivityTimer.setDuration(Duration.seconds(3));
            // Restore cursor
            setCursor(Cursor.DEFAULT);
            if (getScene() != null) {
                getScene().setCursor(Cursor.DEFAULT);
            }
        }

        // Show controls immediately when switching modes
        showControls();
        resetInactivityTimer();
    }

    /**
     * Resets the inactivity timer — called externally on keyboard events, etc.
     */
    public void resetInactivityTimer() {
        if (!controlsVisible) {
            showControls();
        }
        inactivityTimer.stop();
        inactivityTimer.playFromStart();
    }

    public boolean isFullscreenMode() {
        return isFullscreenMode;
    }

    // -------------------------------------------------------
    //  Private: Inactivity timer
    // -------------------------------------------------------

    private void setupInactivityTimer() {
        inactivityTimer = new PauseTransition(Duration.seconds(3));
        inactivityTimer.setOnFinished(e -> {
            if (!isHoveringControls) {
                hideControls();
            }
        });
    }

    // -------------------------------------------------------
    //  Private: Mouse tracking
    // -------------------------------------------------------

    private void setupMouseTracking() {
        setOnMouseMoved(e -> {
            resetInactivityTimer();
            restoreCursor();
        });

        setOnMouseEntered(e -> {
            showControls();
            resetInactivityTimer();
        });

        setOnMouseExited(e -> {
            if (!isHoveringControls) {
                // Speed up hide when mouse leaves
                inactivityTimer.stop();
                inactivityTimer.setDuration(Duration.millis(800));
                inactivityTimer.playFromStart();
                // Restore normal duration after this fires
                inactivityTimer.setOnFinished(ev -> {
                    if (!isHoveringControls) {
                        hideControls();
                    }
                    // Reset duration for next cycle
                    inactivityTimer.setDuration(
                        isFullscreenMode ? Duration.seconds(2) : Duration.seconds(3)
                    );
                    inactivityTimer.setOnFinished(ev2 -> {
                        if (!isHoveringControls) {
                            hideControls();
                        }
                    });
                });
            }
        });

        setOnMouseClicked(e -> {
            resetInactivityTimer();
        });
    }

    // -------------------------------------------------------
    //  Private: Fade animations
    // -------------------------------------------------------

    private void setupFadeAnimations() {
        // Fade in (fast)
        fadeInTransition = new FadeTransition(Duration.millis(250), overlayGroup);
        fadeInTransition.setToValue(1.0);
        fadeInTransition.setInterpolator(Interpolator.EASE_OUT);

        // Fade out (slightly slower for cinematic feel)
        fadeOutTransition = new FadeTransition(Duration.millis(400), overlayGroup);
        fadeOutTransition.setToValue(0.0);
        fadeOutTransition.setInterpolator(Interpolator.EASE_IN);
        fadeOutTransition.setOnFinished(e -> {
            overlayGroup.setMouseTransparent(true);
            controlsVisible = false;
            // Hide cursor in fullscreen
            if (cursorAutoHide) {
                hideCursor();
            }
        });
    }

    private void showControls() {
        if (overlayGroup == null) return;

        if (fadeOutTransition != null) fadeOutTransition.stop();

        overlayGroup.setMouseTransparent(false);
        controlsVisible = true;

        if (fadeInTransition != null) {
            fadeInTransition.setFromValue(overlayGroup.getOpacity());
            fadeInTransition.play();
        } else {
            overlayGroup.setOpacity(1.0);
        }

        restoreCursor();
    }

    private void hideControls() {
        if (overlayGroup == null) return;

        if (fadeInTransition != null) fadeInTransition.stop();

        if (fadeOutTransition != null) {
            fadeOutTransition.setFromValue(overlayGroup.getOpacity());
            fadeOutTransition.play();
        } else {
            overlayGroup.setOpacity(0.0);
            overlayGroup.setMouseTransparent(true);
            controlsVisible = false;
        }
    }

    private void hideCursor() {
        setCursor(Cursor.NONE);
        if (getScene() != null) {
            getScene().setCursor(Cursor.NONE);
        }
    }

    private void restoreCursor() {
        setCursor(Cursor.DEFAULT);
        if (getScene() != null) {
            getScene().setCursor(Cursor.DEFAULT);
        }
    }
}
