package com.smartplayer.ui;

import com.smartplayer.models.Playlist;
import com.smartplayer.models.VideoFile;
import com.smartplayer.player.MediaPlayerManager;
import com.smartplayer.utils.TimeFormatter;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Bottom control bar with media controls styled like YouTube/VLC.
 * Contains: seek bar, time display, play/pause, prev/next, skip,
 * volume, speed, brightness, subtitle toggle, and fullscreen.
 * Fully responsive: collapses secondary controls when resized.
 */
public class ControlBarView extends VBox {

    // Controls
    private Slider seekSlider;
    private Label currentTimeLabel;
    private Label totalTimeLabel;
    private Label remainingTimeLabel;
    private Button playPauseBtn;
    private Button stopBtn;
    private Button previousBtn;
    private Button nextBtn;
    private Button skipBackBtn;
    private Button skipForwardBtn;
    private Button muteBtn;
    private Slider volumeSlider;
    private ComboBox<String> speedCombo;
    private Slider brightnessSlider;
    private Button subtitleBtn;
    private Button fullscreenBtn;
    private Label brightnessIcon;

    // Layout Groups
    private HBox timeDisplay;
    private HBox brightnessGroup;
    private HBox volumeGroup;

    // State
    private boolean isSeeking = false;

    // Interaction callback for overlay auto-hide reset
    private Runnable onInteraction;

    public ControlBarView() {
        getStyleClass().add("control-bar");
        setPadding(new Insets(0, 16, 12, 16));
        setSpacing(6);
        buildUI();
        setupResponsiveBehavior();
    }

    /**
     * Sets a callback that is invoked on any user interaction (slider drag, button click).
     * Used by PlayerView to reset the inactivity timer.
     */
    public void setOnInteraction(Runnable onInteraction) {
        this.onInteraction = onInteraction;
    }

    private void fireInteraction() {
        if (onInteraction != null) {
            onInteraction.run();
        }
    }

    private void buildUI() {
        // --- Seek bar row ---
        seekSlider = new Slider(0, 100, 0);
        seekSlider.getStyleClass().add("seek-slider");
        seekSlider.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(seekSlider, Priority.ALWAYS);

        HBox seekRow = new HBox(seekSlider);
        seekRow.setAlignment(Pos.CENTER);
        seekRow.setPadding(new Insets(2, 0, 0, 0));

        // --- Controls row ---
        HBox controlsRow = new HBox();
        controlsRow.setAlignment(Pos.CENTER);
        controlsRow.setSpacing(4);

        // Left controls: prev, skip back, play/pause, skip fwd, next, stop
        previousBtn = createIconButton("⏮", "Previous (P)", "icon-btn");
        skipBackBtn = createIconButton("⏪", "Back 10s (←)", "icon-btn");
        playPauseBtn = createIconButton("▶", "Play/Pause (Space)", "icon-btn", "play-btn");
        skipForwardBtn = createIconButton("⏩", "Forward 10s (→)", "icon-btn");
        nextBtn = createIconButton("⏭", "Next (N)", "icon-btn");
        stopBtn = createIconButton("⏹", "Stop", "icon-btn");

        HBox leftControls = new HBox(4, previousBtn, skipBackBtn, playPauseBtn, skipForwardBtn, nextBtn, stopBtn);
        leftControls.setAlignment(Pos.CENTER_LEFT);

        // Center: time display
        currentTimeLabel = new Label("0:00");
        currentTimeLabel.getStyleClass().add("time-label");

        Label timeSep = new Label(" / ");
        timeSep.getStyleClass().addAll("time-label", "time-separator");

        totalTimeLabel = new Label("0:00");
        totalTimeLabel.getStyleClass().add("time-label");

        remainingTimeLabel = new Label("");
        remainingTimeLabel.getStyleClass().add("time-label");
        remainingTimeLabel.setStyle("-fx-text-fill: #666680;");
        remainingTimeLabel.setPadding(new Insets(0, 0, 0, 8));

        timeDisplay = new HBox(currentTimeLabel, timeSep, totalTimeLabel, remainingTimeLabel);
        timeDisplay.setAlignment(Pos.CENTER);
        timeDisplay.setPadding(new Insets(0, 12, 0, 12));

        // Right controls: volume, speed, brightness, subtitles, fullscreen
        muteBtn = createIconButton("🔊", "Mute (M)", "icon-btn");

        volumeSlider = new Slider(0, 100, 80);
        volumeSlider.getStyleClass().add("volume-slider");
        volumeSlider.setPrefWidth(90);

        volumeGroup = new HBox(4, muteBtn, volumeSlider);
        volumeGroup.setAlignment(Pos.CENTER);

        // Speed selector
        speedCombo = new ComboBox<>();
        speedCombo.getItems().addAll("0.25x", "0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x");
        speedCombo.setValue("1x");
        speedCombo.getStyleClass().add("speed-combo");
        speedCombo.setTooltip(new Tooltip("Playback Speed"));

        // Brightness
        brightnessIcon = new Label("☀");
        brightnessIcon.getStyleClass().add("icon-btn");
        brightnessIcon.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 14px; -fx-padding: 4;");

        brightnessSlider = new Slider(0.0, 2.0, 1.0);
        brightnessSlider.getStyleClass().add("brightness-slider");
        brightnessSlider.setPrefWidth(70);
        brightnessSlider.setTooltip(new Tooltip("Brightness"));

        brightnessGroup = new HBox(2, brightnessIcon, brightnessSlider);
        brightnessGroup.setAlignment(Pos.CENTER);

        // Subtitle toggle
        subtitleBtn = createIconButton("CC", "Subtitles (S)", "icon-btn");
        subtitleBtn.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        subtitleBtn.getStyleClass().add("subtitle-off");

        // Fullscreen
        fullscreenBtn = createIconButton("⛶", "Fullscreen (F)", "icon-btn");

        // Separator regions for spacing
        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        HBox rightControls = new HBox(8, volumeGroup, speedCombo, brightnessGroup, subtitleBtn, fullscreenBtn);
        rightControls.setAlignment(Pos.CENTER_RIGHT);

        controlsRow.getChildren().addAll(leftControls, leftSpacer, timeDisplay, rightSpacer, rightControls);

        getChildren().addAll(seekRow, controlsRow);
    }
    
