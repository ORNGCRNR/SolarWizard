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
import javafx.util.StringConverter;
import java.util.List;

/**
 * Step 3 - Battery Bank Sizing.
 * Uses Step 2 total energy requirement, then outputs recommended usable energy for panel sizing.
 */
public class Step4BatterySizing implements WizardShell.StepView {

    private final SolarProject project;
    private final WizardShell  shell;
    private final ScrollPane   root = new ScrollPane();
    private final VBox         mainContent = new VBox(16);
    private final VBox         warningBox  = new VBox(6);

    // Spec display labels
    private final Label specBrand    = new Label("Custom");
    private final Label specModel    = new Label("Custom Battery");
    private final Label specVoltage  = new Label("-");
    private final Label specCapacity = new Label("-");
    private final Label specDod      = new Label("-");

    // Result tiles
    private final Label lblSysVoltage   = new Label("0 V");
    private final Label lblReqAh        = new Label("0 Ah");
    private final Label lblTotalCap     = new Label("0 Ah");

    private final Label configBannerLbl = new Label();
    private final Label formulaLbl      = new Label();

    public Step4BatterySizing(SolarProject project, WizardShell shell) {
        this.project = project;
        this.shell   = shell;
        build();
    }

    private void build() {
        mainContent.setPadding(new Insets(24, 28, 24, 28));
        mainContent.getStyleClass().add("step-content");
        mainContent.getChildren().add(UiUtils.stepTitle("3", "Step 3: Battery Bank Sizing", shell::showCurrentStepGuide));
        mainContent.getChildren().add(warningBox);
        mainContent.getChildren().add(buildInputForm());
        mainContent.getChildren().add(buildSpecsCard());
        mainContent.getChildren().add(buildResultTiles());

        // Config banner
        HBox cb = UiUtils.formulaBanner(""); cb.getChildren().set(1, configBannerLbl);
        mainContent.getChildren().add(cb);

        // Formula banner
        HBox fb = UiUtils.formulaBanner(""); fb.getChildren().set(1, formulaLbl);
        mainContent.getChildren().add(fb);
        mainContent.getChildren().add(buildNavRow());

        root.setContent(mainContent);
        root.setFitToWidth(true);
        root.getStyleClass().add("step-scroll");
    }

    // ── Input form ────────────────────────────────────────────────────────────
    private VBox buildInputForm() {
        // Brand / Model
        TextField tfBrand = styledField(project.getBatteryBrand(), "Brand (e.g. Pylontech)");
        tfBrand.textProperty().addListener((o, a, n) -> { project.setBatteryBrand(n); updateSpecs(); });

        TextField tfModel = styledField(project.getBatteryModel(), "Model (e.g. US3000C)");
        tfModel.textProperty().addListener((o, a, n) -> { project.setBatteryModel(n); updateSpecs(); });

        HBox brandRow = new HBox(12, tfBrand, tfModel);
        HBox.setHgrow(tfBrand, Priority.ALWAYS);
        HBox.setHgrow(tfModel, Priority.ALWAYS);

        // Voltage dropdown
        ComboBox<Double> voltageBox = new ComboBox<>();
        voltageBox.getItems().addAll(12.0, 24.0, 48.0, 51.2);
        voltageBox.setValue(project.getBatteryVoltage());
        voltageBox.getStyleClass().add("panel-combo");
        voltageBox.setMaxWidth(Double.MAX_VALUE);
        voltageBox.setMinHeight(34);
        voltageBox.setPrefHeight(34);
        voltageBox.setMaxHeight(34);
        voltageBox.setVisibleRowCount(4);
        voltageBox.setConverter(new StringConverter<Double>() {
            @Override public String toString(Double v) {
                if (v == null) return "";
                return v == 51.2 ? "51.2V (LiFePO4)" : UiUtils.fmt0(v) + "V";
            }
            @Override public Double fromString(String s) { return null; }
        });
        voltageBox.valueProperty().addListener((o, a, n) -> {
            if (n != null) { project.setBatteryVoltage(n); recalculate(); }
        });

        // Capacity (Ah)
        Spinner<Double> spAh = numSp(project.getBatteryCapacityAh(), 1, 99999, 10);
        spAh.valueProperty().addListener((o, a, n) -> { project.setBatteryCapacityAh(n); recalculate(); });

        // Battery Quantity
        Spinner<Integer> spQty = new Spinner<>(1, 9999, project.getBatteryQuantity());
        spQty.setEditable(true);
        spQty.getStyleClass().add("styled-spinner");
        spQty.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(spQty, Priority.ALWAYS);
        spQty.valueProperty().addListener((o, a, n) -> { project.setBatteryQuantity(n); recalculate(); });

        HBox row1 = new HBox(12,
            UiUtils.labeledField("Nominal Voltage of Battery(V)",       voltageBox, "Common: 12V, 24V, 48V, 51.2V (LiFePO4)"),
            UiUtils.labeledField("Capacity (Ah)",     spAh,       null),
            UiUtils.labeledField("Battery Quantity",  spQty,      "Number of batteries in the bank"));
        row1.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        // DOD
        Spinner<Double> spDod = numSp(project.getBatteryDod(), 0.10, 1.00, 0.05);
        spDod.valueProperty().addListener((o, a, n) -> { project.setBatteryDod(n); recalculate(); });

        HBox row2 = new HBox(12,
            UiUtils.labeledField("Depth of Discharge (DOD)", spDod, "0.50 for lead-acid, 0.80 for LiFePO4"));
        row2.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        VBox form = new VBox(14, brandRow, row1, row2);
        form.getStyleClass().add("card");
        form.setPadding(new Insets(16));
        return form;
    }

