# ☀ Solar Sizing Wizard

A desktop app for solar panel system sizing.
Built on Java 23 + JavaFX 23 + Maven 3.9.

---

## 📁 Project Structure

```
SolarWizard/
├── pom.xml
├── run.bat                        ← Quick build + run (Windows)
└── src/main/java/com/solarwizard/
    ├── app/
    │   ├── Launcher.java          ← Entry point (fat JAR compatible)
    │   └── MainApp.java           ← JavaFX Application
    ├── model/
    │   ├── Appliance.java         ← Device model + preset catalog
    │   ├── SolarPanel.java        ← Panel model + product catalog ← EDIT HERE
    │   └── SolarProject.java      ← Central data store for entire wizard
    ├── service/
    │   ├── CalcService.java       ← All formulas (edit formulas here)
    │   └── ValidationService.java ← All warning/validation checks
    ├── util/
    │   └── UiUtils.java           ← Reusable UI factory methods
    └── view/
        ├── DashboardView.java     ← Home screen
        ├── WizardShell.java       ← Sidebar nav + step container
        └── steps/
            ├── Step1LoadAnalysis.java
            ├── Step2PanelSizing.java
            ├── Step3InverterSizing.java
            ├── Step4StringConfig.java
            ├── Step5BatterySizing.java
            ├── Step6WireSizing.java
            ├── Step7BreakerSizing.java
            └── Step8Summary.java
```

---

## 🚀 How to Build & Run


### Option 1 — Manual Maven
```cmd
mvn clean package
java -jar target/SolarWizard-fat.jar
```

### Option 2 — Maven JavaFX plugin (dev mode)
```cmd
mvn javafx:run
```

---

## 📦 Package as Standalone EXE (Windows)

Requires `jpackage` (included in JDK 14+).

**Step 1 — Build the fat JAR:**
```cmd
mvn clean package
```

**Step 2 — Package as EXE:**
```cmd
jpackage ^
  --input target ^
  --name "SolarWizard" ^
  --main-jar SolarWizard-fat.jar ^
  --main-class com.solarwizard.app.Launcher ^
  --type exe ^
  --win-shortcut ^
  --win-menu ^
  --app-version 1.0.0 ^
  --vendor "Your Company" ^
  --dest dist
```

This creates `dist/SolarWizard-1.0.0.exe` — a self-contained installer that
bundles the JRE. Users do NOT need Java installed.

> **Tip:** Install [WiX Toolset](https://wixtoolset.org/) first if jpackage
> reports missing tool errors on Windows.

---

## ✏️ How to Add/Edit Solar Panels

Open `src/main/java/com/solarwizard/model/SolarPanel.java`

Find the `CATALOG` array and add entries:
```java
new SolarPanel(
    "Display Name",    // shown in dropdown
    "Brand",           // manufacturer
    "Model",           // model number
    600,               // wattage (W)
    53.46,             // Voc (V)
    44.60,             // Vmp (V)
    14.35,             // Isc (A)
    13.45,             // Imp (A)
    22.3               // efficiency (%)
),
```

---

## ✏️ How to Add/Edit Appliances

Open `src/main/java/com/solarwizard/model/Appliance.java`

Find the `PRESETS` array:
```java
new Preset("Device Name", wattsDouble, isMotorLoad),
// isMotorLoad = true for fans, AC, fridges, pumps (×3 surge factor)
```

---

## ✏️ How to Change Formulas

All formulas are in one place:
`src/main/java/com/solarwizard/service/CalcService.java`

Each method is documented with the formula source from the Setup Guide.

---

## 🧩 Wizard Steps

| Step | Screen              | Formula Source         |
|------|---------------------|------------------------|
| 1    | Load Analysis       | Chapter 2 — Daily Wh   |
| 2    | Solar Panel Sizing  | Chapter 3 — PV Power   |
| 3    | Inverter Sizing     | Chapter 4 — Surge/Eff  |
| 4    | PV String Config    | Chapter 5 — MPPT limits|
| 5    | Battery Sizing      | Chapter 6 — Ah/DOD     |
| 6    | Wire Sizing         | Chapter 7 — VDI Method |
| 7    | Breaker Sizing      | Chapter 8 — ×1.25 rule |
| 8    | Summary Report      | All chapters compiled  |
