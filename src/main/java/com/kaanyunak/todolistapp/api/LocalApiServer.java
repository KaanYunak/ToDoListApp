package com.kaanyunak.todolistapp.api;

import com.kaanyunak.todolistapp.model.CompletedTask;
import com.kaanyunak.todolistapp.model.Project;
import com.kaanyunak.todolistapp.model.RecurrenceRule;
import com.kaanyunak.todolistapp.model.Task;
import com.kaanyunak.todolistapp.model.TaskCategory;
import com.kaanyunak.todolistapp.persistence.SimpleJson;
import com.kaanyunak.todolistapp.service.TaskService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class LocalApiServer {
    private final TaskService taskService;
    private HttpServer server;
    private int port = 4567;

    public LocalApiServer(TaskService taskService) {
        this.taskService = taskService;
    }

    public void start() {
        for (int candidate = 4567; candidate < 4580; candidate++) {
            try {
                server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), candidate), 0);
                port = candidate;
                server.createContext("/", this::handle);
                server.setExecutor(Executors.newCachedThreadPool());
                server.start();
                return;
            } catch (BindException ignored) {
                // Try the next local port.
            } catch (IOException ex) {
                System.err.println("Yerel API başlatılamadı: " + ex.getMessage());
                return;
            }
        }
    }

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return server != null;
    }

    public String getBaseUrl() {
        return isRunning() ? "http://127.0.0.1:" + port : "Başlatılamadı";
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 204, "");
                return;
            }
            URI uri = exchange.getRequestURI();
            String path = uri.getPath();
            if ("/health".equals(path)) {
                sendJson(exchange, 200, Map.of("ok", true, "app", "Sisifos"));
                return;
            }
            if (!isAuthorized(exchange)) {
                sendJson(exchange, 401, Map.of("error", "X-API-Token header is missing or invalid"));
                return;
            }
            if (path.equals("/api/tasks")) {
                handleTasks(exchange);
                return;
            }
            if (path.startsWith("/api/tasks/")) {
                handleTaskById(exchange, path.substring("/api/tasks/".length()));
                return;
            }
            if (path.equals("/api/projects")) {
                handleProjects(exchange);
                return;
            }
            if (path.startsWith("/api/projects/")) {
                handleProjectById(exchange, path.substring("/api/projects/".length()));
                return;
            }
            if (path.equals("/api/completed")) {
                handleCompleted(exchange);
                return;
            }
            sendJson(exchange, 404, Map.of("error", "Not found"));
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            sendJson(exchange, 500, Map.of("error", ex.getMessage()));
        } finally {
            exchange.close();
        }
    }

    private void handleTasks(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("GET".equalsIgnoreCase(method)) {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            List<Task> tasks;
            if ("DAILY".equalsIgnoreCase(query.get("category"))) {
                tasks = taskService.getDailyTasksForToday();
            } else if ("PROJECT".equalsIgnoreCase(query.get("category"))) {
                tasks = taskService.getProjectTasks(query.get("projectId"));
            } else {
                tasks = taskService.getAllTasks();
            }
            sendJson(exchange, 200, Map.of("tasks", taskList(tasks)));
            return;
        }
        if ("POST".equalsIgnoreCase(method)) {
            Map<String, Object> body = readJsonObject(exchange);
            Task task = taskService.createTask(
                    parseCategory(body.get("category")),
                    stringValue(body.get("title")),
                    stringValue(body.get("notes")),
                    parseDateTime(body.get("deadline")),
                    stringValue(body.get("projectId")),
                    stringValue(body.get("parentId")),
                    RecurrenceRule.fromJson(body.get("recurrence"))
            );
            sendJson(exchange, 201, Map.of("task", task.toJson()));
            return;
        }
        sendJson(exchange, 405, Map.of("error", "Method not allowed"));
    }

    private void handleTaskById(HttpExchange exchange, String taskId) throws IOException {
        String method = exchange.getRequestMethod();
        if ("PATCH".equalsIgnoreCase(method)) {
            Map<String, Object> body = readJsonObject(exchange);
            if (body.containsKey("completed")) {
                boolean completed = Boolean.TRUE.equals(body.get("completed"));
                boolean sendToCompleted = Boolean.TRUE.equals(body.get("sendToCompleted"));
                taskService.setTaskCompleted(taskId, completed, sendToCompleted);
            }
            if (body.containsKey("title") || body.containsKey("notes") || body.containsKey("deadline") || body.containsKey("recurrence")) {
                Task existing = taskService.findTask(taskId).orElse(null);
                if (existing != null) {
                    taskService.updateTask(
                            taskId,
                            body.containsKey("title") ? stringValue(body.get("title")) : existing.getTitle(),
                            body.containsKey("notes") ? stringValue(body.get("notes")) : existing.getNotes(),
                            body.containsKey("deadline") ? parseDateTime(body.get("deadline")) : existing.getDeadline(),
                            body.containsKey("recurrence") ? RecurrenceRule.fromJson(body.get("recurrence")) : existing.getRecurrenceRule()
                    );
                }
            }
            Task task = taskService.findTask(taskId).orElse(null);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("task", task == null ? null : task.toJson());
            sendJson(exchange, 200, response);
            return;
        }
        if ("DELETE".equalsIgnoreCase(method)) {
            boolean deleted = taskService.deleteTask(taskId);
            sendJson(exchange, deleted ? 200 : 404, Map.of("deleted", deleted));
            return;
        }
        sendJson(exchange, 405, Map.of("error", "Method not allowed"));
    }

    private void handleProjects(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("GET".equalsIgnoreCase(method)) {
            List<Object> values = new ArrayList<>();
            for (Project project : taskService.getProjects()) {
                values.add(project.toJson());
            }
            sendJson(exchange, 200, Map.of("projects", values));
            return;
        }
        if ("POST".equalsIgnoreCase(method)) {
            Map<String, Object> body = readJsonObject(exchange);
            Project project = taskService.createProject(stringValue(body.get("name")));
            sendJson(exchange, 201, Map.of("project", project.toJson()));
            return;
        }
        sendJson(exchange, 405, Map.of("error", "Method not allowed"));
    }

    private void handleProjectById(HttpExchange exchange, String projectId) throws IOException {
        String method = exchange.getRequestMethod();
        if ("PATCH".equalsIgnoreCase(method)) {
            Map<String, Object> body = readJsonObject(exchange);
            if (Boolean.TRUE.equals(body.get("completed"))) {
                boolean completed = taskService.completeProject(projectId);
                sendJson(exchange, completed ? 200 : 404, Map.of("completed", completed));
                return;
            }
            sendJson(exchange, 400, Map.of("error", "Only completed=true is supported for project updates"));
            return;
        }
        if ("DELETE".equalsIgnoreCase(method)) {
            boolean deleted = taskService.deleteProject(projectId);
            sendJson(exchange, deleted ? 200 : 404, Map.of("deleted", deleted));
            return;
        }
        sendJson(exchange, 405, Map.of("error", "Method not allowed"));
    }

    private void handleCompleted(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        List<Object> values = new ArrayList<>();
        for (CompletedTask task : taskService.getCompletedTasksNewestFirst()) {
            values.add(task.toJson());
        }
        sendJson(exchange, 200, Map.of("completedTasks", values));
    }

    private boolean isAuthorized(HttpExchange exchange) {
        String token = exchange.getRequestHeaders().getFirst("X-API-Token");
        return taskService.getApiToken().equals(token);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJsonObject(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Object parsed = SimpleJson.parse(body);
        if (parsed instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("Request body must be a JSON object");
    }

    private List<Object> taskList(List<Task> tasks) {
        List<Object> values = new ArrayList<>();
        for (Task task : tasks) {
            values.add(task.toJson());
        }
        return values;
    }

    private TaskCategory parseCategory(Object value) {
        if (value instanceof String text) {
            try {
                return TaskCategory.valueOf(text.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                return TaskCategory.DAILY;
            }
        }
        return TaskCategory.DAILY;
    }

    private String stringValue(Object value) {
        return value instanceof String text ? text : null;
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value instanceof String text && !text.isBlank()) {
            return LocalDateTime.parse(text);
        }
        return null;
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> query = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return query;
        }
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length > 1 ? decode(parts[1]) : "";
            query.put(key, value);
        }
        return query;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void sendJson(HttpExchange exchange, int status, Object value) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        send(exchange, status, SimpleJson.stringify(value));
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream stream = exchange.getResponseBody()) {
            stream.write(bytes);
        }
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "http://localhost");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,PATCH,DELETE,OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,X-API-Token");
    }
}
