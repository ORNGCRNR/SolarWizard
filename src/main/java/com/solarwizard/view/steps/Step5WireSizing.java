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
 * Step 5 — Wire Sizing (VDI Method)
 */
public class Step5WireSizing implements WizardShell.StepView {

    private final SolarProject project;
    private final WizardShell  shell;
    private final ScrollPane   root = new ScrollPane();
    private final VBox         mainContent = new VBox(16);
    private final VBox         warningBox  = new VBox(6);

    private Spinner<Double> spPvDist, spBattDist, spLoadDist, spVDrop;
    private final Label[][] tableCells = new Label[3][6];
    private final Label formulaLbl = new Label();

    private static final String[] ROW_NAMES = {
        "PV to Inverter", "Battery to Inverter", "Inverter to Load"
    };

    public Step5WireSizing(SolarProject project, WizardShell shell) {
        this.project = project;
        this.shell   = shell;
        build();
    }

    private void build() {
        mainContent.setPadding(new Insets(24, 28, 24, 28));
        mainContent.getStyleClass().add("step-content");
        mainContent.getChildren().add(UiUtils.stepTitle("〰", "Step 5: Wire Sizing (VDI Method)"));
        mainContent.getChildren().add(warningBox);
        mainContent.getChildren().add(buildInputRow());
        mainContent.getChildren().add(buildTable());
        HBox fb = UiUtils.formulaBanner(""); fb.getChildren().set(1, formulaLbl);
        mainContent.getChildren().add(fb);
        mainContent.getChildren().add(buildNavRow());
        root.setContent(mainContent);
        root.setFitToWidth(true);
        root.getStyleClass().add("step-scroll");
    }

    private HBox buildInputRow() {
        spPvDist   = numSp(project.getWirePvToInverterM(),      1, 9999, 1);
        spBattDist = numSp(project.getWireBatteryToInverterM(), 1, 9999, 1);
        spLoadDist = numSp(project.getWireInverterToLoadM(),    1, 9999, 1);
        spVDrop    = numSp(project.getWireVoltageDrop(),        0.5, 10, 0.5);

        spPvDist  .valueProperty().addListener((o, a, n) -> { project.setWirePvToInverterM(n);      recalculate(); });
        spBattDist.valueProperty().addListener((o, a, n) -> { project.setWireBatteryToInverterM(n); recalculate(); });
        spLoadDist.valueProperty().addListener((o, a, n) -> { project.setWireInverterToLoadM(n);    recalculate(); });
        spVDrop   .valueProperty().addListener((o, a, n) -> { project.setWireVoltageDrop(n);        recalculate(); });

        HBox row = new HBox(16,
            UiUtils.labeledField("PV to Inverter (m)",      spPvDist,   "Distance from panels to inverter"),
            UiUtils.labeledField("Battery to Inverter (m)", spBattDist, "Keep as short as possible"),
            UiUtils.labeledField("Inverter to Load (m)",    spLoadDist, "Distance from inverter to load panel"),
            UiUtils.labeledField("Voltage Drop (%)",         spVDrop,    "Recommended: ≤2% DC, ≤1% AC"));
        return row;
    }

    private VBox buildTable() {
        double[] colWidths = { 150, 90, 90, 100, 80, 90, 100 };
        String[] headers = { "Section", "Current (A)", "Voltage (V)", "Distance (m)", "VDI", "Wire (AWG)", "Wire (mm²)" };
        HBox headerRow = new HBox();
        headerRow.getStyleClass().add("device-table-header");
        headerRow.setPadding(new Insets(10, 12, 10, 12));

        Label nameHdr = new Label(headers[0]);
        nameHdr.getStyleClass().add("col-header");
        nameHdr.setPrefWidth(colWidths[0]);
        nameHdr.setMinWidth(colWidths[0]);
        headerRow.getChildren().add(nameHdr);
        for (int i = 1; i < headers.length; i++) {
            Label h = new Label(headers[i]);
            h.getStyleClass().add("col-header");
            h.setPrefWidth(colWidths[i]);
            h.setMinWidth(colWidths[i]);
            h.setWrapText(true);
            headerRow.getChildren().add(h);
        }

        VBox tableBox = new VBox(0, headerRow);
        tableBox.getStyleClass().add("wire-table");

        for (int r = 0; r < 3; r++) {
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
                tableCells[r][c].setMaxWidth(Double.MAX_VALUE);
                dataRow.getChildren().add(tableCells[r][c]);
            }
            tableBox.getChildren().add(dataRow);
        }
        return tableBox;
    }

    private void recalculate() {
        CalcService.calculate(project);
        double pvI   = project.getPanelIsc();
        double battI = project.getResultDailyWh() > 0 && project.getBatteryVoltage() > 0
            ? (project.getResultDailyWh() / project.getBatteryVoltage()) / 8.0 : 10;
        double acI   = project.getResultInverterMinW() / 220.0;

        double pvV   = project.getPanelWattage() > 0 ? project.getPanelWattage() / Math.max(pvI, 0.01) : 12;
        double battV = project.getBatteryVoltage();
        double acV   = 220.0;
        double vdf   = project.getWireVoltageDrop() / 100.0;

        fillRow(0, pvI,   pvV,   project.getWirePvToInverterM(),      vdf);
        fillRow(1, battI, battV, project.getWireBatteryToInverterM(), vdf);
        fillRow(2, acI,   acV,   project.getWireInverterToLoadM(),    vdf);

        formulaLbl.setText("VDI = (Current × Distance_ft) / (% Voltage Drop × Voltage) → lookup AWG from table");
        formulaLbl.getStyleClass().add("formula-text");

        warningBox.getChildren().clear();
        for (ValidationService.Warning w : ValidationService.validateWiring(project)) {
            warningBox.getChildren().add(
                w.severity() == ValidationService.Warning.Severity.ERROR
                    ? UiUtils.errorBanner(w.message(), () -> warningBox.getChildren().clear())
                    : UiUtils.warningBanner(w.message()));
        }
    }

    private void fillRow(int row, double current, double voltage, double distM, double vDropFrac) {
        double distFt  = CalcService.metresToFeet(distM);
        double vdi     = CalcService.vdi(current, distFt, voltage, vDropFrac);
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
        sp.setEditable(true); sp.getStyleClass().add("styled-spinner"); return sp;
    }

    private HBox buildNavRow() {
        Button back = new Button("← BACK"); back.getStyleClass().add("nav-back-btn"); back.setOnAction(e -> shell.prevStep());
        Button next = new Button("NEXT →"); next.getStyleClass().add("nav-next-btn"); next.setOnAction(e -> shell.nextStep());
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        return new HBox(8, back, sp, next) {{ setPadding(new Insets(16, 0, 0, 0)); }};
    }

    @Override public Node   getRoot()      { return root; }
    @Override public String getStepTitle() { return "Wire Sizing"; }
    @Override public void   onEnter()      { recalculate(); }
    @Override public void   onLeave()      {}
}
