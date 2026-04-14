package com.semstress;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;

public class ConfiguracaoJogo {
    private static final String ARQUIVO_CONFIG = "/com/semstress/configuracao-jogo.properties";
    private static final ConfiguracaoJogo INSTANCIA = carregar();

    private final int linhasTabuleiro;
    private final int colunasTabuleiro;
    private final int tiposPeca;
    private final int tamanhoMinimoMatch;

    private final int pontuacaoMatch3;
    private final int pontuacaoMatch4;
    private final int pontuacaoMatch5OuMais;
    private final int multiplicadorCascata;
    private final boolean pontuarCascata;

    private final int movimentosIniciais;
    private final int metaPontos;
    private final boolean somenteTrocaAdjacente;
    private final boolean consumirMovimentoTrocaInvalida;
    private final boolean resolverMatchesIniciais;
    private final boolean exibirNumerosPecas;

    private final Long sementeAleatoria;

    ConfiguracaoJogo(
            int linhasTabuleiro,
            int colunasTabuleiro,
            int tiposPeca,
            int tamanhoMinimoMatch,
            int pontuacaoMatch3,
            int pontuacaoMatch4,
            int pontuacaoMatch5OuMais,
            int multiplicadorCascata,
            boolean pontuarCascata,
            int movimentosIniciais,
            int metaPontos,
            boolean somenteTrocaAdjacente,
            boolean consumirMovimentoTrocaInvalida,
            boolean resolverMatchesIniciais,
            boolean exibirNumerosPecas,
            Long sementeAleatoria
    ) {
        this.linhasTabuleiro = linhasTabuleiro;
        this.colunasTabuleiro = colunasTabuleiro;
        this.tiposPeca = tiposPeca;
        this.tamanhoMinimoMatch = tamanhoMinimoMatch;
        this.pontuacaoMatch3 = pontuacaoMatch3;
        this.pontuacaoMatch4 = pontuacaoMatch4;
        this.pontuacaoMatch5OuMais = pontuacaoMatch5OuMais;
        this.multiplicadorCascata = multiplicadorCascata;
        this.pontuarCascata = pontuarCascata;
        this.movimentosIniciais = movimentosIniciais;
        this.metaPontos = metaPontos;
        this.somenteTrocaAdjacente = somenteTrocaAdjacente;
        this.consumirMovimentoTrocaInvalida = consumirMovimentoTrocaInvalida;
        this.resolverMatchesIniciais = resolverMatchesIniciais;
        this.exibirNumerosPecas = exibirNumerosPecas;
        this.sementeAleatoria = sementeAleatoria;
    }

    public static ConfiguracaoJogo get() {
        return INSTANCIA;
    }

    private static ConfiguracaoJogo carregar() {
        Properties props = new Properties();
        try (InputStream input = ConfiguracaoJogo.class.getResourceAsStream(ARQUIVO_CONFIG)) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException ex) {
            System.err.println("Nao foi possivel carregar configuracao-jogo.properties. Usando padrao.");
        }

        int linhas = inteiro(props, "tabuleiro.linhas", 8);
        int colunas = inteiro(props, "tabuleiro.colunas", 6);
        int tipos = inteiro(props, "tabuleiro.tipos_peca", 5);
        int minMatch = inteiro(props, "regras.tamanho_minimo_match", 3);
        int score3 = inteiro(props, "pontuacao.match_3", 500);
        int score4 = inteiro(props, "pontuacao.match_4", 1000);
        int score5 = inteiro(props, "pontuacao.match_5_ou_mais", 1500);
        int cascata = inteiro(props, "pontuacao.multiplicador_cascata", 1);
        boolean pontuarCascata = bool(props, "pontuacao.pontuar_cascata", false);
        int movimentos = inteiro(props, "jogo.movimentos_iniciais", 30);
        int meta = inteiro(props, "jogo.meta_pontos", 10000);
        boolean apenasAdj = bool(props, "regras.somente_troca_adjacente", true);
        boolean consomeInvalida = bool(props, "regras.consumir_movimento_troca_invalida", false);
        boolean resolveInicio = bool(props, "regras.resolver_matches_iniciais", true);
        boolean exibeNumeros = bool(props, "ui.exibir_numeros_pecas", false);
        Long seed = longOpcional(props, "jogo.semente_aleatoria");

        return new ConfiguracaoJogo(
                linhas,
                colunas,
                tipos,
                minMatch,
                score3,
                score4,
                score5,
                cascata,
                pontuarCascata,
                movimentos,
                meta,
                apenasAdj,
                consomeInvalida,
                resolveInicio,
                exibeNumeros,
                seed
        );
    }

    private static int inteiro(Properties props, String chave, int padrao) {
        String valor = props.getProperty(chave);
        if (valor == null || valor.trim().isEmpty()) {
            return padrao;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException ex) {
            return padrao;
        }
    }

    private static boolean bool(Properties props, String chave, boolean padrao) {
        String valor = props.getProperty(chave);
        if (valor == null || valor.trim().isEmpty()) {
            return padrao;
        }
        return Boolean.parseBoolean(valor.trim());
    }

    private static Long longOpcional(Properties props, String chave) {
        String valor = props.getProperty(chave);
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public Random criarRandom() {
        return sementeAleatoria == null ? new Random() : new Random(sementeAleatoria);
    }

    public int getLinhasTabuleiro() {
        return linhasTabuleiro;
    }

    public int getColunasTabuleiro() {
        return colunasTabuleiro;
    }

    public int getTiposPeca() {
        return tiposPeca;
    }

    public int getTamanhoMinimoMatch() {
        return tamanhoMinimoMatch;
    }

    public int getPontuacaoMatch3() {
        return pontuacaoMatch3;
    }

    public int getPontuacaoMatch4() {
        return pontuacaoMatch4;
    }

    public int getPontuacaoMatch5OuMais() {
        return pontuacaoMatch5OuMais;
    }

    public int getMultiplicadorCascata() {
        return multiplicadorCascata;
    }

    public boolean isPontuarCascata() {
        return pontuarCascata;
    }

    public int getMovimentosIniciais() {
        return movimentosIniciais;
    }

    public int getMetaPontos() {
        return metaPontos;
    }

    public boolean isSomenteTrocaAdjacente() {
        return somenteTrocaAdjacente;
    }

    public boolean isConsumirMovimentoTrocaInvalida() {
        return consumirMovimentoTrocaInvalida;
    }

    public boolean isResolverMatchesIniciais() {
        return resolverMatchesIniciais;
    }

    public boolean isExibirNumerosPecas() {
        return exibirNumerosPecas;
    }
}
