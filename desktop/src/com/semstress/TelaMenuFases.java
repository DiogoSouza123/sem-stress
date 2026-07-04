package com.semstress;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class TelaMenuFases extends javax.swing.JFrame {
    private static final Color COR_PLACA = new Color(193, 152, 123, 225);
    private static final Color COR_PLACA_BORDA = new Color(136, 94, 68);
    private static final Color COR_TEXTO = new Color(60, 37, 26);
    private static final Color COR_TEXTO_SECUNDARIO = new Color(83, 56, 42);

    private final List<FaseJogo> fases;
    private ProgressoFases progresso;
    private final Map<Integer, FaseJogo> fasePorId = new HashMap<>();
    private final Map<Integer, PainelCardFase> cardPorFase = new HashMap<>();
    private final ButtonGroup grupoBotoes = new ButtonGroup();

    private final MusicaFundoPlayer musicaFundoPlayer = new MusicaFundoPlayer();
    private boolean musicaMutada = false;
    private FaseJogo faseSelecionada;

    private PainelGradiente painelPrincipal;
    private JPanel painelGridFases;
    private JScrollPane scrollFases;
    private JLabel labelNiveis;
    private JLabel labelTotalPontos;
    private JLabel labelMedia;
    private JLabel labelFaseAtual;
    private JButton botaoJogarFase;
    private JButton botaoSom;

    public TelaMenuFases() {
        this.fases = CatalogoFases.carregar();
        this.progresso = ProgressoFasesRepositorio.carregar(Math.max(1, fases.size()));
        for (FaseJogo fase : fases) {
            fasePorId.put(fase.getId(), fase);
        }

        initUI();
        preencherListaDeFases();
        atualizarPainelProgresso();
        selecionarFaseInicial();
        iniciarMusicaMenu();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                musicaFundoPlayer.parar();
            }
        });
    }

    private void initUI() {
        setTitle("Coffee Crush - Menu de Fases");
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        painelPrincipal = new PainelGradiente(
                ConfiguracaoJogo.get().getRecursoBackgroundTela(),
                TemaUI.COR_GRADIENTE_TOPO,
                TemaUI.COR_GRADIENTE_BASE
        );
        painelPrincipal.setLayout(new BorderLayout(14, 14));
        painelPrincipal.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setContentPane(painelPrincipal);

        painelPrincipal.add(criarTopo(), BorderLayout.NORTH);
        painelPrincipal.add(criarCentro(), BorderLayout.CENTER);
        painelPrincipal.add(criarRodape(), BorderLayout.SOUTH);

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setMinimumSize(getSize());
    }

    private JPanel criarTopo() {
        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);

        JLabel logo = new JLabel();
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        aplicarLogo(logo);
        topo.add(logo, BorderLayout.CENTER);

        botaoSom = new JButton();
        botaoSom.setFocusPainted(false);
        botaoSom.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botaoSom.setFont(new Font("Segoe UI Semibold", Font.BOLD, 14));
        botaoSom.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(95, 65, 47), 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        botaoSom.addActionListener(evt -> alternarSomMenu());
        atualizarBotaoSom();
        JPanel painelSom = new JPanel(new BorderLayout());
        painelSom.setOpaque(false);
        painelSom.add(botaoSom, BorderLayout.NORTH);
        topo.add(painelSom, BorderLayout.EAST);

        JPanel espacoEsquerda = new JPanel();
        espacoEsquerda.setOpaque(false);
        espacoEsquerda.setPreferredSize(new Dimension(122, 1));
        topo.add(espacoEsquerda, BorderLayout.WEST);

        return topo;
    }

    private JPanel criarCentro() {
        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.add(criarPlacaTitulo(), BorderLayout.NORTH);

        JPanel conteudo = new JPanel(new BorderLayout(14, 0));
        conteudo.setOpaque(false);
        conteudo.add(criarPainelProgresso(), BorderLayout.WEST);
        conteudo.add(criarPainelFases(), BorderLayout.CENTER);
        centro.add(conteudo, BorderLayout.CENTER);
        return centro;
    }

    private JPanel criarPlacaTitulo() {
        JPanel placa = new JPanel(new BorderLayout());
        placa.setOpaque(true);
        placa.setBackground(COR_PLACA);
        placa.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COR_PLACA_BORDA, 2, true),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        placa.setPreferredSize(new Dimension(1, 88));

        JLabel titulo = new JLabel("MENU DE FASES", SwingConstants.CENTER);
        titulo.setFont(new Font("Segoe UI Black", Font.BOLD, 52));
        titulo.setForeground(new Color(74, 45, 32));
        placa.add(titulo, BorderLayout.CENTER);
        return placa;
    }

    private JPanel criarPainelProgresso() {
        JPanel painel = new JPanel();
        painel.setPreferredSize(new Dimension(248, 544));
        painel.setOpaque(true);
        painel.setBackground(new Color(246, 239, 232, 232));
        painel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(168, 134, 108), 1, true),
                BorderFactory.createEmptyBorder(16, 14, 16, 14)
        ));
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Progresso Geral");
        titulo.setFont(new Font("Segoe UI Semibold", Font.BOLD, 44));
        titulo.setForeground(COR_TEXTO);
        titulo.setAlignmentX(0.5f);
        painel.add(titulo);
        painel.add(Box.createVerticalStrut(14));

        labelNiveis = criarBlocoValor(painel, "Niveis Completos");
        labelTotalPontos = criarBlocoValor(painel, "Total de Pontos");
        labelMedia = criarBlocoValor(painel, "Pontuacao Media por Nivel");
        labelFaseAtual = criarBlocoValor(painel, "Fase Atual");

        painel.add(Box.createVerticalGlue());
        JLabel dev = new JLabel("<html><center>Desenvolvido por<br/>Diogo Souza</center></html>", SwingConstants.CENTER);
        dev.setFont(new Font("Segoe UI Semibold", Font.BOLD, 20));
        dev.setForeground(new Color(41, 27, 20));
        dev.setAlignmentX(0.5f);
        painel.add(dev);

        return painel;
    }

    private JLabel criarBlocoValor(JPanel pai, String titulo) {
        JLabel lblTitulo = new JLabel("<html><center>" + titulo + "</center></html>", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.PLAIN, 26));
        lblTitulo.setForeground(COR_TEXTO_SECUNDARIO);
        lblTitulo.setAlignmentX(0.5f);

        JLabel lblValor = new JLabel("-", SwingConstants.CENTER);
        lblValor.setFont(new Font("Segoe UI Black", Font.BOLD, 48));
        lblValor.setForeground(new Color(45, 28, 21));
        lblValor.setAlignmentX(0.5f);

        pai.add(lblTitulo);
        pai.add(lblValor);
        pai.add(Box.createVerticalStrut(8));

        return lblValor;
    }

    private JPanel criarPainelFases() {
        JPanel area = new JPanel(new BorderLayout());
        area.setOpaque(true);
        area.setBackground(new Color(247, 239, 230, 236));
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(177, 143, 119), 1, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        painelGridFases = new JPanel(new GridLayout(0, 2, 12, 12));
        painelGridFases.setOpaque(false);

        scrollFases = new JScrollPane(painelGridFases);
        scrollFases.setBorder(BorderFactory.createEmptyBorder());
        scrollFases.getViewport().setOpaque(false);
        scrollFases.setOpaque(false);
        scrollFases.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollFases.getVerticalScrollBar().setUnitIncrement(18);
        scrollFases.setPreferredSize(new Dimension(510, 544));
        area.add(scrollFases, BorderLayout.CENTER);

        return area;
    }

    private JPanel criarRodape() {
        JPanel rodape = new JPanel(new BorderLayout(14, 0));
        rodape.setOpaque(false);

        JButton botaoInicio = new JButton("Voltar ao Inicio");
        estilizarBotaoRodape(botaoInicio, false);
        botaoInicio.addActionListener(evt -> voltarInicio());
        rodape.add(botaoInicio, BorderLayout.WEST);

        botaoJogarFase = new JButton("Jogar Fase Selecionada");
        estilizarBotaoRodape(botaoJogarFase, true);
        botaoJogarFase.addActionListener(evt -> jogarFaseSelecionada());
        botaoJogarFase.setEnabled(false);
        rodape.add(botaoJogarFase, BorderLayout.EAST);

        return rodape;
    }

    private void estilizarBotaoRodape(JButton botao, boolean destaque) {
        botao.setFocusPainted(false);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botao.setFont(new Font("Segoe UI Semibold", Font.BOLD, destaque ? 34 : 30));
        botao.setForeground(new Color(255, 245, 235));
        botao.setBackground(destaque ? new Color(106, 66, 47) : new Color(119, 75, 52));
        botao.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 45, 32), 1, true),
                BorderFactory.createEmptyBorder(10, destaque ? 24 : 20, 10, destaque ? 24 : 20)
        ));
    }

    private void aplicarLogo(JLabel labelLogo) {
        URL recurso = getClass().getResource("/com/semstress/images/coffe-crush-logo.png");
        if (recurso == null) {
            labelLogo.setText("Coffee Crush");
            labelLogo.setFont(new Font("Segoe UI Black", Font.BOLD, 34));
            return;
        }
        javax.swing.ImageIcon original = new javax.swing.ImageIcon(recurso);
        int altura = 208;
        int largura = Math.max(1, (int) Math.round((double) original.getIconWidth() * altura / original.getIconHeight()));
        java.awt.Image imagem = original.getImage().getScaledInstance(largura, altura, java.awt.Image.SCALE_SMOOTH);
        labelLogo.setIcon(new javax.swing.ImageIcon(imagem));
    }

    private void preencherListaDeFases() {
        painelGridFases.removeAll();
        cardPorFase.clear();
        grupoBotoes.clearSelection();
        faseSelecionada = null;
        botaoJogarFase.setEnabled(false);

        for (FaseJogo fase : fases) {
            PainelCardFase card = new PainelCardFase(fase);
            cardPorFase.put(fase.getId(), card);
            grupoBotoes.add(card.getBotao());
            if (progresso.isDesbloqueada(fase.getId())) {
                card.getBotao().addActionListener(evt -> selecionarFase(fase.getId()));
            }
            painelGridFases.add(card);
        }

        atualizarEstiloBotoesFase();
        painelGridFases.revalidate();
        painelGridFases.repaint();
    }

    private void selecionarFase(int idFase) {
        FaseJogo fase = fasePorId.get(idFase);
        if (fase == null || !progresso.isDesbloqueada(idFase)) {
            return;
        }
        faseSelecionada = fase;
        PainelCardFase card = cardPorFase.get(idFase);
        if (card != null) {
            card.getBotao().setSelected(true);
        }
        atualizarEstiloBotoesFase();
        botaoJogarFase.setEnabled(true);
    }

    private void atualizarEstiloBotoesFase() {
        for (FaseJogo fase : fases) {
            PainelCardFase card = cardPorFase.get(fase.getId());
            if (card == null) {
                continue;
            }

            JToggleButton botao = card.getBotao();
            boolean desbloqueada = progresso.isDesbloqueada(fase.getId());
            boolean concluida = progresso.getMelhorPontuacao(fase.getId()) > 0;
            boolean selecionada = botao.isSelected();
            boolean atual = fase.getId() == progresso.getFaseAtual();

            if (!desbloqueada) {
                botao.setEnabled(false);
                botao.setText("BLOQ");
                botao.setFont(new Font("Segoe UI Black", Font.BOLD, 24));
                botao.setForeground(new Color(204, 198, 191));
                botao.setBackground(new Color(113, 108, 104));
                botao.setBorder(BorderFactory.createLineBorder(new Color(77, 74, 71), 2, true));

                card.setBackground(new Color(222, 217, 213, 230));
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(152, 146, 141), 1, true),
                        BorderFactory.createEmptyBorder(8, 8, 8, 8)
                ));
                card.definirStatus("BLOQUEADA", new Color(119, 116, 113), new Color(240, 236, 233));
                card.definirCorTextos(new Color(126, 119, 113), new Color(143, 138, 132));
                continue;
            }

            botao.setEnabled(true);
            botao.setText(String.valueOf(fase.getId()));
            botao.setFont(new Font("Segoe UI Black", Font.BOLD, 56));
            botao.setForeground(new Color(82, 50, 34));

            if (selecionada) {
                botao.setBackground(new Color(255, 233, 197));
                botao.setBorder(BorderFactory.createLineBorder(new Color(214, 160, 87), 3, true));
            } else if (concluida) {
                botao.setBackground(new Color(232, 248, 231));
                botao.setBorder(BorderFactory.createLineBorder(new Color(95, 158, 92), 2, true));
            } else {
                botao.setBackground(new Color(235, 212, 189));
                botao.setBorder(BorderFactory.createLineBorder(new Color(162, 126, 98), 2, true));
            }

            card.setBackground(new Color(249, 240, 230, 235));
            if (selecionada) {
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(214, 160, 87), 2, true),
                        BorderFactory.createEmptyBorder(8, 8, 8, 8)
                ));
            } else {
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(176, 140, 114), 1, true),
                        BorderFactory.createEmptyBorder(8, 8, 8, 8)
                ));
            }

            if (atual) {
                card.definirStatus("ATUAL", new Color(255, 223, 168), new Color(99, 58, 32));
            } else if (concluida) {
                card.definirStatus("CONCLUIDA", new Color(211, 242, 208), new Color(43, 104, 42));
            } else {
                card.definirStatus("LIBERADA", new Color(223, 227, 236), new Color(84, 91, 107));
            }
            card.definirCorTextos(new Color(66, 43, 31), new Color(87, 57, 43));
        }
    }

    private void selecionarFaseInicial() {
        int preferencial = Math.max(1, progresso.getFaseAtual());
        if (!progresso.isDesbloqueada(preferencial)) {
            preferencial = Math.max(1, progresso.getMaiorFaseDesbloqueada());
        }
        if (!fasePorId.containsKey(preferencial) && !fases.isEmpty()) {
            preferencial = fases.get(0).getId();
        }
        selecionarFase(preferencial);
        SwingUtilities.invokeLater(() -> scrollFases.getVerticalScrollBar().setValue(0));
    }

    private void atualizarPainelProgresso() {
        int totalFases = fases.size();
        int completas = progresso.contarFasesCompletas();
        int totalPontos = progresso.totalPontos();
        int media = progresso.mediaPontos();
        int faseAtual = Math.max(1, progresso.getFaseAtual());

        labelNiveis.setText(completas + " / " + totalFases);
        labelTotalPontos.setText(String.valueOf(totalPontos));
        labelMedia.setText(String.valueOf(media));
        labelFaseAtual.setText(String.valueOf(faseAtual));
    }

    private void iniciarMusicaMenu() {
        ConfiguracaoJogo config = ConfiguracaoJogo.get();
        if (!config.isHabilitarMusicaFundo() || musicaMutada) {
            atualizarBotaoSom();
            return;
        }
        musicaFundoPlayer.tocarEmLoop(config.getRecursoMusicaFundo(), config.getVolumeMusicaPercentual());
        atualizarBotaoSom();
    }

    private void alternarSomMenu() {
        musicaMutada = !musicaMutada;
        if (musicaMutada) {
            musicaFundoPlayer.parar();
        } else {
            iniciarMusicaMenu();
        }
        atualizarBotaoSom();
    }

    private void atualizarBotaoSom() {
        if (musicaMutada) {
            botaoSom.setText("Som: OFF");
            botaoSom.setBackground(new Color(229, 214, 201));
            botaoSom.setForeground(new Color(83, 53, 37));
        } else {
            botaoSom.setText("Som: ON");
            botaoSom.setBackground(new Color(248, 236, 224));
            botaoSom.setForeground(new Color(58, 36, 25));
        }
    }

    private void voltarInicio() {
        int primeiraFase = fases.isEmpty() ? 1 : fases.get(0).getId();
        selecionarFase(primeiraFase);
        scrollFases.getVerticalScrollBar().setValue(0);
    }

    private void jogarFaseSelecionada() {
        if (faseSelecionada == null || !progresso.isDesbloqueada(faseSelecionada.getId())) {
            return;
        }

        final int totalFases = Math.max(1, fases.size());
        final boolean[] resultadoRecebidoDaPartida = {false};
        progresso.setFaseAtual(faseSelecionada.getId());
        ProgressoFasesRepositorio.salvar(progresso);
        musicaFundoPlayer.parar();

        setVisible(false);

        final TelaInicial.ResultadoFaseListener listenerResultado = new TelaInicial.ResultadoFaseListener() {
            @Override
            public void onResultadoFase(int idFase, int pontuacaoFinal, boolean venceu) {
                resultadoRecebidoDaPartida[0] = true;
                progresso.registrarResultado(idFase, pontuacaoFinal, venceu, totalFases);
                progresso.setFaseAtual(idFase);
                ProgressoFasesRepositorio.salvar(progresso);
            }
        };

        final boolean[] retornoExecutado = {false};
        final Runnable retornoMenu = new Runnable() {
            @Override
            public void run() {
                if (retornoExecutado[0]) {
                    return;
                }
                retornoExecutado[0] = true;
                if (!resultadoRecebidoDaPartida[0]) {
                    progresso = ProgressoFasesRepositorio.carregar(totalFases);
                }
                atualizarPainelProgresso();
                preencherListaDeFases();
                selecionarFaseInicial();
                setVisible(true);
                iniciarMusicaMenu();
            }
        };

        TelaInicial jogo = new TelaInicial(
                faseSelecionada.getConfiguracao(),
                faseSelecionada.getId(),
                retornoMenu,
                listenerResultado
        );
        jogo.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                retornoMenu.run();
            }
        });
        jogo.setVisible(true);
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new TelaMenuFases().setVisible(true));
    }

    private static final class PainelCardFase extends JPanel {
        private final JToggleButton botao;
        private final JLabel labelStatus;
        private final JLabel labelNome;
        private final JLabel labelDescricao;

        private PainelCardFase(FaseJogo fase) {
            setLayout(new BorderLayout(0, 6));
            setOpaque(true);
            setBackground(new Color(249, 240, 230, 235));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(176, 140, 114), 1, true),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
            ));

            labelStatus = new JLabel(" ", SwingConstants.CENTER);
            labelStatus.setOpaque(true);
            labelStatus.setFont(new Font("Segoe UI Semibold", Font.BOLD, 11));
            labelStatus.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

            JPanel painelStatus = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            painelStatus.setOpaque(false);
            painelStatus.add(labelStatus);
            add(painelStatus, BorderLayout.NORTH);

            botao = new JToggleButton();
            botao.setFocusPainted(false);
            botao.setContentAreaFilled(true);
            botao.setOpaque(true);
            botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            botao.setHorizontalAlignment(SwingConstants.CENTER);
            botao.setPreferredSize(new Dimension(134, 134));
            add(botao, BorderLayout.CENTER);

            labelNome = new JLabel(toHtml(fase.getNome()), SwingConstants.CENTER);
            labelNome.setFont(new Font("Segoe UI Semibold", Font.BOLD, 19));
            labelNome.setForeground(new Color(66, 43, 31));
            labelNome.setAlignmentX(0.5f);

            labelDescricao = new JLabel(toHtml(fase.getDescricao()), SwingConstants.CENTER);
            labelDescricao.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            labelDescricao.setForeground(new Color(87, 57, 43));
            labelDescricao.setAlignmentX(0.5f);

            JPanel painelTexto = new JPanel();
            painelTexto.setOpaque(false);
            painelTexto.setLayout(new BoxLayout(painelTexto, BoxLayout.Y_AXIS));
            painelTexto.add(labelNome);
            if (fase.getDescricao() != null && !fase.getDescricao().trim().isEmpty()) {
                painelTexto.add(Box.createVerticalStrut(2));
                painelTexto.add(labelDescricao);
            }
            add(painelTexto, BorderLayout.SOUTH);
        }

        private JToggleButton getBotao() {
            return botao;
        }

        private void definirStatus(String texto, Color fundo, Color corTexto) {
            labelStatus.setText(texto);
            labelStatus.setBackground(fundo);
            labelStatus.setForeground(corTexto);
        }

        private void definirCorTextos(Color corNome, Color corDescricao) {
            labelNome.setForeground(corNome);
            labelDescricao.setForeground(corDescricao);
        }

        private static String toHtml(String texto) {
            String valor = texto == null ? "" : texto.trim();
            valor = valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            return "<html><div style='text-align:center;'>" + valor + "</div></html>";
        }
    }
}
