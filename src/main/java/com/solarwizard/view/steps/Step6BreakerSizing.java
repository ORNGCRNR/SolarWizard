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
 * Step 6 — Breaker Sizing
 * Formula: Breaker = Current × 1.25, rounded to next standard size.
 */
public class Step6BreakerSizing implements WizardShell.StepView {

    private final SolarProject project;
    private final WizardShell  shell;
    private final ScrollPane   root = new ScrollPane();
    private final VBox         mainContent = new VBox(16);

    private final Label[][] tableCells = new Label[3][5];
    private final Label formulaLbl = new Label();

    private static final String[] ROW_NAMES     = { "PV DC Breaker", "Battery DC MCCB", "AC Breaker" };
    private static final String[] BREAKER_TYPES  = { "DC", "DC", "AC" };
    private static final String[] VOLTAGE_RATINGS= { "500–1000V DC", "48–60V DC", "220–240V AC" };

    public Step6BreakerSizing(SolarProject project, WizardShell shell) {
        this.project = project;
        this.shell   = shell;
        build();
    }

    private void build() {
        mainContent.setPadding(new Insets(24, 28, 24, 28));
        mainContent.getStyleClass().add("step-content");
        mainContent.getChildren().add(UiUtils.stepTitle("⚡", "Step 6: Breaker Sizing"));
        mainContent.getChildren().add(buildTable());
        HBox fb = UiUtils.formulaBanner(""); fb.getChildren().set(1, formulaLbl);
        mainContent.getChildren().add(fb);
        mainContent.getChildren().add(UiUtils.warningBanner(
            "Use DC-rated breakers for PV and battery circuits. Never use AC breakers for DC applications."));
        mainContent.getChildren().add(buildOptionalComponents());
        mainContent.getChildren().add(buildNavRow());
        root.setContent(mainContent);
        root.setFitToWidth(true);
        root.getStyleClass().add("step-scroll");
    }

    private VBox buildTable() {
        String[] headers = { "Breaker", "Current (A)", "× 1.25", "Breaker Size (A)", "Type", "Voltage Rating" };
        HBox headerRow = new HBox();
        headerRow.getStyleClass().add("device-table-header");
        headerRow.setPadding(new Insets(10, 12, 10, 12));
        Label nameHdr = new Label(headers[0]); nameHdr.getStyleClass().add("col-header"); nameHdr.setPrefWidth(160);
        headerRow.getChildren().add(nameHdr);
        for (int i = 1; i < headers.length; i++) {
            Label h = new Label(headers[i]); h.getStyleClass().add("col-header");
            HBox.setHgrow(h, Priority.ALWAYS); headerRow.getChildren().add(h);
        }

        VBox tableBox = new VBox(0, headerRow);
        tableBox.getStyleClass().add("wire-table");

        for (int r = 0; r < 3; r++) {
            HBox dataRow = new HBox();
            dataRow.getStyleClass().add(r % 2 == 0 ? "table-row-even" : "table-row-odd");
            dataRow.setPadding(new Insets(10, 12, 10, 12));
            Label nameCell = new Label(ROW_NAMES[r]);
            nameCell.getStyleClass().add("table-cell"); nameCell.setPrefWidth(160);
            dataRow.getChildren().add(nameCell);
            for (int c = 0; c < 5; c++) {
                tableCells[r][c] = new Label("-");
                tableCells[r][c].getStyleClass().add("table-cell");
                if (c == 2) tableCells[r][c].getStyleClass().add("breaker-size");
                if (c == 3) tableCells[r][c].getStyleClass().add(
                    BREAKER_TYPES[r].equals("DC") ? "badge-dc" : "badge-ac");
                HBox.setHgrow(tableCells[r][c], Priority.ALWAYS);
                dataRow.getChildren().add(tableCells[r][c]);
            }
            tableBox.getChildren().add(dataRow);
        }
        return tableBox;
    }

