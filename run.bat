@echo off
REM ═══════════════════════════════════════════════════
REM  Solar Wizard — Build & Run Script (Windows)
REM  Requirements: Java 23, Maven 3.9+
REM ═══════════════════════════════════════════════════

echo [Solar Wizard] Building project...
mvn clean package -q

IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed. Check output above.
    pause
    exit /b 1
)

echo [Solar Wizard] Build successful! Launching...
java --module-path target/SolarWizard-fat.jar ^
     --add-modules javafx.controls,javafx.fxml,javafx.graphics ^
     -jar target/SolarWizard-fat.jar

REM If the above fails (module issues with fat jar), try:
REM java -jar target/SolarWizard-fat.jar

pause
