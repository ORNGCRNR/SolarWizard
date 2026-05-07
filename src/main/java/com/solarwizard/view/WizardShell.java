package com.solarwizard.view;

import com.solarwizard.model.SolarProject;
import com.solarwizard.service.ProjectStore;
import com.solarwizard.view.steps.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main wizard shell — left sidebar + content area.
 * Includes a persistent "Save Draft" button in the sidebar header.
 */
public class WizardShell {

    public interface StepView {
        javafx.scene.Node getRoot();
        String getStepTitle();
        void   onEnter();
        void   onLeave();
    }

    private static final String[] STEP_LABELS = {
        "Load Analysis", "Panel Sizing", "Inverter Sizing",
        "Battery Sizing", "Wire Sizing", "Breaker Sizing", "Summary Report"
    };
    private static final String[] STEP_ICONS = {
        "⚡", "☀", "🔌", "🔋", "〰", "⚡", "📋"
    };

    private final BorderPane   root        = new BorderPane();
    private final VBox         sideNav     = new VBox(0);
    private final StackPane    contentArea = new StackPane();
    private final List<Button> navButtons  = new ArrayList<>();
    private final Label        saveFeedbackLbl = new Label();

    private final List<StepView> steps;
    private int currentStep = 0;

    public WizardShell(Stage stage, SolarProject project) {
        steps = List.of(
            new Step1LoadAnalysis(project, this),
            new Step2PanelSizing(project, this),
            new Step3InverterSizing(project, this),
            new Step4BatterySizing(project, this),
            new Step5WireSizing(project, this),
            new Step6BreakerSizing(project, this),
            new Step7Summary(project, this)
        );
        buildSidebar(stage, project);
        root.getStyleClass().add("wizard-shell");
        root.setLeft(sideNav);
        root.setCenter(contentArea);
        navigateTo(0);
    }

    private void buildSidebar(Stage stage, SolarProject project) {
        sideNav.getStyleClass().add("sidebar");
        sideNav.setPrefWidth(200);

        // Back to dashboard
        Button backBtn = new Button("← Dashboard");
        backBtn.getStyleClass().add("back-btn");
        backBtn.setMaxWidth(Double.MAX_VALUE);
        backBtn.setOnAction(e -> {
            DashboardView dash = new DashboardView(stage, project);
            stage.getScene().setRoot(dash.getRoot());
        });

        // Save draft button
        Button saveBtn = new Button("💾  Save Draft");
        saveBtn.getStyleClass().add("save-draft-btn");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> saveDraft(project));

        // Feedback label (shows "Saved!" briefly)
        saveFeedbackLbl.getStyleClass().add("save-feedback");
        saveFeedbackLbl.setMaxWidth(Double.MAX_VALUE);
        saveFeedbackLbl.setAlignment(Pos.CENTER);

        Region div = new Region();
        div.getStyleClass().add("sidebar-divider");
        div.setPrefHeight(1);

        sideNav.getChildren().addAll(backBtn, saveBtn, saveFeedbackLbl, div);

        // Step nav buttons
        for (int i = 0; i < STEP_LABELS.length; i++) {
            final int idx = i;
            Button btn = new Button(STEP_ICONS[i] + "  " + STEP_LABELS[i]);
            btn.getStyleClass().add("nav-btn");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> navigateTo(idx));
            navButtons.add(btn);
            sideNav.getChildren().add(btn);
        }

        // Project name label at bottom of sidebar
        Region vSpacer = new Region();
        VBox.setVgrow(vSpacer, Priority.ALWAYS);
        Label projLbl = new Label("📋 " + project.getProjectName());
        projLbl.getStyleClass().add("sidebar-project-name");
        projLbl.setPadding(new Insets(10, 12, 10, 12));
        projLbl.setWrapText(true);
        sideNav.getChildren().addAll(vSpacer, projLbl);
    }

    private void saveDraft(SolarProject project) {
        try {
            ProjectStore.get().save(project);
            showSaveFeedback("✔ Saved!");
        } catch (IOException ex) {
            showSaveFeedback("✕ Save failed");
        }
    }

    private void showSaveFeedback(String message) {
        saveFeedbackLbl.setText(message);
        // Clear after 2 seconds
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() -> saveFeedbackLbl.setText(""));
        }).start();
    }

    public void navigateTo(int idx) {
        if (idx < 0 || idx >= steps.size()) return;
        if (idx != currentStep) steps.get(currentStep).onLeave();
        currentStep = idx;
        navButtons.forEach(b -> b.getStyleClass().remove("nav-btn-active"));
        navButtons.get(currentStep).getStyleClass().add("nav-btn-active");
        steps.get(currentStep).onEnter();
        contentArea.getChildren().setAll(steps.get(currentStep).getRoot());
    }

    public void nextStep() { navigateTo(currentStep + 1); }
    public void prevStep() { navigateTo(currentStep - 1); }
    public Parent getRoot() { return root; }
}
