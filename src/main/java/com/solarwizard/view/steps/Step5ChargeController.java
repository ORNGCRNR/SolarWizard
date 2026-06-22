package com.solarwizard.view.steps;

import com.solarwizard.model.SolarProject;
import com.solarwizard.service.CalcService;
import com.solarwizard.service.ValidationService;
import com.solarwizard.util.UiUtils;
import com.solarwizard.view.WizardShell;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;

/**
 * Step 5 - Solar charge controller sizing and safety checks.
 */
public class Step5ChargeController implements WizardShell.StepView {

    private final SolarProject project;
    private final WizardShell shell;
    private final ScrollPane root = new ScrollPane();
    private final VBox mainContent = new VBox(16);
    private final VBox warningBox = new VBox(6);

    private final Label lblRequiredCurrent = new Label("0 A");
    private final Label lblArrayVoc = new Label("0 V");
    private final Label lblArrayPower = new Label("0 W");
    private final Label formulaLbl = new Label();

    public Step5ChargeController(SolarProject project, WizardShell shell) {
        this.project = project;
        this.shell = shell;
        build();
    }

    private void build() {
        mainContent.setPadding(new Insets(24, 28, 24, 28));
        mainContent.getStyleClass().add("step-content");
        mainContent.getChildren().add(UiUtils.stepTitle("5", "Step 5: Solar Charge Controller Sizing", shell::showCurrentStepGuide));
        mainContent.getChildren().add(warningBox);
        mainContent.getChildren().add(buildForm());
        mainContent.getChildren().add(buildResultTiles());
        HBox fb = UiUtils.formulaBanner("");
        fb.getChildren().set(1, formulaLbl);
        mainContent.getChildren().add(fb);
        mainContent.getChildren().add(UiUtils.warningBanner(
            "Total array Voc must stay below the controller's max PV input voltage."));
        mainContent.getChildren().add(buildNavRow());

        root.setContent(mainContent);
        root.setFitToWidth(true);
        root.getStyleClass().add("step-scroll");
    }

    private VBox buildForm() {
        ComboBox<SolarProject.ChargeControllerType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(SolarProject.ChargeControllerType.MPPT, SolarProject.ChargeControllerType.PWM);
        typeBox.setValue(project.getChargeControllerType());
        typeBox.getStyleClass().add("panel-combo");
        typeBox.setMaxWidth(Double.MAX_VALUE);
        lockComboHeight(typeBox);
        typeBox.valueProperty().addListener((o, a, n) -> {
            if (n != null) {
                project.setChargeControllerType(n);
                recalculate();
            }
        });

        TextField tfBrand = styledField(project.getChargeControllerBrand(), "Brand (e.g. Victron)");
        tfBrand.textProperty().addListener((o, a, n) -> project.setChargeControllerBrand(n));

        TextField tfModel = styledField(project.getChargeControllerModel(), "Model (e.g. SmartSolar 100/50)");
        tfModel.textProperty().addListener((o, a, n) -> project.setChargeControllerModel(n));

        HBox row1 = new HBox(12,
            UiUtils.labeledField("Controller Type", typeBox, "MPPT is recommended for flexible panel wiring"),
            UiUtils.labeledField("Brand", tfBrand, null),
            UiUtils.labeledField("Model", tfModel, null));
        row1.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        Spinner<Double> spRated = numSp(project.getChargeControllerRatedCurrent(), 0, 500, 1);
        spRated.valueProperty().addListener((o, a, n) -> {
            project.setChargeControllerRatedCurrent(n);
            recalculate();
        });

        Spinner<Double> spMaxV = numSp(project.getChargeControllerMaxPvVoltage(), 0, 2000, 5);
        spMaxV.valueProperty().addListener((o, a, n) -> {
            project.setChargeControllerMaxPvVoltage(n);
            recalculate();
        });

        Spinner<Double> spMaxW = numSp(project.getChargeControllerMaxPvPower(), 0, 99999, 100);
        spMaxW.valueProperty().addListener((o, a, n) -> {
            project.setChargeControllerMaxPvPower(n);
            recalculate();
        });

        HBox row2 = new HBox(12,
            UiUtils.labeledField("Rated Charge Current (A)", spRated, "Controller output current limit"),
            UiUtils.labeledField("Max PV Input Voltage (V)", spMaxV, "Array Voc must stay below this"),
            UiUtils.labeledField("Max PV Input Power (W)", spMaxW, "Total array watts must stay within this"));
        row2.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        VBox form = new VBox(14, row1, row2);
        form.getStyleClass().add("card");
        form.setPadding(new Insets(16));
        return form;
    }

