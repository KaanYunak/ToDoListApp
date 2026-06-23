package com.kaanyunak.todolistapp.ui;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class RoundedButton extends JButton {
    private final int radius;

    public RoundedButton(String text) {
        this(text, 16);
    }

    public RoundedButton(String text, int radius) {
        super(text);
        this.radius = radius;
        setOpaque(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color color = getBackground();
        if (getModel().isPressed()) {
            color = adjust(color, -24);
        } else if (getModel().isRollover()) {
            color = adjust(color, 18);
        }
        g.setColor(color);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        g.dispose();
        super.paintComponent(graphics);
    }

    private Color adjust(Color color, int delta) {
        int red = clamp(color.getRed() + delta);
        int green = clamp(color.getGreen() + delta);
        int blue = clamp(color.getBlue() + delta);
        return new Color(red, green, blue);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
