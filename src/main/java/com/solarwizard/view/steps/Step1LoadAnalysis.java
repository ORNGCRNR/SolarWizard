package com.solarwizard.view.steps;

import com.solarwizard.model.Appliance;
import com.solarwizard.model.SolarProject;
import com.solarwizard.service.CalcService;
import com.solarwizard.util.UiUtils;
import com.solarwizard.view.WizardShell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Step 1 - appliance list and wattage capture.
 * Modes: DIRECT | BILL | DEVICE
 * Fixed column alignment in device table, scrollable chip bar.
 */
public class Step1LoadAnalysis implements WizardShell.StepView {

    // Fixed column widths - MUST match header and row exactly
    private static final double W_NAME  = 150;
    private static final double W_WATTS = 95;
    private static final double W_HOURS = 85;
    private static final double W_PEAK  = 80;
    private static final double W_DF    = 60;
    private static final double W_QTY   = 70;
    private static final double W_MOTOR = 80;
    private static final double W_KWH   = 95;
    private static final double W_DEL   = 36;

    private final SolarProject project;
    private final WizardShell  shell;
    private final ScrollPane   root = new ScrollPane();
    private final VBox         mainContent = new VBox(16);
    private final StackPane    modeContent = new StackPane();
    private final VBox         peakWarnBox = new VBox(6);

    private Button btnDirect, btnBill, btnDevice;

    // Result labels
    private final Label lblDailyWh       = new Label("0 Wh");
    private final Label lblDailyKwh      = new Label("0.00 kWh");
    private final Label lblHourlyAvg     = new Label("0 W");
    private final Label lblConnectedLoad = new Label("0 W");
    private final Label lblPeakLoad      = new Label("0 W");
    private final Label lblSystemDf      = new Label("0.00");
    private final Label lblRecInv        = new Label("0 W");
    private final Label formulaLbl       = new Label();

    public Step1LoadAnalysis(SolarProject project, WizardShell shell) {
        this.project = project;
        this.shell   = shell;
        build();
    }

    private void build() {
        mainContent.setPadding(new Insets(24, 28, 24, 28));
        mainContent.getStyleClass().add("step-content");
        mainContent.getChildren().add(UiUtils.stepTitle("1", "Step 1: List Appliances and Wattage"));
        mainContent.getChildren().add(modeContent);
        mainContent.getChildren().add(buildResultTiles());
        mainContent.getChildren().add(buildPeakSummaryCard());
        HBox fb = UiUtils.formulaBanner(""); fb.getChildren().set(1, formulaLbl);
        mainContent.getChildren().add(fb);
        mainContent.getChildren().add(buildNavRow());

        root.setContent(mainContent);
        root.setFitToWidth(true);
        root.getStyleClass().add("step-scroll");
        project.setLoadMode(SolarProject.LoadMode.DEVICE);
        modeContent.getChildren().setAll(buildDeviceMode());
        recalculate();
    }

    // -- Mode bar -----------------------------------------------------------------
    private HBox buildModeBar() {
        btnDirect = UiUtils.modeButton("✎  DIRECT");
        btnBill   = UiUtils.modeButton("⊟  BILL");
        btnDevice = UiUtils.modeButton("⊞  DEVICE");
        btnDirect.setOnAction(e -> setMode(SolarProject.LoadMode.DIRECT));
        btnBill  .setOnAction(e -> setMode(SolarProject.LoadMode.BILL));
        btnDevice.setOnAction(e -> setMode(SolarProject.LoadMode.DEVICE));
        HBox bar = new HBox(4, btnDirect, btnBill, btnDevice);
        bar.getStyleClass().add("mode-bar");
        return bar;
    }

    private void setMode(SolarProject.LoadMode mode) {
        project.setLoadMode(mode);
        btnDirect.getStyleClass().remove("mode-btn-active");
        btnBill  .getStyleClass().remove("mode-btn-active");
        btnDevice.getStyleClass().remove("mode-btn-active");
        switch (mode) {
            case DIRECT -> { btnDirect.getStyleClass().add("mode-btn-active"); modeContent.getChildren().setAll(buildDirectMode()); }
            case BILL   -> { btnBill  .getStyleClass().add("mode-btn-active"); modeContent.getChildren().setAll(buildBillMode()); }
            case DEVICE -> { btnDevice.getStyleClass().add("mode-btn-active"); modeContent.getChildren().setAll(buildDeviceMode()); }
        }
        recalculate();
    }