    private HBox buildResultTiles() {
        lblRequiredCurrent.getStyleClass().addAll("metric-value", "metric-blue");
        lblArrayVoc.getStyleClass().addAll("metric-value", "metric-orange");
        lblArrayPower.getStyleClass().addAll("metric-value", "metric-blue");

        HBox row = new HBox(12,
            makeTile("Required SCC Current", lblRequiredCurrent),
            makeTile("Array Voc", lblArrayVoc),
            makeTile("Array Watts", lblArrayPower));
        row.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        return row;
    }

    private VBox makeTile(String label, Label value) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("metric-label");
        VBox box = new VBox(4, lbl, value);
        box.getStyleClass().add("metric-tile");
        box.setPadding(new Insets(14));
        return box;
    }

    private void recalculate() {
        CalcService.calculate(project);

        lblRequiredCurrent.setText(UiUtils.fmt1(project.getResultRequiredSccCurrent()) + " A");
        lblArrayVoc.setText(UiUtils.fmt1(project.getResultArrayVoc()) + " V");
        lblArrayPower.setText(UiUtils.fmt0(project.getResultTotalPvW()) + " W");

        if (project.getChargeControllerType() == SolarProject.ChargeControllerType.PWM) {
            formulaLbl.setText(String.format(
                "PWM SCC = adjusted Isc %.2f A x 1.25 = %.2f A",
                project.getResultArrayIsc(), project.getResultRequiredSccCurrent()));
        } else {
            formulaLbl.setText(String.format(
                "MPPT SCC = Total Panel Watts %.0f W / Battery Voltage %.0f V = %.2f A",
                project.getResultTotalPvW(), project.getBatteryVoltage(), project.getResultRequiredSccCurrent()));
        }
        formulaLbl.getStyleClass().add("formula-text");

        warningBox.getChildren().clear();
        for (ValidationService.Warning w : ValidationService.validateChargeController(project)) {
            warningBox.getChildren().add(UiUtils.warningBanner(precautionMessage(w)));
        }
    }

    private HBox buildNavRow() {
        Button back = new Button("BACK");
        back.getStyleClass().add("nav-back-btn");
        back.setOnAction(e -> shell.prevStep());

        Button next = new Button("NEXT");
        next.getStyleClass().add("nav-next-btn");
        next.setOnAction(e -> {
            List<ValidationService.Warning> warns = ValidationService.validateChargeController(project);
            boolean hasError = warns.stream().anyMatch(w -> w.severity() == ValidationService.Warning.Severity.ERROR);
            if (hasError) showPrecautionDialog(warns, () -> shell.nextStep());
            else shell.nextStep();
        });

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(8, back, sp, next);
        row.setPadding(new Insets(16, 0, 0, 0));
        return row;
    }

    private void showPrecautionDialog(List<ValidationService.Warning> warns, Runnable proceed) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Safety Precautions");
        alert.setHeaderText("Solar charge controller safety checks (not system errors)");
        StringBuilder sb = new StringBuilder();
        warns.forEach(w -> sb.append("- ").append(precautionMessage(w)).append("\n\n"));
        alert.setContentText(sb.toString().trim());
        alert.getButtonTypes().setAll(
            new ButtonType("GO BACK & FIX", ButtonBar.ButtonData.CANCEL_CLOSE),
            new ButtonType("PROCEED ANYWAY", ButtonBar.ButtonData.OK_DONE));
        alert.showAndWait().ifPresent(bt -> {
            if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) proceed.run();
        });
    }

    private String precautionMessage(ValidationService.Warning warning) {
        return warning.severity() == ValidationService.Warning.Severity.ERROR
            ? "SAFETY PRECAUTION (Proceed at your own risk): " + warning.message()
            : warning.message();
    }

    private TextField styledField(String init, String prompt) {
        TextField tf = new TextField(init);
        tf.setPromptText(prompt);
        tf.getStyleClass().add("styled-field");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private Spinner<Double> numSp(double init, double min, double max, double step) {
        Spinner<Double> sp = UiUtils.doubleSpinner(min, max, init, step);
        sp.setMaxWidth(Double.MAX_VALUE);
        return sp;
    }

    private void lockComboHeight(ComboBox<?> combo) {
        combo.setMinHeight(34);
        combo.setPrefHeight(34);
        combo.setMaxHeight(34);
        combo.setVisibleRowCount(4);
    }

    @Override public Node getRoot() { return root; }
    @Override public String getStepTitle() { return "Charge Controller"; }
    @Override public void onEnter() { recalculate(); }
    @Override public void onLeave() {}
}
