package com.solarwizard.view.steps;

import com.solarwizard.model.SolarProject;
import com.solarwizard.service.CalcService;
import com.solarwizard.service.CalcService.PecEntry;
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
 * Summary report for the 8-stage solar system setup flow.
 */
public class Step7Summary implements WizardShell.StepView {

    private final SolarProject project;
    private final WizardShell shell;
    private final ScrollPane root = new ScrollPane();
    private final VBox mainContent = new VBox(20);

    public Step7Summary(SolarProject project, WizardShell shell) {
        this.project = project;
        this.shell = shell;
        root.setContent(mainContent);
        root.setFitToWidth(true);
        root.getStyleClass().add("step-scroll");
    }

    private void rebuild() {
        mainContent.getChildren().clear();
        mainContent.setPadding(new Insets(24, 32, 32, 32));
        mainContent.getStyleClass().add("step-content");

        CalcService.calculate(project);

        Label titleLbl = new Label("Solar System Summary Report");
        titleLbl.getStyleClass().add("report-title");
        Label projLbl = new Label("Project: " + project.getProjectName());
        projLbl.getStyleClass().add("report-subtitle");
        VBox header = new VBox(4, titleLbl, projLbl);
        header.setAlignment(Pos.CENTER);
        mainContent.getChildren().add(header);

        List<ValidationService.Warning> allWarnings = ValidationService.validateAll(project);
        if (allWarnings.isEmpty()) {
            mainContent.getChildren().add(UiUtils.successBanner("All checks passed for the current inputs."));
        } else {
            for (ValidationService.Warning w : allWarnings) {
                mainContent.getChildren().add(UiUtils.warningBanner(precautionMessage(w)));
            }
        }

        double dailyWh = project.getResultDailyWh();
        mainContent.getChildren().add(sectionCard("Step 1: Appliances and Wattage",
            row("Input Mode", project.getLoadMode().name()),
            row("Appliance Rows", String.valueOf(project.getAppliances().size())),
            row("Total Daily Consumption", UiUtils.fmt0(dailyWh) + " Wh"),
            row("Motor Load Surge Basis", UiUtils.fmt0(CalcService.adjustedLoadWatts(project.getAppliances())) + " W adjusted")
        ));

        mainContent.getChildren().add(sectionCard("Step 2: Total Energy Requirement",
            row("System Loss Buffer", UiUtils.fmt1(project.getSystemLossPercent()) + "%"),
            row("Expected Daily Consumption", UiUtils.fmt0(project.getResultExpectedDailyWh()) + " Wh/day"),
            row("Days of Autonomy", UiUtils.fmt1(project.getAutonomyDays()) + " days"),
            row("Total Energy Requirement", UiUtils.fmt0(project.getResultTotalEnergyRequirementWh()) + " Wh")
        ));

        int batteries = project.getResultBatteriesNeeded();
        double totalAh = batteries * project.getBatteryCapacityAh();
        String battName = (project.getBatteryBrand() + " " + project.getBatteryModel()).trim();
        mainContent.getChildren().add(sectionCard("Step 3: Battery Bank Sizing",
            row("Battery", battName.isBlank() ? "Custom Battery" : battName),
            row("Nominal Voltage", UiUtils.fmt1(project.getBatteryVoltage()) + " V"),
            row("Capacity per Battery", UiUtils.fmt0(project.getBatteryCapacityAh()) + " Ah"),
            row("DOD", UiUtils.fmt0(project.getBatteryDod() * 100) + "%"),
            row("Required Capacity", UiUtils.fmt0(project.getResultBatteryAh()) + " Ah"),
            row("Batteries Needed", batteries + " pcs"),
            row("Total AH of Bank", UiUtils.fmt0(totalAh) + " Ah"),
            row("Recommended Energy Consumption", UiUtils.fmt0(project.getResultRecommendedEnergyWh()) + " Wh")
        ));

        String panelName = (project.getPanelBrand() + " " + project.getPanelModel()).trim();
        mainContent.getChildren().add(sectionCard("Step 4: Solar Panel Sizing",
            row("Panel", panelName.isBlank() ? "Not specified" : panelName),
            row("Panel Pmax", UiUtils.fmt0(project.getPanelWattage()) + " W"),
            row("Panel Vmp / Voc", UiUtils.fmt2(project.getPanelVmp()) + " V / " + UiUtils.fmt2(project.getPanelVoc()) + " V"),
            row("Panel Imp / Isc", UiUtils.fmt2(project.getPanelImp()) + " A / " + UiUtils.fmt2(project.getPanelIsc()) + " A"),
            row("Panels Needed", project.getResultPanelsNeeded() + " pcs"),
            row("Total Array Watts", UiUtils.fmt0(project.getResultTotalPvW()) + " W"),
            row("Wiring Configuration", project.getPanelWiring().name())
        ));

        String sccName = (project.getChargeControllerBrand() + " " + project.getChargeControllerModel()).trim();
        mainContent.getChildren().add(sectionCard("Step 5: Solar Charge Controller Sizing",
            row("Controller", sccName.isBlank() ? "Not specified" : sccName),
            row("Type", project.getChargeControllerType().name()),
            row("Required SCC Current", UiUtils.fmt1(project.getResultRequiredSccCurrent()) + " A"),
            row("Rated Charge Current", UiUtils.fmt1(project.getChargeControllerRatedCurrent()) + " A"),
            row("Array Voc", UiUtils.fmt1(project.getResultArrayVoc()) + " V"),
            row("Max PV Input Voltage", UiUtils.fmt1(project.getChargeControllerMaxPvVoltage()) + " V"),
            row("Max PV Input Power", UiUtils.fmt0(project.getChargeControllerMaxPvPower()) + " W")
        ));

        String invName = (project.getInverterBrand() + " " + project.getInverterModel()).trim();
        mainContent.getChildren().add(sectionCard("Step 6: Inverter Sizing",
            row("Inverter", invName.isBlank() ? "Not specified" : invName),
            row("Adjusted Appliance Load", UiUtils.fmt0(CalcService.adjustedLoadWatts(project.getAppliances())) + " W"),
            row("Minimum Inverter Watts", UiUtils.fmt0(project.getResultInverterMinW()) + " W"),
            row("Selected Rated Power", UiUtils.fmt0(project.getInverterRatedPower()) + " W"),
            row("Battery Voltage", UiUtils.fmt0(project.getBatteryVoltage()) + " V")
        ));

        double inverterWatts = inverterWatts();
        double battV = project.getBatteryVoltage();
        WireResult phase1 = calcWire(project.getResultArrayImp(), project.getResultArrayVmp(),
            project.getWirePvToInverterM(), project.getWireVoltageDrop());
        WireResult phase2 = calcWire(battV > 0 ? project.getResultTotalPvW() / battV : 0,
            battV, project.getWireSccToBatteryM(), project.getWireVoltageDrop());
        WireResult phase3 = calcWire(battV > 0 ? inverterWatts / battV : 0,
            battV, project.getWireBatteryToInverterM(), project.getWireVoltageDrop());
        WireResult phase4 = calcWire(CalcService.inverterAcLoadCurrent(inverterWatts),
            CalcService.AC_LOAD_VOLTAGE, project.getWireInverterToLoadM(), 1.0);

        mainContent.getChildren().add(sectionCard("Step 7: Wire Sizing",
            row("Panels to SCC", wireText(project.getWirePvToInverterM(), phase1.awg())),
            row("  VDrop", vdropText(phase1.vdrop())),
            row("  PEC (Copper)", pecText(phase1.pec())),
            row("SCC to Battery", wireText(project.getWireSccToBatteryM(), phase2.awg())),
            row("  VDrop", vdropText(phase2.vdrop())),
            row("  PEC (Copper)", pecText(phase2.pec())),
            row("Battery to Inverter", wireText(project.getWireBatteryToInverterM(), phase3.awg())),
            row("  VDrop", vdropText(phase3.vdrop())),
            row("  PEC (Copper)", pecText(phase3.pec())),
            row("Inverter to AC Load", wireText(project.getWireInverterToLoadM(), phase4.awg())),
            row("  VDrop", vdropText(phase4.vdrop())),
            row("  PEC (Copper)", pecText(phase4.pec()))
        ));

        mainContent.getChildren().add(sectionCard("Step 8: Circuit Breaker Sizing",
            row("Panels to SCC", breakerText(project.getResultArrayIsc() * 1.25 * 1.25, "DC MCB")),
            row("SCC to Battery", breakerText(phase2BreakerMinimum(), "DC MCB")),
            row("Battery to Inverter", breakerText((battV > 0 ? inverterWatts / battV : 0) * 1.25, "DC MCB")),
            row("Inverter to AC Load", breakerText(CalcService.inverterAcLoadCurrent(inverterWatts) * 1.25, "AC MCB"))
        ));

        mainContent.getChildren().add(buildNavRow());
    }

