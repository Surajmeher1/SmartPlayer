package com.smartplayer.player;

import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.nio.ByteBuffer;

/**
 * Custom JavaFX video surface compatible with VLCJ 4.8.x.
 */
public class JavaFXVideoSurface extends CallbackVideoSurface {

    public JavaFXVideoSurface(ImageView imageView) {
        this(new JavaFXBufferFormatCallback(imageView), new JavaFXRenderCallback(imageView));
    }

    private JavaFXVideoSurface(JavaFXBufferFormatCallback formatCallback, JavaFXRenderCallback renderCallback) {
        super(formatCallback, renderCallback, true, VideoSurfaceAdapters.getVideoSurfaceAdapter());
        formatCallback.setRenderCallback(renderCallback);
    }

    private static class JavaFXBufferFormatCallback implements BufferFormatCallback {
        private final ImageView imageView;
        private int videoWidth;
        private int videoHeight;
        private JavaFXRenderCallback renderCallback;

        JavaFXBufferFormatCallback(ImageView imageView) {
            this.imageView = imageView;
        }

        void setRenderCallback(JavaFXRenderCallback renderCallback) {
            this.renderCallback = renderCallback;
        }

        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            this.videoWidth = sourceWidth;
            this.videoHeight = sourceHeight;
            return new RV32BufferFormat(sourceWidth, sourceHeight);
        }

        @Override
        public void allocatedBuffers(ByteBuffer[] buffers) {
            assert buffers.length >= 1;
            ByteBuffer buffer = buffers[0];
            
            int width = this.videoWidth > 0 ? this.videoWidth : (int) Math.sqrt(buffer.capacity() / 4);
            int height = this.videoHeight > 0 ? this.videoHeight : (int) Math.sqrt(buffer.capacity() / 4);

            Platform.runLater(() -> {
                PixelBuffer<ByteBuffer> pixelBuffer = new PixelBuffer<>(
                        width, height, buffer, PixelFormat.getByteBgraPreInstance()
                );
                WritableImage image = new WritableImage(pixelBuffer);
                imageView.setImage(image);
                
                if (renderCallback != null) {
                    renderCallback.setPixelBuffer(pixelBuffer);
                }
            });
        }
    }

    private static class JavaFXRenderCallback implements RenderCallback {
        private final ImageView imageView;
        private PixelBuffer<ByteBuffer> pixelBuffer;

        JavaFXRenderCallback(ImageView imageView) {
            this.imageView = imageView;
        }

        void setPixelBuffer(PixelBuffer<ByteBuffer> pixelBuffer) {
            this.pixelBuffer = pixelBuffer;
        }

        @Override
        public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
            Platform.runLater(() -> {
                if (pixelBuffer != null) {
                    pixelBuffer.updateBuffer(pb -> null);
                }
            });
        }
    }
}
