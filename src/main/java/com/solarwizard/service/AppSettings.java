package com.solarwizard.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persists application-level settings (e.g. save directory) to a small
 * config file in the user's home directory.
 *
 * Location: %USERPROFILE%/.solarwizard/settings.cfg  (Windows)
 *           ~/.solarwizard/settings.cfg               (Mac/Linux)
 *
 * To add a new setting: add a constant key and a typed getter/setter pair.
 */
public class AppSettings {

    private static final Path CONFIG_DIR  = Path.of(System.getProperty("user.home"), ".solarwizard");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("settings.cfg");

    private static final String KEY_SAVE_DIR = "saveDirectory";

    private final Map<String, String> data = new LinkedHashMap<>();

    // Singleton
    private static AppSettings instance;

    private AppSettings() { load(); }

    public static AppSettings get() {
        if (instance == null) instance = new AppSettings();
        return instance;
    }

    // ── Save directory ────────────────────────────────────────────────────────

    /**
     * Returns the configured save directory, or the user's Documents/SolarWizard
     * folder as a sensible default.
     */
    public Path getSaveDirectory() {
        String raw = data.get(KEY_SAVE_DIR);
        if (raw != null && !raw.isBlank()) {
            Path p = Path.of(raw);
            if (Files.isDirectory(p)) return p;
        }
        // Default: Documents/SolarWizard
        return Path.of(System.getProperty("user.home"), "Documents", "SolarWizard");
    }

    public void setSaveDirectory(Path dir) {
        data.put(KEY_SAVE_DIR, dir.toAbsolutePath().toString());
        persist();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        if (!Files.exists(CONFIG_FILE)) return;
        try {
            for (String line : Files.readAllLines(CONFIG_FILE, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                data.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        } catch (IOException ignored) {}
    }

    private void persist() {
        try {
            Files.createDirectories(CONFIG_DIR);
            var lines = new java.util.ArrayList<String>();
            lines.add("# Solar Wizard Settings — auto-generated");
            data.forEach((k, v) -> lines.add(k + "=" + v));
            Files.write(CONFIG_FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }

    /**
     * Ensures the save directory exists, creating it if necessary.
     * Returns true if the directory is ready to use.
     */
    public boolean ensureSaveDirectoryExists() {
        try {
            Files.createDirectories(getSaveDirectory());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
