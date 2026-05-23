package com.smartplayer;

/**
 * Non-JavaFX launcher class.
 * This is needed as a workaround for the JavaFX module system
 * when running from a fat/shade JAR. The main class must NOT extend
 * javafx.application.Application when using the shade plugin.
 */
public class Launcher {

    public static void main(String[] args) {
        App.main(args);
    }
}
