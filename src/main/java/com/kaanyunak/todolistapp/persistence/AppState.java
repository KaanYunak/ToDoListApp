package com.kaanyunak.todolistapp.persistence;

import com.kaanyunak.todolistapp.model.AppSettings;
import com.kaanyunak.todolistapp.model.CompletedTask;
import com.kaanyunak.todolistapp.model.Project;
import com.kaanyunak.todolistapp.model.Task;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppState {
    private final List<Project> projects = new ArrayList<>();
    private final List<Task> tasks = new ArrayList<>();
    private final List<CompletedTask> completedTasks = new ArrayList<>();
    private AppSettings settings;
    private String apiToken;
    private LocalDate lastRolloverDate;

    public AppState() {
        this.settings = new AppSettings();
        this.apiToken = generateToken();
        this.lastRolloverDate = LocalDate.now();
    }

    public Map<String, Object> toJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        List<Object> projectValues = new ArrayList<>();
        for (Project project : projects) {
            projectValues.add(project.toJson());
        }
        List<Object> taskValues = new ArrayList<>();
        for (Task task : tasks) {
            taskValues.add(task.toJson());
        }
        List<Object> completedValues = new ArrayList<>();
        for (CompletedTask task : completedTasks) {
            completedValues.add(task.toJson());
        }
        map.put("apiToken", apiToken);
        map.put("lastRolloverDate", lastRolloverDate == null ? null : lastRolloverDate.toString());
        map.put("settings", settings.toJson());
        map.put("projects", projectValues);
        map.put("tasks", taskValues);
        map.put("completedTasks", completedValues);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static AppState fromJson(Object value) {
        AppState state = new AppState();
        if (!(value instanceof Map<?, ?> raw)) {
            return state;
        }
        Map<String, Object> map = (Map<String, Object>) raw;
        Object token = map.get("apiToken");
        if (token instanceof String text && !text.isBlank()) {
            state.apiToken = text;
        }
        Object rollover = map.get("lastRolloverDate");
        if (rollover instanceof String text && !text.isBlank()) {
            try {
                state.lastRolloverDate = LocalDate.parse(text);
            } catch (RuntimeException ignored) {
                state.lastRolloverDate = LocalDate.now();
            }
        }
        state.settings = AppSettings.fromJson(map.get("settings"));
        Object projectsValue = map.get("projects");
        if (projectsValue instanceof List<?> projectsList) {
            state.projects.clear();
            for (Object project : projectsList) {
                state.projects.add(Project.fromJson(project));
            }
        }
        Object tasksValue = map.get("tasks");
        if (tasksValue instanceof List<?> taskList) {
            state.tasks.clear();
            for (Object task : taskList) {
                state.tasks.add(Task.fromJson(task));
            }
        }
        Object completedValue = map.get("completedTasks");
        if (completedValue instanceof List<?> completedList) {
            state.completedTasks.clear();
            for (Object completedTask : completedList) {
                state.completedTasks.add(CompletedTask.fromJson(completedTask));
            }
        }
        return state;
    }

    private static String generateToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public List<Project> getProjects() {
        return projects;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public List<CompletedTask> getCompletedTasks() {
        return completedTasks;
    }

    public String getApiToken() {
        return apiToken;
    }

    public AppSettings getSettings() {
        return settings;
    }

    public LocalDate getLastRolloverDate() {
        return lastRolloverDate;
    }

    public void setLastRolloverDate(LocalDate lastRolloverDate) {
        this.lastRolloverDate = lastRolloverDate;
    }
}
