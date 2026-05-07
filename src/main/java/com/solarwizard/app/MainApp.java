package com.solarwizard.app;

import com.solarwizard.model.SolarProject;
import com.solarwizard.view.DashboardView;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * JavaFX application entry point.
 */
public class MainApp extends Application {

    public static final String APP_TITLE   = "Solar Sizing Wizard";
    public static final int    WINDOW_W    = 1280;
    public static final int    WINDOW_H    = 780;

    @Override
    public void start(Stage stage) {
        SolarProject project = new SolarProject();

        DashboardView dashboard = new DashboardView(stage, project);
        Scene scene = new Scene((Parent) dashboard.getRoot(), WINDOW_W, WINDOW_H);
        scene.getStylesheets().add(
            getClass().getResource("/com/solarwizard/css/dark-theme.css").toExternalForm());

        stage.setTitle(APP_TITLE);
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
