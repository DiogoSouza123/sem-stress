package com.semstress;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

public class PainelGradiente extends JPanel {
    private final Color topo;
    private final Color base;

    public PainelGradiente(Color topo, Color base) {
        this.topo = topo;
        this.base = base;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        GradientPaint gradiente = new GradientPaint(0, 0, topo, 0, getHeight(), base);
        g2.setPaint(gradiente);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}
