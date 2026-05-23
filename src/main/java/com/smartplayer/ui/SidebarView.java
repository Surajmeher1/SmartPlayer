package com.smartplayer.ui;

import com.smartplayer.models.Playlist;
import com.smartplayer.models.VideoFile;

import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.scene.control.OverrunStyle;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Left sidebar with folder selector, playlist view, and recently played section.
 * Features a modern dark design with custom playlist cells.
 */
public class SidebarView extends VBox {

    private Button openFolderBtn;
    private Label folderPathLabel;
    private TextField searchField;
    private ListView<VideoFile> playlistListView;
    private VBox recentlyPlayedBox;
    private Label videoCountLabel;

    // Callbacks
    private Consumer<File> onFolderSelected;
    private Consumer<VideoFile> onVideoSelected;
    private Consumer<String> onRecentSelected;

    private Playlist currentPlaylist;
    private FilteredList<VideoFile> filteredPlaylist;

    public SidebarView() {
        getStyleClass().add("sidebar");
        setMinWidth(220);
        setPrefWidth(320);
        setMaxWidth(500);
        buildUI();
    }

    private void buildUI() {
        // --- Header ---
        VBox header = new VBox(4);
        header.getStyleClass().add("sidebar-header");

        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("⚡ SmartPlayer");
        titleLabel.getStyleClass().add("sidebar-title");
        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        
        titleRow.getChildren().addAll(titleLabel, titleSpacer);

        Label subtitleLabel = new Label("Intelligent Media Player");
        subtitleLabel.getStyleClass().add("sidebar-subtitle");

        header.getChildren().addAll(titleRow, subtitleLabel);

        // --- Open Folder Button ---
        openFolderBtn = new Button("📂 Open Folder");
        openFolderBtn.getStyleClass().add("open-folder-btn");
        openFolderBtn.setMaxWidth(Double.MAX_VALUE);
        openFolderBtn.setOnAction(e -> openFolderDialog());

        VBox folderSection = new VBox(8);
        folderSection.setPadding(new Insets(16));

        folderPathLabel = new Label("No folder selected");
        folderPathLabel.getStyleClass().add("folder-path");
        folderPathLabel.setWrapText(true);

        folderSection.getChildren().addAll(openFolderBtn, folderPathLabel);

        // --- Playlist Section ---
        Label playlistHeader = new Label("PLAYLIST");
        playlistHeader.getStyleClass().add("section-header");

        videoCountLabel = new Label("0 videos");
        videoCountLabel.getStyleClass().add("section-header");
        videoCountLabel.setStyle("-fx-text-fill: #555568; -fx-font-weight: normal;");

        HBox playlistHeaderRow = new HBox();
        playlistHeaderRow.setAlignment(Pos.CENTER_LEFT);
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        playlistHeaderRow.getChildren().addAll(playlistHeader, headerSpacer, videoCountLabel);
        playlistHeaderRow.setPadding(new Insets(0, 16, 0, 0));

        // Search bar
        searchField = new TextField();
        searchField.setPromptText("🔍 Search playlist...");
        searchField.getStyleClass().add("search-field");
        VBox.setMargin(searchField, new Insets(0, 16, 8, 16));
        
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (filteredPlaylist != null) {
                filteredPlaylist.setPredicate(video -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    return video.getDisplayName().toLowerCase().contains(newVal.toLowerCase());
                });
            }
        });

        playlistListView = new ListView<>();
        playlistListView.getStyleClass().add("playlist-view");
        playlistListView.setPlaceholder(createEmptyPlaceholder());
        VBox.setVgrow(playlistListView, Priority.ALWAYS);

        // Custom cell factory
        playlistListView.setCellFactory(lv -> new PlaylistCell());

        // Click handler
        playlistListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                VideoFile selected = playlistListView.getSelectionModel().getSelectedItem();
                if (selected != null && onVideoSelected != null) {
                    onVideoSelected.accept(selected);
                }
            }
        });

        // --- Recently Played Section ---
        Label recentHeader = new Label("RECENTLY PLAYED");
        recentHeader.getStyleClass().add("section-header");

        recentlyPlayedBox = new VBox();
        recentlyPlayedBox.setMaxHeight(200);

        // --- Assemble ---
        getChildren().addAll(header, folderSection, new Separator(), playlistHeaderRow, searchField, playlistListView,
                new Separator(), recentHeader, recentlyPlayedBox);
    }

    /**
     * Opens the system directory chooser dialog.
     */
    private void openFolderDialog() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Video Folder");
        Window window = getScene() != null ? getScene().getWindow() : null;
        File dir = chooser.showDialog(window);
        if (dir != null && onFolderSelected != null) {
            folderPathLabel.setText("📁 " + dir.getAbsolutePath());
            onFolderSelected.accept(dir);
        }
    }

    /**
     * Updates the playlist display with new videos.
     */
    public void updatePlaylist(Playlist playlist) {
        this.currentPlaylist = playlist;
        this.filteredPlaylist = new FilteredList<>(playlist.getVideos(), p -> true);
        
        // Re-apply current search text if any
        String search = searchField.getText();
        if (search != null && !search.isEmpty()) {
            filteredPlaylist.setPredicate(video -> video.getDisplayName().toLowerCase().contains(search.toLowerCase()));
        }
        
        playlistListView.setItems(filteredPlaylist);
        videoCountLabel.setText(playlist.size() + " videos");

        // Fade in animation
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), playlistListView);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        // Listen for current index changes to update highlighting
        playlist.currentIndexProperty().addListener((obs, oldIdx, newIdx) -> {
            playlistListView.refresh();
            int idx = newIdx.intValue();
            if (idx >= 0 && idx < playlist.size()) {
                // Find the item in filtered list and scroll to it
                VideoFile playing = playlist.getVideos().get(idx);
                int filteredIdx = filteredPlaylist.indexOf(playing);
                if (filteredIdx >= 0) {
                    playlistListView.scrollTo(filteredIdx);
                }
            }
        });
    }

    /**
     * Updates the recently played section.
     */
    public void updateRecentlyPlayed(List<String> recentPaths) {
        recentlyPlayedBox.getChildren().clear();
        int count = Math.min(recentPaths.size(), 10);
        for (int i = 0; i < count; i++) {
            String path = recentPaths.get(i);
            String name = new File(path).getName();
            Label item = new Label("📁 " + stripExtension(name));
            item.getStyleClass().add("recent-item");
            item.setMaxWidth(Double.MAX_VALUE);
            item.setOnMouseClicked(e -> {
                if (onRecentSelected != null) {
                    onRecentSelected.accept(path);
                }
            });
            recentlyPlayedBox.getChildren().add(item);
        }
    }

    private StackPane createEmptyPlaceholder() {
        VBox emptyBox = new VBox(8);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.getStyleClass().add("empty-state");

        Label icon = new Label("🎬");
        icon.setStyle("-fx-font-size: 36px;");

        Label title = new Label("No videos loaded");
        title.getStyleClass().add("empty-state-title");

        Label subtitle = new Label("Open a folder to scan for videos");
        subtitle.getStyleClass().add("empty-state-subtitle");

        emptyBox.getChildren().addAll(icon, title, subtitle);

        StackPane pane = new StackPane(emptyBox);
        pane.setPadding(new Insets(40));
        return pane;
    }

    private String stripExtension(String name) {
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(0, lastDot) : name;
    }

    // --- Custom playlist cell ---
    private class PlaylistCell extends ListCell<VideoFile> {
        @Override
        protected void updateItem(VideoFile video, boolean empty) {
            super.updateItem(video, empty);

            if (empty || video == null) {
                setText(null);
                setGraphic(null);
                getStyleClass().removeAll("playlist-cell-playing");
                return;
            }

            HBox cell = new HBox(8);
            cell.setAlignment(Pos.CENTER_LEFT);

            // Index number - look up the original index in the main playlist for consistency
            int originalIdx = currentPlaylist != null ? currentPlaylist.getVideos().indexOf(video) : getIndex();
            int displayIdx = originalIdx + 1;
            
            Label indexLabel = new Label(String.format("%02d", displayIdx));
            indexLabel.getStyleClass().add("playlist-index");

            // Check if this is the currently playing item
            boolean isPlaying = currentPlaylist != null && originalIdx == currentPlaylist.getCurrentIndex();

            // Playing indicator or index
            if (isPlaying) {
                indexLabel.setText("▶");
                indexLabel.setStyle("-fx-text-fill: #e94560;");
            }

            // Video title
            Label titleLabel = new Label(video.getDisplayName());
            titleLabel.getStyleClass().add("playlist-title");
            titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            titleLabel.setWrapText(false);
            titleLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(titleLabel, Priority.ALWAYS);

            // Subtitle badge
            if (video.hasSubtitle()) {
                Label subBadge = new Label("CC");
                subBadge.getStyleClass().add("playlist-badge");
                subBadge.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 9px;");
                cell.getChildren().addAll(indexLabel, titleLabel, subBadge);
            } else {
                cell.getChildren().addAll(indexLabel, titleLabel);
            }

            setGraphic(cell);
            setText(null);

            // Apply/remove playing style
            if (isPlaying) {
                if (!getStyleClass().contains("playlist-cell-playing")) {
                    getStyleClass().add("playlist-cell-playing");
                }
            } else {
                getStyleClass().removeAll("playlist-cell-playing");
            }
        }
    }

    // --- Callback setters ---

    public void setOnFolderSelected(Consumer<File> callback) { this.onFolderSelected = callback; }
    public void setOnVideoSelected(Consumer<VideoFile> callback) { this.onVideoSelected = callback; }
    public void setOnRecentSelected(Consumer<String> callback) { this.onRecentSelected = callback; }
    public Button getOpenFolderBtn() { return openFolderBtn; }
    public ListView<VideoFile> getPlaylistListView() { return playlistListView; }
}
