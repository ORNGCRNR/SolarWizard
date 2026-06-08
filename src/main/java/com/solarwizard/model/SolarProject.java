package com.solarwizard.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Central data model for a single solar sizing project.
 * Single source of truth passed between all wizard steps.
 */
public class SolarProject {

    // ── Project metadata ──────────────────────────────────────────────────────
    private String projectName = "New Project";

    // ── Step 1-2: Load and energy requirement ─────────────────────────────────
    public enum LoadMode { DIRECT, BILL, DEVICE }
    public enum PanelWiring { SERIES, PARALLEL }
    public enum ChargeControllerType { PWM, MPPT }

    private LoadMode loadMode     = LoadMode.DEVICE;
    private double sunPeakHours   = 4.0;
    private double systemLossPercent = 30.0;
    private double monthlyKwh     = 0;
    private double monthlyBill    = 0;
    private double ratePerKwh     = 0;
    private final ObservableList<Appliance> appliances = FXCollections.observableArrayList();

    // ── Step 4: Solar Panel Sizing (Custom only) ──────────────────────────────
    private String  panelBrand      = "";
    private String  panelModel      = "";
    private double  panelWattage    = 0;
    private double  panelVoc        = 0;
    private double  panelVmp        = 0;
    private double  panelIsc        = 0;
    private double  panelImp        = 0;
    private double  panelEfficiency = 0;
    private boolean panelSafetyFactor = true;
    private PanelWiring panelWiring = PanelWiring.PARALLEL;

    // Solar charge controller sizing
    private ChargeControllerType chargeControllerType = ChargeControllerType.MPPT;
    private String  chargeControllerBrand = "";
    private String  chargeControllerModel = "";
    private double  chargeControllerRatedCurrent = 0;
    private double  chargeControllerMaxPvVoltage = 0;
    private double  chargeControllerMaxPvPower = 0;

    // ── Step 6: Inverter Sizing (Custom) ──────────────────────────────────────
    private String  inverterBrand      = "";
    private String  inverterModel      = "";
    private double  inverterRatedPower = 0;   // W
    private double  inverterMaxPvInput = 0;   // W
    private double  inverterSysVoltage = 12;  // V
    private double  inverterBattMinV   = 40;  // V
    private double  inverterBattMaxV   = 60;  // V
    private int     inverterMpptCount  = 1;
    private double  inverterMaxVPerMppt= 500; // V
    private double  inverterMaxIPerMppt= 15;  // A
    private double  inverterMaxPvPerMppt = 3500; // W
    private double  inverterAcOutput   = 0;   // W

    // ── Step 3: Battery Sizing ────────────────────────────────────────────────
    private String  batteryBrand      = "";
    private String  batteryModel      = "";
    private double  batteryVoltage    = 12;
    private double  batteryCapacityAh = 100;
    private double  batteryDod        = 0.50;
    private int     batteryQuantity   = 1;
    private double  autonomyHours     = 48;   // stored as hours; UI presents days of autonomy

    // ── Step 7: Wire Sizing ───────────────────────────────────────────────────
    private double wirePvToInverterM      = 10;
    private double wireSccToBatteryM      = 2;
    private double wireBatteryToInverterM = 2;
    private double wireInverterToLoadM    = 15;
    private double wireVoltageDrop        = 2;

    // ── Computed results (set by CalcService) ─────────────────────────────────
    private double resultDailyWh        = 0;
    private double resultExpectedDailyWh = 0;
    private double resultTotalEnergyRequirementWh = 0;
    private double resultRequiredPvW    = 0;
    private int    resultPanelsNeeded   = 0;
    private double resultTotalPvW       = 0;
    private double resultInverterMinW   = 0;
    private double resultBatteryAh      = 0;
    private int    resultBatteriesNeeded= 0;
    private double resultBatteryBankEnergyWh = 0;
    private double resultRecommendedEnergyWh = 0;
    private double resultArrayVoc       = 0;
    private double resultArrayVmp       = 0;
    private double resultArrayIsc       = 0;
    private double resultArrayImp       = 0;
    private double resultRequiredSccCurrent = 0;

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public String    getProjectName()           { return projectName; }
    public void      setProjectName(String v)   { projectName = v; }

    public LoadMode  getLoadMode()              { return loadMode; }
    public void      setLoadMode(LoadMode v)    { loadMode = v; }
    public double    getSunPeakHours()          { return sunPeakHours; }
    public void      setSunPeakHours(double v)  { sunPeakHours = v; }
    public double    getSystemLossPercent()     { return systemLossPercent; }
    public void      setSystemLossPercent(double v) { systemLossPercent = v; }
    public double    getMonthlyKwh()            { return monthlyKwh; }
    public void      setMonthlyKwh(double v)    { monthlyKwh = v; }
    public double    getMonthlyBill()           { return monthlyBill; }
    public void      setMonthlyBill(double v)   { monthlyBill = v; }
    public double    getRatePerKwh()            { return ratePerKwh; }
    public void      setRatePerKwh(double v)    { ratePerKwh = v; }
    public ObservableList<Appliance> getAppliances() { return appliances; }

