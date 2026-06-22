package com.solarwizard.util;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.stage.Window;
import java.util.function.Function;
import java.util.function.Predicate;

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
        attachNumericValidation(
            sp,
            text -> {
                double value = Double.parseDouble(text);
                if (!Double.isFinite(value)) {
                    throw new NumberFormatException("Non-finite number");
                }
                return value;
            },
            value -> value >= min && value <= max);
        return sp;
    }

    public static Spinner<Integer> intSpinner(int min, int max, int init) {
        Spinner<Integer> sp = new Spinner<>(min, max, init);
        sp.setEditable(true);
        sp.getStyleClass().add("styled-spinner");
        sp.setPrefWidth(100);
        attachNumericValidation(
            sp,
            Integer::parseInt,
            value -> value >= min && value <= max);
        return sp;
    }

    private static <T> void attachNumericValidation(
            Spinner<T> spinner,
            Function<String, T> parser,
            Predicate<T> isInRange) {
        TextField editor = spinner.getEditor();

        Runnable validate = () -> setInputInvalid(
            spinner,
            editor,
            !isValidNumberText(editor.getText(), parser, isInRange));

        editor.textProperty().addListener((o, oldText, newText) -> validate.run());
        editor.setOnAction(e -> {
            if (!commitSpinnerText(spinner, parser, isInRange)) {
                setInputInvalid(spinner, editor, true);
            }
        });
        editor.focusedProperty().addListener((o, wasFocused, focused) -> {
            if (focused) {
                validate.run();
            } else if (!commitSpinnerText(spinner, parser, isInRange)) {
                setInputInvalid(spinner, editor, true);
            }
        });
        spinner.valueProperty().addListener((o, oldValue, newValue) -> {
            if (!editor.isFocused()) {
                setInputInvalid(spinner, editor, false);
            }
        });
        validate.run();
    }

    private static <T> boolean commitSpinnerText(
            Spinner<T> spinner,
            Function<String, T> parser,
            Predicate<T> isInRange) {
        String text = spinner.getEditor().getText();
        if (!isValidNumberText(text, parser, isInRange)) {
            return false;
        }
        spinner.getValueFactory().setValue(parser.apply(text.trim()));
        return true;
    }

    private static <T> boolean isValidNumberText(
            String text,
            Function<String, T> parser,
            Predicate<T> isInRange) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        try {
            T value = parser.apply(text.trim());
            return isInRange.test(value);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static void setInputInvalid(
            Spinner<?> spinner,
            TextField editor,
            boolean invalid) {
        setStyleClass(spinner, "input-error", invalid);
        setStyleClass(editor, "input-error", invalid);
    }

    private static void setStyleClass(Node node, String styleClass, boolean enabled) {
        if (enabled) {
            if (!node.getStyleClass().contains(styleClass)) {
                node.getStyleClass().add(styleClass);
            }
        } else {
            node.getStyleClass().remove(styleClass);
        }
    }

    /** Labeled input group */
    public static VBox labeledField(String labelText, Node field, String hint) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("field-label");
        VBox box = new VBox(4, lbl, field);
        if (hint != null && !hint.isEmpty()) {
            lbl.setTooltip(new Tooltip(hint));
            if (field instanceof Control control) {
                control.setTooltip(new Tooltip(hint));
            }
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
        return stepTitle(icon, title, null);
    }

    public static HBox stepTitle(String icon, String title, Runnable onGuide) {
        Label ico = new Label(icon);
        ico.getStyleClass().add("step-icon");
        Label lbl = new Label(title);
        lbl.getStyleClass().add("step-title");
        HBox box = new HBox(10, ico, lbl);
        if (onGuide != null) {
            box.getChildren().add(guideButton(onGuide));
        }
        box.getStyleClass().add("step-title-bar");
        return box;
    }

    public static Button guideButton(Runnable onGuide) {
        Button btn = new Button("?");
        btn.getStyleClass().add("step-help-btn");
        btn.setTooltip(new Tooltip("Open guide for this step"));
        btn.setMinSize(24, 24);
        btn.setPrefSize(24, 24);
        btn.setMaxSize(24, 24);
        btn.setAlignment(Pos.CENTER);
        btn.setOnAction(e -> onGuide.run());
        return btn;
    }

    public static void showGuideDialog(Window owner, String titleText, String guide) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(titleText + " Guide");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStyleClass().add("guide-dialog");

        Label title = new Label(titleText);
        title.getStyleClass().add("guide-title");

        TextArea guideText = new TextArea(guide);
        guideText.getStyleClass().add("guide-text");
        guideText.setEditable(false);
        guideText.setWrapText(true);
        guideText.setPrefColumnCount(54);
        guideText.setPrefRowCount(18);

        VBox content = new VBox(12, title, guideText);
        content.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(content);

        if (owner != null) {
            dialog.initOwner(owner);
        }

        dialog.showAndWait();
    }

    /** Format double to 2 decimal places */
    public static String fmt2(double v) { return String.format("%.2f", v); }
    public static String fmtPercent(double ratio) { return String.format("%.0f%%", ratio * 100.0); }
    public static String fmt1(double v) { return String.format("%.1f", v); }
    public static String fmt0(double v) { return String.format("%.0f", v); }
}
