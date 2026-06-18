package com.solarwizard.view;

import com.solarwizard.model.SolarProject;
import com.solarwizard.service.AppSettings;
import com.solarwizard.service.GuideService;
import com.solarwizard.service.ProjectSerializer;
import com.solarwizard.service.ProjectStore;
import com.solarwizard.service.ThemeService;
import com.solarwizard.util.UiUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Dashboard — home screen with full project persistence.
 *
 * Features:
 *  - Projects persist to disk via ProjectStore + ProjectSerializer
 *  - Save directory is configurable and remembered between sessions
 *  - Import / Export .swproj files
 *  - Rename / Delete / Open projects
 */
public class DashboardView {

    private final BorderPane  root  = new BorderPane();
    private final Stage       stage;
    private final SolarProject activeProject;  // shared mutable project context

    private Label     statCountLbl;
    private VBox      projectListBox;
    private TextField searchField;

    public DashboardView(Stage stage, SolarProject activeProject) {
        this.stage         = stage;
        this.activeProject = activeProject;
        build();
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private void build() {
        root.getStyleClass().add("dashboard");
        root.setCenter(buildMainArea());
        root.setBottom(buildBottomBar());
    }

    private StackPane buildMainArea() {
        ScrollPane center = buildCenter();
        Button guideBtn = UiUtils.guideButton(this::showDashboardGuide);
        StackPane.setAlignment(guideBtn, Pos.TOP_LEFT);
        StackPane.setMargin(guideBtn, new Insets(12, 0, 0, 12));

        StackPane area = new StackPane(center, guideBtn);
        area.getStyleClass().add("dashboard");
        return area;
    }

    private void showDashboardGuide() {
        UiUtils.showGuideDialog(
            root.getScene() != null ? root.getScene().getWindow() : null,
            "Main Menu",
            GuideService.guideFor("Main Menu"));
    }

    private ScrollPane buildCenter() {
        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(36, 0, 24, 0));
        content.setMaxWidth(640);

        // Title
        Label sunIcon = new Label("☀");
        sunIcon.getStyleClass().add("dashboard-sun-toggle");
        sunIcon.setTooltip(new Tooltip("A little sunshine"));
        sunIcon.setOnMouseClicked(e -> ThemeService.toggleWithFade(stage.getScene()));

        Label title = new Label("Solar Sizing Wizard");
        title.getStyleClass().add("dashboard-title");
        HBox titleRow = new HBox(10, sunIcon, title);
        titleRow.getStyleClass().add("dashboard-title-row");
        titleRow.setAlignment(Pos.CENTER);

        Label subtitle = new Label("Design your off-grid solar system step by step");
        subtitle.getStyleClass().add("dashboard-subtitle");
        VBox titleBox  = new VBox(6, titleRow, subtitle);
        titleBox.setAlignment(Pos.CENTER);

        // Top row: stat card + save-directory card side by side
        HBox topRow = new HBox(16, buildStatCard(), buildSaveDirectoryCard());
        topRow.setAlignment(Pos.CENTER);

        content.getChildren().addAll(titleBox, topRow, buildPastProjectsSection());

        HBox wrapper = new HBox(content);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.getStyleClass().add("dashboard");

        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().addAll("dashboard-scroll", "dashboard");
        return scroll;
    }

    // ── Stat card ─────────────────────────────────────────────────────────────

    private VBox buildStatCard() {
        statCountLbl = new Label(String.valueOf(ProjectStore.get().count()));
        statCountLbl.getStyleClass().add("stat-count");
        Label icon  = new Label("📁"); icon.getStyleClass().add("stat-icon");
        Label label = new Label("Total Projects"); label.getStyleClass().add("stat-label");

        VBox card = new VBox(6, icon, statCountLbl, label);
        card.getStyleClass().add("stat-card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setPrefWidth(180);
        return card;
    }

    // ── Save directory card ───────────────────────────────────────────────────

    private VBox buildSaveDirectoryCard() {
        Label title = new Label("📂  Save Location");
        title.getStyleClass().add("specs-title");

        Label pathLbl = new Label(AppSettings.get().getSaveDirectory().toString());
        pathLbl.getStyleClass().add("field-hint");
        pathLbl.setWrapText(true);
        pathLbl.setMaxWidth(280);

        Button changeBtn = new Button("⊟  Change Directory");
        changeBtn.getStyleClass().add("row-icon-btn");
        changeBtn.setOnAction(e -> changeSaveDirectory(pathLbl));

        VBox card = new VBox(8, title, pathLbl, changeBtn);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setPrefWidth(280);
        return card;
    }

    private void changeSaveDirectory(Label pathLbl) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose Save Directory for Projects");
        File current = AppSettings.get().getSaveDirectory().toFile();
        if (current.exists()) chooser.setInitialDirectory(current);

        File chosen = chooser.showDialog(stage);
        if (chosen != null) {
            AppSettings.get().setSaveDirectory(chosen.toPath());
            pathLbl.setText(chosen.getAbsolutePath());
            ProjectStore.get().refresh();
            refreshList(searchField != null ? searchField.getText() : "");
            statCountLbl.setText(String.valueOf(ProjectStore.get().count()));
        }
    }

