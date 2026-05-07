package com.solarwizard.view.steps;

import com.solarwizard.model.SolarProject;
import com.solarwizard.service.CalcService;
import com.solarwizard.util.UiUtils;
import com.solarwizard.view.WizardShell;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Step 2 — Solar Panel Sizing (Custom input only, no FROM LIST).
 * Clean 3-column aligned field layout.
 */
public class Step2PanelSizing implements WizardShell.StepView {

    private final SolarProject project;
    private final WizardShell  shell;
    private final ScrollPane   root = new ScrollPane();
    private final VBox         mainContent = new VBox(16);

    private final Label lblReqPv   = new Label("0 W");
    private final Label lblPanels  = new Label("0 pcs");
    private final Label lblTotalPv = new Label("0 W");
    private final Label formulaLbl = new Label();

    public Step2PanelSizing(SolarProject project, WizardShell shell) {
        this.project = project;
        this.shell   = shell;
        build();
    }

    private void build() {
        mainContent.setPadding(new Insets(24, 28, 24, 28));
        mainContent.getStyleClass().add("step-content");
        mainContent.getChildren().add(UiUtils.stepTitle("☀", "Step 2: Solar Panel Sizing"));
        mainContent.getChildren().add(buildForm());
        mainContent.getChildren().add(buildSafetyToggle());
        mainContent.getChildren().add(buildResultTiles());
        HBox fb = UiUtils.formulaBanner(""); fb.getChildren().set(1, formulaLbl);
        mainContent.getChildren().add(fb);
        mainContent.getChildren().add(buildNavRow());

        root.setContent(mainContent);
        root.setFitToWidth(true);
        root.getStyleClass().add("step-scroll");
    }

    private VBox buildForm() {
        // Brand / Model row
        TextField tfBrand = new TextField(project.getPanelBrand());
        tfBrand.setPromptText("Brand (e.g. Jinko Solar)");
        tfBrand.getStyleClass().add("styled-field");
        tfBrand.setMaxWidth(Double.MAX_VALUE);
        tfBrand.textProperty().addListener((o, a, n) -> project.setPanelBrand(n));

        TextField tfModel = new TextField(project.getPanelModel());
        tfModel.setPromptText("Model (e.g. Tiger Neo 600W)");
        tfModel.getStyleClass().add("styled-field");
        tfModel.setMaxWidth(Double.MAX_VALUE);
        tfModel.textProperty().addListener((o, a, n) -> project.setPanelModel(n));

        HBox brandRow = new HBox(12, tfBrand, tfModel);
        HBox.setHgrow(tfBrand, Priority.ALWAYS);
        HBox.setHgrow(tfModel, Priority.ALWAYS);

        // Row 1: Wattage | Voc | Vmp
        Spinner<Double> spW   = numSp(project.getPanelWattage(),    0, 99999, 10);
        Spinner<Double> spVoc = numSp(project.getPanelVoc(),        0, 999,   0.01);
        Spinner<Double> spVmp = numSp(project.getPanelVmp(),        0, 999,   0.01);

        spW  .valueProperty().addListener((o, a, n) -> { project.setPanelWattage(n); recalculate(); });
        spVoc.valueProperty().addListener((o, a, n) -> { project.setPanelVoc(n); });
        spVmp.valueProperty().addListener((o, a, n) -> { project.setPanelVmp(n); });

        HBox row1 = new HBox(12,
            UiUtils.labeledField("Wattage (W)", spW,   "Rated power output of the panel in Watts"),
            UiUtils.labeledField("Voc (V)",     spVoc, "Open-Circuit Voltage — max voltage when no load is connected"),
            UiUtils.labeledField("Vmp (V)",     spVmp, "Voltage at Maximum Power — operating voltage at peak output"));
        row1.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        // Row 2: Isc | Imp | Efficiency
        Spinner<Double> spIsc = numSp(project.getPanelIsc(),        0, 999,   0.01);
        Spinner<Double> spImp = numSp(project.getPanelImp(),        0, 999,   0.01);
        Spinner<Double> spEff = numSp(project.getPanelEfficiency(), 0, 100,   0.1);

        spIsc.valueProperty().addListener((o, a, n) -> project.setPanelIsc(n));
        spImp.valueProperty().addListener((o, a, n) -> project.setPanelImp(n));
        spEff.valueProperty().addListener((o, a, n) -> project.setPanelEfficiency(n));

        HBox row2 = new HBox(12,
            UiUtils.labeledField("Isc (A)",        spIsc, "Short-Circuit Current — max current when output is shorted"),
            UiUtils.labeledField("Imp (A)",        spImp, "Current at Maximum Power — operating current at peak output"),
            UiUtils.labeledField("Efficiency (%)", spEff, "How much sunlight the panel converts to electricity"));
        row2.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        VBox form = new VBox(14, brandRow, row1, row2);
        form.getStyleClass().add("card");
        form.setPadding(new Insets(16));
        return form;
    }

    private Spinner<Double> numSp(double init, double min, double max, double step) {
        Spinner<Double> sp = new Spinner<>(min, max, init, step);
        sp.setEditable(true); sp.getStyleClass().add("styled-spinner");
        sp.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(sp, Priority.ALWAYS);
        return sp;
    }

    private CheckBox buildSafetyToggle() {
        CheckBox cb = new CheckBox("Safety Factor (×1.25)");
        cb.getStyleClass().add("toggle-check");
        cb.setSelected(project.isPanelSafetyFactor());
        cb.selectedProperty().addListener((o, a, n) -> { project.setPanelSafetyFactor(n); recalculate(); });
        return cb;
    }

    private HBox buildResultTiles() {
        lblReqPv  .getStyleClass().addAll("metric-value", "metric-blue");
        lblPanels .getStyleClass().addAll("metric-value", "metric-blue");
        lblTotalPv.getStyleClass().addAll("metric-value", "metric-blue");
        HBox row = new HBox(12,
            makeTile("Required PV Power", lblReqPv),
            makeTile("Panels Needed",     lblPanels),
            makeTile("Total PV Power",    lblTotalPv));
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
        lblReqPv  .setText(UiUtils.fmt0(project.getResultRequiredPvW()) + " W");
        lblPanels .setText(project.getResultPanelsNeeded() + " pcs");
        lblTotalPv.setText(UiUtils.fmt0(project.getResultTotalPvW()) + " W");
        formulaLbl.setText(String.format(
            "PV Power = (%.0f Wh / %.1fh) × 1.25 = %.0f W  |  Panels = %.0f W / %.0f W = %d pcs",
            project.getResultDailyWh(), project.getSunPeakHours(),
            project.getResultRequiredPvW(), project.getResultRequiredPvW(),
            project.getPanelWattage(), project.getResultPanelsNeeded()));
        formulaLbl.getStyleClass().add("formula-text");
    }

    private HBox buildNavRow() {
        Button back = new Button("← BACK"); back.getStyleClass().add("nav-back-btn"); back.setOnAction(e -> shell.prevStep());
        Button next = new Button("NEXT →"); next.getStyleClass().add("nav-next-btn"); next.setOnAction(e -> shell.nextStep());
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(8, back, sp, next); row.setPadding(new Insets(16, 0, 0, 0));
        return row;
    }

    @Override public Node   getRoot()      { return root; }
    @Override public String getStepTitle() { return "Panel Sizing"; }
    @Override public void   onEnter()      { recalculate(); }
    @Override public void   onLeave()      {}
}
