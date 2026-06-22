package com.solarwizard.view.steps;

import com.solarwizard.model.SolarProject;
import com.solarwizard.service.CalcService;
import com.solarwizard.service.CalcService.AwgEntry;
import com.solarwizard.service.CalcService.PecEntry;
import com.solarwizard.service.ValidationService;
import com.solarwizard.util.UiUtils;
import com.solarwizard.view.WizardShell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;

/**
 * Step 7 - Wire sizing using the VDI method from the decision flow.
 */
public class Step5WireSizing implements WizardShell.StepView {

    private final SolarProject project;
    private final WizardShell shell;
    private final ScrollPane root = new ScrollPane();
    private final VBox mainContent = new VBox(16);
    private final VBox warningBox = new VBox(6);

    private final Label[][] tableCells = new Label[4][8];
    private final Label pecFormulaLbl = new Label();

    private static final String[] ROW_NAMES = {
        "Panels to SCC", "SCC to Battery", "Battery to Inverter", "Inverter to AC Load"
    };

    private static final String[] PEC_STANDARD_HEADERS = {
        "AWG",
        "Metric Size (mm²)",
        "Max Ampacity (75°C Copper)",
        "Overcurrent Protection Max (Breaker Size)"
    };

    private static final String[][] PEC_STANDARD_ROWS = {
        { "18 AWG", "0.75 mm²", "7 A", "7 A" },
        { "16 AWG", "1.31 mm²", "10 A", "10 A" },
        { "14 AWG", "2.0 mm²", "15 A", "15 A" },
        { "12 AWG", "3.5 mm²", "20 A", "20 A" },
        { "10 AWG", "5.5 mm²", "30 A", "30 A" },
        { "8 AWG", "8.0 mm²", "50 A", "40 A" },
        { "6 AWG", "14 mm²", "65 A", "55 A" },
        { "4 AWG", "22 mm²", "85 A", "80 A" },
        { "2 AWG", "30 mm²", "115 A", "100 A" },
        { "1 AWG", "38 mm²", "130 A", "125 A" },
        { "1/0 AWG", "50 mm²", "150 A", "150 A" },
        { "2/0 AWG", "60 mm²", "175 A", "175 A" },
        { "3/0 AWG", "80 mm²", "200 A", "200 A" },
        { "4/0 AWG", "100 mm²", "230 A", "225 A" },
        { "250 kcmil", "125 mm²", "255 A", "250 A" },
        { "300 kcmil", "150 mm²", "285 A", "300 A" },
        { "400 kcmil", "200 mm²", "335 A", "350 A" },
        { "500 kcmil", "250 mm²", "380 A", "400 A" }
    };

    public Step5WireSizing(SolarProject project, WizardShell shell) {
        this.project = project;
        this.shell = shell;
        build();
    }

    private void build() {
        mainContent.setPadding(new Insets(24, 28, 24, 28));
        mainContent.getStyleClass().add("step-content");
        mainContent.getChildren().add(UiUtils.stepTitle("7", "Step 7: Wire Sizing (VDI Method)", shell::showCurrentStepGuide));
        mainContent.getChildren().add(warningBox);
        mainContent.getChildren().add(buildInputGrid());
        mainContent.getChildren().add(buildTable());
        mainContent.getChildren().add(buildPecStandardsTable());

        HBox pecFb = UiUtils.formulaBanner("");
        pecFormulaLbl.setText("PEC Table 2.50.6.13 — Copper conductor minimum size based on overcurrent device rating.");
        pecFormulaLbl.getStyleClass().add("formula-text");
        pecFb.getChildren().set(1, pecFormulaLbl);
        mainContent.getChildren().add(pecFb);

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
        double[] colWidths = { 150, 88, 88, 92, 84, 112, 150, 72, 72 };
        String[] headers = {
            "Phase", "Current (A)", "Voltage (V)", "Distance (m)",
            "Wire (AWG)", "PEC Breaker (A)", "Wire Size mm²(mm dia.)",
            "VDrop (V)", "VDrop (%)"
        };

        HBox headerRow = new HBox();
        headerRow.getStyleClass().add("device-table-header");
        headerRow.setPadding(new Insets(10, 12, 10, 12));
        for (int i = 0; i < headers.length; i++) {
            Label h = new Label(headers[i]);
            h.getStyleClass().add("col-header");
            h.setPrefWidth(colWidths[i]);
            h.setMinWidth(colWidths[i]);
            h.setWrapText(true);
            h.setAlignment(Pos.CENTER);
            h.setTextAlignment(TextAlignment.CENTER);
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
            nameCell.setAlignment(Pos.CENTER);
            nameCell.setTextAlignment(TextAlignment.CENTER);
            dataRow.getChildren().add(nameCell);

            for (int c = 0; c < 8; c++) {
                tableCells[r][c] = new Label("-");
                tableCells[r][c].getStyleClass().add("table-cell");
                if (c == 3) tableCells[r][c].getStyleClass().add("awg-value");
                tableCells[r][c].setPrefWidth(colWidths[c + 1]);
                tableCells[r][c].setMinWidth(colWidths[c + 1]);
                tableCells[r][c].setWrapText(true);
                tableCells[r][c].setAlignment(Pos.CENTER);
                tableCells[r][c].setTextAlignment(TextAlignment.CENTER);
                dataRow.getChildren().add(tableCells[r][c]);
            }
            tableBox.getChildren().add(dataRow);
        }
        return tableBox;
    }