    // -- DIRECT -------------------------------------------------------------------
    private Node buildDirectMode() {
        Spinner<Double> spM = UiUtils.doubleSpinner(0, 99999, project.getMonthlyKwh(), 10);
        spM.valueProperty().addListener((o, a, n) -> { project.setMonthlyKwh(n); recalculate(); });
        Spinner<Double> spP = UiUtils.doubleSpinner(0.5, 12, project.getSunPeakHours(), 0.5);
        spP.valueProperty().addListener((o, a, n) -> { project.setSunPeakHours(n); recalculate(); });
        HBox row = new HBox(16,
            UiUtils.labeledField("Monthly Consumption (kWh)", spM, "Check your electric bill"),
            UiUtils.labeledField("Sun Peak Hours",            spP, "Philippines average: 4-5 hours"));
        return row;
    }

    // -- BILL ---------------------------------------------------------------------
    private Node buildBillMode() {
        Spinner<Double> spB = UiUtils.doubleSpinner(0, 999999, project.getMonthlyBill(), 100);
        spB.valueProperty().addListener((o, a, n) -> { project.setMonthlyBill(n); recalculate(); });
        Spinner<Double> spR = UiUtils.doubleSpinner(0, 100, project.getRatePerKwh(), 0.5);
        spR.valueProperty().addListener((o, a, n) -> { project.setRatePerKwh(n); recalculate(); });
        Spinner<Double> spP = UiUtils.doubleSpinner(0.5, 12, project.getSunPeakHours(), 0.5);
        spP.valueProperty().addListener((o, a, n) -> { project.setSunPeakHours(n); recalculate(); });
        HBox row = new HBox(16,
            UiUtils.labeledField("Monthly Electric Bill (₱)", spB, "Total monthly bill"),
            UiUtils.labeledField("Rate per kWh (₱)",          spR, "e.g. 11.50"),
            UiUtils.labeledField("Sun Peak Hours",              spP, "Philippines average: 4-5 hours"));
        return row;
    }

    // -- DEVICE -------------------------------------------------------------------
    private Node buildDeviceMode() {
        HBox header = buildDeviceTableHeader();
        VBox deviceRows = new VBox(4);

        for (Appliance a : project.getAppliances()) {
            deviceRows.getChildren().add(buildDeviceRow(a, deviceRows));
        }
        if (project.getAppliances().isEmpty()) showDeviceEmptyState(deviceRows);

        Button addBtn = new Button("+ ADD DEVICE");
        addBtn.getStyleClass().add("add-device-btn");
        addBtn.setOnAction(e -> {
            deviceRows.getChildren().removeIf(n -> n instanceof VBox v && v.getStyleClass().contains("empty-state"));
            Appliance a = new Appliance("", 0, 0, 1, false, 0.0);
            project.getAppliances().add(a);
            deviceRows.getChildren().add(buildDeviceRow(a, deviceRows));
            recalculate();
        });

        Label tableTitle = new Label("⊟  Appliances / Devices");
        tableTitle.getStyleClass().add("table-title");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox tableTop = new HBox(tableTitle, sp, addBtn);
        tableTop.setAlignment(Pos.CENTER_LEFT);
        tableTop.setPadding(new Insets(0, 0, 8, 0));

        VBox deviceTable = new VBox(0, tableTop, header, deviceRows);
        deviceTable.getStyleClass().add("device-table");
        deviceTable.setPadding(new Insets(12));

        ScrollPane chipScroll = buildChipScrollBar(deviceRows);

        return new VBox(12, deviceTable, chipScroll);
    }

    private HBox buildDeviceTableHeader() {
        HBox header = new HBox(8);
        header.getStyleClass().add("device-table-header");
        header.setPadding(new Insets(6, 8, 6, 8));
        header.getChildren().addAll(
            colHdr("Device Name", W_NAME),
            colHdr("Watts",       W_WATTS),
            colHdr("Hrs/Day",     W_HOURS),
            colHdr("Peak Hr",     W_PEAK,
                "Hours this appliance runs during the system peak usage window. Used to derive the Demand Factor."),
            colHdr("DF",          W_DF,
                "Demand Factor - derived automatically from Peak Hr / Hrs/Day. Read-only."),
            colHdr("Qty",         W_QTY),
            colHdr("Motor?",      W_MOTOR),
            colHdr("Wh/day",      W_KWH),
            colHdr("",            W_DEL)
        );
        return header;
    }