    // ── Specs card ────────────────────────────────────────────────────────────
    private VBox buildSpecsCard() {
        Label title = new Label("🔋  Battery Specifications");
        title.getStyleClass().add("specs-title");

        VBox card = new VBox(0, title,
            specRow("Brand",    specBrand),
            specRow("Model",    specModel),
            specRow("Nominal Voltage",  specVoltage),
            specRow("Capacity", specCapacity),
            specRow("DOD",      specDod));
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
        specBrand   .setText(project.getBatteryBrand().isBlank() ? "Custom" : project.getBatteryBrand());
        specModel   .setText(project.getBatteryModel().isBlank() ? "Custom Battery" : project.getBatteryModel());
        specVoltage .setText(UiUtils.fmt1(project.getBatteryVoltage()) + "V");
        specCapacity.setText(UiUtils.fmt0(project.getBatteryCapacityAh()) + " Ah (" +
                             UiUtils.fmt0(project.getBatteryVoltage() * project.getBatteryCapacityAh()) + " Wh)");
        specDod     .setText(UiUtils.fmt0(project.getBatteryDod() * 100) + "%");
    }

    // ── Result tiles ──────────────────────────────────────────────────────────
    private VBox buildResultTiles() {
        lblSysVoltage .getStyleClass().addAll("metric-value", "metric-blue");
        lblReqAh      .getStyleClass().addAll("metric-value", "metric-orange");
        lblTotalCap   .getStyleClass().addAll("metric-value", "metric-blue");

        HBox row1 = new HBox(12,
            makeTile("System Voltage",    lblSysVoltage),
            makeTile("Required Capacity", lblReqAh),
            makeTile("Total AH of Bank",  lblTotalCap));
        row1.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        return new VBox(12, row1);
    }

    private VBox makeTile(String label, Label value) {
        Label lbl = new Label(label); lbl.getStyleClass().add("metric-label");
        VBox box = new VBox(4, lbl, value);
        box.getStyleClass().add("metric-tile"); box.setPadding(new Insets(14));
        return box;
    }

    // ── Calculation ───────────────────────────────────────────────────────────
    private void recalculate() {
        CalcService.calculate(project);
        int    batteries     = project.getResultBatteriesNeeded();
        double reqAh         = project.getResultBatteryAh();
        double totalAh       = batteries * project.getBatteryCapacityAh();
        double totalWh       = project.getResultBatteryBankEnergyWh();
        double recommendedWh = project.getResultRecommendedEnergyWh();

        lblSysVoltage .setText(UiUtils.fmt0(project.getBatteryVoltage()) + " V");
        lblReqAh      .setText(UiUtils.fmt0(reqAh) + " Ah");
        lblTotalCap   .setText(UiUtils.fmt0(totalAh) + " Ah");

        configBannerLbl.setText(String.format(
            "Battery output to next stage: Recommended Energy Consumption = %.0f Wh (Battery Energy %.0f Wh x DOD %.0f%%)",
            recommendedWh, totalWh, project.getBatteryDod() * 100));
        configBannerLbl.getStyleClass().add("formula-text");

        configBannerLbl.setText(String.format(
            "Battery output to next stage: Recommended Energy Consumption = %.0f Wh (Battery Energy %.0f Wh x DOD %.0f%%)",
            recommendedWh, totalWh, project.getBatteryDod() * 100));
        configBannerLbl.getStyleClass().add("formula-text");

        formulaLbl.setText(String.format(
            "Required AH = %.0f Wh / %.0f V = %.0f Ah  |  Total Bank = %d x %.0f Ah = %.0f Ah",
            project.getResultTotalEnergyRequirementWh(), project.getBatteryVoltage(), reqAh,
            batteries, project.getBatteryCapacityAh(), totalAh));
        formulaLbl.getStyleClass().add("formula-text");

        updateSpecs();
        warningBox.getChildren().clear();
        for (ValidationService.Warning w : ValidationService.validateBattery(project)) {
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
            List<ValidationService.Warning> warns = ValidationService.validateBattery(project);
            boolean hasError = warns.stream().anyMatch(w -> w.severity() == ValidationService.Warning.Severity.ERROR);
            if (hasError) showPrecautionDialog(warns, () -> shell.nextStep());
            else shell.nextStep();
        });
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        return new HBox(8, back, sp, next) {{ setPadding(new Insets(16, 0, 0, 0)); }};
    }

    private void showPrecautionDialog(List<ValidationService.Warning> warns, Runnable proceed) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Precautions"); alert.setHeaderText("⚠  Warnings:");
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

    @Override public Node   getRoot()      { return root; }
    @Override public String getStepTitle() { return "Battery Bank"; }
    @Override public void   onEnter()      { recalculate(); }
    @Override public void   onLeave()      {}
}
