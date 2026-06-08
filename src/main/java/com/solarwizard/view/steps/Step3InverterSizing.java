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
 * Step 6 - Inverter Sizing.
 * Uses appliance surge loads from Step 1 and battery voltage from Step 3.
 */
public class Step3InverterSizing implements WizardShell.StepView {

    private final SolarProject project;
    private final WizardShell  shell;
    private final ScrollPane   root = new ScrollPane();
    private final VBox         mainContent = new VBox(16);
    private final VBox         warningBox  = new VBox(6);

    // Result tiles
    private final Label lblTotalPv    = new Label("0 W");
    private final Label lblPeakLoad   = new Label("0 W");
    private final Label lblWithMargin = new Label("0 W");
    private final Label formulaLbl    = new Label();

    public Step3InverterSizing(SolarProject project, WizardShell shell) {
        this.project = project;
        this.shell   = shell;
        build();
    }

    private void build() {
        mainContent.setPadding(new Insets(24, 28, 24, 28));
        mainContent.getStyleClass().add("step-content");
        mainContent.getChildren().add(UiUtils.stepTitle("6", "Step 6: Inverter Sizing"));
        mainContent.getChildren().add(warningBox);
        mainContent.getChildren().add(buildInputForm());
        mainContent.getChildren().add(buildResultTiles());
        HBox fb = UiUtils.formulaBanner("");
        fb.getChildren().set(1, formulaLbl);
        mainContent.getChildren().add(fb);
        mainContent.getChildren().add(buildNavRow());

        root.setContent(mainContent);
        root.setFitToWidth(true);
        root.getStyleClass().add("step-scroll");
    }

    private VBox buildInputForm() {
        TextField tfBrand = styledField(project.getInverterBrand(), "Brand (e.g. DEYE)");
        tfBrand.textProperty().addListener((o, a, n) -> project.setInverterBrand(n));

        TextField tfModel = styledField(project.getInverterModel(), "Model (e.g. SUN-5K-SG03LP1)");
        tfModel.textProperty().addListener((o, a, n) -> project.setInverterModel(n));

        HBox brandRow = new HBox(12, tfBrand, tfModel);
        HBox.setHgrow(tfBrand, Priority.ALWAYS);
        HBox.setHgrow(tfModel, Priority.ALWAYS);

        Spinner<Double> spRated = numSp(project.getInverterRatedPower(), 0, 99999, 100);
        spRated.valueProperty().addListener((o, a, n) -> {
            project.setInverterRatedPower(n);
            recalculate();
        });

        HBox ratedRow = new HBox(12,
            UiUtils.labeledField("Rated Power (W)", spRated, "Continuous AC output power"));
        ratedRow.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        VBox form = new VBox(14, brandRow, ratedRow);
        form.getStyleClass().add("card");
        form.setPadding(new Insets(16));
        return form;
    }

    private HBox buildResultTiles() {
        lblTotalPv   .getStyleClass().addAll("metric-value", "metric-blue");
        lblPeakLoad  .getStyleClass().addAll("metric-value", "metric-orange");
        lblWithMargin.getStyleClass().addAll("metric-value", "metric-green");
        HBox row = new HBox(12,
            makeTile("Connected Load (All On)", lblTotalPv),
            makeTile("Peak Simultaneous Load",  lblPeakLoad),
            makeTile("Minimum Inverter (Peak)", lblWithMargin));
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
        double adjustedLoad = CalcService.adjustedLoadWatts(project.getAppliances());
        double peakLoad = CalcService.peakSimultaneousWatts(project.getAppliances());
        double withMargin = project.getResultInverterMinW();

        lblTotalPv   .setText(UiUtils.fmt0(adjustedLoad) + " W");
        lblPeakLoad  .setText(UiUtils.fmt0(peakLoad) + " W");
        lblWithMargin.setText(UiUtils.fmt0(withMargin) + " W");

        formulaLbl.setText(String.format(
            "Connected Load (all appliances on) = %.0f W\nPeak Load (demand factor applied) = %.0f W\nMinimum Inverter = %.0f x 1.20 = %.0f W",
            adjustedLoad, peakLoad, peakLoad, withMargin));
        formulaLbl.getStyleClass().add("formula-text");

        warningBox.getChildren().clear();
        for (ValidationService.Warning w : ValidationService.validateInverter(project)) {
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
            List<ValidationService.Warning> warns = ValidationService.validateInverter(project);
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
        alert.setHeaderText("Solar inverter safety checks (not system errors)");
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
        Spinner<Double> sp = new Spinner<>(min, max, init, step);
        sp.setEditable(true);
        sp.getStyleClass().add("styled-spinner");
        sp.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(sp, Priority.ALWAYS);
        return sp;
    }

    @Override public Node   getRoot()      { return root; }
    @Override public String getStepTitle() { return "Inverter Sizing"; }
    @Override public void   onEnter()      { recalculate(); }
    @Override public void   onLeave()      {}
}
