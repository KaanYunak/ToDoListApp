package com.kaanyunak.todolistapp.ui;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class RoundedPanel extends JPanel {
    private final int radius;
    private Color borderColor;

    public RoundedPanel(int radius, Color background) {
        this(radius, background, null);
    }

    public RoundedPanel(int radius, Color background, Color borderColor) {
        this.radius = radius;
        this.borderColor = borderColor;
        setOpaque(false);
        setBackground(background);
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(getBackground());
        g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        if (borderColor != null) {
            g.setColor(borderColor);
            g.setStroke(new BasicStroke(1.2f));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        }
        g.dispose();
        super.paintComponent(graphics);
    }
}