    private Label colHdr(String text, double width) {
        return colHdr(text, width, null);
    }

    private Label colHdr(String text, double width, String tooltipText) {
        Label l = new Label(text);
        l.getStyleClass().add("col-header");
        l.setPrefWidth(width);
        l.setMinWidth(width);
        l.setMaxWidth(width);
        if (tooltipText != null && !tooltipText.isBlank()) {
            l.setTooltip(new Tooltip(tooltipText));
        }
        return l;
    }

    private HBox buildDeviceRow(Appliance appliance, VBox deviceRows) {
        ComboBox<String> nameBox = new ComboBox<>();
        nameBox.setEditable(true);
        nameBox.getStyleClass().add("device-combo");
        for (Appliance.Preset p : Appliance.PRESETS) nameBox.getItems().add(p.name());
        nameBox.setValue(appliance.getName());
        nameBox.setPrefWidth(W_NAME); nameBox.setMinWidth(W_NAME); nameBox.setMaxWidth(W_NAME);

        Spinner<Double> spW = fixedSpinner(appliance.getWatts(), 0, 99999, 10, W_WATTS);
        spW.valueProperty().addListener((o, a, n) -> { appliance.setWatts(n); recalculate(); });

        Spinner<Double> spH = fixedSpinner(appliance.getHoursPerDay(), 0, 24, 0.5, W_HOURS);
        spH.valueProperty().addListener((o, a, n) -> { appliance.setHoursPerDay(n); recalculate(); });

        Spinner<Double> spPeak = fixedSpinner(appliance.getPeakHours(), 0, 999, 0.25, W_PEAK);
        spPeak.valueProperty().addListener((o, a, n) -> { appliance.setPeakHours(n); recalculate(); });

        Label peakWarnLbl = new Label("⚠ Peak > Daily");
        peakWarnLbl.getStyleClass().add("cell-warn-label");
        peakWarnLbl.setStyle("-fx-text-fill: #f97316; -fx-font-size: 10px;");
        peakWarnLbl.setManaged(false);
        peakWarnLbl.setVisible(false);

        VBox peakCell = new VBox(2, spPeak, peakWarnLbl);
        peakCell.setPrefWidth(W_PEAK);
        peakCell.setMinWidth(W_PEAK);
        peakCell.setMaxWidth(W_PEAK);

        Label dfLbl = new Label(UiUtils.fmt2(appliance.getDemandFactor()));
        dfLbl.setPrefWidth(W_DF);
        dfLbl.setMinWidth(W_DF);
        dfLbl.setMaxWidth(W_DF);

        Spinner<Integer> spQ = new Spinner<>(1, 99, appliance.getQuantity());
        spQ.setEditable(true); spQ.getStyleClass().add("styled-spinner");
        spQ.setPrefWidth(W_QTY); spQ.setMinWidth(W_QTY); spQ.setMaxWidth(W_QTY);
        spQ.valueProperty().addListener((o, a, n) -> { appliance.setQuantity(n); recalculate(); });

        CheckBox motorCb = new CheckBox();
        motorCb.setSelected(appliance.isMotorLoad());
        motorCb.setPrefWidth(W_MOTOR);
        motorCb.setMinWidth(W_MOTOR);
        motorCb.setMaxWidth(W_MOTOR);
        motorCb.selectedProperty().addListener((o, a, n) -> {
            appliance.setMotorLoad(n);
            recalculate();
        });

        Label whLbl = new Label(UiUtils.fmt0(appliance.getDailyWh()) + " Wh");
        whLbl.getStyleClass().add("kwh-label");
        whLbl.setPrefWidth(W_KWH); whLbl.setMinWidth(W_KWH); whLbl.setMaxWidth(W_KWH);

        Runnable updateWh = () -> whLbl.setText(UiUtils.fmt0(appliance.getDailyWh()) + " Wh");
        spW.valueProperty().addListener((o, a, n) -> updateWh.run());
        spH.valueProperty().addListener((o, a, n) -> updateWh.run());
        spQ.valueProperty().addListener((o, a, n) -> updateWh.run());

        Runnable updatePeakCell = () -> {
            double df = appliance.getDemandFactor();
            dfLbl.setText(UiUtils.fmt2(df));
            dfLbl.setStyle(df > 1.0 ? "-fx-text-fill: #f97316;" : "");

            boolean overrun = appliance.getHoursPerDay() > 0 && appliance.getPeakHours() > appliance.getHoursPerDay();
            peakWarnLbl.setManaged(overrun);
            peakWarnLbl.setVisible(overrun);

            spPeak.getEditor().setStyle(appliance.getPeakHours() <= 0.0 ? "-fx-text-fill: #f59e0b;" : "");
        };
        appliance.hoursPerDayProperty().addListener((o, a, n) -> updatePeakCell.run());
        appliance.peakHoursProperty().addListener((o, a, n) -> updatePeakCell.run());
        updatePeakCell.run();

        nameBox.valueProperty().addListener((obs, o, name) -> {
            appliance.setName(name);
            for (Appliance.Preset p : Appliance.PRESETS) {
                if (p.name().equals(name)) {
                    appliance.setWatts(p.watts());
                    appliance.setMotorLoad(p.isMotorLoad());
                    appliance.setPeakHours(p.defaultPeakHours());
                    spW.getValueFactory().setValue(p.watts());
                    motorCb.setSelected(p.isMotorLoad());
                    spPeak.getValueFactory().setValue(p.defaultPeakHours());
                    break;
                }
            }
            recalculate();
        });

        Button del = new Button("🗑");
        del.getStyleClass().add("delete-btn");
        del.setPrefWidth(W_DEL); del.setMinWidth(W_DEL); del.setMaxWidth(W_DEL);
        del.setOnAction(e -> {
            project.getAppliances().remove(appliance);
            deviceRows.getChildren().remove(del.getParent());
            if (project.getAppliances().isEmpty()) showDeviceEmptyState(deviceRows);
            recalculate();
        });

        HBox row = new HBox(8, nameBox, spW, spH, peakCell, dfLbl, spQ, motorCb, whLbl, del);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("device-row");
        row.setPadding(new Insets(4, 8, 4, 8));
        return row;
    }

