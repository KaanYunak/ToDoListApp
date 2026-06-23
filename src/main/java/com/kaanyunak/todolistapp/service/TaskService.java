package com.kaanyunak.todolistapp.service;

import com.kaanyunak.todolistapp.model.AppSettings;
import com.kaanyunak.todolistapp.model.CompletedTask;
import com.kaanyunak.todolistapp.model.Project;
import com.kaanyunak.todolistapp.model.RecurrenceRule;
import com.kaanyunak.todolistapp.model.Task;
import com.kaanyunak.todolistapp.model.TaskCategory;
import com.kaanyunak.todolistapp.persistence.AppState;
import com.kaanyunak.todolistapp.persistence.DataStore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class TaskService {
    private final DataStore dataStore;
    private final AppState state;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public TaskService(DataStore dataStore) {
        this.dataStore = dataStore;
        this.state = dataStore.load();
        rolloverDailyTasksIfNeeded();
    }

    public synchronized Project createProject(String name) {
        String safeName = name == null || name.isBlank() ? "Yeni Proje" : name.trim();
        Project project = new Project(safeName);
        state.getProjects().add(project);
        persistAndNotify();
        return project;
    }

    public synchronized Project ensureDefaultProject() {
        if (state.getProjects().isEmpty()) {
            return createProject("Genel Proje");
        }
        return state.getProjects().get(0);
    }

    public synchronized Task createTask(
            TaskCategory category,
            String title,
            String notes,
            LocalDateTime deadline,
            String projectId,
            String parentId,
            RecurrenceRule recurrenceRule
    ) {
        Task task = new Task();
        task.setCategory(category);
        task.setTitle(title == null || title.isBlank() ? "Yeni görev" : title.trim());
        task.setNotes(notes);
        task.setDeadline(deadline);
        task.setParentId(parentId);
        task.setRecurrenceRule(category == TaskCategory.DAILY ? recurrenceRule : RecurrenceRule.none());
        if (category == TaskCategory.PROJECT) {
            String safeProjectId = projectId == null || projectId.isBlank() ? ensureDefaultProject().getId() : projectId;
            task.setProjectId(safeProjectId);
        }
        state.getTasks().add(task);
        persistAndNotify();
        return task;
    }

    public synchronized Optional<Task> updateTask(
            String taskId,
            String title,
            String notes,
            LocalDateTime deadline,
            RecurrenceRule recurrenceRule
    ) {
        Optional<Task> task = findTask(taskId);
        task.ifPresent(existing -> {
            existing.setTitle(title == null || title.isBlank() ? existing.getTitle() : title.trim());
            existing.setNotes(notes);
            existing.setDeadline(deadline);
            if (existing.getCategory() == TaskCategory.DAILY) {
                existing.setRecurrenceRule(recurrenceRule);
            }
            existing.setUpdatedAt(LocalDateTime.now());
            persistAndNotify();
        });
        return task;
    }

    public synchronized Optional<Task> setTaskCompleted(String taskId, boolean completed, boolean sendProjectTaskToCompleted) {
        Optional<Task> task = findTask(taskId);
        task.ifPresent(existing -> {
            if (existing.getCategory() == TaskCategory.DAILY) {
                existing.setCompleted(completed);
                existing.setCompletedAt(completed ? LocalDateTime.now() : null);
                existing.setUpdatedAt(LocalDateTime.now());
            } else {
                existing.setCompleted(completed);
                existing.setCompletedAt(completed ? LocalDateTime.now() : null);
                existing.setUpdatedAt(LocalDateTime.now());
                if (completed && sendProjectTaskToCompleted) {
                    archiveProjectTask(existing);
                }
            }
            persistAndNotify();
        });
        return task;
    }

    public synchronized boolean deleteTask(String taskId) {
        Set<String> idsToRemove = collectTaskAndChildren(taskId);
        if (idsToRemove.isEmpty()) {
            return false;
        }
        boolean removed = state.getTasks().removeIf(task -> idsToRemove.contains(task.getId()));
        if (removed) {
            persistAndNotify();
        }
        return removed;
    }

    public synchronized boolean deleteProject(String projectId) {
        boolean removed = state.getProjects().removeIf(project -> project.getId().equals(projectId));
        if (removed) {
            state.getTasks().removeIf(task -> projectId.equals(task.getProjectId()));
            persistAndNotify();
        }
        return removed;
    }

    public synchronized boolean completeProject(String projectId) {
        Optional<Project> project = findProject(projectId);
        if (project.isEmpty()) {
            return false;
        }
        LocalDateTime completedAt = LocalDateTime.now();
        String projectName = project.get().getName();
        List<Task> projectTasks = state.getTasks().stream()
                .filter(task -> projectId.equals(task.getProjectId()))
                .toList();
        for (Task task : projectTasks) {
            state.getCompletedTasks().add(CompletedTask.fromTask(task, projectName, completedAt));
        }
        state.getCompletedTasks().add(CompletedTask.fromProject(project.get(), completedAt));
        state.getTasks().removeIf(task -> projectId.equals(task.getProjectId()));
        state.getProjects().removeIf(candidate -> candidate.getId().equals(projectId));
        persistAndNotify();
        return true;
    }

    public synchronized void rolloverDailyTasksIfNeeded() {
        LocalDate today = LocalDate.now();
        LocalDate lastRollover = state.getLastRolloverDate();
        boolean changed = false;
        if (lastRollover == null || lastRollover.isBefore(today)) {
            for (Task task : state.getTasks()) {
                if (task.getCategory() == TaskCategory.DAILY && task.isCompleted() && task.getCompletedAt() != null) {
                    state.getCompletedTasks().add(CompletedTask.fromTask(task, null, task.getCompletedAt()));
                    task.setCompleted(false);
                    task.setCompletedAt(null);
                    task.setUpdatedAt(LocalDateTime.now());
                    changed = true;
                }
            }
            state.setLastRolloverDate(today);
            changed = true;
        }
        if (changed) {
            persistAndNotify();
        }
    }

    public synchronized List<Task> getDailyTasksForToday() {
        LocalDate today = LocalDate.now();
        return state.getTasks().stream()
                .filter(task -> task.getCategory() == TaskCategory.DAILY)
                .filter(task -> task.getRecurrenceRule() == null || task.getRecurrenceRule().isActiveOn(today))
                .sorted(Comparator.comparing(Task::getCreatedAt))
                .toList();
    }

    public synchronized List<Task> getProjectTasks(String projectId) {
        return state.getTasks().stream()
                .filter(task -> task.getCategory() == TaskCategory.PROJECT)
                .filter(task -> projectId == null || projectId.equals(task.getProjectId()))
                .sorted(Comparator.comparing(Task::getCreatedAt))
                .toList();
    }

    public synchronized List<Task> getAllTasks() {
        return new ArrayList<>(state.getTasks());
    }

    public synchronized List<Project> getProjects() {
        return new ArrayList<>(state.getProjects());
    }

    public synchronized List<CompletedTask> getCompletedTasksNewestFirst() {
        return state.getCompletedTasks().stream()
                .sorted(Comparator.comparing(CompletedTask::getCompletedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    public synchronized Optional<Task> findTask(String taskId) {
        return state.getTasks().stream()
                .filter(task -> task.getId().equals(taskId))
                .findFirst();
    }

    public synchronized Optional<Project> findProject(String projectId) {
        return state.getProjects().stream()
                .filter(project -> project.getId().equals(projectId))
                .findFirst();
    }

    public synchronized String getProjectName(String projectId) {
        return findProject(projectId).map(Project::getName).orElse(null);
    }

    public synchronized AppSettings getSettings() {
        return state.getSettings();
    }

    public synchronized void updateSettings(String backgroundColor, String taskColor, String taskCompletedColor, String panelColor, String exeIconPath) {
        AppSettings settings = state.getSettings();
        settings.setBackgroundColor(backgroundColor);
        settings.setTaskColor(taskColor);
        settings.setTaskCompletedColor(taskCompletedColor);
        settings.setPanelColor(panelColor);
        settings.setExeIconPath(exeIconPath);
        persistAndNotify();
    }

    public String getApiToken() {
        return state.getApiToken();
    }

    public String getDataFilePath() {
        return dataStore.getDataFile().toString();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void archiveProjectTask(Task task) {
        Set<String> idsToArchive = collectTaskAndChildren(task.getId());
        String projectName = getProjectName(task.getProjectId());
        List<Task> archivedTasks = state.getTasks().stream()
                .filter(candidate -> idsToArchive.contains(candidate.getId()))
                .toList();
        LocalDateTime completedAt = task.getCompletedAt() == null ? LocalDateTime.now() : task.getCompletedAt();
        for (Task archivedTask : archivedTasks) {
            state.getCompletedTasks().add(CompletedTask.fromTask(archivedTask, projectName, completedAt));
        }
        state.getTasks().removeIf(candidate -> idsToArchive.contains(candidate.getId()));
    }

    private Set<String> collectTaskAndChildren(String taskId) {
        Set<String> collected = new HashSet<>();
        collectTaskAndChildren(taskId, collected);
        return collected;
    }

    private void collectTaskAndChildren(String taskId, Set<String> collected) {
        Optional<Task> task = findTask(taskId);
        if (task.isEmpty() || !collected.add(taskId)) {
            return;
        }
        for (Task child : state.getTasks()) {
            if (taskId.equals(child.getParentId())) {
                collectTaskAndChildren(child.getId(), collected);
            }
        }
    }

    private void persistAndNotify() {
        dataStore.save(state);
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