    private VBox buildPecStandardsTable() {
        Label title = new Label("PEC Standards with Ampere Rating Code");
        title.getStyleClass().add("table-title");

        double[] colWidths = { 140, 150, 190, 260 };

        HBox headerRow = new HBox();
        headerRow.getStyleClass().add("device-table-header");
        headerRow.setPadding(new Insets(10, 12, 10, 12));
        for (int i = 0; i < PEC_STANDARD_HEADERS.length; i++) {
            headerRow.getChildren().add(tableLabel(PEC_STANDARD_HEADERS[i], colWidths[i], "col-header"));
        }

        VBox tableBox = new VBox(0, headerRow);
        tableBox.getStyleClass().add("wire-table");

        for (int r = 0; r < PEC_STANDARD_ROWS.length; r++) {
            HBox dataRow = new HBox();
            dataRow.getStyleClass().add(r % 2 == 0 ? "table-row-even" : "table-row-odd");
            dataRow.setPadding(new Insets(10, 12, 10, 12));

            for (int c = 0; c < PEC_STANDARD_ROWS[r].length; c++) {
                dataRow.getChildren().add(tableLabel(PEC_STANDARD_ROWS[r][c], colWidths[c], "table-cell"));
            }
            tableBox.getChildren().add(dataRow);
        }

        return new VBox(8, title, tableBox);
    }

    private Label tableLabel(String text, double width, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setPrefWidth(width);
        label.setMinWidth(width);
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.CENTER);
        return label;
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
        fillRow(3, CalcService.inverterAcLoadCurrent(inverterWatts), CalcService.AC_LOAD_VOLTAGE,
            project.getWireInverterToLoadM(), 1.0);

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
        int breakerA = CalcService.standardBreakerSize(current);
        PecEntry pecEntry = CalcService.lookupPecCopper(breakerA);

        tableCells[row][0].setText(UiUtils.fmt1(current));
        tableCells[row][1].setText(UiUtils.fmt1(voltage));
        tableCells[row][2].setText(UiUtils.fmt0(distM));
        tableCells[row][3].setText(String.valueOf(entry.awg()));
        tableCells[row][4].setText(UiUtils.fmt0(breakerA) + " A");
        tableCells[row][5].setText(pecEntry != null
            ? UiUtils.fmt1(pecEntry.mmDia()) + " mm²"
            : "—");

        double vdropV   = CalcService.voltageDropVolts(current, distM, entry.areaMm2());
        double vdropPct = CalcService.voltageDropPercent(vdropV, voltage);
        tableCells[row][6].setText(UiUtils.fmt2(vdropV) + " V");
        tableCells[row][7].setText(UiUtils.fmt2(vdropPct) + "%");
    }

    private Spinner<Double> numSp(double init, double min, double max, double step) {
        Spinner<Double> sp = UiUtils.doubleSpinner(min, max, init, step);
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
