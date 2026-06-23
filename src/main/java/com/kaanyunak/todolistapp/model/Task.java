package com.kaanyunak.todolistapp.model;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class Task {
    private String id;
    private TaskCategory category;
    private String projectId;
    private String parentId;
    private String title;
    private String notes;
    private LocalDateTime deadline;
    private boolean completed;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private RecurrenceRule recurrenceRule;

    public Task() {
        this.id = UUID.randomUUID().toString();
        this.category = TaskCategory.DAILY;
        this.title = "";
        this.notes = "";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.recurrenceRule = RecurrenceRule.none();
    }

    public Map<String, Object> toJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("category", category.name());
        map.put("projectId", projectId);
        map.put("parentId", parentId);
        map.put("title", title);
        map.put("notes", notes);
        map.put("deadline", deadline == null ? null : deadline.toString());
        map.put("completed", completed);
        map.put("completedAt", completedAt == null ? null : completedAt.toString());
        map.put("createdAt", createdAt == null ? null : createdAt.toString());
        map.put("updatedAt", updatedAt == null ? null : updatedAt.toString());
        map.put("recurrence", recurrenceRule == null ? RecurrenceRule.none().toJson() : recurrenceRule.toJson());
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Task fromJson(Object value) {
        Task task = new Task();
        if (!(value instanceof Map<?, ?> raw)) {
            return task;
        }
        Map<String, Object> map = (Map<String, Object>) raw;
        task.id = stringValue(map.get("id"), task.id);
        task.category = parseCategory(map.get("category"), TaskCategory.DAILY);
        task.projectId = nullableString(map.get("projectId"));
        task.parentId = nullableString(map.get("parentId"));
        task.title = stringValue(map.get("title"), "");
        task.notes = stringValue(map.get("notes"), "");
        task.deadline = parseDateTime(map.get("deadline"), null);
        task.completed = map.get("completed") instanceof Boolean completedValue && completedValue;
        task.completedAt = parseDateTime(map.get("completedAt"), null);
        task.createdAt = parseDateTime(map.get("createdAt"), LocalDateTime.now());
        task.updatedAt = parseDateTime(map.get("updatedAt"), task.createdAt);
        task.recurrenceRule = RecurrenceRule.fromJson(map.get("recurrence"));
        return task;
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

    public void setId(String id) {
        this.id = id;
    }

    public TaskCategory getCategory() {
        return category;
    }

    public void setCategory(TaskCategory category) {
        this.category = category == null ? TaskCategory.DAILY : category;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public RecurrenceRule getRecurrenceRule() {
        return recurrenceRule;
    }

    public void setRecurrenceRule(RecurrenceRule recurrenceRule) {
        this.recurrenceRule = recurrenceRule == null ? RecurrenceRule.none() : recurrenceRule;
    }
}