    private VBox sectionCard(String title, Node... rows) {
        Label t = new Label(title);
        t.getStyleClass().add("report-section-title");
        Separator sep = new Separator();
        sep.getStyleClass().add("report-sep");
        VBox card = new VBox(0, t, sep);
        card.getStyleClass().add("report-card");
        card.setPadding(new Insets(16));
        card.getChildren().addAll(rows);
        return card;
    }

    private HBox row(String key, String value) {
        Label k = new Label(key);
        k.getStyleClass().add("report-key");
        Label v = new Label(value);
        v.getStyleClass().add("report-value");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(k, sp, v);
        row.getStyleClass().add("report-row");
        row.setPadding(new Insets(8, 4, 8, 4));
        return row;
    }

    private String precautionMessage(ValidationService.Warning warning) {
        return warning.severity() == ValidationService.Warning.Severity.ERROR
            ? "SAFETY PRECAUTION (Proceed at your own risk): " + warning.message()
            : warning.message();
    }

    private record AwgResult(int awg, String mm2) {}
    private record PecWireResult(int breakerSize, PecEntry entry) {}
    private record VDropResult(double volts, double percent) {}
    private record WireResult(AwgResult awg, PecWireResult pec, VDropResult vdrop) {}

    private WireResult calcWire(double current, double voltage, double distM, double voltageDropPct) {
        double distFt = CalcService.metresToFeet(distM);
        double vdi = CalcService.vdi(current, distFt, voltage, voltageDropPct);
        CalcService.AwgEntry e = CalcService.lookupAwg(vdi);
        double vdropV   = CalcService.voltageDropVolts(current, distM, e.areaMm2());
        double vdropPct = CalcService.voltageDropPercent(vdropV, voltage);
        return new WireResult(
            new AwgResult(e.awg(), UiUtils.fmt2(e.areaMm2())),
            calcPecWire(current),
            new VDropResult(vdropV, vdropPct));
    }