    private void setupResponsiveBehavior() {
        widthProperty().addListener((obs, oldVal, newVal) -> {
            double width = newVal.doubleValue();
            
            // Hide brightness on medium screens
            boolean showBrightness = width > 750;
            brightnessGroup.setVisible(showBrightness);
            brightnessGroup.setManaged(showBrightness);
            
            // Hide speed combo and remaining time on small screens
            boolean showSpeed = width > 600;
            speedCombo.setVisible(showSpeed);
            speedCombo.setManaged(showSpeed);
            remainingTimeLabel.setVisible(showSpeed);
            remainingTimeLabel.setManaged(showSpeed);
            
            // Hide time display on very small screens
            boolean showTime = width > 500;
            timeDisplay.setVisible(showTime);
            timeDisplay.setManaged(showTime);
            
            // Shrink volume slider
            if (width < 550) {
                volumeSlider.setPrefWidth(50);
            } else {
                volumeSlider.setPrefWidth(90);
            }
        });
    }

    private Button createIconButton(String icon, String tooltip, String... styleClasses) {
        Button btn = new Button(icon);
        btn.getStyleClass().addAll(styleClasses);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setFocusTraversable(false);
        return btn;
    }

    /**
     * Binds this control bar to the MediaPlayerManager for reactive updates.
     */
    public void bindToPlayer(MediaPlayerManager player, Playlist playlist) {
        // Time updates
        player.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
            if (!isSeeking) {
                long current = newVal.longValue();
                long total = player.totalDurationProperty().get();
                currentTimeLabel.setText(TimeFormatter.format(current));
                if (total > 0) {
                    seekSlider.setValue((double) current / total * 100);
                    remainingTimeLabel.setText(TimeFormatter.formatRemaining(current, total));
                }
            }
        });

        player.totalDurationProperty().addListener((obs, oldVal, newVal) -> {
            totalTimeLabel.setText(TimeFormatter.format(newVal.longValue()));
        });

        // Play/Pause button state
        player.playingProperty().addListener((obs, oldVal, isPlaying) -> {
            playPauseBtn.setText(isPlaying ? "⏸" : "▶");
        });

        // Mute button state
        player.mutedProperty().addListener((obs, oldVal, isMuted) -> {
            muteBtn.setText(isMuted ? "🔇" : "🔊");
        });

        // Seek bar interaction
        seekSlider.setOnMousePressed(e -> { isSeeking = true; fireInteraction(); });
        seekSlider.setOnMouseReleased(e -> {
            isSeeking = false;
            fireInteraction();
            long total = player.totalDurationProperty().get();
            if (total > 0) {
                long seekTo = (long) (seekSlider.getValue() / 100 * total);
                player.seek(seekTo);
            }
        });

        seekSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            isSeeking = isChanging;
        });

        // Button actions
        playPauseBtn.setOnAction(e -> { player.togglePlayPause(); fireInteraction(); });
        stopBtn.setOnAction(e -> { player.stop(); fireInteraction(); });
        skipBackBtn.setOnAction(e -> { player.skip(-10000); fireInteraction(); });
        skipForwardBtn.setOnAction(e -> { player.skip(10000); fireInteraction(); });
        muteBtn.setOnAction(e -> { player.toggleMute(); fireInteraction(); });

        previousBtn.setOnAction(e -> {
            VideoFile prev = playlist.previous();
            // Handled by app controller
        });

        nextBtn.setOnAction(e -> {
            VideoFile next = playlist.next();
            // Handled by app controller
        });

        // Volume slider
        volumeSlider.setValue(player.volumeProperty().get());
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            player.setVolume(newVal.intValue());
            fireInteraction();
        });

        // Speed combo
        speedCombo.setOnAction(e -> {
            String selected = speedCombo.getValue();
            float rate = Float.parseFloat(selected.replace("x", ""));
            player.setRate(rate);
            fireInteraction();
        });

        // Brightness slider
        brightnessSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            player.setBrightness(newVal.floatValue());
            fireInteraction();
        });
    }

    // --- Accessors for external wiring ---

    public Button getPlayPauseBtn() { return playPauseBtn; }
    public Button getStopBtn() { return stopBtn; }
    public Button getPreviousBtn() { return previousBtn; }
    public Button getNextBtn() { return nextBtn; }
    public Button getSkipBackBtn() { return skipBackBtn; }
    public Button getSkipForwardBtn() { return skipForwardBtn; }
    public Button getMuteBtn() { return muteBtn; }
    public Slider getVolumeSlider() { return volumeSlider; }
    public ComboBox<String> getSpeedCombo() { return speedCombo; }
    public Slider getBrightnessSlider() { return brightnessSlider; }
    public Button getSubtitleBtn() { return subtitleBtn; }
    public Button getFullscreenBtn() { return fullscreenBtn; }
    public Slider getSeekSlider() { return seekSlider; }
}
