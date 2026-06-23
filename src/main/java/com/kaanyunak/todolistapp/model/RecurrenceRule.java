package com.kaanyunak.todolistapp.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RecurrenceRule {
    private RecurrenceType type;
    private EnumSet<DayOfWeek> weekdays;
    private int intervalDays;
    private LocalDate anchorDate;

    public RecurrenceRule() {
        this.type = RecurrenceType.NONE;
        this.weekdays = EnumSet.noneOf(DayOfWeek.class);
        this.intervalDays = 1;
        this.anchorDate = LocalDate.now();
    }

    public static RecurrenceRule none() {
        return new RecurrenceRule();
    }

    public boolean isActiveOn(LocalDate date) {
        if (type == RecurrenceType.NONE || type == RecurrenceType.DAILY) {
            return true;
        }
        if (type == RecurrenceType.WEEKDAYS) {
            return weekdays.isEmpty() || weekdays.contains(date.getDayOfWeek());
        }
        if (type == RecurrenceType.EVERY_X_DAYS) {
            int safeInterval = Math.max(1, intervalDays);
            LocalDate safeAnchor = anchorDate == null ? date : anchorDate;
            long days = java.time.temporal.ChronoUnit.DAYS.between(safeAnchor, date);
            return days >= 0 && days % safeInterval == 0;
        }
        return true;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type.name());
        List<String> dayNames = new ArrayList<>();
        for (DayOfWeek day : weekdays) {
            dayNames.add(day.name());
        }
        map.put("weekdays", dayNames);
        map.put("intervalDays", intervalDays);
        map.put("anchorDate", anchorDate == null ? null : anchorDate.toString());
        return map;
    }

    @SuppressWarnings("unchecked")
    public static RecurrenceRule fromJson(Object value) {
        RecurrenceRule rule = new RecurrenceRule();
        if (!(value instanceof Map<?, ?> raw)) {
            return rule;
        }
        Map<String, Object> map = (Map<String, Object>) raw;
        Object typeValue = map.get("type");
        if (typeValue instanceof String text) {
            try {
                rule.type = RecurrenceType.valueOf(text);
            } catch (IllegalArgumentException ignored) {
                rule.type = RecurrenceType.NONE;
            }
        }
        Object daysValue = map.get("weekdays");
        if (daysValue instanceof List<?> days) {
            rule.weekdays.clear();
            for (Object day : days) {
                if (day instanceof String dayName) {
                    try {
                        rule.weekdays.add(DayOfWeek.valueOf(dayName));
                    } catch (IllegalArgumentException ignored) {
                        // Ignore invalid day names from older or edited data files.
                    }
                }
            }
        }
        Object intervalValue = map.get("intervalDays");
        if (intervalValue instanceof Number number) {
            rule.intervalDays = Math.max(1, number.intValue());
        }
        Object anchorValue = map.get("anchorDate");
        if (anchorValue instanceof String text && !text.isBlank()) {
            try {
                rule.anchorDate = LocalDate.parse(text);
            } catch (RuntimeException ignored) {
                rule.anchorDate = LocalDate.now();
            }
        }
        return rule;
    }

    public RecurrenceType getType() {
        return type;
    }

    public void setType(RecurrenceType type) {
        this.type = type == null ? RecurrenceType.NONE : type;
    }

    public EnumSet<DayOfWeek> getWeekdays() {
        return weekdays;
    }

    public void setWeekdays(EnumSet<DayOfWeek> weekdays) {
        this.weekdays = weekdays == null ? EnumSet.noneOf(DayOfWeek.class) : weekdays;
    }

    public int getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(int intervalDays) {
        this.intervalDays = Math.max(1, intervalDays);
    }

    public LocalDate getAnchorDate() {
        return anchorDate;
    }

    public void setAnchorDate(LocalDate anchorDate) {
        this.anchorDate = anchorDate == null ? LocalDate.now() : anchorDate;
    }
}
