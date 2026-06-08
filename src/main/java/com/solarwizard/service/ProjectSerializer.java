package com.solarwizard.service;

import com.solarwizard.model.Appliance;
import com.solarwizard.model.SolarProject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializes and deserializes SolarProject to/from a simple JSON-like
 * key=value flat file format (.swproj).
 *
 * No third-party JSON library needed — keeps the fat JAR minimal.
 * Format is human-readable and easy to edit manually if needed.
 *
 * To add a new field: add a write line in serialize() and a case in deserialize().
 */
public class ProjectSerializer {

    public static final String FILE_EXTENSION = ".swproj";
    public static final String FILE_DESCRIPTION = "Solar Wizard Project";

    // ── Serialize ─────────────────────────────────────────────────────────────

    /**
     * Writes a SolarProject to a .swproj file at the given path.
     */
    public static void save(SolarProject p, Path filePath) throws IOException {
        List<String> lines = new ArrayList<>();

        lines.add("# Solar Wizard Project File");
        lines.add("# Do not edit manually unless you know what you are doing.");
        lines.add("");

        // Metadata
        w(lines, "projectName",          p.getProjectName());

        // Step 1
        w(lines, "loadMode",             p.getLoadMode().name());
        w(lines, "sunPeakHours",         p.getSunPeakHours());
        w(lines, "systemLossPercent",    p.getSystemLossPercent());
        w(lines, "monthlyKwh",           p.getMonthlyKwh());
        w(lines, "monthlyBill",          p.getMonthlyBill());
        w(lines, "ratePerKwh",           p.getRatePerKwh());

        // Appliances
        w(lines, "applianceCount",       p.getAppliances().size());
        for (int i = 0; i < p.getAppliances().size(); i++) {
            Appliance a = p.getAppliances().get(i);
            w(lines, "appliance." + i + ".name",        a.getName());
            w(lines, "appliance." + i + ".watts",       a.getWatts());
            w(lines, "appliance." + i + ".hoursPerDay", a.getHoursPerDay());
            w(lines, "appliance." + i + ".quantity",    a.getQuantity());
            w(lines, "appliance." + i + ".motorLoad",   a.isMotorLoad());
            w(lines, "appliance." + i + ".peakHours",   a.getPeakHours());
        }

        // Step 2
        w(lines, "panelBrand",           p.getPanelBrand());
        w(lines, "panelModel",           p.getPanelModel());
        w(lines, "panelWattage",         p.getPanelWattage());
        w(lines, "panelVoc",             p.getPanelVoc());
        w(lines, "panelVmp",             p.getPanelVmp());
        w(lines, "panelIsc",             p.getPanelIsc());
        w(lines, "panelImp",             p.getPanelImp());
        w(lines, "panelEfficiency",      p.getPanelEfficiency());
        w(lines, "panelSafetyFactor",    p.isPanelSafetyFactor());
        w(lines, "panelWiring",          p.getPanelWiring().name());

        // Step 5
        w(lines, "chargeControllerType",         p.getChargeControllerType().name());
        w(lines, "chargeControllerBrand",        p.getChargeControllerBrand());
        w(lines, "chargeControllerModel",        p.getChargeControllerModel());
        w(lines, "chargeControllerRatedCurrent", p.getChargeControllerRatedCurrent());
        w(lines, "chargeControllerMaxPvVoltage", p.getChargeControllerMaxPvVoltage());
        w(lines, "chargeControllerMaxPvPower",   p.getChargeControllerMaxPvPower());

        // Step 3
        w(lines, "inverterBrand",        p.getInverterBrand());
        w(lines, "inverterModel",        p.getInverterModel());
        w(lines, "inverterRatedPower",   p.getInverterRatedPower());
        w(lines, "inverterMaxPvInput",   p.getInverterMaxPvInput());
        w(lines, "inverterSysVoltage",   p.getInverterSysVoltage());
        w(lines, "inverterBattMinV",     p.getInverterBattMinV());
        w(lines, "inverterBattMaxV",     p.getInverterBattMaxV());
        w(lines, "inverterMpptCount",    p.getInverterMpptCount());
        w(lines, "inverterMaxVPerMppt",  p.getInverterMaxVPerMppt());
        w(lines, "inverterMaxIPerMppt",  p.getInverterMaxIPerMppt());
        w(lines, "inverterMaxPvPerMppt", p.getInverterMaxPvPerMppt());
        w(lines, "inverterAcOutput",     p.getInverterAcOutput());

        // Step 4
        w(lines, "batteryBrand",         p.getBatteryBrand());
        w(lines, "batteryModel",         p.getBatteryModel());
        w(lines, "batteryVoltage",       p.getBatteryVoltage());
        w(lines, "batteryCapacityAh",    p.getBatteryCapacityAh());
        w(lines, "batteryDod",           p.getBatteryDod());
        w(lines, "batteryQuantity",      p.getBatteryQuantity());
        w(lines, "autonomyHours",        p.getAutonomyHours());

        // Step 5
        w(lines, "wirePvToInverterM",      p.getWirePvToInverterM());
        w(lines, "wireSccToBatteryM",      p.getWireSccToBatteryM());
        w(lines, "wireBatteryToInverterM", p.getWireBatteryToInverterM());
        w(lines, "wireInverterToLoadM",    p.getWireInverterToLoadM());
        w(lines, "wireVoltageDrop",        p.getWireVoltageDrop());

        Files.write(filePath, lines, StandardCharsets.UTF_8);
    }

    // ── Deserialize ───────────────────────────────────────────────────────────

    /**
     * Reads a .swproj file and returns a fully populated SolarProject.
     */
    public static SolarProject load(Path filePath) throws IOException {
        SolarProject p = new SolarProject();
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);