    private Spinner<Double> fixedSpinner(double init, double min, double max, double step, double width) {
        Spinner<Double> sp = new Spinner<>(min, max, init, step);
        sp.setEditable(true); sp.getStyleClass().add("styled-spinner");
        sp.setPrefWidth(width); sp.setMinWidth(width); sp.setMaxWidth(width);
        return sp;
    }

    private void showDeviceEmptyState(VBox deviceRows) {
        Label icon = new Label("⚡"); icon.getStyleClass().add("empty-icon");
        Label msg  = new Label("No devices added yet."); msg.getStyleClass().add("empty-msg");
        Label sub  = new Label("Click \"Add Device\" to start."); sub.getStyleClass().add("empty-sub");
        VBox box = new VBox(6, icon, msg, sub);
        box.setAlignment(Pos.CENTER); box.setPadding(new Insets(24));
        box.getStyleClass().add("empty-state");
        deviceRows.getChildren().add(box);
    }

    private ScrollPane buildChipScrollBar(VBox deviceRows) {
        HBox chips = new HBox(8);
        chips.setPadding(new Insets(6));
        for (Appliance.Preset preset : Appliance.PRESETS) {
            Button chip = new Button("+ " + preset.name());
            chip.getStyleClass().add("chip-btn");
            chip.setOnAction(e -> {
                deviceRows.getChildren().removeIf(n -> n instanceof VBox v && v.getStyleClass().contains("empty-state"));
                Appliance a = new Appliance(
                    preset.name(), preset.watts(), 8, 1, preset.isMotorLoad(), preset.defaultPeakHours());
                project.getAppliances().add(a);
                deviceRows.getChildren().add(buildDeviceRow(a, deviceRows));
                recalculate();
            });
            chips.getChildren().add(chip);
        }
        ScrollPane scroll = new ScrollPane(chips);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setFitToHeight(true);
        scroll.setPrefHeight(54);
        scroll.getStyleClass().add("chip-scroll");
        return scroll;
    }

    // -- Result tiles -------------------------------------------------------------
    private HBox buildResultTiles() {
        lblDailyWh  .getStyleClass().addAll("metric-value", "metric-blue");
        lblDailyKwh .getStyleClass().addAll("metric-value", "metric-blue");
        lblHourlyAvg.getStyleClass().addAll("metric-value", "metric-default");
        HBox row = new HBox(12,
            makeTile("Daily Consumption", lblDailyWh),
            makeTile("Daily Consumption", lblDailyKwh),
            makeTile("Hourly Average",    lblHourlyAvg));
        row.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        return row;
    }

