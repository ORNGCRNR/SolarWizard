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
 * Step 2 - computes the total energy requirement from the appliance list.
 */
public class Step2EnergyRequirement implements WizardShell.StepView {

    private final SolarProject project;
    private final WizardShell shell;
    private final ScrollPane root = new ScrollPane();
    private final VBox mainContent = new VBox(16);

    private final Label lblDailyWh = new Label("0 Wh");
    private final Label lblExpectedDailyWh = new Label("0 Wh/day");
    private final Label lblTotalRequirement = new Label("0 Wh");
    private final Label formulaLbl = new Label();

    public Step2EnergyRequirement(SolarProject project, WizardShell shell) {
        this.project = project;
        this.shell = shell;
        build();
    }

    private void build() {
        mainContent.setPadding(new Insets(24, 28, 24, 28));
        mainContent.getStyleClass().add("step-content");
        mainContent.getChildren().add(UiUtils.stepTitle("2", "Step 2: Compute Total Daily Energy"));
        mainContent.getChildren().add(buildInputForm());
        mainContent.getChildren().add(buildResultTiles());
        HBox fb = UiUtils.formulaBanner("");
        fb.getChildren().set(1, formulaLbl);
        mainContent.getChildren().add(fb);
        mainContent.getChildren().add(UiUtils.warningBanner(
            "The reference flow recommends at least 2 days of autonomy for rainy-season planning."));
        mainContent.getChildren().add(buildNavRow());

        root.setContent(mainContent);
        root.setFitToWidth(true);
        root.getStyleClass().add("step-scroll");
    }

    private VBox buildInputForm() {
        Spinner<Double> spLoss = numSp(project.getSystemLossPercent(), 0, 100, 1);
        spLoss.valueProperty().addListener((o, a, n) -> {
            project.setSystemLossPercent(n);
            recalculate();
        });

        Spinner<Double> spDays = numSp(project.getAutonomyDays(), 0.5, 30, 0.5);
        spDays.valueProperty().addListener((o, a, n) -> {
            project.setAutonomyDays(n);
            recalculate();
        });

        HBox row = new HBox(12,
            UiUtils.labeledField("System Loss Buffer (%)", spLoss, "Reference value: 30%"),
            UiUtils.labeledField("Days of Autonomy", spDays, "Battery backup days without solar input"));
        row.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        VBox form = new VBox(14, row);
        form.getStyleClass().add("card");
        form.setPadding(new Insets(16));
        return form;
    }

    private HBox buildResultTiles() {
        lblDailyWh.getStyleClass().addAll("metric-value", "metric-blue");
        lblExpectedDailyWh.getStyleClass().addAll("metric-value", "metric-blue");
        lblTotalRequirement.getStyleClass().addAll("metric-value", "metric-green");

        HBox row = new HBox(12,
            makeTile("Total Daily Consumption", lblDailyWh),
            makeTile("Expected Daily Consumption", lblExpectedDailyWh),
            makeTile("Total Energy Requirement", lblTotalRequirement));
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

    private Spinner<Double> numSp(double init, double min, double max, double step) {
        Spinner<Double> sp = new Spinner<>(min, max, init, step);
        sp.setEditable(true);
        sp.getStyleClass().add("styled-spinner");
        sp.setMaxWidth(Double.MAX_VALUE);
        return sp;
    }

    private void recalculate() {
        CalcService.calculate(project);

        lblDailyWh.setText(UiUtils.fmt0(project.getResultDailyWh()) + " Wh");
        lblExpectedDailyWh.setText(UiUtils.fmt0(project.getResultExpectedDailyWh()) + " Wh/day");
        lblTotalRequirement.setText(UiUtils.fmt0(project.getResultTotalEnergyRequirementWh()) + " Wh");

        formulaLbl.setText(String.format(
            "Expected Daily = %.0f Wh x %.2f = %.0f Wh/day  |  Total Requirement = %.0f Wh/day x %.1f days = %.0f Wh",
            project.getResultDailyWh(),
            1.0 + (project.getSystemLossPercent() / 100.0),
            project.getResultExpectedDailyWh(),
            project.getResultExpectedDailyWh(),
            project.getAutonomyDays(),
            project.getResultTotalEnergyRequirementWh()));
        formulaLbl.getStyleClass().add("formula-text");
    }

    private HBox buildNavRow() {
        Button back = new Button("BACK");
        back.getStyleClass().add("nav-back-btn");
        back.setOnAction(e -> shell.prevStep());

        Button next = new Button("NEXT");
        next.getStyleClass().add("nav-next-btn");
        next.setOnAction(e -> shell.nextStep());

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(8, back, sp, next);
        row.setPadding(new Insets(16, 0, 0, 0));
        return row;
    }

    @Override public Node getRoot() { return root; }
    @Override public String getStepTitle() { return "Energy Requirement"; }
    @Override public void onEnter() { recalculate(); }
    @Override public void onLeave() {}
}
