package com.solarwizard.model;

import javafx.beans.property.*;

/**
 * Represents a single appliance/device in the load analysis.
 * Motor loads (fans, AC, fridges) require a surge multiplier for inverter sizing.
 */
public class Appliance {

    // -- Preset appliance catalog -------------------------------------------------
    public record Preset(String name, double watts, boolean isMotorLoad, double defaultPeakHours) {}

    public static final Preset[] PRESETS = {
        new Preset("Air Conditioner (Window)", 1200, true,  3.00),
        new Preset("Air Conditioner (Split)",  750,  true,  3.00),
        new Preset("Refrigerator",             150,  true,  4.00),
        new Preset("Washing Machine",          500,  true,  0.50),
        new Preset("Electric Fan",              75,  true,  3.00),
        new Preset("LED TV",                   100,  false, 3.00),
        new Preset("LED Light Bulb",            10,  false, 3.00),
        new Preset("Phone Charger",             15,  false, 1.00),
        new Preset("Laptop",                    65,  false, 2.00),
        new Preset("Rice Cooker",              700,  false, 0.50),
        new Preset("Electric Kettle",         1500,  false, 0.25),
        new Preset("Router / Modem",            15,  false, 4.00),
        new Preset("Desktop PC",               300,  false, 2.00),
        new Preset("Water Pump",               500,  true,  0.50),
        new Preset("Microwave Oven",          1000,  false, 0.25),
    };

    // -- Properties ---------------------------------------------------------------
    private final StringProperty  name        = new SimpleStringProperty("");
    private final DoubleProperty  watts       = new SimpleDoubleProperty(0);
    private final DoubleProperty  hoursPerDay = new SimpleDoubleProperty(1);
    private final IntegerProperty quantity    = new SimpleIntegerProperty(1);
    private final BooleanProperty motorLoad   = new SimpleBooleanProperty(false);
    private final DoubleProperty  peakHours   = new SimpleDoubleProperty(0.0);

    public Appliance() {}

    public Appliance(String name, double watts, double hoursPerDay, int quantity, boolean motorLoad) {
        this(name, watts, hoursPerDay, quantity, motorLoad, 0.0);
    }

    public Appliance(String name, double watts, double hoursPerDay,
                     int quantity, boolean motorLoad, double peakHours) {
        this.name.set(name);
        this.watts.set(watts);
        this.hoursPerDay.set(hoursPerDay);
        this.quantity.set(quantity);
        this.motorLoad.set(motorLoad);
        this.peakHours.set(peakHours);
    }

    // -- Computed values ----------------------------------------------------------
    /** Wh per month for this appliance row */
    public double getMonthlyWh() {
        return watts.get() * hoursPerDay.get() * quantity.get() * 30;
    }

    /** Wh per day for this appliance row */
    public double getDailyWh() {
        return watts.get() * hoursPerDay.get() * quantity.get();
    }

    /** kWh per month */
    public double getMonthlyKwh() { return getMonthlyWh() / 1000.0; }

    /** Adjusted watts for inverter sizing (motor loads x3) */
    public double getAdjustedWatts() {
        return motorLoad.get()
            ? watts.get() * quantity.get() * 3.0
            : watts.get() * quantity.get();
    }

    /**
     * Derived demand factor = peakHours / hoursPerDay.
     * This returns the raw ratio so the UI can detect and show overrun warnings.
     */
    public double getDemandFactor() {
        if (hoursPerDay.get() <= 0) return 0.0;
        return peakHours.get() / hoursPerDay.get();
    }

    /** Peak watts contribution to simultaneous load. DF is clamped to 1.0. */
    public double getPeakWatts() {
        double df = hoursPerDay.get() <= 0 ? 0.0
            : Math.min(peakHours.get() / hoursPerDay.get(), 1.0);
        return getAdjustedWatts() * df;
    }

    // -- Property accessors -------------------------------------------------------
    public StringProperty  nameProperty()        { return name; }
    public DoubleProperty  wattsProperty()       { return watts; }
    public DoubleProperty  hoursPerDayProperty() { return hoursPerDay; }
    public IntegerProperty quantityProperty()    { return quantity; }
    public BooleanProperty motorLoadProperty()   { return motorLoad; }
    public DoubleProperty  peakHoursProperty()   { return peakHours; }

    public String  getName()        { return name.get(); }
    public double  getWatts()       { return watts.get(); }
    public double  getHoursPerDay() { return hoursPerDay.get(); }
    public int     getQuantity()    { return quantity.get(); }
    public boolean isMotorLoad()    { return motorLoad.get(); }
    public double  getPeakHours()   { return peakHours.get(); }

    public void setName(String v)        { name.set(v); }
    public void setWatts(double v)       { watts.set(v); }
    public void setHoursPerDay(double v) { hoursPerDay.set(v); }
    public void setQuantity(int v)       { quantity.set(v); }
    public void setMotorLoad(boolean v)  { motorLoad.set(v); }
    public void setPeakHours(double v)   { peakHours.set(v); }
}
