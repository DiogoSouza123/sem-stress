package com.semstress;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

public class GameOver extends JFrame {

    public GameOver(String tituloResultado, int pontos, Runnable onRestart) {
        setTitle("Resultado");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(360, 200);
        setLocationRelativeTo(null);

        JLabel titulo = new JLabel(tituloResultado, SwingConstants.CENTER);
        titulo.setFont(new Font("Tahoma", Font.BOLD, 20));

        JLabel pontosLabel = new JLabel("Pontuacao final: " + pontos, SwingConstants.CENTER);
        pontosLabel.setFont(new Font("Tahoma", Font.PLAIN, 15));

        JButton reiniciar = new JButton("Jogar novamente");
        reiniciar.addActionListener(evt -> {
            if (onRestart != null) {
                onRestart.run();
            }
            dispose();
        });

        JButton fechar = new JButton("Fechar");
        fechar.addActionListener(evt -> dispose());

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        botoes.add(reiniciar);
        botoes.add(fechar);

        JPanel conteudo = new JPanel(new BorderLayout(8, 8));
        conteudo.add(titulo, BorderLayout.NORTH);
        conteudo.add(pontosLabel, BorderLayout.CENTER);
        conteudo.add(botoes, BorderLayout.SOUTH);

        setContentPane(conteudo);
    }
}
