package com.solarwizard.service;

import com.solarwizard.model.SolarProject;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Single source of truth for all project management.
 *
 * Responsibilities:
 *  - Scanning the save directory for .swproj files on startup
 *  - Creating, renaming, deleting, saving projects to disk
 *  - Import (copy an external .swproj into the save directory)
 *  - Export (copy a project file to a user-chosen destination)
 *
 * All UI-facing calls go through this class — no direct file I/O in views.
 */
public class ProjectStore {

    private static final DateTimeFormatter TIMESTAMP_FMT =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /** Lightweight descriptor shown in the dashboard list */
    public record ProjectMeta(String name, Path filePath) {
        public String fileName() { return filePath.getFileName().toString(); }
    }

    // Singleton
    private static ProjectStore instance;
    private final List<ProjectMeta> projects = new ArrayList<>();

    private ProjectStore() { refresh(); }

    public static ProjectStore get() {
        if (instance == null) instance = new ProjectStore();
        return instance;
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    public List<ProjectMeta> getAll() { return Collections.unmodifiableList(projects); }

    public int count() { return projects.size(); }

    // ── Refresh from disk ─────────────────────────────────────────────────────

    /**
     * Rescans the save directory and rebuilds the in-memory list.
     * Call after any create/delete/import operation.
     */
    public void refresh() {
        projects.clear();
        Path dir = AppSettings.get().getSaveDirectory();
        AppSettings.get().ensureSaveDirectoryExists();

        if (!Files.isDirectory(dir)) return;

        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.toString().endsWith(ProjectSerializer.FILE_EXTENSION))
                  .sorted(Comparator.comparingLong(p -> {
                      try { return Files.getLastModifiedTime(p).toMillis(); }
                      catch (IOException e) { return 0L; }
                  }))
                  .forEach(path -> {
                      // Use filename (without extension) as display name,
                      // but prefer the stored projectName field if readable
                      String displayName = nameFromFile(path);
                      projects.add(new ProjectMeta(displayName, path));
                  });
        } catch (IOException ignored) {}

        // Most recent first
        Collections.reverse(projects);
    }

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Creates a new blank project, saves it to disk, and returns it.
     */
    public SolarProject createNew(String projectName) throws IOException {
        SolarProject p = new SolarProject();
        p.setProjectName(projectName);
        save(p);
        refresh();
        return p;
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    /**
     * Saves (or overwrites) a project to disk.
     * Filename = sanitized project name + .swproj
     */
    public void save(SolarProject project) throws IOException {
        AppSettings.get().ensureSaveDirectoryExists();
        Path filePath = resolveFilePath(project.getProjectName());
        ProjectSerializer.save(project, filePath);
        refresh();
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    /**
     * Loads a project from a ProjectMeta descriptor.
     */
    public SolarProject load(ProjectMeta meta) throws IOException {
        return ProjectSerializer.load(meta.filePath());
    }

    // ── Rename ────────────────────────────────────────────────────────────────

    /**
     * Renames a project: updates the file on disk and the stored projectName field.
     */
    public void rename(ProjectMeta meta, String newName) throws IOException {
        SolarProject p = load(meta);
        p.setProjectName(newName);

        // Delete old file and write new one
        Files.deleteIfExists(meta.filePath());
        save(p);
        refresh();
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void delete(ProjectMeta meta) throws IOException {
        Files.deleteIfExists(meta.filePath());
        refresh();
    }

    // ── Import ────────────────────────────────────────────────────────────────

    /**
     * Imports a .swproj file from an external path into the save directory.
     * Returns the loaded project so the UI can open it immediately.
     */
    public SolarProject importFrom(Path sourcePath) throws IOException {
        SolarProject p = ProjectSerializer.load(sourcePath);
        // Save into current save directory (avoids name collision via timestamp)
        Path dest = resolveFilePath(p.getProjectName());
        if (Files.exists(dest)) {
            // Append timestamp to avoid overwriting
            String ts   = LocalDateTime.now().format(TIMESTAMP_FMT);
            String name = p.getProjectName() + "_" + ts;
            p.setProjectName(name);
            dest = resolveFilePath(name);
        }
        Files.copy(sourcePath, dest, StandardCopyOption.REPLACE_EXISTING);
        refresh();
        return p;
    }

    // ── Export ────────────────────────────────────────────────────────────────

    /**
     * Exports a project file to a user-chosen destination path.
     */
    public void exportTo(ProjectMeta meta, Path destinationPath) throws IOException {
        Files.copy(meta.filePath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Path resolveFilePath(String projectName) {
        String safe = sanitizeFileName(projectName);
        return AppSettings.get().getSaveDirectory().resolve(safe + ProjectSerializer.FILE_EXTENSION);
    }

    /** Removes characters that are illegal in file names */
    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private String nameFromFile(Path path) {
        // Try to read the projectName field from the file
        try {
            for (String line : Files.readAllLines(path)) {
                if (line.startsWith("projectName=")) {
                    String v = line.substring("projectName=".length()).trim();
                    if (!v.isBlank()) return v;
                }
            }
        } catch (IOException ignored) {}
        // Fallback: filename without extension
        String fn = path.getFileName().toString();
        return fn.substring(0, fn.length() - ProjectSerializer.FILE_EXTENSION.length());
    }
}