    // ── Past Projects section ─────────────────────────────────────────────────

    private VBox buildPastProjectsSection() {
        Label sectionTitle = new Label("⊟  Past Computed Projects");
        sectionTitle.getStyleClass().add("section-title");

        searchField = new TextField();
        searchField.setPromptText("🔍  Search projects...");
        searchField.getStyleClass().add("search-field");
        searchField.setMaxWidth(Double.MAX_VALUE);
        searchField.textProperty().addListener((obs, o, n) -> refreshList(n));

        // Import button
        Button importBtn = new Button("⤓  Import");
        importBtn.getStyleClass().add("row-icon-btn");
        importBtn.setOnAction(e -> importProject());

        HBox headerRow = new HBox(8, sectionTitle, new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, importBtn);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        projectListBox = new VBox(8);
        refreshList("");

        VBox section = new VBox(12, headerRow, searchField, projectListBox);
        section.getStyleClass().add("card");
        section.setPadding(new Insets(20));
        section.setMaxWidth(640);
        return section;
    }

    // ── Project list ──────────────────────────────────────────────────────────

    private void refreshList(String filter) {
        projectListBox.getChildren().clear();
        List<ProjectStore.ProjectMeta> all = ProjectStore.get().getAll();

        List<ProjectStore.ProjectMeta> filtered = all.stream()
            .filter(m -> filter.isBlank() || m.name().toLowerCase().contains(filter.toLowerCase()))
            .toList();

        if (filtered.isEmpty()) {
            projectListBox.getChildren().add(buildEmptyState());
        } else {
            filtered.forEach(meta -> projectListBox.getChildren().add(buildProjectRow(meta)));
        }
    }

    private VBox buildEmptyState() {
        Label icon = new Label("⊞"); icon.getStyleClass().add("empty-icon");
        Label msg  = new Label("No projects yet"); msg.getStyleClass().add("empty-msg");
        Label sub  = new Label("Start your first solar panel sizing design"); sub.getStyleClass().add("empty-sub");
        Button btn = new Button("+ CREATE FIRST PROJECT"); btn.getStyleClass().add("create-btn");
        btn.setOnAction(e -> createAndOpenProject());
        VBox box = new VBox(8, icon, msg, sub, btn);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        return box;
    }

