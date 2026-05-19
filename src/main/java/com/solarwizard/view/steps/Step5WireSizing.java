package com.solarwizard.view.steps;

import com.solarwizard.model.SolarProject;
import com.solarwizard.service.CalcService;
import com.solarwizard.service.CalcService.AwgEntry;
import com.solarwizard.service.ValidationService;
import com.solarwizard.util.UiUtils;
import com.solarwizard.view.WizardShell;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Step 7 - Wire sizing using the VDI method from the decision flow.
 */
public class Step5WireSizing implements WizardShell.StepView {

    private final SolarProject project;
    private final WizardShell shell;
    private final ScrollPane root = new ScrollPane();
    private final VBox mainContent = new VBox(16);
    private final VBox warningBox = new VBox(6);

    private final Label[][] tableCells = new Label[4][6];
    private final Label formulaLbl = new Label();

    private static final String[] ROW_NAMES = {
        "Panels to SCC", "SCC to Battery", "Battery to Inverter", "Inverter to AC Load"
    };

    public Step5WireSizing(SolarProject project, WizardShell shell) {
        this.project = project;
        this.shell = shell;
        build();
    }

    private void build() {
        mainContent.setPadding(new Insets(24, 28, 24, 28));
        mainContent.getStyleClass().add("step-content");
        mainContent.getChildren().add(UiUtils.stepTitle("7", "Step 7: Wire Sizing (VDI Method)"));
        mainContent.getChildren().add(warningBox);
        mainContent.getChildren().add(buildInputGrid());
        mainContent.getChildren().add(buildTable());
        HBox fb = UiUtils.formulaBanner("");
        fb.getChildren().set(1, formulaLbl);
        mainContent.getChildren().add(fb);
        mainContent.getChildren().add(buildNavRow());

        root.setContent(mainContent);
        root.setFitToWidth(true);
        root.getStyleClass().add("step-scroll");
    }

    private VBox buildInputGrid() {
        Spinner<Double> spPvDist = numSp(project.getWirePvToInverterM(), 1, 9999, 1);
        spPvDist.valueProperty().addListener((o, a, n) -> {
            project.setWirePvToInverterM(n);
            recalculate();
        });

        Spinner<Double> spSccDist = numSp(project.getWireSccToBatteryM(), 1, 9999, 1);
        spSccDist.valueProperty().addListener((o, a, n) -> {
            project.setWireSccToBatteryM(n);
            recalculate();
        });

        Spinner<Double> spBattDist = numSp(project.getWireBatteryToInverterM(), 1, 9999, 1);
        spBattDist.valueProperty().addListener((o, a, n) -> {
            project.setWireBatteryToInverterM(n);
            recalculate();
        });

        Spinner<Double> spLoadDist = numSp(project.getWireInverterToLoadM(), 1, 9999, 1);
        spLoadDist.valueProperty().addListener((o, a, n) -> {
            project.setWireInverterToLoadM(n);
            recalculate();
        });

        Spinner<Double> spVDrop = numSp(project.getWireVoltageDrop(), 0.5, 10, 0.5);
        spVDrop.valueProperty().addListener((o, a, n) -> {
            project.setWireVoltageDrop(n);
            recalculate();
        });

        HBox row1 = new HBox(12,
            UiUtils.labeledField("Panels to SCC (m)", spPvDist, "Distance from array to charge controller"),
            UiUtils.labeledField("SCC to Battery (m)", spSccDist, "Distance from controller to battery bank"),
            UiUtils.labeledField("Battery to Inverter (m)", spBattDist, "Keep this run short"));
        row1.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        HBox row2 = new HBox(12,
            UiUtils.labeledField("Inverter to AC Load (m)", spLoadDist, "Distance from inverter to main AC load"),
            UiUtils.labeledField("DC Voltage Drop (%)", spVDrop, "Reference uses 2% for DC phases"));
        row2.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        VBox form = new VBox(14, row1, row2);
        form.getStyleClass().add("card");
        form.setPadding(new Insets(16));
        return form;
    }

