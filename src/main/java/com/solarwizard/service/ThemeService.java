package com.solarwizard.service;

import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.util.Duration;

/**
 * Scene-level theme switching. The dark theme remains the base stylesheet,
 * while light mode adds an override sheet after it.
 */
public final class ThemeService {

    private static final String DARK_THEME = "/com/solarwizard/css/dark-theme.css";
    private static final String LIGHT_THEME = "/com/solarwizard/css/light-theme.css";
    private static final Duration FADE_TIME = Duration.millis(160);

    private static boolean lightMode;
    private static boolean animating;

    private ThemeService() {}

    public static void applyInitialTheme(Scene scene) {
        applyTheme(scene, false);
    }

    public static boolean isLightMode() {
        return lightMode;
    }

    public static void toggleWithFade(Scene scene) {
        if (scene == null || animating) return;

        Parent root = scene.getRoot();
        if (root == null) {
            applyTheme(scene, !lightMode);
            return;
        }

        animating = true;

        FadeTransition fadeOut = new FadeTransition(FADE_TIME, root);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.42);
        fadeOut.setOnFinished(e -> applyTheme(scene, !lightMode));

        FadeTransition fadeIn = new FadeTransition(FADE_TIME, root);
        fadeIn.setFromValue(0.42);
        fadeIn.setToValue(1.0);

        SequentialTransition transition = new SequentialTransition(fadeOut, fadeIn);
        transition.setOnFinished(e -> animating = false);
        transition.play();
    }

    private static void applyTheme(Scene scene, boolean useLightMode) {
        if (scene == null) return;

        String dark = stylesheet(DARK_THEME);
        String light = stylesheet(LIGHT_THEME);

        scene.getStylesheets().remove(dark);
        scene.getStylesheets().remove(light);
        scene.getStylesheets().add(dark);

        if (useLightMode) {
            scene.getStylesheets().add(light);
        }

        lightMode = useLightMode;
    }

    private static String stylesheet(String path) {
        var url = ThemeService.class.getResource(path);
        if (url == null) {
            throw new IllegalStateException("Missing stylesheet: " + path);
        }
        return url.toExternalForm();
    }
}
