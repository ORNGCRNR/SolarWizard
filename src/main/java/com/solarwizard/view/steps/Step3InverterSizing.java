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

    // Spec display labels
    private final Label specRated    = new Label("-");
    private final Label specMaxPv    = new Label("-");
    private final Label specSysV     = new Label("-");
    private final Label specBattV    = new Label("-");
    private final Label specMpptCnt  = new Label("-");
    private final Label specMaxVMppt = new Label("-");
    private final Label specMaxIMppt = new Label("-");

    // Result tiles
    private final Label lblTotalPv   = new Label("0 W");
    private final Label lblPeakLoad  = new Label("0 W");
    private final Label lblWithMargin= new Label("0 W");
    private final Label formulaLbl   = new Label();

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
        mainContent.getChildren().add(buildSpecsCard());
        mainContent.getChildren().add(buildResultTiles());
        HBox fb = UiUtils.formulaBanner(""); fb.getChildren().set(1, formulaLbl);
        mainContent.getChildren().add(fb);
        mainContent.getChildren().add(buildNavRow());

        root.setContent(mainContent);
        root.setFitToWidth(true);
        root.getStyleClass().add("step-scroll");
    }

    // ── Input form ────────────────────────────────────────────────────────────
    private VBox buildInputForm() {
        TextField tfBrand = styledField(project.getInverterBrand(), "Brand (e.g. DEYE)");
        tfBrand.textProperty().addListener((o, a, n) -> { project.setInverterBrand(n); updateSpecs(); });

        TextField tfModel = styledField(project.getInverterModel(), "Model (e.g. SUN-5K-SG03LP1)");
        tfModel.textProperty().addListener((o, a, n) -> { project.setInverterModel(n); updateSpecs(); });

        HBox brandRow = new HBox(12, tfBrand, tfModel);
        HBox.setHgrow(tfBrand, Priority.ALWAYS);
        HBox.setHgrow(tfModel, Priority.ALWAYS);

        // Row 1: Rated Power | Max PV Input
        Spinner<Double> spRated  = numSp(project.getInverterRatedPower(),  0, 99999, 100);
        Spinner<Double> spMaxPv  = numSp(project.getInverterMaxPvInput(),  0, 99999, 100);
        spRated .valueProperty().addListener((o, a, n) -> { project.setInverterRatedPower(n);  recalculate(); });
        spMaxPv .valueProperty().addListener((o, a, n) -> { project.setInverterMaxPvInput(n);  recalculate(); });

        HBox row1 = new HBox(12,
            UiUtils.labeledField("Rated Power (W)",    spRated, "Continuous AC output power"),
            UiUtils.labeledField("Max PV Input (W)",   spMaxPv, "Max total PV array input power"));
        row1.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        // Row 2: System Voltage | Battery Min V | Battery Max V
        ComboBox<Double> cbSysV = new ComboBox<>();
        cbSysV.getItems().addAll(12.0, 24.0, 48.0);
        cbSysV.setValue(project.getInverterSysVoltage());
        cbSysV.getStyleClass().add("panel-combo");
        cbSysV.setMaxWidth(Double.MAX_VALUE);
        lockComboHeight(cbSysV);
        cbSysV.valueProperty().addListener((o, a, n) -> { if (n != null) { project.setInverterSysVoltage(n); recalculate(); } });

        Spinner<Double> spBattMin = numSp(project.getInverterBattMinV(), 0, 600, 1);
        Spinner<Double> spBattMax = numSp(project.getInverterBattMaxV(), 0, 600, 1);
        spBattMin.valueProperty().addListener((o, a, n) -> { project.setInverterBattMinV(n); updateSpecs(); });
        spBattMax.valueProperty().addListener((o, a, n) -> { project.setInverterBattMaxV(n); updateSpecs(); });

        HBox row2 = new HBox(12,
            UiUtils.labeledField("Input Voltage (V)",  cbSysV,   "Must match the battery bank voltage"),
            UiUtils.labeledField("Battery Min V",      spBattMin, "Min battery voltage accepted"),
            UiUtils.labeledField("Battery Max V",      spBattMax, "Max battery voltage accepted"));
        row2.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        // Row 3: MPPT Count | Max V per MPPT | Max I per MPPT
        Spinner<Integer> spMppt   = new Spinner<>(1, 20, project.getInverterMpptCount());
        spMppt.setEditable(true); spMppt.getStyleClass().add("styled-spinner"); spMppt.setMaxWidth(Double.MAX_VALUE);
        spMppt.valueProperty().addListener((o, a, n) -> { project.setInverterMpptCount(n); updateSpecs(); });

        Spinner<Double> spMaxVMppt = numSp(project.getInverterMaxVPerMppt(), 0, 2000, 10);
        Spinner<Double> spMaxIMppt = numSp(project.getInverterMaxIPerMppt(), 0, 200,  1);
        spMaxVMppt.valueProperty().addListener((o, a, n) -> { project.setInverterMaxVPerMppt(n); updateSpecs(); });
        spMaxIMppt.valueProperty().addListener((o, a, n) -> { project.setInverterMaxIPerMppt(n); updateSpecs(); });

        HBox row3 = new HBox(12,
            UiUtils.labeledField("MPPT Count",         spMppt,    "Number of MPPT channels"),
            UiUtils.labeledField("Max V per MPPT (V)", spMaxVMppt,"Maximum input voltage per MPPT"),
            UiUtils.labeledField("Max I per MPPT (A)", spMaxIMppt,"Maximum input current per MPPT"));
        row3.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        // Row 4: Max PV per MPPT | AC Output
        Spinner<Double> spMaxPvMppt = numSp(project.getInverterMaxPvPerMppt(), 0, 99999, 100);
        Spinner<Double> spAcOut     = numSp(project.getInverterAcOutput(),      0, 99999, 100);
        spMaxPvMppt.valueProperty().addListener((o, a, n) -> { project.setInverterMaxPvPerMppt(n); updateSpecs(); });
        spAcOut    .valueProperty().addListener((o, a, n) -> { project.setInverterAcOutput(n);     updateSpecs(); });

        HBox row4 = new HBox(12,
            UiUtils.labeledField("Max PV per MPPT (W)", spMaxPvMppt, "Max PV power per MPPT channel"),
            UiUtils.labeledField("AC Output (W)",       spAcOut,     "AC output power capacity"));
        row4.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        VBox form = new VBox(14, brandRow, row1, row2, row3, row4);
        form.getStyleClass().add("card");
        form.setPadding(new Insets(16));
        return form;
    }

    // ── Specs card ────────────────────────────────────────────────────────────
    private VBox buildSpecsCard() {
        Label title = new Label("ℹ  Inverter Specifications");
        title.getStyleClass().add("specs-title");

        VBox card = new VBox(0, title,
            specRow("Rated Power",         specRated),
            specRow("Max PV Input",        specMaxPv),
            specRow("Input Voltage",       specSysV),
            specRow("Battery Voltage Range", specBattV),
            specRow("MPPT Count",          specMpptCnt),
            specRow("Max V per MPPT",      specMaxVMppt),
            specRow("Max I per MPPT",      specMaxIMppt));
        card.getStyleClass().add("specs-card");
        card.setPadding(new Insets(16));
        updateSpecs();
        return card;
    }

    private HBox specRow(String key, Label value) {
        Label k = new Label(key); k.getStyleClass().add("spec-key");
        value.getStyleClass().add("spec-value");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(k, sp, value);
        row.getStyleClass().add("spec-row"); row.setPadding(new Insets(8, 0, 8, 0));
        return row;
    }

    private void updateSpecs() {
        specRated   .setText(UiUtils.fmt0(project.getInverterRatedPower())  + "W");
        specMaxPv   .setText(UiUtils.fmt0(project.getInverterMaxPvInput())  + "W");
        specSysV    .setText(UiUtils.fmt0(project.getInverterSysVoltage())  + "V");
        specBattV   .setText(UiUtils.fmt0(project.getInverterBattMinV()) + "–" + UiUtils.fmt0(project.getInverterBattMaxV()) + "V");
        specMpptCnt .setText(String.valueOf(project.getInverterMpptCount()));
        specMaxVMppt.setText(UiUtils.fmt0(project.getInverterMaxVPerMppt()) + "V");
        specMaxIMppt.setText(UiUtils.fmt0(project.getInverterMaxIPerMppt()) + "A");
    }

    // ── Result tiles ──────────────────────────────────────────────────────────
    private HBox buildResultTiles() {
        lblTotalPv   .getStyleClass().addAll("metric-value", "metric-blue");
        lblPeakLoad  .getStyleClass().addAll("metric-value", "metric-orange");
        lblWithMargin.getStyleClass().addAll("metric-value", "metric-blue");
        HBox row = new HBox(12,
            makeTile("Adjusted Load Watts", lblTotalPv),
            makeTile("Battery Voltage",     lblPeakLoad),
            makeTile("Minimum Inverter",    lblWithMargin));
        row.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        return row;
    }

    private VBox makeTile(String label, Label value) {
        Label lbl = new Label(label); lbl.getStyleClass().add("metric-label");
        VBox box = new VBox(4, lbl, value);
        box.getStyleClass().add("metric-tile"); box.setPadding(new Insets(14));
        return box;
    }

    private void recalculate() {
        CalcService.calculate(project);
        double adjustedLoad = CalcService.adjustedLoadWatts(project.getAppliances());
        double withMargin = project.getResultInverterMinW();

        lblTotalPv   .setText(UiUtils.fmt0(adjustedLoad) + " W");
        lblPeakLoad  .setText(UiUtils.fmt0(project.getBatteryVoltage()) + " V");
        lblWithMargin.setText(UiUtils.fmt0(withMargin) + " W");

        formulaLbl.setText(String.format(
            "Adjusted Load = motor watts x 3 + non-motor watts = %.0f W  |  Inverter Watts = %.0f x 1.20 = %.0f W",
            adjustedLoad, adjustedLoad, withMargin));
        formulaLbl.getStyleClass().add("formula-text");

        updateSpecs();
        warningBox.getChildren().clear();
        for (ValidationService.Warning w : ValidationService.validateInverter(project)) {
            warningBox.getChildren().add(
                w.severity() == ValidationService.Warning.Severity.ERROR
                    ? UiUtils.errorBanner(w.message(), () -> warningBox.getChildren().clear())
                    : UiUtils.warningBanner(w.message()));
        }
    }

    private HBox buildNavRow() {
        Button back = new Button("← BACK"); back.getStyleClass().add("nav-back-btn"); back.setOnAction(e -> shell.prevStep());
        Button next = new Button("NEXT →"); next.getStyleClass().add("nav-next-btn");
        next.setOnAction(e -> {
            List<ValidationService.Warning> warns = ValidationService.validateInverter(project);
            boolean hasError = warns.stream().anyMatch(w -> w.severity() == ValidationService.Warning.Severity.ERROR);
            if (hasError) showPrecautionDialog(warns, () -> shell.nextStep());
            else shell.nextStep();
        });
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        return new HBox(8, back, sp, next) {{ setPadding(new Insets(16, 0, 0, 0)); }};
    }

    private void showPrecautionDialog(List<ValidationService.Warning> warns, Runnable proceed) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Precautions"); alert.setHeaderText("⚠  Warnings — not advisable:");
        StringBuilder sb = new StringBuilder();
        warns.forEach(w -> sb.append("• ").append(w.message()).append("\n\n"));
        alert.setContentText(sb.toString().trim());
        alert.getButtonTypes().setAll(
            new ButtonType("GO BACK & FIX", ButtonBar.ButtonData.CANCEL_CLOSE),
            new ButtonType("PROCEED ANYWAY", ButtonBar.ButtonData.OK_DONE));
        alert.showAndWait().ifPresent(bt -> { if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) proceed.run(); });
    }

    private TextField styledField(String init, String prompt) {
        TextField tf = new TextField(init); tf.setPromptText(prompt);
        tf.getStyleClass().add("styled-field"); tf.setMaxWidth(Double.MAX_VALUE); return tf;
    }

    private Spinner<Double> numSp(double init, double min, double max, double step) {
        Spinner<Double> sp = new Spinner<>(min, max, init, step);
        sp.setEditable(true); sp.getStyleClass().add("styled-spinner");
        sp.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(sp, Priority.ALWAYS); return sp;
    }

    private void lockComboHeight(ComboBox<?> combo) {
        combo.setMinHeight(34);
        combo.setPrefHeight(34);
        combo.setMaxHeight(34);
        combo.setVisibleRowCount(4);
    }

    @Override public Node   getRoot()      { return root; }
    @Override public String getStepTitle() { return "Inverter Sizing"; }
    @Override public void   onEnter()      { recalculate(); }
    @Override public void   onLeave()      {}
}
