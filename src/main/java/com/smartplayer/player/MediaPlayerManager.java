package com.smartplayer.player;

import com.smartplayer.models.VideoFile;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.image.ImageView;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

/**
 * Manages the VLCJ EmbeddedMediaPlayer lifecycle, video surface binding,
 * and exposes observable properties for UI binding.
 *
 * Handles play, pause, stop, seek, rate, volume, brightness,
 * and fires callbacks for auto-next and time updates.
 */
public class MediaPlayerManager {

    private MediaPlayerFactory factory;
    private EmbeddedMediaPlayer mediaPlayer;
    private ImageView videoImageView;

    // Observable properties for UI binding
    private final LongProperty currentTime = new SimpleLongProperty(0);
    private final LongProperty totalDuration = new SimpleLongProperty(0);
    private final BooleanProperty playing = new SimpleBooleanProperty(false);
    private final BooleanProperty muted = new SimpleBooleanProperty(false);
    private final IntegerProperty volume = new SimpleIntegerProperty(80);
    private final FloatProperty rate = new SimpleFloatProperty(1.0f);
    private final StringProperty currentTitle = new SimpleStringProperty("No video loaded");

    // Callbacks
    private Runnable onFinished;
    private Runnable onError;

    /**
     * Initializes the VLCJ media player and binds it to a JavaFX ImageView.
     *
     * @param imageView The JavaFX ImageView to render video frames into
     */
    public void initialize(ImageView imageView) {
        this.videoImageView = imageView;

        // Create the media player factory with default args
        factory = new MediaPlayerFactory();
        mediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();

        // Set the custom JavaFX video surface (compatible with vlcj 4.8.x)
        mediaPlayer.videoSurface().set(new JavaFXVideoSurface(videoImageView));

        // Register event listeners
        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void mediaPlayerReady(MediaPlayer mp) {
                Platform.runLater(() -> {
                    totalDuration.set(mp.status().length());
                    playing.set(true);
                });
            }

            @Override
            public void timeChanged(MediaPlayer mp, long newTime) {
                Platform.runLater(() -> currentTime.set(newTime));
            }

            @Override
            public void lengthChanged(MediaPlayer mp, long newLength) {
                Platform.runLater(() -> totalDuration.set(newLength));
            }

            @Override
            public void finished(MediaPlayer mp) {
                Platform.runLater(() -> {
                    playing.set(false);
                    if (onFinished != null) {
                        onFinished.run();
                    }
                });
            }

            @Override
            public void error(MediaPlayer mp) {
                Platform.runLater(() -> {
                    playing.set(false);
                    System.err.println("Media player error occurred");
                    if (onError != null) {
                        onError.run();
                    }
                });
            }

            @Override
            public void playing(MediaPlayer mp) {
                Platform.runLater(() -> playing.set(true));
            }

            @Override
            public void paused(MediaPlayer mp) {
                Platform.runLater(() -> playing.set(false));
            }

            @Override
            public void stopped(MediaPlayer mp) {
                Platform.runLater(() -> {
                    playing.set(false);
                    currentTime.set(0);
                });
            }
        });
    }

    /**
     * Plays a video file. Optionally resumes from a saved position.
     *
     * @param video The VideoFile to play
     * @param resumePosition Position in ms to resume from (0 = start)
     */
    public void play(VideoFile video, long resumePosition) {
        if (mediaPlayer == null) return;

        currentTitle.set(video.getDisplayName());

        // Start playing the media
        mediaPlayer.media().play(video.getFilePath());

        // Resume from saved position if available
        if (resumePosition > 0) {
            // Use a small delay to ensure media is loaded before seeking
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    mediaPlayer.controls().setTime(resumePosition);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        // Set volume
        mediaPlayer.audio().setVolume(volume.get());
    }

    /**
     * Toggles play/pause state.
     */
    public void togglePlayPause() {
        if (mediaPlayer == null) return;

        if (mediaPlayer.status().isPlaying()) {
            mediaPlayer.controls().pause();
        } else {
            mediaPlayer.controls().play();
        }
    }

    /**
     * Stops playback entirely.
     */
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.controls().stop();
            currentTitle.set("No video loaded");
        }
    }

    /**
     * Seeks to a specific position.
     * @param timeMs Position in milliseconds
     */
    public void seek(long timeMs) {
        if (mediaPlayer != null && mediaPlayer.status().isPlayable()) {
            mediaPlayer.controls().setTime(timeMs);
        }
    }

    /**
     * Skips forward or backward by the given number of milliseconds.
     * @param deltaMs Positive = forward, negative = backward
     */
    public void skip(long deltaMs) {
        if (mediaPlayer != null && mediaPlayer.status().isPlayable()) {
            long newTime = mediaPlayer.status().time() + deltaMs;
            newTime = Math.max(0, Math.min(newTime, mediaPlayer.status().length()));
            mediaPlayer.controls().setTime(newTime);
        }
    }

    /**
     * Sets the playback volume (0-200, VLC scale).
     * @param vol Volume level (0-100 from UI, mapped to 0-200 for VLC)
     */
    public void setVolume(int vol) {
        volume.set(vol);
        if (mediaPlayer != null) {
            mediaPlayer.audio().setVolume(vol);
        }
    }

    /**
     * Toggles mute state.
     */
    public void toggleMute() {
        if (mediaPlayer != null) {
            boolean newMute = !muted.get();
            muted.set(newMute);
            mediaPlayer.audio().setMute(newMute);
        }
    }

    /**
     * Sets the playback rate.
     * @param newRate Speed multiplier (e.g., 0.5, 1.0, 1.5, 2.0)
     */
    public void setRate(float newRate) {
        rate.set(newRate);
        if (mediaPlayer != null) {
            mediaPlayer.controls().setRate(newRate);
        }
    }

    /**
     * Sets the video brightness.
     * @param brightness Value (0.0 to 2.0, 1.0 = normal)
     */
    public void setBrightness(float brightness) {
        if (mediaPlayer != null) {
            mediaPlayer.video().setAdjustVideo(true);
            mediaPlayer.video().setBrightness(brightness);
        }
    }

    /**
     * Gets the current playback position in milliseconds.
     */
    public long getCurrentTimeMs() {
        if (mediaPlayer != null && mediaPlayer.status().isPlayable()) {
            return mediaPlayer.status().time();
        }
        return 0;
    }

    /**
     * Releases all native resources. Must be called on application shutdown.
     */
    public void dispose() {
        if (mediaPlayer != null) {
            mediaPlayer.controls().stop();
            mediaPlayer.release();
        }
        if (factory != null) {
            factory.release();
        }
    }

    // --- Subtitle methods ---

    /**
     * Loads an external subtitle file.
     */
    public void loadSubtitle(String subtitlePath) {
        if (mediaPlayer != null && subtitlePath != null) {
            mediaPlayer.subpictures().setSubTitleFile(subtitlePath);
        }
    }

    /**
     * Toggles subtitles on/off.
     */
    public void toggleSubtitles(boolean enabled) {
        if (mediaPlayer != null) {
            if (enabled) {
                // Re-enable the first subtitle track
                var tracks = mediaPlayer.subpictures().trackDescriptions();
                if (tracks.size() > 1) {
                    mediaPlayer.subpictures().setTrack(tracks.get(1).id());
                }
            } else {
                mediaPlayer.subpictures().setTrack(-1);
            }
        }
    }

    // --- Property accessors for UI binding ---

    public LongProperty currentTimeProperty() { return currentTime; }
    public LongProperty totalDurationProperty() { return totalDuration; }
    public BooleanProperty playingProperty() { return playing; }
    public BooleanProperty mutedProperty() { return muted; }
    public IntegerProperty volumeProperty() { return volume; }
    public FloatProperty rateProperty() { return rate; }
    public StringProperty currentTitleProperty() { return currentTitle; }

    public void setOnFinished(Runnable onFinished) { this.onFinished = onFinished; }
    public void setOnError(Runnable onError) { this.onError = onError; }

    public EmbeddedMediaPlayer getMediaPlayer() { return mediaPlayer; }
}
