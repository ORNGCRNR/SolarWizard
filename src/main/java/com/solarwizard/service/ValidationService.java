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
        double totalPv = p.getResultTotalPvW();
        double maxPv   = p.getInverterMaxPvInput();
        if (maxPv > 0 && totalPv > maxPv) {
            w.add(new Warning(
                String.format("Total PV power (%.0fW) exceeds inverter max PV input (%.0fW).", totalPv, maxPv),
                Warning.Severity.ERROR));
        }
        double withMargin = p.getResultInverterMinW();
        double rated      = p.getInverterRatedPower();
        if (rated > 0 && withMargin > rated) {
            w.add(new Warning(
                String.format("Required load with margin (%.0fW) exceeds inverter rated power (%.0fW).", withMargin, rated),
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
        double usableWh = p.getBatteryVoltage() * p.getBatteryCapacityAh()
                          * p.getResultBatteriesNeeded() * p.getBatteryDod();
        double requiredWh = p.getResultDailyWh() * (p.getAutonomyHours() / 24.0);
        if (usableWh < requiredWh)
            w.add(new Warning(
                String.format("Usable energy (%.0fWh) < required (%.0fWh). Increase capacity or batteries.", usableWh, requiredWh),
                Warning.Severity.ERROR));
        return w;
    }

    public static List<Warning> validateWiring(SolarProject p) {
        List<Warning> w = new ArrayList<>();
        if (p.getWireVoltageDrop() > 3)
            w.add(new Warning("Voltage drop above 3% causes significant energy loss. Recommended: ≤2% DC, ≤1% AC.", Warning.Severity.CAUTION));
        return w;
    }

    public static List<Warning> validateAll(SolarProject p) {
        List<Warning> all = new ArrayList<>();
        all.addAll(validateInverter(p));
        all.addAll(validateBattery(p));
        all.addAll(validateWiring(p));
        return all;
    }
}