    private VBox buildOptionalComponents() {
        Label title = new Label("🛡  Optional Protection Components");
        title.getStyleClass().add("section-title");

        HBox row1 = new HBox(12,
            protCard("⚡", "DC Surge Protection Device (SPD)",
                "Location", "PV Array to Inverter DC input",
                "Type",     "Type II DC SPD",
                "Voltage",  "600V DC",
                "Purpose",  "Lightning & surge protection"),
            protCard("⚡", "AC Surge Protection Device (SPD)",
                "Location", "Inverter AC output / Main panel",
                "Type",     "Type II AC SPD",
                "Voltage",  "275V AC (single phase)",
                "Purpose",  "Grid surge & transient protection")
        );
        HBox row2 = new HBox(12,
            protCard("⇄", "Automatic Transfer Switch (ATS)",
                "Location", "Between inverter output & grid/genset",
                "Rating",   "Based on AC output current",
                "Poles",    "2P (single phase) / 4P (three phase)",
                "Purpose",  "Auto switch between solar & grid"),
            protCard("⇢", "DC Isolator / Disconnect Switch",
                "Location", "PV array & battery to inverter",
                "PV Iso.",  "Isc × 1.25 A, 600V DC",
                "Batt Iso.","Battery current × 1.25 A",
                "Purpose",  "Safe manual disconnect for maintenance")
        );
        row1.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        row2.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        HBox info = UiUtils.formulaBanner(
            "These components are highly recommended for safety and code compliance.");
        return new VBox(12, title, row1, row2, info);
    }

    private VBox protCard(String icon, String title, String... kvPairs) {
        Label t = new Label(icon + "  " + title); t.getStyleClass().add("specs-title");
        VBox card = new VBox(0); card.getStyleClass().add("specs-card"); card.setPadding(new Insets(14));
        card.getChildren().add(t);
        for (int i = 0; i < kvPairs.length; i += 2) {
            Label k = new Label(kvPairs[i]);   k.getStyleClass().add("spec-key");
            Label v = new Label(kvPairs[i+1]); v.getStyleClass().addAll("spec-value", "spec-highlight");
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            HBox row = new HBox(k, sp, v);
            row.getStyleClass().add("spec-row"); row.setPadding(new Insets(7, 0, 7, 0));
            card.getChildren().add(row);
        }
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private void recalculate() {
        CalcService.calculate(project);
        double pvI   = project.getPanelIsc();
        double battI = project.getResultDailyWh() > 0 && project.getBatteryVoltage() > 0
            ? (project.getResultDailyWh() / project.getBatteryVoltage()) / 8.0 : 10;
        double acI   = project.getResultInverterMinW() / 220.0;
        double[] currents = { pvI, battI, acI };

        for (int r = 0; r < 3; r++) {
            double c    = currents[r];
            double x125 = c * 1.25;
            int    size = CalcService.breakerSize(c);
            tableCells[r][0].setText(UiUtils.fmt1(c));
            tableCells[r][1].setText(UiUtils.fmt1(x125));
            tableCells[r][2].setText(size + "A");
            tableCells[r][3].setText(BREAKER_TYPES[r]);
            tableCells[r][4].setText(VOLTAGE_RATINGS[r]);
        }

        formulaLbl.setText(
            "Formula: Breaker = Current × 1.25 → round up to next standard size (10, 15, 20, 25, 30, 40, 50, 60, 80, 100A…)");
        formulaLbl.getStyleClass().add("formula-text");
    }

    private HBox buildNavRow() {
        Button back = new Button("← BACK"); back.getStyleClass().add("nav-back-btn"); back.setOnAction(e -> shell.prevStep());
        Button next = new Button("VIEW REPORT →"); next.getStyleClass().add("nav-next-btn"); next.setOnAction(e -> shell.nextStep());
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        return new HBox(8, back, sp, next) {{ setPadding(new Insets(16, 0, 0, 0)); }};
    }

    @Override public Node   getRoot()      { return root; }
    @Override public String getStepTitle() { return "Breaker Sizing"; }
    @Override public void   onEnter()      { recalculate(); }
    @Override public void   onLeave()      {}
}
