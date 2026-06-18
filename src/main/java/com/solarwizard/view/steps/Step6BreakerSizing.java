package com.solarwizard.view.steps;

import com.solarwizard.model.SolarProject;
import com.solarwizard.service.CalcService;
import com.solarwizard.util.UiUtils;
import com.solarwizard.view.WizardShell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;

/**
 * Step 8 - Circuit breaker sizing.
 */
public class Step6BreakerSizing implements WizardShell.StepView {

    private final SolarProject project;
    private final WizardShell shell;
    private final ScrollPane root = new ScrollPane();
    private final VBox mainContent = new VBox(16);

    private final Label[][] tableCells = new Label[4][5];
    private final Label formulaLbl = new Label();

    private static final String[] ROW_NAMES = {
        "Panels to SCC", "SCC to Battery", "Battery to Inverter", "Inverter to AC Load"
    };
    private static final String[] BREAKER_TYPES = { "DC MCB", "DC MCB", "DC MCB", "AC MCB" };

    public Step6BreakerSizing(SolarProject project, WizardShell shell) {
        this.project = project;
        this.shell = shell;
        build();
    }

    private void build() {
        mainContent.setPadding(new Insets(24, 28, 24, 28));
        mainContent.getStyleClass().add("step-content");
        mainContent.getChildren().add(UiUtils.stepTitle("8", "Step 8: Circuit Breaker Sizing", shell::showCurrentStepGuide));
        mainContent.getChildren().add(buildTable());
        HBox fb = UiUtils.formulaBanner("");
        fb.getChildren().set(1, formulaLbl);
        mainContent.getChildren().add(fb);
        mainContent.getChildren().add(UiUtils.warningBanner(
            "Use DC-rated breakers for PV, SCC, and battery circuits. AC breakers are only for AC load wiring."));
        mainContent.getChildren().add(buildOptionalComponents());
        mainContent.getChildren().add(buildNavRow());

        root.setContent(mainContent);
        root.setFitToWidth(true);
        root.getStyleClass().add("step-scroll");
    }

