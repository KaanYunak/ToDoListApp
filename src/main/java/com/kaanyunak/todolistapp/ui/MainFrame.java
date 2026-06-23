package com.kaanyunak.todolistapp.ui;

import com.kaanyunak.todolistapp.api.LocalApiServer;
import com.kaanyunak.todolistapp.model.AppSettings;
import com.kaanyunak.todolistapp.model.CompletedTask;
import com.kaanyunak.todolistapp.model.Project;
import com.kaanyunak.todolistapp.model.Task;
import com.kaanyunak.todolistapp.model.TaskCategory;
import com.kaanyunak.todolistapp.service.TaskService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainFrame extends JFrame {
    private static final Color WHITE = Color.WHITE;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TaskService taskService;
    private final LocalApiServer apiServer;
    private CardLayout cards;
    private JPanel content;
    private JPanel dailyList;
    private JPanel projectList;
    private JPanel completedList;
    private JComboBox<Project> projectCombo;
    private Color background;
    private Color panelColor;
    private Color taskColor;
    private Color taskCompletedColor;

    public MainFrame(TaskService taskService, LocalApiServer apiServer) {
        super("ToDoListApp");
        this.taskService = taskService;
        this.apiServer = apiServer;
        configureWindow();
        setContentPane(buildRoot());
        taskService.addListener(() -> SwingUtilities.invokeLater(this::refreshAll));
        new Timer(60_000, event -> taskService.rolloverDailyTasksIfNeeded()).start();
        refreshAll();
    }

    private void configureWindow() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 700));
        setSize(1240, 780);
        setLocationRelativeTo(null);
    }

    private JSplitPane buildRoot() {
        applyTheme();
        cards = new CardLayout();
        content = new JPanel(cards);
        dailyList = verticalPanel();
        projectList = verticalPanel();
        completedList = verticalPanel();
        projectCombo = new JComboBox<>();

        JPanel sidebar = buildSidebar();
        content.setBackground(background);
        content.add(buildDailyPanel(), "daily");
        content.add(buildProjectPanel(), "projects");
        content.add(buildCompletedPanel(), "completed");
        content.add(buildApiPanel(), "api");
        content.add(buildSettingsPanel(), "settings");

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, content);
        split.setDividerLocation(230);
        split.setDividerSize(1);
        split.setBorder(null);
        split.setBackground(background);
        return split;
    }

    private void applyTheme() {
        AppSettings settings = taskService.getSettings();
        background = colorFromHex(settings.getBackgroundColor(), Color.BLACK);
        panelColor = colorFromHex(settings.getPanelColor(), new Color(16, 16, 16));
        taskColor = colorFromHex(settings.getTaskColor(), new Color(176, 22, 38));
        taskCompletedColor = colorFromHex(settings.getTaskCompletedColor(), new Color(116, 17, 29));
    }

    private JPanel buildSidebar() {
        JPanel panel = verticalPanel();
        panel.setBackground(panelColor);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 18, 24, 18));

        JLabel title = new JLabel("ToDoListApp");
        title.setForeground(WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);

        JLabel subtitle = new JLabel("Daily + Project Tasks");
        subtitle.setForeground(new Color(170, 170, 170));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(24));

        panel.add(navButton("Günlük Görevler", "daily"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(navButton("Projeler", "projects"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(navButton("Tamamlananlar", "completed"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(navButton("LLM API", "api"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(navButton("Ayarlar", "settings"));
        panel.add(Box.createVerticalGlue());

        JLabel dataPath = new JLabel("<html><body style='width:175px'>Veri: " + taskService.getDataFilePath() + "</body></html>");
        dataPath.setForeground(new Color(165, 165, 165));
        dataPath.setFont(dataPath.getFont().deriveFont(10f));
        panel.add(dataPath);
        return panel;
    }

    private JButton navButton(String text, String card) {
        RoundedButton button = flatButton(text);
        button.setHorizontalAlignment(JButton.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.addActionListener(event -> cards.show(content, card));
        return button;
    }

    private JPanel buildDailyPanel() {
        JPanel panel = pagePanel("Günlük Görevler");
        JButton add = actionButton("Görev ekle");
        add.addActionListener(event -> openTaskDialog(TaskCategory.DAILY, null, null, null));
        panel.add(toolbar(add));
        panel.add(scroll(dailyList));
        return panel;
    }

    private JPanel buildProjectPanel() {
        JPanel panel = pagePanel("Projeler");
        JPanel toolbar = new JPanel(new GridBagLayout());
        toolbar.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        projectCombo.setBackground(new Color(32, 32, 32));
        projectCombo.setForeground(WHITE);
        toolbar.add(projectCombo, gbc);

        gbc.weightx = 0;
        JButton newProject = actionButton("Proje ekle");
        JButton completeProject = actionButton("Projeyi tamamla");
        JButton deleteProject = actionButton("Projeyi sil");
        JButton addTask = actionButton("Görev ekle");
        toolbar.add(newProject, gbc);
        toolbar.add(completeProject, gbc);
        toolbar.add(deleteProject, gbc);
        gbc.insets = new Insets(0, 0, 0, 0);
        toolbar.add(addTask, gbc);

        newProject.addActionListener(event -> createProject());
        completeProject.addActionListener(event -> completeSelectedProject());
        deleteProject.addActionListener(event -> deleteSelectedProject());
        addTask.addActionListener(event -> {
            Project project = selectedProject();
            if (project == null) {
                project = taskService.ensureDefaultProject();
                refreshProjects();
            }
            openTaskDialog(TaskCategory.PROJECT, project.getId(), null, null);
        });
        projectCombo.addActionListener(event -> refreshProjectTasks());

        panel.add(toolbar);
        panel.add(Box.createVerticalStrut(14));
        panel.add(scroll(projectList));
        return panel;
    }

    private JPanel buildCompletedPanel() {
        JPanel panel = pagePanel("Tamamlanan Görevler");
        panel.add(scroll(completedList));
        return panel;
    }

    private JPanel buildApiPanel() {
        JPanel panel = pagePanel("LLM API");
        RoundedPanel apiBox = new RoundedPanel(22, panelColor, new Color(48, 48, 48));
        apiBox.setLayout(new BoxLayout(apiBox, BoxLayout.Y_AXIS));
        apiBox.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        apiBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        apiBox.add(infoWithCopy("Durum", apiServer.isRunning() ? "Çalışıyor" : "Başlatılamadı", null));
        apiBox.add(Box.createVerticalStrut(10));
        apiBox.add(infoWithCopy("Adres", apiServer.getBaseUrl(), apiServer.getBaseUrl()));
        apiBox.add(Box.createVerticalStrut(10));
        apiBox.add(infoWithCopy("Token", taskService.getApiToken(), taskService.getApiToken()));
        apiBox.add(Box.createVerticalStrut(16));

        String sample = apiSample();
        JTextArea sampleArea = new JTextArea(sample);
        sampleArea.setEditable(false);
        sampleArea.setBackground(new Color(20, 20, 20));
        sampleArea.setForeground(WHITE);
        sampleArea.setCaretColor(WHITE);
        sampleArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        apiBox.add(sampleArea);
        apiBox.add(Box.createVerticalStrut(12));

        JButton copySample = actionButton("Bağlantı örneğini kopyala");
        copySample.addActionListener(event -> copyToClipboard(sample));
        apiBox.add(copySample);

        panel.add(apiBox);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private String apiSample() {
        String baseUrl = apiServer.getBaseUrl();
        String token = taskService.getApiToken();
        return """
PowerShell ile test:
$headers = @{ "X-API-Token" = "%s" }
Invoke-RestMethod "%s/api/tasks" -Headers $headers

LLM aracına verilecek bilgiler:
Base URL: %s
Header: X-API-Token: %s

Sık kullanılan uçlar:
GET /api/tasks
POST /api/tasks
PATCH /api/tasks/{id}
PATCH /api/projects/{id}
GET /api/completed
""".formatted(token, baseUrl, baseUrl, token);
    }

    private JPanel buildSettingsPanel() {
        JPanel panel = pagePanel("Ayarlar");
        AppSettings settings = taskService.getSettings();

        RoundedPanel box = new RoundedPanel(22, panelColor, new Color(48, 48, 48));
        box.setLayout(new GridBagLayout());
        box.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField backgroundField = settingField(settings.getBackgroundColor());
        JTextField taskField = settingField(settings.getTaskColor());
        JTextField completedField = settingField(settings.getTaskCompletedColor());
        JTextField panelField = settingField(settings.getPanelColor());
        JTextField iconField = settingField(settings.getExeIconPath());

        addSettingRow(box, 0, "Arka plan", backgroundField, colorButton(backgroundField));
        addSettingRow(box, 1, "Görev rengi", taskField, colorButton(taskField));
        addSettingRow(box, 2, "Tamamlanan görev rengi", completedField, colorButton(completedField));
        addSettingRow(box, 3, "Panel rengi", panelField, colorButton(panelField));
        addSettingRow(box, 4, "Exe icon (.ico)", iconField, fileButton(iconField));

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        JButton save = actionButton("Ayarları kaydet");
        JButton copyPackage = actionButton("Exe komutunu kopyala");
        save.addActionListener(event -> saveSettings(backgroundField, taskField, completedField, panelField, iconField));
        copyPackage.addActionListener(event -> copyToClipboard(packageCommand(iconField.getText().trim())));
        actions.add(save);
        actions.add(copyPackage);

        panel.add(box);
        panel.add(Box.createVerticalStrut(14));
        panel.add(actions);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel infoWithCopy(String label, String value, String copyValue) {
        JPanel panel = new JPanel(new BorderLayout(8, 2));
        panel.setOpaque(false);
        JLabel name = new JLabel(label);
        name.setForeground(new Color(190, 190, 190));
        JLabel text = new JLabel(value);
        text.setForeground(WHITE);
        text.setFont(text.getFont().deriveFont(Font.BOLD));
        panel.add(name, BorderLayout.NORTH);
        panel.add(text, BorderLayout.CENTER);
        if (copyValue != null) {
            JButton copy = tinyButton("Kopyala");
            copy.addActionListener(event -> copyToClipboard(copyValue));
            panel.add(copy, BorderLayout.EAST);
        }
        return panel;
    }

    private void addSettingRow(JPanel target, int row, String label, JTextField field, JButton action) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = row;
        gbc.insets = new Insets(0, 0, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        JLabel name = new JLabel(label);
        name.setForeground(WHITE);
        target.add(name, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        target.add(field, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        target.add(action, gbc);
    }

    private JTextField settingField(String value) {
        JTextField field = new JTextField(value == null ? "" : value);
        field.setBackground(new Color(26, 26, 26));
        field.setForeground(WHITE);
        field.setCaretColor(WHITE);
        field.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        return field;
    }

    private JButton colorButton(JTextField target) {
        JButton button = tinyButton("Seç");
        button.addActionListener(event -> {
            Color current = colorFromHex(target.getText(), taskColor);
            Color selected = JColorChooser.showDialog(this, "Renk seç", current);
            if (selected != null) {
                target.setText(toHex(selected));
            }
        });
        return button;
    }

    private JButton fileButton(JTextField target) {
        JButton button = tinyButton("Dosya");
        button.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(target.getText().isBlank() ? null : new File(target.getText()));
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                target.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        return button;
    }

    private JPanel pagePanel(String titleText) {
        JPanel panel = verticalPanel();
        panel.setBackground(background);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 30, 28, 30));
        JLabel title = new JLabel(titleText);
        title.setForeground(WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(18));
        return panel;
    }

    private JPanel toolbar(JButton button) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(button, BorderLayout.WEST);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        panel.add(Box.createVerticalStrut(14), BorderLayout.SOUTH);
        return panel;
    }

    private JScrollPane scroll(JPanel body) {
        JScrollPane scrollPane = new JScrollPane(body);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(background);
        scrollPane.setBackground(background);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private JPanel verticalPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(background);
        return panel;
    }

    private RoundedButton flatButton(String text) {
        RoundedButton button = new RoundedButton(text, 18);
        button.setBackground(new Color(28, 28, 28));
        button.setForeground(WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private RoundedButton actionButton(String text) {
        RoundedButton button = flatButton(text);
        button.setBackground(new Color(42, 42, 42));
        return button;
    }

    private void refreshAll() {
        if (content == null) {
            return;
        }
        refreshProjects();
        refreshDailyTasks();
        refreshProjectTasks();
        refreshCompletedTasks();
    }

    private void refreshProjects() {
        if (projectCombo == null) {
            return;
        }
        Object selected = projectCombo.getSelectedItem();
        projectCombo.removeAllItems();
        for (Project project : taskService.getProjects()) {
            projectCombo.addItem(project);
        }
        if (selected instanceof Project selectedProject) {
            for (int i = 0; i < projectCombo.getItemCount(); i++) {
                if (projectCombo.getItemAt(i).getId().equals(selectedProject.getId())) {
                    projectCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void refreshDailyTasks() {
        dailyList.removeAll();
        List<Task> tasks = taskService.getDailyTasksForToday();
        addTaskTree(dailyList, tasks, TaskCategory.DAILY, null, null, 0);
        if (tasks.isEmpty()) {
            dailyList.add(emptyLabel("Bugün için görev yok."));
        }
        dailyList.revalidate();
        dailyList.repaint();
    }

    private void refreshProjectTasks() {
        projectList.removeAll();
        Project project = selectedProject();
        if (project == null) {
            projectList.add(emptyLabel("Henüz proje yok."));
        } else {
            List<Task> tasks = taskService.getProjectTasks(project.getId());
            addTaskTree(projectList, tasks, TaskCategory.PROJECT, project.getId(), null, 0);
            if (tasks.isEmpty()) {
                projectList.add(emptyLabel("Bu projede görev yok."));
            }
        }
        projectList.revalidate();
        projectList.repaint();
    }

    private void refreshCompletedTasks() {
        completedList.removeAll();
        List<CompletedTask> tasks = taskService.getCompletedTasksNewestFirst();
        for (CompletedTask task : tasks) {
            completedList.add(completedCard(task));
            completedList.add(Box.createVerticalStrut(10));
        }
        if (tasks.isEmpty()) {
            completedList.add(emptyLabel("Tamamlanan görev yok."));
        }
        completedList.revalidate();
        completedList.repaint();
    }

    private void addTaskTree(JPanel target, List<Task> tasks, TaskCategory category, String projectId, String parentId, int depth) {
        Map<String, List<Task>> byParent = tasks.stream()
                .collect(Collectors.groupingBy(task -> task.getParentId() == null ? "" : task.getParentId()));
        addTaskTree(target, byParent, category, projectId, parentId == null ? "" : parentId, depth);
    }

    private void addTaskTree(JPanel target, Map<String, List<Task>> byParent, TaskCategory category, String projectId, String parentId, int depth) {
        List<Task> children = byParent.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparing(Task::getCreatedAt))
                .toList();
        for (Task task : children) {
            target.add(taskRow(task, category, projectId, depth));
            target.add(Box.createVerticalStrut(depth == 0 ? 10 : 5));
            addTaskTree(target, byParent, category, projectId, task.getId(), depth + 1);
        }
    }

    private JPanel taskRow(Task task, TaskCategory category, String projectId, int depth) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));
        if (depth > 0) {
            JPanel left = new JPanel(new BorderLayout());
            left.setOpaque(false);
            left.setPreferredSize(new Dimension(28 + depth * 26, 92));
            left.add(new BranchPanel(new Color(130, 130, 130)), BorderLayout.EAST);
            row.add(left, BorderLayout.WEST);
        }
        row.add(taskCard(task, category, projectId, depth), BorderLayout.CENTER);
        return row;
    }

    private JPanel taskCard(Task task, TaskCategory category, String projectId, int depth) {
        RoundedPanel card = new RoundedPanel(22, task.isCompleted() ? taskCompletedColor : taskColor, new Color(255, 255, 255, 36));
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(13, 14, 13, 14));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.insets = new Insets(0, 0, 0, 12);
        LedButton led = new LedButton(task.isCompleted());
        led.addActionListener(event -> completeTask(task));
        card.add(led, gbc);

        gbc.gridx = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel title = new JLabel(task.getTitle());
        title.setForeground(WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        card.add(title, gbc);

        gbc.gridy = 1;
        JLabel meta = new JLabel(metaText(task, depth));
        meta.setForeground(new Color(245, 218, 218));
        meta.setFont(meta.getFont().deriveFont(12f));
        card.add(meta, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        JButton sub = tinyButton("Alt görev");
        JButton edit = tinyButton("Güncelle");
        JButton delete = tinyButton("Sil");
        sub.addActionListener(event -> openTaskDialog(category, projectId, task.getId(), null));
        edit.addActionListener(event -> openTaskDialog(category, projectId, task.getParentId(), task));
        delete.addActionListener(event -> deleteTask(task));
        actions.add(sub);
        actions.add(edit);
        actions.add(delete);
        card.add(actions, gbc);
        return card;
    }

    private String metaText(Task task, int depth) {
        StringBuilder builder = new StringBuilder();
        if (depth > 0) {
            builder.append("Alt görev seviye ").append(depth);
        }
        if (task.getDeadline() != null) {
            appendSeparator(builder);
            builder.append("Deadline: ").append(task.getDeadline().format(DATE_TIME));
        }
        if (task.isCompleted() && task.getCompletedAt() != null) {
            appendSeparator(builder);
            builder.append("Tamamlandı: ").append(task.getCompletedAt().format(DATE_TIME));
        }
        if (!task.getNotes().isBlank()) {
            appendSeparator(builder);
            builder.append(task.getNotes());
        }
        return builder.isEmpty() ? " " : builder.toString();
    }

    private void appendSeparator(StringBuilder builder) {
        if (!builder.isEmpty()) {
            builder.append("  |  ");
        }
    }

    private JButton tinyButton(String text) {
        RoundedButton button = new RoundedButton(text, 14);
        button.setBackground(new Color(30, 30, 30));
        button.setForeground(WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
        return button;
    }

    private JPanel completedCard(CompletedTask task) {
        RoundedPanel card = new RoundedPanel(20, new Color(30, 30, 30), new Color(54, 54, 54));
        card.setLayout(new BorderLayout(10, 6));
        card.setBorder(BorderFactory.createEmptyBorder(13, 15, 13, 15));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel title = new JLabel(task.getTitle());
        title.setForeground(WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        String scope = task.getProjectName() == null ? "Günlük" : "Proje: " + task.getProjectName();
        JLabel meta = new JLabel(scope + "  |  " + task.getCompletedAt().format(DATE_TIME));
        meta.setForeground(new Color(185, 185, 185));
        card.add(title, BorderLayout.NORTH);
        card.add(meta, BorderLayout.CENTER);
        return card;
    }

    private JLabel emptyLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(170, 170, 170));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void openTaskDialog(TaskCategory category, String projectId, String parentId, Task task) {
        TaskDialog dialog = new TaskDialog(this, category, task);
        if (!dialog.showDialog()) {
            return;
        }
        if (task == null) {
            taskService.createTask(
                    category,
                    dialog.getTitleValue(),
                    dialog.getNotesValue(),
                    dialog.getDeadlineValue(),
                    projectId,
                    parentId,
                    dialog.getRecurrenceRuleValue()
            );
        } else {
            taskService.updateTask(
                    task.getId(),
                    dialog.getTitleValue(),
                    dialog.getNotesValue(),
                    dialog.getDeadlineValue(),
                    dialog.getRecurrenceRuleValue()
            );
        }
    }

    private void completeTask(Task task) {
        if (task.getCategory() == TaskCategory.PROJECT) {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "Tamamlanan görevlere gönderilsin mi?",
                    "Görev tamamlandı",
                    JOptionPane.YES_NO_OPTION
            );
            taskService.setTaskCompleted(task.getId(), true, result == JOptionPane.YES_OPTION);
        } else {
            taskService.setTaskCompleted(task.getId(), !task.isCompleted(), false);
        }
    }

    private void deleteTask(Task task) {
        int result = JOptionPane.showConfirmDialog(this, "Görev ve alt görevleri silinsin mi?", "Sil", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            taskService.deleteTask(task.getId());
        }
    }

    private void createProject() {
        JTextField field = new JTextField();
        int result = JOptionPane.showConfirmDialog(this, field, "Proje adı", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION && !field.getText().trim().isBlank()) {
            taskService.createProject(field.getText());
        }
    }

    private void completeSelectedProject() {
        Project project = selectedProject();
        if (project == null) {
            return;
        }
        int result = JOptionPane.showConfirmDialog(this, "Proje ve içindeki görevler tamamlananlara taşınsın mı?", "Projeyi tamamla", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            taskService.completeProject(project.getId());
        }
    }

    private void deleteSelectedProject() {
        Project project = selectedProject();
        if (project == null) {
            return;
        }
        int result = JOptionPane.showConfirmDialog(this, "Proje ve görevleri silinsin mi?", "Projeyi sil", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            taskService.deleteProject(project.getId());
        }
    }

    private Project selectedProject() {
        Object selected = projectCombo == null ? null : projectCombo.getSelectedItem();
        return selected instanceof Project project ? project : null;
    }

    private void saveSettings(JTextField backgroundField, JTextField taskField, JTextField completedField, JTextField panelField, JTextField iconField) {
        if (!isHexColor(backgroundField.getText()) || !isHexColor(taskField.getText()) || !isHexColor(completedField.getText()) || !isHexColor(panelField.getText())) {
            JOptionPane.showMessageDialog(this, "Renkler #RRGGBB formatında olmalı.");
            return;
        }
        taskService.updateSettings(
                backgroundField.getText().trim(),
                taskField.getText().trim(),
                completedField.getText().trim(),
                panelField.getText().trim(),
                iconField.getText().trim()
        );
        setContentPane(buildRoot());
        revalidate();
        repaint();
        refreshAll();
        cards.show(content, "settings");
    }

    private String packageCommand(String iconPath) {
        String command = "powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\\scripts\\build-desktop-app.ps1";
        if (iconPath != null && !iconPath.isBlank()) {
            command += " -IconPath \"" + iconPath + "\"";
        }
        return command;
    }

    private void copyToClipboard(String value) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
        JOptionPane.showMessageDialog(this, "Kopyalandı.");
    }

    private Color colorFromHex(String value, Color fallback) {
        if (!isHexColor(value)) {
            return fallback;
        }
        return Color.decode(value.trim());
    }

    private boolean isHexColor(String value) {
        return value != null && value.trim().matches("#[0-9a-fA-F]{6}");
    }

    private String toHex(Color color) {
        return "#%02X%02X%02X".formatted(color.getRed(), color.getGreen(), color.getBlue());
    }
}
