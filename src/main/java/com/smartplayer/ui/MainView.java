package com.smartplayer.ui;

import com.smartplayer.models.VideoFile;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.File;
import java.util.function.Consumer;

/**
 * Root layout assembling sidebar, player area, and control bar overlay
 * into a StackPane with a SplitPane. Controls are now floating overlays
 * inside the PlayerView instead of stacked VBox children.
 * Also handles drag-and-drop folder support.
 */
public class MainView extends StackPane {

    private final SidebarView sidebarView;
    private final PlayerView playerView;
    private final ControlBarView controlBarView;
    private final HBox nowPlayingBar;
    private final Label nowPlayingTitle;
    private final Label nowPlayingInfo;
    
    private final SplitPane splitPane;
    private final StackPane centerArea;

    // Drop overlay
    private StackPane dropOverlay;

    // Callback for drag-and-drop folder
    private Consumer<File> onFolderDropped;
    
    private boolean isSidebarVisible = true;
    private double lastDividerPosition = 0.3; // Default 30%

    public MainView() {
        // Create components
        sidebarView = new SidebarView();
        playerView = new PlayerView();
        controlBarView = new ControlBarView();

        // Now playing bar (will become a top overlay inside PlayerView)
        nowPlayingTitle = new Label("No video loaded");
        nowPlayingTitle.getStyleClass().add("now-playing-title");
        nowPlayingTitle.setMaxWidth(Double.MAX_VALUE);

        nowPlayingInfo = new Label("");
        nowPlayingInfo.getStyleClass().add("now-playing-subtitle");

        // Hamburger Menu Button
        javafx.scene.control.Button menuBtn = new javafx.scene.control.Button("☰");
        menuBtn.getStyleClass().add("menu-btn");
        menuBtn.setOnAction(e -> toggleSidebar());

        VBox nowPlayingText = new VBox(2, nowPlayingTitle, nowPlayingInfo);
        nowPlayingText.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nowPlayingText, Priority.ALWAYS);

        nowPlayingBar = new HBox(15, menuBtn, nowPlayingText);
        nowPlayingBar.getStyleClass().add("now-playing-bar");
        nowPlayingBar.setAlignment(Pos.CENTER_LEFT);

        // Inject overlay controls into PlayerView (floating overlays, not stacked)
        playerView.setupOverlayControls(controlBarView, nowPlayingBar);

        // Center area: just the PlayerView (which now hosts overlays internally)
        centerArea = new StackPane(playerView);
        centerArea.setMinWidth(400);

        // SplitPane layout
        splitPane = new SplitPane();
        splitPane.getItems().addAll(sidebarView, centerArea);
        splitPane.setDividerPositions(lastDividerPosition);
        SplitPane.setResizableWithParent(sidebarView, false);

        // Layout
        getChildren().add(splitPane);

        // Setup drag and drop
        setupDragAndDrop();
        
        // Listen to window size changes for responsive behavior
        widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() < 600 && isSidebarVisible) {
                toggleSidebar(); // Auto-collapse on small window
            } else if (newVal.doubleValue() > 800 && !isSidebarVisible && oldVal.doubleValue() < newVal.doubleValue()) {
                // Auto-expand on larger window (optional, can be disabled if user prefers manual)
            }
        });
    }

    /**
     * Toggles the sidebar visibility with a smooth animation and completely hides it.
     */
    public void toggleSidebar() {
        if (isSidebarVisible) {
            if (!splitPane.getDividers().isEmpty()) {
                lastDividerPosition = splitPane.getDividers().get(0).getPosition();
            }
            Timeline timeline = new Timeline();
            KeyValue kvMin = new KeyValue(sidebarView.minWidthProperty(), 0);
            KeyValue kvMax = new KeyValue(sidebarView.maxWidthProperty(), 0);
            KeyFrame kf = new KeyFrame(Duration.millis(250), kvMin, kvMax);
            timeline.getKeyFrames().add(kf);
            timeline.setOnFinished(e -> {
                splitPane.getItems().remove(sidebarView); // Completely remove
            });
            timeline.play();
        } else {
            sidebarView.setMinWidth(0);
            sidebarView.setMaxWidth(0);
            splitPane.getItems().add(0, sidebarView); // Re-add
            SplitPane.setResizableWithParent(sidebarView, false);

            Timeline timeline = new Timeline();
            KeyValue kvMin = new KeyValue(sidebarView.minWidthProperty(), 220);
            KeyValue kvMax = new KeyValue(sidebarView.maxWidthProperty(), 500);
            KeyFrame kf = new KeyFrame(Duration.millis(250), kvMin, kvMax);
            timeline.getKeyFrames().add(kf);
            timeline.setOnFinished(e -> {
                if (!splitPane.getDividers().isEmpty()) {
                    splitPane.setDividerPositions(lastDividerPosition > 0.05 ? lastDividerPosition : 0.3);
                }
            });
            timeline.play();
        }
        isSidebarVisible = !isSidebarVisible;
    }

    /**
     * Sets up drag-and-drop for folder input.
     */
    private void setupDragAndDrop() {
        // Create drop overlay (hidden by default)
        dropOverlay = new StackPane();
        dropOverlay.getStyleClass().add("drop-overlay");
        dropOverlay.setVisible(false);
        dropOverlay.setMouseTransparent(true);

        Label dropLabel = new Label("📂 Drop folder here");
        dropLabel.getStyleClass().add("drop-overlay-text");
        dropOverlay.getChildren().add(dropLabel);

        // Add the overlay on top of everything
        getChildren().add(dropOverlay);

        setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                File file = event.getDragboard().getFiles().get(0);
                if (file.isDirectory()) {
                    event.acceptTransferModes(TransferMode.COPY);
                    dropOverlay.setVisible(true);
                }
            }
            event.consume();
        });

        setOnDragExited(event -> {
            dropOverlay.setVisible(false);
            event.consume();
        });

        setOnDragDropped(event -> {
            dropOverlay.setVisible(false);
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                if (file.isDirectory() && onFolderDropped != null) {
                    onFolderDropped.accept(file);
                }
            }
            event.setDropCompleted(true);
            event.consume();
        });
    }

    /**
     * Updates the now-playing bar with current video information.
     */
    public void updateNowPlaying(VideoFile video, int index, int total) {
        if (video != null) {
            nowPlayingTitle.setText("▶ " + video.getDisplayName());
            nowPlayingInfo.setText(String.format("Playing %d of %d", index + 1, total));
        } else {
            nowPlayingTitle.setText("No video loaded");
            nowPlayingInfo.setText("");
        }
    }

    // --- Accessors ---
    public boolean isSidebarVisible() { return isSidebarVisible; }

    public SidebarView getSidebarView() { return sidebarView; }
    public PlayerView getPlayerView() { return playerView; }
    public ControlBarView getControlBarView() { return controlBarView; }
    public HBox getNowPlayingBar() { return nowPlayingBar; }

    public void setOnFolderDropped(Consumer<File> callback) { this.onFolderDropped = callback; }
}