        int applianceCount = 0;
        // Temporary appliance storage
        java.util.Map<String, String> raw = new java.util.LinkedHashMap<>();

        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String key = line.substring(0, eq).trim();
            String val = line.substring(eq + 1).trim();
            raw.put(key, val);
        }

        for (var entry : raw.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            try {
                switch (key) {
                    case "projectName"          -> p.setProjectName(val);
                    case "loadMode"             -> p.setLoadMode(SolarProject.LoadMode.valueOf(val));
                    case "sunPeakHours"         -> p.setSunPeakHours(d(val));
                    case "systemLossPercent"    -> p.setSystemLossPercent(d(val));
                    case "monthlyKwh"           -> p.setMonthlyKwh(d(val));
                    case "monthlyBill"          -> p.setMonthlyBill(d(val));
                    case "ratePerKwh"           -> p.setRatePerKwh(d(val));
                    case "applianceCount"       -> applianceCount = i(val);
                    case "panelBrand"           -> p.setPanelBrand(val);
                    case "panelModel"           -> p.setPanelModel(val);
                    case "panelWattage"         -> p.setPanelWattage(d(val));
                    case "panelVoc"             -> p.setPanelVoc(d(val));
                    case "panelVmp"             -> p.setPanelVmp(d(val));
                    case "panelIsc"             -> p.setPanelIsc(d(val));
                    case "panelImp"             -> p.setPanelImp(d(val));
                    case "panelEfficiency"      -> p.setPanelEfficiency(d(val));
                    case "panelSafetyFactor"    -> p.setPanelSafetyFactor(b(val));
                    case "panelWiring"          -> p.setPanelWiring(SolarProject.PanelWiring.valueOf(val));
                    case "chargeControllerType" -> p.setChargeControllerType(SolarProject.ChargeControllerType.valueOf(val));
                    case "chargeControllerBrand" -> p.setChargeControllerBrand(val);
                    case "chargeControllerModel" -> p.setChargeControllerModel(val);
                    case "chargeControllerRatedCurrent" -> p.setChargeControllerRatedCurrent(d(val));
                    case "chargeControllerMaxPvVoltage" -> p.setChargeControllerMaxPvVoltage(d(val));
                    case "chargeControllerMaxPvPower" -> p.setChargeControllerMaxPvPower(d(val));
                    case "inverterBrand"        -> p.setInverterBrand(val);
                    case "inverterModel"        -> p.setInverterModel(val);
                    case "inverterRatedPower"   -> p.setInverterRatedPower(d(val));
                    case "inverterMaxPvInput"   -> p.setInverterMaxPvInput(d(val));
                    case "inverterSysVoltage"   -> p.setInverterSysVoltage(d(val));
                    case "inverterBattMinV"     -> p.setInverterBattMinV(d(val));
                    case "inverterBattMaxV"     -> p.setInverterBattMaxV(d(val));
                    case "inverterMpptCount"    -> p.setInverterMpptCount(i(val));
                    case "inverterMaxVPerMppt"  -> p.setInverterMaxVPerMppt(d(val));
                    case "inverterMaxIPerMppt"  -> p.setInverterMaxIPerMppt(d(val));
                    case "inverterMaxPvPerMppt" -> p.setInverterMaxPvPerMppt(d(val));
                    case "inverterAcOutput"     -> p.setInverterAcOutput(d(val));
                    case "batteryBrand"         -> p.setBatteryBrand(val);
                    case "batteryModel"         -> p.setBatteryModel(val);
                    case "batteryVoltage"       -> p.setBatteryVoltage(d(val));
                    case "batteryCapacityAh"    -> p.setBatteryCapacityAh(d(val));
                    case "batteryDod"           -> p.setBatteryDod(d(val));
                    case "batteryQuantity"      -> p.setBatteryQuantity(i(val));
                    case "autonomyHours"        -> p.setAutonomyHours(d(val));
                    case "wirePvToInverterM"    -> p.setWirePvToInverterM(d(val));
                    case "wireSccToBatteryM"    -> p.setWireSccToBatteryM(d(val));
                    case "wireBatteryToInverterM" -> p.setWireBatteryToInverterM(d(val));
                    case "wireInverterToLoadM"  -> p.setWireInverterToLoadM(d(val));
                    case "wireVoltageDrop"      -> p.setWireVoltageDrop(d(val));
                    default -> {} // future fields — silently skip
                }
            } catch (Exception ignored) {
                // Corrupted field — skip gracefully
            }
        }

        // Rebuild appliances from indexed keys
        for (int idx = 0; idx < applianceCount; idx++) {
            String name     = raw.getOrDefault("appliance." + idx + ".name",        "Device");
            double watts    = d(raw.getOrDefault("appliance." + idx + ".watts",     "0"));
            double hours    = d(raw.getOrDefault("appliance." + idx + ".hoursPerDay","1"));
            int    qty      = i(raw.getOrDefault("appliance." + idx + ".quantity",  "1"));
            boolean motor   = b(raw.getOrDefault("appliance." + idx + ".motorLoad", "false"));
            double peakHours = d(raw.getOrDefault("appliance." + idx + ".peakHours", "0.0"));
            p.getAppliances().add(new Appliance(name, watts, hours, qty, motor, peakHours));
        }

        return p;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static void w(List<String> lines, String key, Object val) {
        lines.add(key + "=" + val);
    }
    private static double  d(String v) { return Double.parseDouble(v); }
    private static int     i(String v) { return Integer.parseInt(v); }
    private static boolean b(String v) { return Boolean.parseBoolean(v); }
}
