package com.kaanyunak.todolistapp.model;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class Project {
    private String id;
    private String name;
    private LocalDateTime createdAt;

    public Project() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    public Project(String name) {
        this();
        this.name = name;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("createdAt", createdAt == null ? null : createdAt.toString());
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Project fromJson(Object value) {
        Project project = new Project();
        if (!(value instanceof Map<?, ?> raw)) {
            return project;
        }
        Map<String, Object> map = (Map<String, Object>) raw;
        project.id = stringValue(map.get("id"), project.id);
        project.name = stringValue(map.get("name"), "Yeni Proje");
        project.createdAt = parseDateTime(map.get("createdAt"), LocalDateTime.now());
        return project;
    }

    private static String stringValue(Object value, String fallback) {
        return value instanceof String text && !text.isBlank() ? text : fallback;
    }

    private static LocalDateTime parseDateTime(Object value, LocalDateTime fallback) {
        if (value instanceof String text && !text.isBlank()) {
            try {
                return LocalDateTime.parse(text);
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return name;
    }
}
