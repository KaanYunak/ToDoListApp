package com.kaanyunak.todolistapp.model;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class CompletedTask {
    private String id;
    private String sourceTaskId;
    private String title;
    private String notes;
    private TaskCategory category;
    private String projectName;
    private LocalDateTime completedAt;

    public CompletedTask() {
        this.id = UUID.randomUUID().toString();
        this.completedAt = LocalDateTime.now();
    }

    public static CompletedTask fromTask(Task task, String projectName, LocalDateTime completedAt) {
        CompletedTask completedTask = new CompletedTask();
        completedTask.sourceTaskId = task.getId();
        completedTask.title = task.getTitle();
        completedTask.notes = task.getNotes();
        completedTask.category = task.getCategory();
        completedTask.projectName = projectName;
        completedTask.completedAt = completedAt == null ? LocalDateTime.now() : completedAt;
        return completedTask;
    }

    public static CompletedTask fromProject(Project project, LocalDateTime completedAt) {
        CompletedTask completedTask = new CompletedTask();
        completedTask.sourceTaskId = project.getId();
        completedTask.title = "Proje tamamlandı: " + project.getName();
        completedTask.notes = "";
        completedTask.category = TaskCategory.PROJECT;
        completedTask.projectName = project.getName();
        completedTask.completedAt = completedAt == null ? LocalDateTime.now() : completedAt;
        return completedTask;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("sourceTaskId", sourceTaskId);
        map.put("title", title);
        map.put("notes", notes);
        map.put("category", category == null ? TaskCategory.DAILY.name() : category.name());
        map.put("projectName", projectName);
        map.put("completedAt", completedAt == null ? null : completedAt.toString());
        return map;
    }

    @SuppressWarnings("unchecked")
    public static CompletedTask fromJson(Object value) {
        CompletedTask completedTask = new CompletedTask();
        if (!(value instanceof Map<?, ?> raw)) {
            return completedTask;
        }
        Map<String, Object> map = (Map<String, Object>) raw;
        completedTask.id = stringValue(map.get("id"), completedTask.id);
        completedTask.sourceTaskId = nullableString(map.get("sourceTaskId"));
        completedTask.title = stringValue(map.get("title"), "");
        completedTask.notes = stringValue(map.get("notes"), "");
        completedTask.category = parseCategory(map.get("category"), TaskCategory.DAILY);
        completedTask.projectName = nullableString(map.get("projectName"));
        completedTask.completedAt = parseDateTime(map.get("completedAt"), LocalDateTime.now());
        return completedTask;
    }

    private static String nullableString(Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    private static String stringValue(Object value, String fallback) {
        return value instanceof String text ? text : fallback;
    }

    private static TaskCategory parseCategory(Object value, TaskCategory fallback) {
        if (value instanceof String text) {
            try {
                return TaskCategory.valueOf(text);
            } catch (IllegalArgumentException ignored) {
                return fallback;
            }
        }
        return fallback;
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

    public String getSourceTaskId() {
        return sourceTaskId;
    }

    public String getTitle() {
        return title;
    }

    public String getNotes() {
        return notes;
    }

    public TaskCategory getCategory() {
        return category;
    }

    public String getProjectName() {
        return projectName;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
