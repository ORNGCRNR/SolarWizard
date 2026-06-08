package com.solarwizard.service;

import com.solarwizard.model.SolarProject;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates project parameters against setup guide guidelines.
 */
public class ValidationService {

    public record Warning(String message, Severity severity) {
        public enum Severity { ERROR, CAUTION }
    }

    public static List<Warning> validateInverter(SolarProject p) {
        List<Warning> w = new ArrayList<>();
        double withMargin = p.getResultInverterMinW();
        double rated      = p.getInverterRatedPower();
        if (rated > 0 && withMargin > rated) {
            w.add(new Warning(
                String.format("Required load with margin (%.0fW) exceeds inverter rated power (%.0fW).", withMargin, rated),
                Warning.Severity.ERROR));
        }
        double inverterInputVoltage = p.getBatteryVoltage();
        if (inverterInputVoltage > 0 && p.getBatteryVoltage() > 0
            && Math.abs(inverterInputVoltage - p.getBatteryVoltage()) > 0.1) {
            w.add(new Warning(
                String.format("Inverter input voltage (%.0fV) should match the battery bank voltage (%.0fV).",
                    inverterInputVoltage, p.getBatteryVoltage()),
                Warning.Severity.ERROR));
        }
        return w;
    }

    public static List<Warning> validateChargeController(SolarProject p) {
        List<Warning> w = new ArrayList<>();
        if (p.getChargeControllerType() == SolarProject.ChargeControllerType.PWM
            && p.getPanelWiring() == SolarProject.PanelWiring.SERIES
            && p.getResultPanelsNeeded() > 1) {
            w.add(new Warning("PWM controllers should use nominal-voltage-matched panels in parallel, not a higher-voltage series array.",
                Warning.Severity.ERROR));
        }
        if (p.getChargeControllerRatedCurrent() > 0
            && p.getResultRequiredSccCurrent() > p.getChargeControllerRatedCurrent()) {
            w.add(new Warning(
                String.format("Required SCC current (%.1fA) exceeds rated charge current (%.1fA).",
                    p.getResultRequiredSccCurrent(), p.getChargeControllerRatedCurrent()),
                Warning.Severity.ERROR));
        }
        if (p.getChargeControllerMaxPvVoltage() > 0
            && p.getResultArrayVoc() >= p.getChargeControllerMaxPvVoltage()) {
            w.add(new Warning(
                String.format("Array Voc (%.1fV) must stay below controller max PV input voltage (%.1fV).",
                    p.getResultArrayVoc(), p.getChargeControllerMaxPvVoltage()),
                Warning.Severity.ERROR));
        }
        if (p.getChargeControllerMaxPvPower() > 0
            && p.getResultTotalPvW() > p.getChargeControllerMaxPvPower()) {
            w.add(new Warning(
                String.format("Total PV power (%.0fW) exceeds controller max PV input power (%.0fW).",
                    p.getResultTotalPvW(), p.getChargeControllerMaxPvPower()),
                Warning.Severity.ERROR));
        }
        return w;
    }

    public static List<Warning> validateBattery(SolarProject p) {
        List<Warning> w = new ArrayList<>();
        if (p.getBatteryDod() > 0.90)
            w.add(new Warning("DOD above 90% may shorten battery lifespan significantly.", Warning.Severity.CAUTION));
        if (p.getBatteryDod() < 0.40)
            w.add(new Warning("DOD below 40% is very conservative. Typical minimum: 50% lead-acid, 80% LiFePO4.", Warning.Severity.CAUTION));
        double usableWh = p.getResultRecommendedEnergyWh();
        double requiredWh = p.getResultExpectedDailyWh();
        if (usableWh < requiredWh)
            w.add(new Warning(
                String.format("Recommended usable energy (%.0fWh) is below expected daily consumption (%.0fWh/day). Increase capacity or batteries.", usableWh, requiredWh),
                Warning.Severity.ERROR));
        return w;
    }

    public static List<Warning> validateWiring(SolarProject p) {
        List<Warning> w = new ArrayList<>();
        if (p.getWireVoltageDrop() > 3)
            w.add(new Warning("Voltage drop above 3% causes significant energy loss. Recommended: ≤2% DC, ≤1% AC.", Warning.Severity.CAUTION));
        for (double current : wireRunCurrents(p)) {
            int breakerA = CalcService.standardBreakerSize(current);
            if (CalcService.lookupPecCopper(breakerA) == null) {
                w.add(new Warning(
                    "Wire run current exceeds PEC Table 2.50.6.13 maximum (6000 A). Manual sizing required.",
                    Warning.Severity.ERROR));
                break;
            }
        }
        return w;
    }

    public static List<Warning> validateAll(SolarProject p) {
        List<Warning> all = new ArrayList<>();
        all.addAll(validateChargeController(p));
        all.addAll(validateInverter(p));
        all.addAll(validateBattery(p));
        all.addAll(validateWiring(p));
        return all;
    }

    private static double[] wireRunCurrents(SolarProject p) {
        double inverterWatts = inverterWatts(p);
        double batteryVoltage = p.getBatteryVoltage();
        return new double[] {
            p.getResultArrayImp(),
            batteryVoltage > 0 ? p.getResultTotalPvW() / batteryVoltage : 0,
            batteryVoltage > 0 ? inverterWatts / batteryVoltage : 0,
            inverterWatts / 220.0
        };
    }

    private static double inverterWatts(SolarProject p) {
        return p.getInverterRatedPower() > 0
            ? p.getInverterRatedPower()
            : p.getResultInverterMinW();
    }
}