    private VBox buildTable() {
        double[] colWidths = { 165, 105, 125, 130, 85, 125 };
        String[] headers = { "Phase", "Current (A)", "Formula Current", "Breaker Size (A)", "Type", "Voltage" };

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

            for (int c = 0; c < 5; c++) {
                tableCells[r][c] = new Label("-");
                tableCells[r][c].getStyleClass().add("table-cell");
                if (c == 2) tableCells[r][c].getStyleClass().add("breaker-size");
                if (c == 3) tableCells[r][c].getStyleClass().add(r < 3 ? "badge-dc" : "badge-ac");
                tableCells[r][c].setPrefWidth(colWidths[c + 1]);
                tableCells[r][c].setMinWidth(colWidths[c + 1]);
                tableCells[r][c].setAlignment(Pos.CENTER);
                tableCells[r][c].setTextAlignment(TextAlignment.CENTER);
                dataRow.getChildren().add(tableCells[r][c]);
            }
            tableBox.getChildren().add(dataRow);
        }
        return tableBox;
    }

    private VBox buildOptionalComponents() {
        Label title = new Label("Optional Protection Components");
        title.getStyleClass().add("section-title");

        HBox row1 = new HBox(12,
            protCard("DC Surge Protection Device (SPD)",
                "Location", "PV array to SCC",
                "Type", "Type II DC SPD",
                "Purpose", "Lightning and surge protection"),
            protCard("AC Surge Protection Device (SPD)",
                "Location", "Inverter AC output / main panel",
                "Type", "Type II AC SPD",
                "Purpose", "Grid surge and transient protection")
        );
        HBox row2 = new HBox(12,
            protCard("Automatic Transfer Switch (ATS)",
                "Location", "Between inverter output and grid/genset",
                "Rating", "Based on AC output current",
                "Purpose", "Automatic source transfer"),
            protCard("DC Isolator / Disconnect Switch",
                "Location", "PV array, SCC, and battery side",
                "Rating", "Based on each DC phase current",
                "Purpose", "Manual maintenance disconnect")
        );
        row1.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        row2.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        return new VBox(12, title, row1, row2);
    }

    private VBox protCard(String title, String... kvPairs) {
        Label t = new Label(title);
        t.getStyleClass().add("specs-title");
        VBox card = new VBox(0);
        card.getStyleClass().add("specs-card");
        card.setPadding(new Insets(14));
        card.getChildren().add(t);
        for (int i = 0; i < kvPairs.length; i += 2) {
            Label k = new Label(kvPairs[i]);
            k.getStyleClass().add("spec-key");
            Label v = new Label(kvPairs[i + 1]);
            v.getStyleClass().addAll("spec-value", "spec-highlight");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            HBox row = new HBox(k, sp, v);
            row.getStyleClass().add("spec-row");
            row.setPadding(new Insets(7, 0, 7, 0));
            card.getChildren().add(row);
        }
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private void recalculate() {
        CalcService.calculate(project);

        double inverterWatts = inverterWatts();
        double batteryVoltage = project.getBatteryVoltage();

        double phase1Current = project.getResultArrayIsc();
        double phase1Minimum = phase1Current * 1.25 * 1.25;

        double phase2Current = phase2Current();
        double phase2Minimum = phase2Current * 1.25;

        double phase3Current = batteryVoltage > 0 ? inverterWatts / batteryVoltage : 0;
        double phase3Minimum = phase3Current * 1.25;

        double phase4Current = CalcService.inverterAcLoadCurrent(inverterWatts);
        double phase4Minimum = phase4Current * 1.25;

        fillRow(0, phase1Current, phase1Minimum, CalcService.standardBreakerSize(phase1Minimum), "DC MCB", "PV DC");
        fillRow(1, phase2Current, phase2Minimum, CalcService.standardBreakerSize(phase2Minimum), "DC MCB", UiUtils.fmt0(batteryVoltage) + "V DC");
        fillRow(2, phase3Current, phase3Minimum, CalcService.standardBreakerSize(phase3Minimum), "DC MCB", UiUtils.fmt0(batteryVoltage) + "V DC");
        fillRow(3, phase4Current, phase4Minimum, CalcService.standardBreakerSize(phase4Minimum), "AC MCB",
            UiUtils.fmt0(CalcService.AC_LOAD_VOLTAGE) + "V AC");

        formulaLbl.setText(
            "Phase 1: Isc_adjusted x 1.25 x 1.25. Phase 2: SCC rated current x 1.25 preferred. Phase 3/4: inverter current x 1.25.");
        formulaLbl.getStyleClass().add("formula-text");
    }

    private double phase2Current() {
        if (project.getChargeControllerRatedCurrent() > 0) {
            return project.getChargeControllerRatedCurrent();
        }
        return project.getBatteryVoltage() > 0
            ? project.getResultTotalPvW() / project.getBatteryVoltage()
            : 0;
    }

    private double inverterWatts() {
        return project.getInverterRatedPower() > 0
            ? project.getInverterRatedPower()
            : project.getResultInverterMinW();
    }

    private void fillRow(int row, double current, double formulaCurrent, int breakerSize, String type, String voltage) {
        tableCells[row][0].setText(UiUtils.fmt1(current));
        tableCells[row][1].setText(UiUtils.fmt1(formulaCurrent));
        tableCells[row][2].setText(breakerSize + "A");
        tableCells[row][3].setText(type);
        tableCells[row][4].setText(voltage);
    }

    private HBox buildNavRow() {
        Button back = new Button("BACK");
        back.getStyleClass().add("nav-back-btn");
        back.setOnAction(e -> shell.prevStep());

        Button next = new Button("VIEW REPORT");
        next.getStyleClass().add("nav-next-btn");
        next.setOnAction(e -> shell.nextStep());

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(8, back, sp, next);
        row.setPadding(new Insets(16, 0, 0, 0));
        return row;
    }

    @Override public Node getRoot() { return root; }
    @Override public String getStepTitle() { return "Circuit Breakers"; }
    @Override public void onEnter() { recalculate(); }
    @Override public void onLeave() {}
}
