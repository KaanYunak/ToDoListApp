package com.kaanyunak.todolistapp.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class AppSettings {
    private String backgroundColor = "#050505";
    private String taskColor = "#B01626";
    private String taskCompletedColor = "#74111D";
    private String panelColor = "#111111";
    private String exeIconPath = "";

    public Map<String, Object> toJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("backgroundColor", backgroundColor);
        map.put("taskColor", taskColor);
        map.put("taskCompletedColor", taskCompletedColor);
        map.put("panelColor", panelColor);
        map.put("exeIconPath", exeIconPath);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static AppSettings fromJson(Object value) {
        AppSettings settings = new AppSettings();
        if (!(value instanceof Map<?, ?> raw)) {
            return settings;
        }
        Map<String, Object> map = (Map<String, Object>) raw;
        settings.backgroundColor = stringValue(map.get("backgroundColor"), settings.backgroundColor);
        settings.taskColor = stringValue(map.get("taskColor"), settings.taskColor);
        settings.taskCompletedColor = stringValue(map.get("taskCompletedColor"), settings.taskCompletedColor);
        settings.panelColor = stringValue(map.get("panelColor"), settings.panelColor);
        settings.exeIconPath = stringValue(map.get("exeIconPath"), settings.exeIconPath);
        return settings;
    }

    private static String stringValue(Object value, String fallback) {
        return value instanceof String text ? text : fallback;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getTaskColor() {
        return taskColor;
    }

    public void setTaskColor(String taskColor) {
        this.taskColor = taskColor;
    }

    public String getTaskCompletedColor() {
        return taskCompletedColor;
    }

    public void setTaskCompletedColor(String taskCompletedColor) {
        this.taskCompletedColor = taskCompletedColor;
    }

    public String getPanelColor() {
        return panelColor;
    }

    public void setPanelColor(String panelColor) {
        this.panelColor = panelColor;
    }

    public String getExeIconPath() {
        return exeIconPath;
    }

    public void setExeIconPath(String exeIconPath) {
        this.exeIconPath = exeIconPath == null ? "" : exeIconPath;
    }
}