    public String    getPanelBrand()            { return panelBrand; }
    public void      setPanelBrand(String v)    { panelBrand = v; }
    public String    getPanelModel()            { return panelModel; }
    public void      setPanelModel(String v)    { panelModel = v; }
    public double    getPanelWattage()          { return panelWattage; }
    public void      setPanelWattage(double v)  { panelWattage = v; }
    public double    getPanelVoc()              { return panelVoc; }
    public void      setPanelVoc(double v)      { panelVoc = v; }
    public double    getPanelVmp()              { return panelVmp; }
    public void      setPanelVmp(double v)      { panelVmp = v; }
    public double    getPanelIsc()              { return panelIsc; }
    public void      setPanelIsc(double v)      { panelIsc = v; }
    public double    getPanelImp()              { return panelImp; }
    public void      setPanelImp(double v)      { panelImp = v; }
    public double    getPanelEfficiency()       { return panelEfficiency; }
    public void      setPanelEfficiency(double v){ panelEfficiency = v; }
    public boolean   isPanelSafetyFactor()      { return panelSafetyFactor; }
    public void      setPanelSafetyFactor(boolean v){ panelSafetyFactor = v; }
    public PanelWiring getPanelWiring()         { return panelWiring; }
    public void      setPanelWiring(PanelWiring v){ panelWiring = v; }

    public ChargeControllerType getChargeControllerType() { return chargeControllerType; }
    public void      setChargeControllerType(ChargeControllerType v){ chargeControllerType = v; }
    public String    getChargeControllerBrand() { return chargeControllerBrand; }
    public void      setChargeControllerBrand(String v){ chargeControllerBrand = v; }
    public String    getChargeControllerModel() { return chargeControllerModel; }
    public void      setChargeControllerModel(String v){ chargeControllerModel = v; }
    public double    getChargeControllerRatedCurrent(){ return chargeControllerRatedCurrent; }
    public void      setChargeControllerRatedCurrent(double v){ chargeControllerRatedCurrent = v; }
    public double    getChargeControllerMaxPvVoltage(){ return chargeControllerMaxPvVoltage; }
    public void      setChargeControllerMaxPvVoltage(double v){ chargeControllerMaxPvVoltage = v; }
    public double    getChargeControllerMaxPvPower(){ return chargeControllerMaxPvPower; }
    public void      setChargeControllerMaxPvPower(double v){ chargeControllerMaxPvPower = v; }

    public String    getInverterBrand()         { return inverterBrand; }
    public void      setInverterBrand(String v) { inverterBrand = v; }
    public String    getInverterModel()         { return inverterModel; }
    public void      setInverterModel(String v) { inverterModel = v; }
    public double    getInverterRatedPower()    { return inverterRatedPower; }
    public void      setInverterRatedPower(double v){ inverterRatedPower = v; }
    public double    getInverterMaxPvInput()    { return inverterMaxPvInput; }
    public void      setInverterMaxPvInput(double v){ inverterMaxPvInput = v; }
    public double    getInverterSysVoltage()    { return inverterSysVoltage; }
    public void      setInverterSysVoltage(double v){ inverterSysVoltage = v; }
    public double    getInverterBattMinV()      { return inverterBattMinV; }
    public void      setInverterBattMinV(double v){ inverterBattMinV = v; }
    public double    getInverterBattMaxV()      { return inverterBattMaxV; }
    public void      setInverterBattMaxV(double v){ inverterBattMaxV = v; }
    public int       getInverterMpptCount()     { return inverterMpptCount; }
    public void      setInverterMpptCount(int v){ inverterMpptCount = v; }
    public double    getInverterMaxVPerMppt()   { return inverterMaxVPerMppt; }
    public void      setInverterMaxVPerMppt(double v){ inverterMaxVPerMppt = v; }
    public double    getInverterMaxIPerMppt()   { return inverterMaxIPerMppt; }
    public void      setInverterMaxIPerMppt(double v){ inverterMaxIPerMppt = v; }
    public double    getInverterMaxPvPerMppt()  { return inverterMaxPvPerMppt; }
    public void      setInverterMaxPvPerMppt(double v){ inverterMaxPvPerMppt = v; }
    public double    getInverterAcOutput()      { return inverterAcOutput; }
    public void      setInverterAcOutput(double v){ inverterAcOutput = v; }