    private VBox buildPeakSummaryCard() {
        Label title = new Label("Peak Hour Load Estimate");
        title.getStyleClass().add("table-title");

        lblConnectedLoad.getStyleClass().addAll("metric-value", "metric-blue");
        lblPeakLoad.getStyleClass().addAll("metric-value", "metric-orange");
        lblSystemDf.getStyleClass().addAll("metric-value", "metric-blue");
        lblRecInv.getStyleClass().addAll("metric-value", "metric-green");

        HBox tiles = new HBox(12,
            makeTile("Connected Load (All On)",   lblConnectedLoad),
            makeTile("Peak Simultaneous Load",    lblPeakLoad),
            makeTile("Demand Factor (System)",    lblSystemDf));
        tiles.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        HBox formula = UiUtils.formulaBanner(
            "Peak Load = Σ (adjustedWatts × min(peakHrs ÷ hrsPerDay, 1.0))  |  Inverter = Peak Load × 1.20");

        VBox card = new VBox(10, title, peakWarnBox, tiles, formula);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        return card;
    }

    private VBox makeTile(String label, Label value) {
        Label lbl = new Label(label); lbl.getStyleClass().add("metric-label");
        VBox box = new VBox(4, lbl, value);
        box.getStyleClass().add("metric-tile"); box.setPadding(new Insets(14));
        return box;
    }

    private void recalculate() {
        CalcService.calculate(project);
        double dailyWh = project.getResultDailyWh();
        lblDailyWh  .setText(UiUtils.fmt0(dailyWh) + " Wh");
        lblDailyKwh .setText(UiUtils.fmt2(dailyWh / 1000) + " kWh");
        lblHourlyAvg.setText(UiUtils.fmt0(CalcService.hourlyAvgWatts(dailyWh)) + " W");

        double connectedLoad = CalcService.adjustedLoadWatts(project.getAppliances());
        double peakLoad = CalcService.peakSimultaneousWatts(project.getAppliances());
        double systemDf = connectedLoad > 0 ? peakLoad / connectedLoad : 0;
        double recInv = CalcService.inverterWithMargin(peakLoad);

        lblConnectedLoad.setText(UiUtils.fmt0(connectedLoad) + " W");
        lblPeakLoad.setText(UiUtils.fmt0(peakLoad) + " W");
        lblSystemDf.setText(UiUtils.fmt2(systemDf));
        lblRecInv.setText(UiUtils.fmt0(recInv) + " W");

        peakWarnBox.getChildren().clear();
        if (CalcService.hasPeakHoursOverrun(project.getAppliances())) {
            peakWarnBox.getChildren().add(UiUtils.warningBanner(
                "One or more appliances have Peak Hr greater than Hrs/Day. Demand Factor has been clamped to 1.0 for those rows in calculations."));
        }

        double monthlyKwh = switch (project.getLoadMode()) {
            case DIRECT -> project.getMonthlyKwh();
            case BILL   -> CalcService.monthlyKwhFromBill(project.getMonthlyBill(), project.getRatePerKwh());
            case DEVICE -> CalcService.monthlyKwhFromDevices(project.getAppliances());
        };
        String formula = switch (project.getLoadMode()) {
            case DEVICE -> String.format(
                "Formula: Total Daily Energy Consumption = sum(Watts x Hours/Day x Qty) = %.0f Wh", dailyWh);
            case DIRECT, BILL -> String.format(
                "Formula: Daily Wh = (%.1f kWh / 30 days) x 1,000 = %.0f Wh", monthlyKwh, dailyWh);
        };
        formulaLbl.setText(formula);
        formulaLbl.getStyleClass().add("formula-text");
    }

    private HBox buildNavRow() {
        Button next = new Button("NEXT →"); next.getStyleClass().add("nav-next-btn");
        next.setOnAction(e -> shell.nextStep());
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(sp, next); row.setPadding(new Insets(16, 0, 0, 0));
        return row;
    }

    @Override public Node   getRoot()      { return root; }
    @Override public String getStepTitle() { return "Appliances"; }
    @Override public void   onEnter()      { recalculate(); }
    @Override public void   onLeave()      {}
}
