package com.semstress;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;

public class ConfiguracaoJogo {
    private static final String ARQUIVO_CONFIG = "/com/semstress/configuracao-jogo.properties";
    private static final String RECURSO_BACKGROUND_PADRAO = "/com/semstress/images/background.gif";
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
    private final boolean habilitarMusicaFundo;
    private final String recursoMusicaFundo;
    private final int volumeMusicaPercentual;
    private final boolean habilitarAnimacaoExplosao;
    private final String recursoAnimacaoExplosao;
    private final int duracaoAnimacaoExplosaoMs;
    private final int intervaloAnimacaoQuedaMs;
    private final int pausaEntreCascatasMs;
    private final String recursoBackgroundTela;

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
            boolean habilitarMusicaFundo,
            String recursoMusicaFundo,
            int volumeMusicaPercentual,
            Long sementeAleatoria
    ) {
        this(
                linhasTabuleiro,
                colunasTabuleiro,
                tiposPeca,
                tamanhoMinimoMatch,
                pontuacaoMatch3,
                pontuacaoMatch4,
                pontuacaoMatch5OuMais,
                multiplicadorCascata,
                pontuarCascata,
                movimentosIniciais,
                metaPontos,
                somenteTrocaAdjacente,
                consumirMovimentoTrocaInvalida,
                resolverMatchesIniciais,
                exibirNumerosPecas,
                habilitarMusicaFundo,
                recursoMusicaFundo,
                volumeMusicaPercentual,
                true,
                "/com/semstress/images/grenade.gif",
                420,
                80,
                120,
                RECURSO_BACKGROUND_PADRAO,
                sementeAleatoria
        );
    }

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
            boolean habilitarMusicaFundo,
            String recursoMusicaFundo,
            int volumeMusicaPercentual,
            boolean habilitarAnimacaoExplosao,
            String recursoAnimacaoExplosao,
            int duracaoAnimacaoExplosaoMs,
            int intervaloAnimacaoQuedaMs,
            int pausaEntreCascatasMs,
            String recursoBackgroundTela,
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
        this.habilitarMusicaFundo = habilitarMusicaFundo;
        this.recursoMusicaFundo = recursoMusicaFundo;
        this.volumeMusicaPercentual = volumeMusicaPercentual;
        this.habilitarAnimacaoExplosao = habilitarAnimacaoExplosao;
        this.recursoAnimacaoExplosao = recursoAnimacaoExplosao;
        this.duracaoAnimacaoExplosaoMs = duracaoAnimacaoExplosaoMs;
        this.intervaloAnimacaoQuedaMs = intervaloAnimacaoQuedaMs;
        this.pausaEntreCascatasMs = pausaEntreCascatasMs;
        this.recursoBackgroundTela = recursoBackgroundTela == null || recursoBackgroundTela.trim().isEmpty()
                ? RECURSO_BACKGROUND_PADRAO
                : recursoBackgroundTela.trim();
        this.sementeAleatoria = sementeAleatoria;
    }

    public static ConfiguracaoJogo get() {
        return INSTANCIA;
    }

    public static ConfiguracaoJogo fromProperties(Properties props, ConfiguracaoJogo padrao) {
        return carregarDeProperties(props, padrao);
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
        return carregarDeProperties(props, null);
    }

    private static ConfiguracaoJogo carregarDeProperties(Properties props, ConfiguracaoJogo padrao) {
        ConfiguracaoJogo base = padrao == null ? null : padrao;
        int linhas = inteiro(props, "tabuleiro.linhas", base == null ? 8 : base.getLinhasTabuleiro());
        int colunas = inteiro(props, "tabuleiro.colunas", base == null ? 6 : base.getColunasTabuleiro());
        int tipos = inteiro(props, "tabuleiro.tipos_peca", base == null ? 5 : base.getTiposPeca());
        int minMatch = inteiro(props, "regras.tamanho_minimo_match", base == null ? 3 : base.getTamanhoMinimoMatch());
        int score3 = inteiro(props, "pontuacao.match_3", base == null ? 500 : base.getPontuacaoMatch3());
        int score4 = inteiro(props, "pontuacao.match_4", base == null ? 1000 : base.getPontuacaoMatch4());
        int score5 = inteiro(props, "pontuacao.match_5_ou_mais", base == null ? 1500 : base.getPontuacaoMatch5OuMais());
        int cascata = inteiro(props, "pontuacao.multiplicador_cascata", base == null ? 1 : base.getMultiplicadorCascata());
        boolean pontuarCascata = bool(props, "pontuacao.pontuar_cascata", base != null && base.isPontuarCascata());
        int movimentos = inteiro(props, "jogo.movimentos_iniciais", base == null ? 30 : base.getMovimentosIniciais());
        int meta = inteiro(props, "jogo.meta_pontos", base == null ? 10000 : base.getMetaPontos());
        boolean apenasAdj = bool(props, "regras.somente_troca_adjacente", base == null || base.isSomenteTrocaAdjacente());
        boolean consomeInvalida = bool(props, "regras.consumir_movimento_troca_invalida", base != null && base.isConsumirMovimentoTrocaInvalida());
        boolean resolveInicio = bool(props, "regras.resolver_matches_iniciais", base == null || base.isResolverMatchesIniciais());
        boolean exibeNumeros = bool(props, "ui.exibir_numeros_pecas", base != null && base.isExibirNumerosPecas());
        boolean habilitarMusica = bool(props, "audio.habilitar_musica_fundo", base == null || base.isHabilitarMusicaFundo());
        String recursoMusica = texto(
                props,
                "audio.recurso_musica_fundo",
                base == null ? "/com/semstress/audio/musica-exemplo.wav" : base.getRecursoMusicaFundo()
        );
        int volumeMusica = inteiro(props, "audio.volume_percentual", base == null ? 30 : base.getVolumeMusicaPercentual());
        boolean habilitarAnimacaoExplosao = bool(props, "animacao.explosao.habilitada", base == null || base.isHabilitarAnimacaoExplosao());
        String recursoAnimacaoExplosao = texto(
                props,
                "animacao.explosao.recurso",
                base == null ? "/com/semstress/images/grenade.gif" : base.getRecursoAnimacaoExplosao()
        );
        int duracaoExplosaoMs = inteiro(props, "animacao.explosao.duracao_ms", base == null ? 420 : base.getDuracaoAnimacaoExplosaoMs());
        int intervaloQuedaMs = inteiro(props, "animacao.queda.intervalo_ms", base == null ? 80 : base.getIntervaloAnimacaoQuedaMs());
        int pausaEntreCascatasMs = inteiro(props, "animacao.cascata.pausa_ms", base == null ? 120 : base.getPausaEntreCascatasMs());
        String recursoBackground = texto(
                props,
                "ui.recurso_background",
                base == null ? RECURSO_BACKGROUND_PADRAO : base.getRecursoBackgroundTela()
        );
        Long seed = longOpcional(props, "jogo.semente_aleatoria");
        if (seed == null && base != null) {
            seed = base.getSementeAleatoria();
        }

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
                habilitarMusica,
                recursoMusica,
                volumeMusica,
                habilitarAnimacaoExplosao,
                recursoAnimacaoExplosao,
                duracaoExplosaoMs,
                intervaloQuedaMs,
                pausaEntreCascatasMs,
                recursoBackground,
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

    private static String texto(Properties props, String chave, String padrao) {
        String valor = props.getProperty(chave);
        if (valor == null || valor.trim().isEmpty()) {
            return padrao;
        }
        return valor.trim();
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

    public boolean isHabilitarMusicaFundo() {
        return habilitarMusicaFundo;
    }

    public String getRecursoMusicaFundo() {
        return recursoMusicaFundo;
    }

    public int getVolumeMusicaPercentual() {
        return volumeMusicaPercentual;
    }

    public boolean isHabilitarAnimacaoExplosao() {
        return habilitarAnimacaoExplosao;
    }

    public String getRecursoAnimacaoExplosao() {
        return recursoAnimacaoExplosao;
    }

    public int getDuracaoAnimacaoExplosaoMs() {
        return duracaoAnimacaoExplosaoMs;
    }

    public int getIntervaloAnimacaoQuedaMs() {
        return intervaloAnimacaoQuedaMs;
    }

    public int getPausaEntreCascatasMs() {
        return pausaEntreCascatasMs;
    }

    public String getRecursoBackgroundTela() {
        return recursoBackgroundTela;
    }

    public Long getSementeAleatoria() {
        return sementeAleatoria;
    }
}
