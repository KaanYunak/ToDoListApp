package com.kaanyunak.todolistapp.persistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataStore {
    private final Path dataFile;

    public DataStore() {
        String configuredPath = System.getProperty("todolist.data");
        if (configuredPath != null && !configuredPath.isBlank()) {
            this.dataFile = Path.of(configuredPath);
        } else {
            this.dataFile = Path.of(System.getProperty("user.home"), ".todolistapp", "data.json");
        }
    }

    public AppState load() {
        if (!Files.exists(dataFile)) {
            return new AppState();
        }
        try {
            String json = Files.readString(dataFile, StandardCharsets.UTF_8);
            return AppState.fromJson(SimpleJson.parse(json));
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
}
