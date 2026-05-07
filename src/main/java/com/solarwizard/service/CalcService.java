package com.solarwizard.service;

import com.solarwizard.model.Appliance;
import com.solarwizard.model.SolarProject;
import java.util.List;

/**
 * Pure calculation service — no UI dependencies.
 * All formulas sourced from "Solar System Setup Guide".
 * To change a formula: edit only this class.
 */
public class CalcService {

    // ── AWG lookup table ──────────────────────────────────────────────────────
    public record AwgEntry(int awg, double areaMm2, int vdiMax, int maxAmps) {}

    public static final AwgEntry[] AWG_TABLE = {
        new AwgEntry(16, 1.31,  1,  10),
        new AwgEntry(14, 2.08,  2,  15),
        new AwgEntry(12, 3.31,  3,  20),
        new AwgEntry(10, 5.26,  5,  30),
        new AwgEntry( 8, 8.37,  8,  55),
        new AwgEntry( 6,13.30, 12,  75),
        new AwgEntry( 4,21.10, 20,  95),
        new AwgEntry( 2,33.60, 31, 130),
        new AwgEntry( 0,53.50, 49, 170),
    };

    private static final int[] STANDARD_INVERTER_SIZES =
        {300, 500, 1000, 1500, 2000, 3000, 5000, 6000, 8000, 10000};

    // ── Step 1: Load Analysis ─────────────────────────────────────────────────
    public static double dailyWhFromMonthlyKwh(double monthlyKwh) {
        return (monthlyKwh / 30.0) * 1000.0;
    }

    public static double monthlyKwhFromBill(double bill, double ratePerKwh) {
        if (ratePerKwh <= 0) return 0;
        return bill / ratePerKwh;
    }

    public static double requiredSolarKwFromBill(double bill, double ratePerKwh, double psh) {
        if (ratePerKwh <= 0 || psh <= 0) return 0;
        return (monthlyKwhFromBill(bill, ratePerKwh) / 30.0) / psh;
    }

    public static double dailyWhFromDevices(List<Appliance> appliances) {
        return appliances.stream()
            .mapToDouble(a -> a.getWatts() * a.getHoursPerDay() * a.getQuantity())
            .sum();
    }

    public static double monthlyKwhFromDevices(List<Appliance> appliances) {
        return appliances.stream().mapToDouble(Appliance::getMonthlyKwh).sum();
    }

    public static double hourlyAvgWatts(double dailyWh) { return dailyWh / 24.0; }

    // ── Step 2: Solar Panel Sizing ────────────────────────────────────────────
    /** Formula: (Daily Wh / PSH) × 1.25 safety factor */
    public static double requiredPvPower(double dailyWh, double psh, boolean safetyFactor) {
        if (psh <= 0) return 0;
        double raw = dailyWh / psh;
        return safetyFactor ? raw * 1.25 : raw;
    }

    public static int panelsNeeded(double requiredPvW, double panelWattage) {
        if (panelWattage <= 0) return 0;
        return (int) Math.ceil(requiredPvW / panelWattage);
    }

    public static double totalPvPower(int panelCount, double panelWattage) {
        return panelCount * panelWattage;
    }

    // ── Step 3: Inverter Sizing ───────────────────────────────────────────────
    /** Peak load = Daily Wh / PSH; With margin = Peak × 1.20 */
    public static double peakLoadWatts(double dailyWh, double psh) {
        if (psh <= 0) return 0;
        return dailyWh / psh;
    }

    public static double inverterWithMargin(double peakLoad) {
        return peakLoad * 1.20;
    }

    public static int recommendedInverterWatts(double minWatts) {
        for (int size : STANDARD_INVERTER_SIZES) {
            if (size >= minWatts) return size;
        }
        return STANDARD_INVERTER_SIZES[STANDARD_INVERTER_SIZES.length - 1];
    }

    // ── Step 4: Battery Sizing ────────────────────────────────────────────────
    /** Formula: Capacity (Ah) = (Daily Wh × autonomy hours / 24) / (DOD × voltage) */
    public static double requiredBatteryAh(double dailyWh, double autonomyHours,
                                           double dod, double voltage) {
        if (dod <= 0 || voltage <= 0) return 0;
        double autonomyDays = autonomyHours / 24.0;
        return (dailyWh * autonomyDays) / (dod * voltage);
    }

    public static int batteriesNeeded(double requiredAh, double batteryAh) {
        if (batteryAh <= 0) return 0;
        return (int) Math.ceil(requiredAh / batteryAh);
    }

    // ── Step 5: Wire Sizing (VDI Method) ─────────────────────────────────────
    public static double metresToFeet(double metres) { return metres * 3.28084; }

    public static double vdi(double currentA, double distanceFt,
                             double voltage, double voltageDropPct) {
        if (voltage <= 0 || voltageDropPct <= 0) return 0;
        return (currentA * distanceFt) / (voltage * voltageDropPct);
    }

    public static AwgEntry lookupAwg(double vdi) {
        for (AwgEntry entry : AWG_TABLE) {
            if (vdi <= entry.vdiMax()) return entry;
        }
        return AWG_TABLE[AWG_TABLE.length - 1];
    }

    // ── Step 6: Breaker Sizing ────────────────────────────────────────────────
    public static int breakerSize(double currentA) {
        double min = currentA * 1.25;
        int[] standards = {10, 15, 20, 25, 30, 40, 50, 60, 70, 80, 100, 125, 150, 175, 200};
        for (int s : standards) { if (s >= min) return s; }
        return (int) Math.ceil(min / 10) * 10;
    }

    // ── Full project recalculation ────────────────────────────────────────────
    public static void calculate(SolarProject p) {
        // Step 1
        double dailyWh = switch (p.getLoadMode()) {
            case DIRECT -> dailyWhFromMonthlyKwh(p.getMonthlyKwh());
            case BILL   -> dailyWhFromMonthlyKwh(monthlyKwhFromBill(p.getMonthlyBill(), p.getRatePerKwh()));
            case DEVICE -> dailyWhFromDevices(p.getAppliances());
        };
        p.setResultDailyWh(dailyWh);

        // Step 2
        double reqPv = requiredPvPower(dailyWh, p.getSunPeakHours(), p.isPanelSafetyFactor());
        p.setResultRequiredPvW(reqPv);
        int panels = panelsNeeded(reqPv, p.getPanelWattage());
        p.setResultPanelsNeeded(panels);
        p.setResultTotalPvW(totalPvPower(panels, p.getPanelWattage()));

        // Step 3
        double peak = peakLoadWatts(dailyWh, p.getSunPeakHours());
        p.setResultInverterMinW(inverterWithMargin(peak));

        // Step 4
        double reqAh = requiredBatteryAh(dailyWh, p.getAutonomyHours(),
                                          p.getBatteryDod(), p.getBatteryVoltage());
        p.setResultBatteryAh(reqAh);
        p.setResultBatteriesNeeded(batteriesNeeded(reqAh, p.getBatteryCapacityAh()));
    }
}
