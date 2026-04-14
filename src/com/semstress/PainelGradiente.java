package com.semstress;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class PainelGradiente extends JPanel {
    private final Color topo;
    private final Color base;
    private final Image imagemFundo;

    public PainelGradiente(Color topo, Color base) {
        this(null, topo, base);
    }

    public PainelGradiente(String caminhoImagem, Color topo, Color base) {
        this.topo = topo;
        this.base = base;
        this.imagemFundo = carregarImagem(caminhoImagem);
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        if (imagemFundo != null) {
            desenharImagemModoCover(g2);
        } else {
            GradientPaint gradiente = new GradientPaint(0, 0, topo, 0, getHeight(), base);
            g2.setPaint(gradiente);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        g2.dispose();
        super.paintComponent(g);
    }

    private Image carregarImagem(String caminhoImagem) {
        if (caminhoImagem == null || caminhoImagem.trim().isEmpty()) {
            return null;
        }
        URL url = getClass().getResource(caminhoImagem);
        if (url == null) {
            return null;
        }
        return new ImageIcon(url).getImage();
    }

    private void desenharImagemModoCover(Graphics2D g2) {
        int larguraImagem = imagemFundo.getWidth(this);
        int alturaImagem = imagemFundo.getHeight(this);

        if (larguraImagem <= 0 || alturaImagem <= 0) {
            g2.drawImage(imagemFundo, 0, 0, getWidth(), getHeight(), this);
            return;
        }

        double escala = Math.max(
                getWidth() / (double) larguraImagem,
                getHeight() / (double) alturaImagem
        );
        int larguraDesenho = (int) Math.ceil(larguraImagem * escala);
        int alturaDesenho = (int) Math.ceil(alturaImagem * escala);
        int x = (getWidth() - larguraDesenho) / 2;
        int y = (getHeight() - alturaDesenho) / 2;

        g2.drawImage(imagemFundo, x, y, larguraDesenho, alturaDesenho, this);
    }
}
