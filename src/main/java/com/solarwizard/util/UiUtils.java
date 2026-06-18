package com.solarwizard.util;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.scene.Node;

/**
 * Reusable UI factory methods to keep controllers clean and consistent.
 */
public final class UiUtils {

    private UiUtils() {}

    /** Dark card container */
    public static VBox card(Node... children) {
        VBox box = new VBox(10, children);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(16));
        return box;
    }

    /** Metric display tile */
    public static VBox metricTile(String label, String value, String styleClass) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("metric-label");
        Label val = new Label(value);
        val.getStyleClass().addAll("metric-value", styleClass);
        VBox box = new VBox(4, lbl, val);
        box.getStyleClass().add("metric-tile");
        box.setPadding(new Insets(14));
        return box;
    }

    /** Blue info banner with formula text */
    public static HBox formulaBanner(String text) {
        Label icon = new Label("ℹ");
        icon.getStyleClass().add("info-icon");
        Label lbl = new Label(text);
        lbl.getStyleClass().add("formula-text");
        lbl.setWrapText(true);
        HBox box = new HBox(8, icon, lbl);
        box.getStyleClass().add("formula-banner");
        box.setPadding(new Insets(10, 14, 10, 14));
        HBox.setHgrow(lbl, Priority.ALWAYS);
        return box;
    }

    /** Orange warning banner */
    public static HBox warningBanner(String text) {
        Label icon = new Label("⚠");
        icon.getStyleClass().add("warn-icon");
        Label lbl = new Label(text);
        lbl.getStyleClass().add("warn-text");
        lbl.setWrapText(true);
        HBox box = new HBox(8, icon, lbl);
        box.getStyleClass().add("warning-banner");
        box.setPadding(new Insets(10, 14, 10, 14));
        HBox.setHgrow(lbl, Priority.ALWAYS);
        return box;
    }

    /** Red error banner with dismiss X */
    public static HBox errorBanner(String text, Runnable onDismiss) {
        Label icon = new Label("✕");
        icon.getStyleClass().add("error-icon");
        Label lbl = new Label(text);
        lbl.getStyleClass().add("error-text");
        lbl.setWrapText(true);
        Button close = new Button("✕");
        close.getStyleClass().add("dismiss-btn");
        close.setOnAction(e -> onDismiss.run());
        HBox box = new HBox(8, icon, lbl, close);
        box.getStyleClass().add("error-banner");
        box.setPadding(new Insets(10, 14, 10, 14));
        HBox.setHgrow(lbl, Priority.ALWAYS);
        return box;
    }

    /** Green success banner */
    public static HBox successBanner(String text) {
        Label icon = new Label("✔");
        icon.getStyleClass().add("success-icon");
        Label lbl = new Label(text);
        lbl.getStyleClass().add("success-text");
        lbl.setWrapText(true);
        HBox box = new HBox(8, icon, lbl);
        box.getStyleClass().add("success-banner");
        box.setPadding(new Insets(10, 14, 10, 14));
        HBox.setHgrow(lbl, Priority.ALWAYS);
        return box;
    }

    /** Styled spinner for numeric input */
    public static Spinner<Double> doubleSpinner(double min, double max, double init, double step) {
        Spinner<Double> sp = new Spinner<>(min, max, init, step);
        sp.setEditable(true);
        sp.getStyleClass().add("styled-spinner");
        sp.setPrefWidth(140);
        return sp;
    }

    public static Spinner<Integer> intSpinner(int min, int max, int init) {
        Spinner<Integer> sp = new Spinner<>(min, max, init);
        sp.setEditable(true);
        sp.getStyleClass().add("styled-spinner");
        sp.setPrefWidth(100);
        return sp;
    }

    /** Labeled input group */
    public static VBox labeledField(String labelText, Node field, String hint) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("field-label");
        VBox box = new VBox(4, lbl, field);
        if (hint != null && !hint.isEmpty()) {
            Label hintLbl = new Label(hint);
            hintLbl.getStyleClass().add("field-hint");
            box.getChildren().add(hintLbl);
        }
        return box;
    }

    /** Mode toggle button */
    public static Button modeButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("mode-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btn, Priority.ALWAYS);
        return btn;
    }

    /** Step section title */
    public static HBox stepTitle(String icon, String title) {
        Label ico = new Label(icon);
        ico.getStyleClass().add("step-icon");
        Label lbl = new Label(title);
        lbl.getStyleClass().add("step-title");
        HBox box = new HBox(10, ico, lbl);
        box.getStyleClass().add("step-title-bar");
        return box;
    }

    /** Format double to 2 decimal places */
    public static String fmt2(double v) { return String.format("%.2f", v); }
    public static String fmtPercent(double ratio) { return String.format("%.0f%%", ratio * 100.0); }
    public static String fmt1(double v) { return String.format("%.1f", v); }
    public static String fmt0(double v) { return String.format("%.0f", v); }
}
