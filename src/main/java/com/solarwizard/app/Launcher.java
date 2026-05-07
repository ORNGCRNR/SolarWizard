package com.solarwizard.app;

/**
 * Separate launcher class required so the fat JAR can launch JavaFX
 * without the module system blocking the Application class.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
