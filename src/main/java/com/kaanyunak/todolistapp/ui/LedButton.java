package com.kaanyunak.todolistapp.ui;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class LedButton extends JButton {
    private boolean active;

    public LedButton(boolean active) {
        this.active = active;
        setPreferredSize(new Dimension(34, 34));
        setMinimumSize(new Dimension(34, 34));
        setMaximumSize(new Dimension(34, 34));
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
        setToolTipText("Tamamla");
    }

    public void setActive(boolean active) {
        this.active = active;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int size = Math.min(getWidth(), getHeight()) - 10;
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;
        if (active) {
            g.setColor(new Color(43, 209, 92));
            g.fillOval(x, y, size, size);
            g.setColor(new Color(180, 255, 200));
            g.setStroke(new BasicStroke(2f));
            g.drawOval(x + 3, y + 3, size - 6, size - 6);
        } else {
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2.2f));
            g.drawOval(x, y, size, size);
        }
        g.dispose();
    }
}
