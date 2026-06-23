package com.kaanyunak.todolistapp.ui;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class BranchPanel extends JPanel {
    private final Color color;

    public BranchPanel(Color color) {
        this.color = color;
        setOpaque(false);
        setPreferredSize(new Dimension(28, 1));
        setMinimumSize(new Dimension(28, 1));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int midY = getHeight() / 2;
        g.drawLine(10, 0, 10, midY);
        g.drawLine(10, midY, getWidth() - 3, midY);
        g.dispose();
    }
}