    private PecWireResult calcPecWire(double current) {
        int breakerSize = CalcService.standardBreakerSize(current);
        return new PecWireResult(breakerSize, CalcService.lookupPecCopper(breakerSize));
    }

    private String wireText(double distanceM, AwgResult awg) {
        return UiUtils.fmt0(distanceM) + " m -> AWG " + awg.awg() + " (" + awg.mm2() + " mm2)";
    }

    private String pecText(PecWireResult result) {
        PecEntry entry = result.entry();
        if (entry == null) {
            return "—";
        }
        return "Breaker " + result.breakerSize() + " A → "
            + UiUtils.fmt1(entry.mmDia()) + " mm dia.";
    }

    private String vdropText(VDropResult v) {
        return UiUtils.fmt2(v.volts()) + " V  (" + UiUtils.fmt2(v.percent()) + "%)";
    }

    private String breakerText(double minimumCurrent, String type) {
        return CalcService.standardBreakerSize(minimumCurrent) + " A " + type;
    }

    private double phase2BreakerMinimum() {
        double current = project.getChargeControllerRatedCurrent() > 0
            ? project.getChargeControllerRatedCurrent()
            : (project.getBatteryVoltage() > 0 ? project.getResultTotalPvW() / project.getBatteryVoltage() : 0);
        return current * 1.25;
    }

    private double inverterWatts() {
        return project.getInverterRatedPower() > 0
            ? project.getInverterRatedPower()
            : project.getResultInverterMinW();
    }

    private HBox buildNavRow() {
        Button back = new Button("BACK");
        back.getStyleClass().add("nav-back-btn");
        back.setOnAction(e -> shell.prevStep());

        Button newP = new Button("New Project");
        newP.getStyleClass().add("nav-next-btn");
        newP.setOnAction(e -> shell.navigateTo(0));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(8, back, sp, newP);
        row.setPadding(new Insets(24, 0, 0, 0));
        return row;
    }

    @Override public Node getRoot() { return root; }
    @Override public String getStepTitle() { return "Summary Report"; }
    @Override public void onEnter() { rebuild(); }
    @Override public void onLeave() {}
}
