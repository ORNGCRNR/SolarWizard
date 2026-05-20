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

    // ── Step 2: Energy Requirement ────────────────────────────────────────────
    public static double expectedDailyWh(double dailyWh, double lossPercent) {
        return dailyWh * (1.0 + (lossPercent / 100.0));
    }

    public static double totalEnergyRequirementWh(double expectedDailyWh, double autonomyDays) {
        return expectedDailyWh * autonomyDays;
    }

    /** Formula from the decision flow: Required Ah = Total Energy Requirement / battery voltage. */
    public static double requiredBatteryAh(double totalEnergyRequirementWh, double voltage) {
        if (voltage <= 0) return 0;
        return totalEnergyRequirementWh / voltage;
    }

    public static double batteryBankEnergyWh(int batteries, double voltage, double capacityAh) {
        return batteries * voltage * capacityAh;
    }

    public static double recommendedEnergyConsumptionWh(double batteryBankEnergyWh, double dod) {
        return batteryBankEnergyWh * dod;
    }

    /** Formula: Solar Panel Array Size = recommended energy consumption / peak sun hours. */
    public static double requiredPvPower(double dailyWh, double psh) {
        if (psh <= 0) return 0;
        return dailyWh / psh;
    }

    public static int panelsNeeded(double requiredPvW, double panelWattage) {
        if (panelWattage <= 0) return 0;
        return (int) Math.ceil(requiredPvW / panelWattage);
    }

    public static double totalPvPower(int panelCount, double panelWattage) {
        return panelCount * panelWattage;
    }

    public static int parallelStringCount(SolarProject p) {
        int panels = Math.max(p.getResultPanelsNeeded(), 0);
        if (panels == 0) return 0;
        return p.getPanelWiring() == SolarProject.PanelWiring.PARALLEL ? panels : 1;
    }

    public static int seriesPanelCount(SolarProject p) {
        int panels = Math.max(p.getResultPanelsNeeded(), 0);
        if (panels == 0) return 0;
        return p.getPanelWiring() == SolarProject.PanelWiring.SERIES ? panels : 1;
    }

    public static double adjustedArrayVoc(SolarProject p) {
        return p.getPanelVoc() * seriesPanelCount(p);
    }

    public static double adjustedArrayVmp(SolarProject p) {
        return p.getPanelVmp() * seriesPanelCount(p);
    }

    public static double adjustedArrayIsc(SolarProject p) {
        return p.getPanelIsc() * parallelStringCount(p);
    }

    public static double adjustedArrayImp(SolarProject p) {
        return p.getPanelImp() * parallelStringCount(p);
    }

    public static double requiredChargeControllerCurrent(SolarProject p) {
        if (p.getChargeControllerType() == SolarProject.ChargeControllerType.PWM) {
            return adjustedArrayIsc(p) * 1.25;
        }
        if (p.getBatteryVoltage() <= 0) return 0;
        return p.getResultTotalPvW() / p.getBatteryVoltage();
    }

    // ── Step 6: Inverter Sizing ───────────────────────────────────────────────
    /** Sum of adjusted appliance watts, with motor loads multiplied by 3. */
    public static double adjustedLoadWatts(List<Appliance> appliances) {
        return appliances.stream().mapToDouble(Appliance::getAdjustedWatts).sum();
    }

    /**
     * Peak simultaneous load = sum of peakWatts across all appliances.
     * peakWatts = adjustedWatts x min(peakHours/hoursPerDay, 1.0)
     * This is what the inverter must handle during the worst-case peak hour.
     */
    public static double peakSimultaneousWatts(List<Appliance> appliances) {
        return appliances.stream().mapToDouble(Appliance::getPeakWatts).sum();
    }

    /**
     * Returns true if any appliance has peakHours > hoursPerDay.
     * Used to trigger a validation warning in the UI.
     */
    public static boolean hasPeakHoursOverrun(List<Appliance> appliances) {
        return appliances.stream()
            .anyMatch(a -> a.getHoursPerDay() > 0
                        && a.getPeakHours() > a.getHoursPerDay());
    }

    public static double inverterWithMargin(double adjustedLoadWatts) {
        return adjustedLoadWatts * 1.20;
    }

    public static int recommendedInverterWatts(double minWatts) {
        for (int size : STANDARD_INVERTER_SIZES) {
            if (size >= minWatts) return size;
        }
        return STANDARD_INVERTER_SIZES[STANDARD_INVERTER_SIZES.length - 1];
    }

    // ── Shared sizing helpers ─────────────────────────────────────────────────
    public static int batteriesNeeded(double requiredAh, double batteryAh) {
        if (batteryAh <= 0) return 0;
        return (int) Math.ceil(requiredAh / batteryAh);
    }

    // ── Step 7: Wire Sizing (VDI Method) ─────────────────────────────────────
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

    // ── Step 8: Breaker Sizing ────────────────────────────────────────────────
    public static int breakerSize(double currentA) {
        return standardBreakerSize(currentA * 1.25);
    }

    public static int standardBreakerSize(double minimumAmps) {
        int[] standards = {10, 15, 20, 25, 30, 40, 50, 60, 70, 80, 100, 125, 150, 175, 200};
        for (int s : standards) { if (s >= minimumAmps) return s; }
        return (int) Math.ceil(minimumAmps / 10) * 10;
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
        double expectedWh = expectedDailyWh(dailyWh, p.getSystemLossPercent());
        double totalEnergyRequirement = totalEnergyRequirementWh(expectedWh, p.getAutonomyDays());
        p.setResultExpectedDailyWh(expectedWh);
        p.setResultTotalEnergyRequirementWh(totalEnergyRequirement);

        // Step 3
        double reqAh = requiredBatteryAh(totalEnergyRequirement, p.getBatteryVoltage());
        p.setResultBatteryAh(reqAh);
        int batteries = batteriesNeeded(reqAh, p.getBatteryCapacityAh());
        p.setResultBatteriesNeeded(batteries);
        double bankEnergyWh = batteryBankEnergyWh(batteries, p.getBatteryVoltage(), p.getBatteryCapacityAh());
        p.setResultBatteryBankEnergyWh(bankEnergyWh);
        p.setResultRecommendedEnergyWh(recommendedEnergyConsumptionWh(bankEnergyWh, p.getBatteryDod()));

        // Step 4
        double reqPv = requiredPvPower(p.getResultRecommendedEnergyWh(), p.getSunPeakHours());
        p.setResultRequiredPvW(reqPv);
        int panels = panelsNeeded(reqPv, p.getPanelWattage());
        p.setResultPanelsNeeded(panels);
        p.setResultTotalPvW(totalPvPower(panels, p.getPanelWattage()));

        p.setResultArrayVoc(adjustedArrayVoc(p));
        p.setResultArrayVmp(adjustedArrayVmp(p));
        p.setResultArrayIsc(adjustedArrayIsc(p));
        p.setResultArrayImp(adjustedArrayImp(p));

        // Step 5
        p.setResultRequiredSccCurrent(requiredChargeControllerCurrent(p));

        // Step 6
        p.setResultInverterMinW(inverterWithMargin(peakSimultaneousWatts(p.getAppliances())));
    }
}