    public String    getBatteryBrand()          { return batteryBrand; }
    public void      setBatteryBrand(String v)  { batteryBrand = v; }
    public String    getBatteryModel()          { return batteryModel; }
    public void      setBatteryModel(String v)  { batteryModel = v; }
    public double    getBatteryVoltage()        { return batteryVoltage; }
    public void      setBatteryVoltage(double v){ batteryVoltage = v; }
    public double    getBatteryCapacityAh()     { return batteryCapacityAh; }
    public void      setBatteryCapacityAh(double v){ batteryCapacityAh = v; }
    public double    getBatteryDod()            { return batteryDod; }
    public void      setBatteryDod(double v)    { batteryDod = v; }
    public int       getBatteryQuantity()        { return batteryQuantity; }
    public void      setBatteryQuantity(int v)   { batteryQuantity = v; }
    public double    getAutonomyHours()         { return autonomyHours; }
    public void      setAutonomyHours(double v) { autonomyHours = v; }
    public double    getAutonomyDays()          { return autonomyHours / 24.0; }
    public void      setAutonomyDays(double v)  { autonomyHours = v * 24.0; }

    public double    getWirePvToInverterM()     { return wirePvToInverterM; }
    public void      setWirePvToInverterM(double v){ wirePvToInverterM = v; }
    public double    getWireSccToBatteryM()     { return wireSccToBatteryM; }
    public void      setWireSccToBatteryM(double v){ wireSccToBatteryM = v; }
    public double    getWireBatteryToInverterM(){ return wireBatteryToInverterM; }
    public void      setWireBatteryToInverterM(double v){ wireBatteryToInverterM = v; }
    public double    getWireInverterToLoadM()   { return wireInverterToLoadM; }
    public void      setWireInverterToLoadM(double v){ wireInverterToLoadM = v; }
    public double    getWireVoltageDrop()       { return wireVoltageDrop; }
    public void      setWireVoltageDrop(double v){ wireVoltageDrop = v; }

    public double    getResultDailyWh()         { return resultDailyWh; }
    public void      setResultDailyWh(double v) { resultDailyWh = v; }
    public double    getResultExpectedDailyWh() { return resultExpectedDailyWh; }
    public void      setResultExpectedDailyWh(double v){ resultExpectedDailyWh = v; }
    public double    getResultTotalEnergyRequirementWh(){ return resultTotalEnergyRequirementWh; }
    public void      setResultTotalEnergyRequirementWh(double v){ resultTotalEnergyRequirementWh = v; }
    public double    getResultRequiredPvW()     { return resultRequiredPvW; }
    public void      setResultRequiredPvW(double v){ resultRequiredPvW = v; }
    public int       getResultPanelsNeeded()    { return resultPanelsNeeded; }
    public void      setResultPanelsNeeded(int v){ resultPanelsNeeded = v; }
    public double    getResultTotalPvW()        { return resultTotalPvW; }
    public void      setResultTotalPvW(double v){ resultTotalPvW = v; }
    public double    getResultInverterMinW()    { return resultInverterMinW; }
    public void      setResultInverterMinW(double v){ resultInverterMinW = v; }
    public double    getResultBatteryAh()       { return resultBatteryAh; }
    public void      setResultBatteryAh(double v){ resultBatteryAh = v; }
    public int       getResultBatteriesNeeded() { return resultBatteriesNeeded; }
    public void      setResultBatteriesNeeded(int v){ resultBatteriesNeeded = v; }
    public double    getResultBatteryBankEnergyWh(){ return resultBatteryBankEnergyWh; }
    public void      setResultBatteryBankEnergyWh(double v){ resultBatteryBankEnergyWh = v; }
    public double    getResultRecommendedEnergyWh(){ return resultRecommendedEnergyWh; }
    public void      setResultRecommendedEnergyWh(double v){ resultRecommendedEnergyWh = v; }
    public double    getResultArrayVoc()        { return resultArrayVoc; }
    public void      setResultArrayVoc(double v){ resultArrayVoc = v; }
    public double    getResultArrayVmp()        { return resultArrayVmp; }
    public void      setResultArrayVmp(double v){ resultArrayVmp = v; }
    public double    getResultArrayIsc()        { return resultArrayIsc; }
    public void      setResultArrayIsc(double v){ resultArrayIsc = v; }
    public double    getResultArrayImp()        { return resultArrayImp; }
    public void      setResultArrayImp(double v){ resultArrayImp = v; }
    public double    getResultRequiredSccCurrent(){ return resultRequiredSccCurrent; }
    public void      setResultRequiredSccCurrent(double v){ resultRequiredSccCurrent = v; }
}
