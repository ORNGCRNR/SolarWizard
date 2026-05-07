package com.solarwizard.view.steps;

import com.solarwizard.model.SolarProject;
import com.solarwizard.service.CalcService;
import com.solarwizard.service.ValidationService;
import com.solarwizard.util.UiUtils;
import com.solarwizard.view.WizardShell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;

/**
 * Step 7 — Summary Report
 * Compiles all results from all steps into one readable report.
 */
public class Step7Summary implements WizardShell.StepView {

    private final SolarProject project;
    private final WizardShell  shell;
    private final ScrollPane   root = new ScrollPane();
    private final VBox         mainContent = new VBox(20);

    public Step7Summary(SolarProject project, WizardShell shell) {
        this.project = project;
        this.shell   = shell;
        root.setContent(mainContent);
        root.setFitToWidth(true);
        root.getStyleClass().add("step-scroll");
    }

    private void rebuild() {
        mainContent.getChildren().clear();
        mainContent.setPadding(new Insets(24, 32, 32, 32));
        mainContent.getStyleClass().add("step-content");

        CalcService.calculate(project);

        // Header
        Label titleLbl = new Label("📋  Solar System Summary Report");
        titleLbl.getStyleClass().add("report-title");
        Label projLbl = new Label("Project: " + project.getProjectName());
        projLbl.getStyleClass().add("report-subtitle");
        VBox header = new VBox(4, titleLbl, projLbl);
        header.setAlignment(Pos.CENTER);
        mainContent.getChildren().add(header);

        // Validation banners
        List<ValidationService.Warning> allWarnings = ValidationService.validateAll(project);
        if (allWarnings.isEmpty()) {
            mainContent.getChildren().add(
                UiUtils.successBanner("✔  All checks passed — your system design looks good!"));
        } else {
            for (ValidationService.Warning w : allWarnings) {
                mainContent.getChildren().add(
                    w.severity() == ValidationService.Warning.Severity.ERROR
                        ? UiUtils.errorBanner(w.message(), () -> {})
                        : UiUtils.warningBanner(w.message()));
            }
        }

        // ── Step 1: Load ──────────────────────────────────────────────────────
        double dailyWh = project.getResultDailyWh();
        mainContent.getChildren().add(sectionCard("⚡  Step 1: Load Analysis",
            row("Input Mode",        project.getLoadMode().name()),
            row("Sun Peak Hours",    UiUtils.fmt1(project.getSunPeakHours()) + " h/day"),
            row("Daily Consumption", UiUtils.fmt0(dailyWh) + " Wh  (" + UiUtils.fmt2(dailyWh / 1000) + " kWh)"),
            row("Hourly Average",    UiUtils.fmt0(CalcService.hourlyAvgWatts(dailyWh)) + " W")
        ));

        // ── Step 2: Panels ────────────────────────────────────────────────────
        String panelName = (project.getPanelBrand() + " " + project.getPanelModel()).trim();
        mainContent.getChildren().add(sectionCard("☀  Step 2: Solar Panel Sizing",
            row("Panel",             panelName.isBlank() ? "Not specified" : panelName),
            row("Wattage",           UiUtils.fmt0(project.getPanelWattage()) + " W"),
            row("Voc",               UiUtils.fmt2(project.getPanelVoc()) + " V"),
            row("Isc",               UiUtils.fmt2(project.getPanelIsc()) + " A"),
            row("Safety Factor",     project.isPanelSafetyFactor() ? "×1.25 applied" : "Not applied"),
            row("Required PV Power", UiUtils.fmt0(project.getResultRequiredPvW()) + " W"),
            row("Panels Needed",     project.getResultPanelsNeeded() + " pcs"),
            row("Total PV Array",    UiUtils.fmt0(project.getResultTotalPvW()) + " W")
        ));

        // ── Step 3: Inverter ──────────────────────────────────────────────────
        double peak       = CalcService.peakLoadWatts(dailyWh, project.getSunPeakHours());
        double withMargin = CalcService.inverterWithMargin(peak);
        String invName    = (project.getInverterBrand() + " " + project.getInverterModel()).trim();
        mainContent.getChildren().add(sectionCard("🔌  Step 3: Inverter Sizing",
            row("Inverter",          invName.isBlank() ? "Not specified" : invName),
            row("Rated Power",       UiUtils.fmt0(project.getInverterRatedPower()) + " W"),
            row("Max PV Input",      UiUtils.fmt0(project.getInverterMaxPvInput()) + " W"),
            row("System Voltage",    UiUtils.fmt0(project.getInverterSysVoltage()) + " V"),
            row("Battery Range",     UiUtils.fmt0(project.getInverterBattMinV()) + "–" + UiUtils.fmt0(project.getInverterBattMaxV()) + "V"),
            row("MPPT Count",        String.valueOf(project.getInverterMpptCount())),
            row("Peak Load",         UiUtils.fmt0(peak) + " W"),
            row("With 20% Margin",   UiUtils.fmt0(withMargin) + " W")
        ));

        // ── Step 4: Battery ───────────────────────────────────────────────────
        int    batteries = project.getResultBatteriesNeeded();
        double totalWh   = batteries * project.getBatteryVoltage() * project.getBatteryCapacityAh();
        String battName  = (project.getBatteryBrand() + " " + project.getBatteryModel()).trim();
        mainContent.getChildren().add(sectionCard("🔋  Step 4: Battery Sizing",
            row("Battery",           battName.isBlank() ? "Custom Battery" : battName),
            row("Voltage",           UiUtils.fmt1(project.getBatteryVoltage()) + " V"),
            row("Capacity per Unit", UiUtils.fmt0(project.getBatteryCapacityAh()) + " Ah"),
            row("DOD",               UiUtils.fmt0(project.getBatteryDod() * 100) + "%"),
            row("Autonomy",          UiUtils.fmt0(project.getAutonomyHours()) + " hours"),
            row("Required Capacity", UiUtils.fmt0(project.getResultBatteryAh()) + " Ah"),
            row("Batteries Needed",  batteries + " pcs"),
            row("Total Energy",      UiUtils.fmt0(totalWh) + " Wh")
        ));

        // ── Step 5: Wire Sizing ───────────────────────────────────────────────
        double pvI   = project.getPanelIsc();
        double battI = dailyWh > 0 && project.getBatteryVoltage() > 0
            ? (dailyWh / project.getBatteryVoltage()) / 8.0 : 0;
        double acI   = project.getResultInverterMinW() / 220.0;
        double vdf   = project.getWireVoltageDrop() / 100.0;

        AwgResult pvAwg   = calcAwg(pvI,   project.getPanelWattage() > 0 ? project.getPanelWattage() / Math.max(pvI, 0.01) : 12, project.getWirePvToInverterM(),      vdf);
        AwgResult battAwg = calcAwg(battI, project.getBatteryVoltage(),   project.getWireBatteryToInverterM(), vdf);
        AwgResult acAwg   = calcAwg(acI,   220.0,                         project.getWireInverterToLoadM(),    vdf);

        mainContent.getChildren().add(sectionCard("〰  Step 5: Wire Sizing",
            row("PV to Inverter",     UiUtils.fmt0(project.getWirePvToInverterM()) + " m  →  AWG " + pvAwg.awg + " (" + pvAwg.mm2 + " mm²)"),
            row("Battery to Inverter",UiUtils.fmt0(project.getWireBatteryToInverterM()) + " m  →  AWG " + battAwg.awg + " (" + battAwg.mm2 + " mm²)"),
            row("Inverter to Load",   UiUtils.fmt0(project.getWireInverterToLoadM()) + " m  →  AWG " + acAwg.awg + " (" + acAwg.mm2 + " mm²)"),
            row("Voltage Drop Limit", UiUtils.fmt1(project.getWireVoltageDrop()) + "%")
        ));

        // ── Step 6: Breakers ──────────────────────────────────────────────────
        mainContent.getChildren().add(sectionCard("⚡  Step 6: Breaker Sizing",
            row("PV DC Breaker",   CalcService.breakerSize(pvI)   + " A DC  (500–1000V DC)"),
            row("Battery DC MCCB", CalcService.breakerSize(battI) + " A DC  (48–60V DC)"),
            row("AC Breaker",      CalcService.breakerSize(acI)   + " A AC  (220–240V AC)")
        ));

        mainContent.getChildren().add(buildNavRow());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private VBox sectionCard(String title, Node... rows) {
        Label t = new Label(title); t.getStyleClass().add("report-section-title");
        Separator sep = new Separator(); sep.getStyleClass().add("report-sep");
        VBox card = new VBox(0, t, sep);
        card.getStyleClass().add("report-card");
        card.setPadding(new Insets(16));
        card.getChildren().addAll(rows);
        return card;
    }

    private HBox row(String key, String value) {
        Label k = new Label(key);   k.getStyleClass().add("report-key");
        Label v = new Label(value); v.getStyleClass().add("report-value");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(k, sp, v);
        row.getStyleClass().add("report-row");
        row.setPadding(new Insets(8, 4, 8, 4));
        return row;
    }

    private record AwgResult(int awg, String mm2) {}

    private AwgResult calcAwg(double current, double voltage, double distM, double vDropFrac) {
        double distFt = CalcService.metresToFeet(distM);
        double vdi    = CalcService.vdi(current, distFt, voltage, vDropFrac);
        CalcService.AwgEntry e = CalcService.lookupAwg(vdi);
        return new AwgResult(e.awg(), UiUtils.fmt2(e.areaMm2()));
    }

    private HBox buildNavRow() {
        Button back  = new Button("← BACK"); back.getStyleClass().add("nav-back-btn"); back.setOnAction(e -> shell.prevStep());
        Button newP  = new Button("⊞ New Project"); newP.getStyleClass().add("nav-next-btn"); newP.setOnAction(e -> shell.navigateTo(0));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        return new HBox(8, back, sp, newP) {{ setPadding(new Insets(24, 0, 0, 0)); }};
    }

    @Override public Node   getRoot()      { return root; }
    @Override public String getStepTitle() { return "Summary Report"; }
    @Override public void   onEnter()      { rebuild(); }
    @Override public void   onLeave()      {}
}
