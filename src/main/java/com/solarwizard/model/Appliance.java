package com.solarwizard.model;

import javafx.beans.property.*;

/**
 * Represents a single appliance/device in the load analysis.
 * Motor loads (fans, AC, fridges) require a surge multiplier for inverter sizing.
 */
public class Appliance {

    // ── Preset appliance catalog ─────────────────────────────────────────────
    public record Preset(String name, double watts, boolean isMotorLoad) {}

    public static final Preset[] PRESETS = {
        new Preset("Air Conditioner (Window)", 1200, true),
        new Preset("Air Conditioner (Split)",  750,  true),
        new Preset("Refrigerator",             150,  true),
        new Preset("Washing Machine",          500,  true),
        new Preset("Electric Fan",              75,  true),
        new Preset("LED TV",                   100,  false),
        new Preset("LED Light Bulb",            10,  false),
        new Preset("Phone Charger",             15,  false),
        new Preset("Laptop",                    65,  false),
        new Preset("Rice Cooker",              700,  false),
        new Preset("Electric Kettle",         1500,  false),
        new Preset("Router / Modem",            15,  false),
        new Preset("Desktop PC",              300,  false),
        new Preset("Water Pump",              500,  true),
        new Preset("Microwave Oven",         1000,  false),
    };

    // ── Properties ───────────────────────────────────────────────────────────
    private final StringProperty  name        = new SimpleStringProperty("");
    private final DoubleProperty  watts       = new SimpleDoubleProperty(0);
    private final DoubleProperty  hoursPerDay = new SimpleDoubleProperty(1);
    private final IntegerProperty quantity    = new SimpleIntegerProperty(1);
    private final BooleanProperty motorLoad   = new SimpleBooleanProperty(false);

    public Appliance() {}

    public Appliance(String name, double watts, double hoursPerDay, int quantity, boolean motorLoad) {
        this.name.set(name);
        this.watts.set(watts);
        this.hoursPerDay.set(hoursPerDay);
        this.quantity.set(quantity);
        this.motorLoad.set(motorLoad);
    }

    // ── Computed values ───────────────────────────────────────────────────────
    /** Wh per month for this appliance row */
    public double getMonthlyWh() {
        return watts.get() * hoursPerDay.get() * quantity.get() * 30;
    }

    /** kWh per month */
    public double getMonthlyKwh() { return getMonthlyWh() / 1000.0; }

    /** Adjusted watts for inverter sizing (motor loads ×3) */
    public double getAdjustedWatts() {
        return motorLoad.get()
            ? watts.get() * quantity.get() * 3.0
            : watts.get() * quantity.get();
    }

    // ── Property accessors ────────────────────────────────────────────────────
    public StringProperty  nameProperty()        { return name; }
    public DoubleProperty  wattsProperty()       { return watts; }
    public DoubleProperty  hoursPerDayProperty() { return hoursPerDay; }
    public IntegerProperty quantityProperty()    { return quantity; }
    public BooleanProperty motorLoadProperty()   { return motorLoad; }

    public String  getName()        { return name.get(); }
    public double  getWatts()       { return watts.get(); }
    public double  getHoursPerDay() { return hoursPerDay.get(); }
    public int     getQuantity()    { return quantity.get(); }
    public boolean isMotorLoad()    { return motorLoad.get(); }

    public void setName(String v)        { name.set(v); }
    public void setWatts(double v)       { watts.set(v); }
    public void setHoursPerDay(double v) { hoursPerDay.set(v); }
    public void setQuantity(int v)       { quantity.set(v); }
    public void setMotorLoad(boolean v)  { motorLoad.set(v); }
}
