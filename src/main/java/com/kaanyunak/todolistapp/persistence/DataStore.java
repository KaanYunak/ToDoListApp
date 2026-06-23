package com.kaanyunak.todolistapp.persistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataStore {
    private final Path dataFile;
    private final Path legacyDataFile;

    public DataStore() {
        String configuredPath = System.getProperty("sisifos.data");
        if (configuredPath == null || configuredPath.isBlank()) {
            configuredPath = System.getProperty("todolist.data");
        }
        if (configuredPath != null && !configuredPath.isBlank()) {
            this.dataFile = Path.of(configuredPath);
            this.legacyDataFile = null;
        } else {
            this.dataFile = Path.of(System.getProperty("user.home"), ".sisifos", "data.json");
            this.legacyDataFile = Path.of(System.getProperty("user.home"), ".todolistapp", "data.json");
        }
    }

    public AppState load() {
        Path sourceFile = Files.exists(dataFile) ? dataFile : legacySourceIfAvailable();
        if (sourceFile == null) {
            return new AppState();
        }
        try {
            String json = Files.readString(sourceFile, StandardCharsets.UTF_8);
            AppState state = AppState.fromJson(SimpleJson.parse(json));
            if (!sourceFile.equals(dataFile)) {
                save(state);
            }
            return state;
        } catch (IOException | IllegalArgumentException ex) {
            System.err.println("Veri dosyası okunamadı, yeni durum başlatılıyor: " + ex.getMessage());
            return new AppState();
        }
    }

    public void save(AppState state) {
        try {
            if (dataFile.getParent() != null) {
                Files.createDirectories(dataFile.getParent());
            }
            Files.writeString(dataFile, SimpleJson.stringify(state.toJson()), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            System.err.println("Veri dosyası kaydedilemedi: " + ex.getMessage());
        }
    }

    public Path getDataFile() {
        return dataFile;
    }

    private Path legacySourceIfAvailable() {
        if (legacyDataFile != null && Files.exists(legacyDataFile)) {
            return legacyDataFile;
        }
        return null;
    }
}
