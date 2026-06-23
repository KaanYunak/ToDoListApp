package com.kaanyunak.todolistapp.ui;

import com.kaanyunak.todolistapp.model.RecurrenceRule;
import com.kaanyunak.todolistapp.model.RecurrenceType;
import com.kaanyunak.todolistapp.model.Task;
import com.kaanyunak.todolistapp.model.TaskCategory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

public class TaskDialog extends JDialog {
    private final JTextField titleField = new JTextField();
    private final JTextArea notesArea = new JTextArea(4, 28);
    private final JComboBox<String> dayCombo = new JComboBox<>();
    private final JComboBox<String> monthCombo = new JComboBox<>();
    private final JComboBox<String> yearCombo = new JComboBox<>();
    private final JComboBox<String> hourCombo = new JComboBox<>();
    private final JComboBox<String> minuteCombo = new JComboBox<>();
    private final JCheckBox noDeadline = new JCheckBox("Deadline yok");
    private final JComboBox<RecurrenceType> recurrenceType = new JComboBox<>(RecurrenceType.values());
    private final Map<DayOfWeek, JCheckBox> dayBoxes = new EnumMap<>(DayOfWeek.class);
    private final JSpinner intervalSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 365, 1));
    private final TaskCategory category;
    private boolean saved;

    public TaskDialog(Component owner, TaskCategory category, Task task) {
        super(JOptionPane.getFrameForComponent(owner), task == null ? "Görev oluştur" : "Görevi güncelle", true);
        this.category = category;
        setContentPane(buildContent(task));
        pack();
        setLocationRelativeTo(owner);
        setMinimumSize(new Dimension(520, getHeight()));
    }

    public boolean showDialog() {
        setVisible(true);
        return saved;
    }

    public String getTitleValue() {
        return titleField.getText().trim();
    }

    public String getNotesValue() {
        return notesArea.getText().trim();
    }

    public LocalDateTime getDeadlineValue() {
        if (noDeadline.isSelected()) {
            return null;
        }
        int year = Integer.parseInt((String) yearCombo.getSelectedItem());
        int month = Integer.parseInt((String) monthCombo.getSelectedItem());
        int day = Integer.parseInt((String) dayCombo.getSelectedItem());
        String hourValue = (String) hourCombo.getSelectedItem();
        String minuteValue = (String) minuteCombo.getSelectedItem();
        if (hourValue == null || hourValue.isBlank()) {
            return LocalDate.of(year, month, day).plusDays(1).atStartOfDay();
        }
        int hour = Integer.parseInt(hourValue);
        int minute = minuteValue == null || minuteValue.isBlank() ? 0 : Integer.parseInt(minuteValue);
        return LocalDateTime.of(year, month, day, hour, minute);
    }

    public RecurrenceRule getRecurrenceRuleValue() {
        RecurrenceRule rule = new RecurrenceRule();
        RecurrenceType selected = (RecurrenceType) recurrenceType.getSelectedItem();
        rule.setType(selected);
        rule.setIntervalDays((Integer) intervalSpinner.getValue());
        rule.setAnchorDate(LocalDate.now());
        EnumSet<DayOfWeek> selectedDays = EnumSet.noneOf(DayOfWeek.class);
        for (Map.Entry<DayOfWeek, JCheckBox> entry : dayBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedDays.add(entry.getKey());
            }
        }
        rule.setWeekdays(selectedDays);
        return rule;
    }

    private JPanel buildContent(Task task) {
        JPanel root = new RoundedPanel(20, new Color(12, 12, 12), new Color(55, 55, 55));
        root.setLayout(new BorderLayout(0, 16));
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        styleTextField(titleField);
        styleTextArea(notesArea);
        fields.add(label("Başlık"));
        fields.add(titleField);
        fields.add(Box.createVerticalStrut(12));
        fields.add(label("Not"));
        fields.add(notesArea);
        fields.add(Box.createVerticalStrut(12));
        fields.add(label("Deadline"));
        fields.add(buildDeadlinePanel());

        if (category == TaskCategory.DAILY) {
            fields.add(Box.createVerticalStrut(12));
            fields.add(label("Tekrar"));
            styleCombo(recurrenceType);
            fields.add(recurrenceType);
            fields.add(Box.createVerticalStrut(8));
            fields.add(buildWeekdayPanel());
            fields.add(Box.createVerticalStrut(8));
            fields.add(label("Her X günde bir"));
            fields.add(intervalSpinner);
        }

        fillDefaults(task);
        root.add(fields, BorderLayout.CENTER);
        root.add(buildActions(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildDeadlinePanel() {
        LocalDate today = LocalDate.now();
        for (int year = today.getYear(); year <= today.getYear() + 8; year++) {
            yearCombo.addItem(String.valueOf(year));
        }
        for (int month = 1; month <= 12; month++) {
            monthCombo.addItem(String.format("%02d", month));
        }
        for (int hour = 0; hour <= 23; hour++) {
            hourCombo.addItem(String.format("%02d", hour));
        }
        minuteCombo.addItem("");
        for (int minute = 0; minute <= 55; minute += 5) {
            minuteCombo.addItem(String.format("%02d", minute));
        }
        hourCombo.insertItemAt("", 0);
        hourCombo.setSelectedIndex(0);
        yearCombo.addActionListener(event -> refreshDays());
        monthCombo.addActionListener(event -> refreshDays());
        noDeadline.setOpaque(false);
        noDeadline.setForeground(Color.WHITE);
        noDeadline.addActionListener(event -> setDeadlineEnabled(!noDeadline.isSelected()));
        styleCombo(dayCombo);
        styleCombo(monthCombo);
        styleCombo(yearCombo);
        styleCombo(hourCombo);
        styleCombo(minuteCombo);

        yearCombo.setSelectedItem(String.valueOf(today.getYear()));
        monthCombo.setSelectedItem(String.format("%02d", today.getMonthValue()));
        refreshDays();
        dayCombo.setSelectedItem(String.format("%02d", today.getDayOfMonth()));

        JPanel grid = new JPanel(new GridLayout(2, 5, 8, 8));
        grid.setOpaque(false);
        grid.add(label("Gün"));
        grid.add(label("Ay"));
        grid.add(label("Yıl"));
        grid.add(label("Saat"));
        grid.add(label("Dakika"));
        grid.add(dayCombo);
        grid.add(monthCombo);
        grid.add(yearCombo);
        grid.add(hourCombo);
        grid.add(minuteCombo);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.add(grid, BorderLayout.CENTER);
        panel.add(noDeadline, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshDays() {
        Object selected = dayCombo.getSelectedItem();
        Object yearValue = yearCombo.getSelectedItem();
        Object monthValue = monthCombo.getSelectedItem();
        if (yearValue == null || monthValue == null) {
            return;
        }
        int year = Integer.parseInt((String) yearValue);
        int month = Integer.parseInt((String) monthValue);
        int days = YearMonth.of(year, month).lengthOfMonth();
        dayCombo.removeAllItems();
        for (int day = 1; day <= days; day++) {
            dayCombo.addItem(String.format("%02d", day));
        }
        if (selected != null) {
            int selectedDay = Math.min(Integer.parseInt((String) selected), days);
            dayCombo.setSelectedItem(String.format("%02d", selectedDay));
        }
    }

    private void setDeadlineEnabled(boolean enabled) {
        dayCombo.setEnabled(enabled);
        monthCombo.setEnabled(enabled);
        yearCombo.setEnabled(enabled);
        hourCombo.setEnabled(enabled);
        minuteCombo.setEnabled(enabled);
    }

    private JPanel buildWeekdayPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        addDayBox(panel, DayOfWeek.MONDAY, "Pzt");
        addDayBox(panel, DayOfWeek.TUESDAY, "Sal");
        addDayBox(panel, DayOfWeek.WEDNESDAY, "Çar");
        addDayBox(panel, DayOfWeek.THURSDAY, "Per");
        addDayBox(panel, DayOfWeek.FRIDAY, "Cum");
        addDayBox(panel, DayOfWeek.SATURDAY, "Cmt");
        addDayBox(panel, DayOfWeek.SUNDAY, "Paz");
        return panel;
    }

    private void addDayBox(JPanel panel, DayOfWeek day, String text) {
        JCheckBox box = new JCheckBox(text);
        box.setForeground(Color.WHITE);
        box.setOpaque(false);
        dayBoxes.put(day, box);
        panel.add(box);
    }

    private JPanel buildActions() {
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        RoundedButton cancel = button("Vazgeç");
        RoundedButton save = button("Kaydet");
        cancel.addActionListener(event -> dispose());
        save.addActionListener(event -> {
            if (titleField.getText().trim().isBlank()) {
                JOptionPane.showMessageDialog(this, "Başlık boş bırakılamaz.");
                return;
            }
            saved = true;
            dispose();
        });
        actions.add(cancel);
        actions.add(save);
        return actions;
    }

    private RoundedButton button(String text) {
        RoundedButton button = new RoundedButton(text);
        button.setBackground(new Color(35, 35, 35));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        return button;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void styleTextField(JTextField field) {
        field.setBackground(new Color(26, 26, 26));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
    }

    private void styleTextArea(JTextArea area) {
        area.setBackground(new Color(26, 26, 26));
        area.setForeground(Color.WHITE);
        area.setCaretColor(Color.WHITE);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setBackground(new Color(26, 26, 26));
        combo.setForeground(Color.WHITE);
    }

    private void fillDefaults(Task task) {
        LocalDateTime dateTime = task == null || task.getDeadline() == null ? null : task.getDeadline();
        if (task != null) {
            titleField.setText(task.getTitle());
            notesArea.setText(task.getNotes());
        }
        if (dateTime != null) {
            noDeadline.setSelected(false);
            yearCombo.setSelectedItem(String.valueOf(dateTime.getYear()));
            monthCombo.setSelectedItem(String.format("%02d", dateTime.getMonthValue()));
            refreshDays();
            dayCombo.setSelectedItem(String.format("%02d", dateTime.getDayOfMonth()));
            hourCombo.setSelectedItem(String.format("%02d", dateTime.getHour()));
            minuteCombo.setSelectedItem(String.format("%02d", dateTime.getMinute() - (dateTime.getMinute() % 5)));
        } else {
            noDeadline.setSelected(true);
            setDeadlineEnabled(false);
        }
        RecurrenceRule rule = task == null ? null : task.getRecurrenceRule();
        if (rule != null && category == TaskCategory.DAILY) {
            recurrenceType.setSelectedItem(rule.getType());
            intervalSpinner.setValue(rule.getIntervalDays());
            for (DayOfWeek day : rule.getWeekdays()) {
                JCheckBox box = dayBoxes.get(day);
                if (box != null) {
                    box.setSelected(true);
                }
            }
        }
    }
}