    private VBox buildTable() {
        double[] colWidths = { 160, 95, 95, 100, 80, 90, 100 };
        String[] headers = { "Phase", "Current (A)", "Voltage (V)", "Distance (m)", "VDI", "Wire (AWG)", "Wire (mm2)" };

        HBox headerRow = new HBox();
        headerRow.getStyleClass().add("device-table-header");
        headerRow.setPadding(new Insets(10, 12, 10, 12));
        for (int i = 0; i < headers.length; i++) {
            Label h = new Label(headers[i]);
            h.getStyleClass().add("col-header");
            h.setPrefWidth(colWidths[i]);
            h.setMinWidth(colWidths[i]);
            h.setWrapText(true);
            headerRow.getChildren().add(h);
        }

        VBox tableBox = new VBox(0, headerRow);
        tableBox.getStyleClass().add("wire-table");

        for (int r = 0; r < ROW_NAMES.length; r++) {
            HBox dataRow = new HBox();
            dataRow.getStyleClass().add(r % 2 == 0 ? "table-row-even" : "table-row-odd");
            dataRow.setPadding(new Insets(10, 12, 10, 12));

            Label nameCell = new Label(ROW_NAMES[r]);
            nameCell.getStyleClass().add("table-cell");
            nameCell.setPrefWidth(colWidths[0]);
            nameCell.setMinWidth(colWidths[0]);
            dataRow.getChildren().add(nameCell);

            for (int c = 0; c < 6; c++) {
                tableCells[r][c] = new Label("-");
                tableCells[r][c].getStyleClass().add("table-cell");
                if (c == 4) tableCells[r][c].getStyleClass().add("awg-value");
                tableCells[r][c].setPrefWidth(colWidths[c + 1]);
                tableCells[r][c].setMinWidth(colWidths[c + 1]);
                dataRow.getChildren().add(tableCells[r][c]);
            }
            tableBox.getChildren().add(dataRow);
        }
        return tableBox;
    }

    private void recalculate() {
        CalcService.calculate(project);

        double inverterWatts = inverterWatts();
        double batteryVoltage = project.getBatteryVoltage();

        fillRow(0, project.getResultArrayImp(), project.getResultArrayVmp(),
            project.getWirePvToInverterM(), project.getWireVoltageDrop());
        fillRow(1, batteryVoltage > 0 ? project.getResultTotalPvW() / batteryVoltage : 0,
            batteryVoltage, project.getWireSccToBatteryM(), project.getWireVoltageDrop());
        fillRow(2, batteryVoltage > 0 ? inverterWatts / batteryVoltage : 0,
            batteryVoltage, project.getWireBatteryToInverterM(), project.getWireVoltageDrop());
        fillRow(3, inverterWatts / 220.0, 220.0,
            project.getWireInverterToLoadM(), 1.0);

        formulaLbl.setText("VDI = (Current x Distance_ft) / (Voltage x %VoltageDrop). Use the next higher VDI from the AWG chart.");
        formulaLbl.getStyleClass().add("formula-text");

        warningBox.getChildren().clear();
        for (ValidationService.Warning w : ValidationService.validateWiring(project)) {
            warningBox.getChildren().add(
                w.severity() == ValidationService.Warning.Severity.ERROR
                    ? UiUtils.errorBanner(w.message(), () -> warningBox.getChildren().clear())
                    : UiUtils.warningBanner(w.message()));
        }
    }

    private double inverterWatts() {
        return project.getInverterRatedPower() > 0
            ? project.getInverterRatedPower()
            : project.getResultInverterMinW();
    }

    private void fillRow(int row, double current, double voltage, double distM, double voltageDropPct) {
        double distFt = CalcService.metresToFeet(distM);
        double vdi = CalcService.vdi(current, distFt, voltage, voltageDropPct);
        AwgEntry entry = CalcService.lookupAwg(vdi);
        tableCells[row][0].setText(UiUtils.fmt1(current));
        tableCells[row][1].setText(UiUtils.fmt1(voltage));
        tableCells[row][2].setText(UiUtils.fmt0(distM));
        tableCells[row][3].setText(UiUtils.fmt1(vdi));
        tableCells[row][4].setText(String.valueOf(entry.awg()));
        tableCells[row][5].setText(UiUtils.fmt2(entry.areaMm2()));
    }

    private Spinner<Double> numSp(double init, double min, double max, double step) {
        Spinner<Double> sp = new Spinner<>(min, max, init, step);
        sp.setEditable(true);
        sp.getStyleClass().add("styled-spinner");
        sp.setMaxWidth(Double.MAX_VALUE);
        return sp;
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
    @Override public String getStepTitle() { return "Wire Sizing"; }
    @Override public void onEnter() { recalculate(); }
    @Override public void onLeave() {}
}
