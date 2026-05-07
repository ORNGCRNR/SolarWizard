package com.solarwizard.model;

/**
 * Represents a solar panel product with all electrical specifications.
 * To add new panels: add entries to the CATALOG array below.
 */
public class SolarPanel {

    // ── Panel catalog — edit freely to add/remove products ───────────────────
    public static final SolarPanel[] CATALOG = {
        new SolarPanel("Jinko Tiger Neo 400W",       "Jinko Solar", "Tiger Neo 400W",  400, 49.52, 41.32, 10.20, 9.69, 21.0),
        new SolarPanel("Jinko Tiger Neo 450W",       "Jinko Solar", "Tiger Neo 450W",  450, 49.95, 41.83, 11.40, 10.76, 21.5),
        new SolarPanel("Jinko Tiger Neo 550W",       "Jinko Solar", "Tiger Neo 550W",  550, 49.52, 41.32, 13.96, 13.31, 21.4),
        new SolarPanel("Jinko Tiger Neo 600W",       "Jinko Solar", "Tiger Neo 600W",  600, 53.46, 44.60, 14.35, 13.45, 22.3),
        new SolarPanel("Jinko Tiger Neo 580W Bifacial","Jinko Solar","Tiger Neo 580W Bifacial",580,49.52,41.83,14.73,13.96,22.1),
        new SolarPanel("Canadian Solar HiKu6 450W",  "Canadian Solar","HiKu6 450W",   450, 49.80, 41.80, 11.39, 10.77, 20.9),
        new SolarPanel("Canadian Solar HiKu6 550W",  "Canadian Solar","HiKu6 550W",   550, 49.80, 41.80, 13.92, 13.16, 21.1),
        new SolarPanel("LONGi Hi-MO 6 550W",         "LONGi Solar",  "Hi-MO 6 550W",  550, 50.30, 42.00, 13.85, 13.09, 21.3),
        new SolarPanel("LONGi Hi-MO X6 600W",        "LONGi Solar",  "Hi-MO X6 600W", 600, 50.60, 42.60, 14.93, 14.09, 22.0),
        new SolarPanel("Risen Titan S 550W",         "Risen Energy", "Titan S 550W",  550, 49.68, 41.68, 13.97, 13.19, 21.2),
        new SolarPanel("Trina Vertex S+ 410W",       "Trina Solar",  "Vertex S+ 410W",410, 46.72, 39.48, 11.12, 10.50, 21.4),
        new SolarPanel("Trina Vertex S+ 500W",       "Trina Solar",  "Vertex S+ 500W",500, 46.72, 39.48, 13.53, 12.76, 21.6),
        new SolarPanel("Generic 100W Panel",         "Generic",      "100W",           100, 22.40, 18.50,  5.99,  5.50, 16.0),
        new SolarPanel("Generic 200W Panel",         "Generic",      "200W",           200, 22.66, 18.50, 11.85, 10.85, 17.0),
        new SolarPanel("Generic 300W Panel",         "Generic",      "300W",           300, 32.00, 27.00, 11.80, 11.10, 18.0),
    };

    // ── Fields ────────────────────────────────────────────────────────────────
    private final String displayName;
    private final String brand;
    private final String model;
    private final double wattage;
    private final double voc;       // Open-circuit voltage (V)
    private final double vmp;       // Voltage at max power (V)
    private final double isc;       // Short-circuit current (A)
    private final double imp;       // Current at max power (A)
    private final double efficiency;// Panel efficiency (%)

    public SolarPanel(String displayName, String brand, String model,
                      double wattage, double voc, double vmp,
                      double isc, double imp, double efficiency) {
        this.displayName = displayName;
        this.brand       = brand;
        this.model       = model;
        this.wattage     = wattage;
        this.voc         = voc;
        this.vmp         = vmp;
        this.isc         = isc;
        this.imp         = imp;
        this.efficiency  = efficiency;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────
    public String getDisplayName() { return displayName; }
    public String getBrand()       { return brand; }
    public String getModel()       { return model; }
    public double getWattage()     { return wattage; }
    public double getVoc()         { return voc; }
    public double getVmp()         { return vmp; }
    public double getIsc()         { return isc; }
    public double getImp()         { return imp; }
    public double getEfficiency()  { return efficiency; }

    @Override
    public String toString() { return displayName + " (" + (int)wattage + "W)"; }
}