    private HBox buildProjectRow(ProjectStore.ProjectMeta meta) {
        Label nameLbl = new Label("☀  " + meta.name());
        nameLbl.getStyleClass().add("project-name");

        Label fileLbl = new Label(meta.fileName());
        fileLbl.getStyleClass().add("field-hint");

        VBox nameBox = new VBox(2, nameLbl, fileLbl);
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        // Open
        Button openBtn = new Button("▶ Open");
        openBtn.getStyleClass().add("row-open-btn");
        openBtn.setOnAction(e -> openProject(meta));

        // Export
        Button exportBtn = new Button("⤒");
        exportBtn.getStyleClass().add("row-icon-btn");
        exportBtn.setTooltip(new Tooltip("Export .swproj file"));
        exportBtn.setOnAction(e -> exportProject(meta));

        // Rename
        Button renameBtn = new Button("✎");
        renameBtn.getStyleClass().add("row-icon-btn");
        renameBtn.setTooltip(new Tooltip("Rename"));
        renameBtn.setOnAction(e -> renameProject(meta));

        // Delete
        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("row-delete-btn");
        deleteBtn.setTooltip(new Tooltip("Delete"));
        deleteBtn.setOnAction(e -> deleteProject(meta));

        HBox row = new HBox(8, nameBox, spacer, openBtn, exportBtn, renameBtn, deleteBtn);
        row.getStyleClass().add("project-row");
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void createAndOpenProject() {
        String name = promptName("New Project", "Project " + (ProjectStore.get().count() + 1));
        if (name == null) return;
        try {
            SolarProject p = ProjectStore.get().createNew(name);
            openWizard(p);
        } catch (IOException ex) {
            showError("Could not create project", ex.getMessage());
        }
    }

    private void openProject(ProjectStore.ProjectMeta meta) {
        try {
            SolarProject p = ProjectStore.get().load(meta);
            openWizard(p);
        } catch (IOException ex) {
            showError("Could not open project", ex.getMessage());
        }
    }

    private void renameProject(ProjectStore.ProjectMeta meta) {
        String newName = promptName("Rename Project", meta.name());
        if (newName == null || newName.equals(meta.name())) return;
        try {
            ProjectStore.get().rename(meta, newName);
            refreshList(searchField.getText());
            statCountLbl.setText(String.valueOf(ProjectStore.get().count()));
        } catch (IOException ex) {
            showError("Could not rename project", ex.getMessage());
        }
    }

    private void deleteProject(ProjectStore.ProjectMeta meta) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Project");
        confirm.setHeaderText(null);
        confirm.setContentText("Permanently delete \"" + meta.name() + "\"?\n\nThis cannot be undone.");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    ProjectStore.get().delete(meta);
                    refreshList(searchField.getText());
                    statCountLbl.setText(String.valueOf(ProjectStore.get().count()));
                } catch (IOException ex) {
                    showError("Could not delete project", ex.getMessage());
                }
            }
        });
    }

    private void importProject() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Import Solar Wizard Project");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
            ProjectSerializer.FILE_DESCRIPTION + " (*" + ProjectSerializer.FILE_EXTENSION + ")",
            "*" + ProjectSerializer.FILE_EXTENSION));

        File chosen = fc.showOpenDialog(stage);
        if (chosen == null) return;

        try {
            SolarProject p = ProjectStore.get().importFrom(chosen.toPath());
            refreshList(searchField.getText());
            statCountLbl.setText(String.valueOf(ProjectStore.get().count()));

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Import Successful");
            info.setHeaderText(null);
            info.setContentText("Project \"" + p.getProjectName() + "\" imported successfully.\nWould you like to open it now?");
            info.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            info.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) openWizard(p);
            });
        } catch (IOException ex) {
            showError("Import Failed", "Could not import file:\n" + ex.getMessage());
        }
    }

    private void exportProject(ProjectStore.ProjectMeta meta) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Project As");
        fc.setInitialFileName(meta.name() + ProjectSerializer.FILE_EXTENSION);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
            ProjectSerializer.FILE_DESCRIPTION + " (*" + ProjectSerializer.FILE_EXTENSION + ")",
            "*" + ProjectSerializer.FILE_EXTENSION));

        File dest = fc.showSaveDialog(stage);
        if (dest == null) return;

        try {
            ProjectStore.get().exportTo(meta, dest.toPath());
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Export Successful");
            info.setHeaderText(null);
            info.setContentText("Project exported to:\n" + dest.getAbsolutePath());
            info.showAndWait();
        } catch (IOException ex) {
            showError("Export Failed", ex.getMessage());
        }
    }

    // ── Wizard navigation ─────────────────────────────────────────────────────

    private void openWizard(SolarProject project) {
        WizardShell wizard = new WizardShell(stage, project);
        stage.getScene().setRoot(wizard.getRoot());
    }

    // ── Bottom bar ────────────────────────────────────────────────────────────

    private HBox buildBottomBar() {
        Button startBtn = new Button("⊞  Start Drafting Now");
        startBtn.getStyleClass().add("start-btn");
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setOnAction(e -> createAndOpenProject());
        HBox bar = new HBox(startBtn);
        bar.getStyleClass().add("bottom-bar");
        bar.setPadding(new Insets(14, 40, 14, 40));
        HBox.setHgrow(startBtn, Priority.ALWAYS);
        return bar;
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private String promptName(String title, String defaultValue) {
        TextInputDialog dlg = new TextInputDialog(defaultValue);
        dlg.setTitle(title);
        dlg.setHeaderText(null);
        dlg.setContentText("Project name:");
        return dlg.showAndWait().map(String::trim).filter(s -> !s.isBlank()).orElse(null);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public Parent getRoot() { return root; }
}
