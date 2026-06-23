package com.kaanyunak.todolistapp;

import com.kaanyunak.todolistapp.model.RecurrenceRule;
import com.kaanyunak.todolistapp.model.Task;
import com.kaanyunak.todolistapp.model.TaskCategory;
import com.kaanyunak.todolistapp.persistence.DataStore;
import com.kaanyunak.todolistapp.service.TaskService;

import java.nio.file.Files;
import java.nio.file.Path;

public class SmokeTest {
    public static void main(String[] args) throws Exception {
        Path tempData = Files.createTempFile("sisifos-smoke", ".json");
        System.setProperty("sisifos.data", tempData.toString());

        TaskService service = new TaskService(new DataStore());
        var project = service.createProject("Smoke Project");
        Task projectTask = service.createTask(
                TaskCategory.PROJECT,
                "Ship app",
                "",
                null,
                project.getId(),
                null,
                RecurrenceRule.none()
        );
        service.setTaskCompleted(projectTask.getId(), true, true);

        Task dailyTask = service.createTask(
                TaskCategory.DAILY,
                "Daily task",
                "",
                null,
                null,
                null,
                RecurrenceRule.none()
        );
        service.setTaskCompleted(dailyTask.getId(), true, false);

        boolean projectArchived = service.getCompletedTasksNewestFirst().stream()
                .anyMatch(task -> "Ship app".equals(task.getTitle()));
        boolean dailyStillVisibleToday = service.getDailyTasksForToday().stream()
                .anyMatch(task -> "Daily task".equals(task.getTitle()) && task.isCompleted());

        Files.deleteIfExists(tempData);

        if (!projectArchived || !dailyStillVisibleToday) {
            throw new IllegalStateException("Smoke test failed");
        }
        System.out.println("SMOKE_OK");
    }
}
