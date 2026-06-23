package com.kaanyunak.todolistapp;

import com.kaanyunak.todolistapp.api.LocalApiServer;
import com.kaanyunak.todolistapp.persistence.DataStore;
import com.kaanyunak.todolistapp.service.TaskService;
import com.kaanyunak.todolistapp.ui.MainFrame;

import javax.swing.SwingUtilities;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DataStore dataStore = new DataStore();
            TaskService taskService = new TaskService(dataStore);
            LocalApiServer apiServer = new LocalApiServer(taskService);
            apiServer.start();

            MainFrame frame = new MainFrame(taskService, apiServer);
            frame.setVisible(true);
        });
    }
}
