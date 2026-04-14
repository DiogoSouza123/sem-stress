package com.semstress;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;

public final class TemaUI {
    public static final Color COR_GRADIENTE_TOPO = new Color(244, 176, 136);
    public static final Color COR_GRADIENTE_BASE = new Color(224, 134, 91);
    public static final Color COR_FUNDO_JANELA = new Color(248, 234, 224);
    public static final Color COR_FUNDO_PRINCIPAL = new Color(232, 149, 106);
    public static final Color COR_FUNDO_TABULEIRO = new Color(235, 209, 191);
    public static final Color COR_FUNDO_PAINEL_INFO = new Color(222, 205, 194);
    public static final Color COR_BORDA_PAINEL = new Color(176, 136, 109);
    public static final Color COR_BORDA_TABULEIRO = new Color(178, 143, 119);
    public static final Color COR_TEXTO_TITULO = new Color(66, 44, 32);
    public static final Color COR_TEXTO_VALOR = new Color(40, 27, 20);
    public static final Color COR_TEXTO_RODAPE = new Color(83, 57, 41);

    public static final Color COR_BOTAO_FUNDO = new Color(124, 79, 54);
    public static final Color COR_BOTAO_HOVER = new Color(140, 90, 62);
    public static final Color COR_BOTAO_PRESSIONADO = new Color(99, 62, 43);
    public static final Color COR_BOTAO_DESABILITADO = new Color(161, 126, 102);
    public static final Color COR_BOTAO_BORDA = new Color(69, 43, 30);
    public static final Color COR_BOTAO_TEXTO = new Color(250, 240, 233);
    public static final Color COR_BOTAO_SOMBRA = new Color(52, 30, 19, 90);
    public static final Color COR_DQUE_CARD = new Color(241, 228, 219);

    public static final Font FONTE_INDICADOR_TITULO = new Font("Segoe UI Semibold", Font.BOLD, 16);
    public static final Font FONTE_INDICADOR_VALOR = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONTE_RODAPE = new Font("Segoe UI", Font.PLAIN, 16);
    public static final Font FONTE_BOTAO = new Font("Segoe UI Semibold", Font.BOLD, 15);

    private TemaUI() {
    }

    public static void aplicarTemaBase(
            JFrame frame,
            JPanel painelPrincipal,
            JPanel painelTabuleiro,
            JPanel painelInfo,
            JLabel rodape
    ) {
        frame.getContentPane().setBackground(COR_FUNDO_JANELA);

        painelPrincipal.setBackground(COR_FUNDO_PRINCIPAL);
        painelTabuleiro.setBackground(COR_FUNDO_TABULEIRO);
        painelInfo.setBackground(COR_FUNDO_PAINEL_INFO);

        painelInfo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COR_BORDA_PAINEL, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));

        painelTabuleiro.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COR_BORDA_TABULEIRO, 1, true),
                new EmptyBorder(8, 8, 8, 8)
        ));

        rodape.setForeground(COR_TEXTO_RODAPE);
        rodape.setFont(FONTE_RODAPE);
    }

    public static void aplicarTemaIndicadores(
            JLabel pontosTitulo,
            JLabel pontosValor,
            JLabel metaTitulo,
            JLabel metaValor,
            JLabel movimentosTitulo,
            JLabel movimentosValor
    ) {
        JLabel[] titulos = {pontosTitulo, metaTitulo, movimentosTitulo};
        JLabel[] valores = {pontosValor, metaValor, movimentosValor};

        for (JLabel titulo : titulos) {
            titulo.setForeground(COR_TEXTO_TITULO);
            titulo.setFont(FONTE_INDICADOR_TITULO);
            titulo.setHorizontalAlignment(SwingConstants.CENTER);
        }

        for (JLabel valor : valores) {
            valor.setForeground(COR_TEXTO_VALOR);
            valor.setFont(FONTE_INDICADOR_VALOR);
            valor.setHorizontalAlignment(SwingConstants.CENTER);
        }
    }

    public static void aplicarTemaBotaoPrimario(JButton botao) {
        botao.setUI(new BotaoPrimarioUI());
        botao.setFont(FONTE_BOTAO);
        botao.setForeground(COR_BOTAO_TEXTO);
        botao.setText("Iniciar");
        botao.setFocusPainted(false);
        botao.setBorder(new EmptyBorder(8, 16, 8, 16));
        botao.setContentAreaFilled(false);
        botao.setOpaque(false);
        botao.setRolloverEnabled(true);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botao.setPreferredSize(new Dimension(130, 38));
        botao.setMinimumSize(new Dimension(130, 38));
    }

    private static final class BotaoPrimarioUI extends BasicButtonUI {
        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton botao = (AbstractButton) c;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color fundo;
            if (!botao.isEnabled()) {
                fundo = COR_BOTAO_DESABILITADO;
            } else if (botao.getModel().isPressed()) {
                fundo = COR_BOTAO_PRESSIONADO;
            } else if (botao.getModel().isRollover()) {
                fundo = COR_BOTAO_HOVER;
            } else {
                fundo = COR_BOTAO_FUNDO;
            }

            int largura = c.getWidth();
            int altura = c.getHeight();
            int raio = 14;

            g2.setColor(COR_BOTAO_SOMBRA);
            g2.fillRoundRect(2, 3, largura - 3, altura - 2, raio, raio);
            g2.setColor(fundo);
            g2.fillRoundRect(0, 0, largura - 1, altura - 1, raio, raio);
            g2.setColor(COR_BOTAO_BORDA);
            g2.drawRoundRect(0, 0, largura - 1, altura - 1, raio, raio);
            g2.dispose();

            super.paint(g, c);
        }
    }
}
