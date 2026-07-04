package com.semstress;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

public final class ProgressoFasesRepositorio {
    private static final Path ARQUIVO = Paths.get("save", "progresso-fases.properties");

    private ProgressoFasesRepositorio() {
    }

    public static ProgressoFases carregar(int totalFases) {
        Properties props = new Properties();
        if (Files.exists(ARQUIVO)) {
            try (Reader reader = Files.newBufferedReader(ARQUIVO, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException ex) {
                System.err.println("Nao foi possivel ler progresso de fases. Criando novo.");
            }
        }

        int maiorDesbloqueada = inteiro(props, "fase.maior_desbloqueada", 1);
        int faseAtual = inteiro(props, "fase.atual", 1);
        ProgressoFases progresso = new ProgressoFases(maiorDesbloqueada, faseAtual);

        for (String key : props.stringPropertyNames()) {
            if (!key.startsWith("fase.") || !key.endsWith(".melhor_pontuacao")) {
                continue;
            }
            String idTexto = key.substring("fase.".length(), key.length() - ".melhor_pontuacao".length());
            try {
                int id = Integer.parseInt(idTexto);
                int pontos = inteiro(props, key, 0);
                if (id > 0 && pontos > 0) {
                    progresso.registrarResultado(id, pontos, false, totalFases);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (progresso.getMaiorFaseDesbloqueada() > totalFases) {
            progresso = new ProgressoFases(totalFases, Math.min(progresso.getFaseAtual(), totalFases));
        }
        return progresso;
    }

    public static void salvar(ProgressoFases progresso) {
        Properties props = new Properties();
        props.setProperty("fase.maior_desbloqueada", String.valueOf(progresso.getMaiorFaseDesbloqueada()));
        props.setProperty("fase.atual", String.valueOf(progresso.getFaseAtual()));
        for (Map.Entry<Integer, Integer> entry : progresso.getMelhorPontuacaoPorFase().entrySet()) {
            props.setProperty(
                    "fase." + entry.getKey() + ".melhor_pontuacao",
                    String.valueOf(entry.getValue())
            );
        }

        try {
            Path pasta = ARQUIVO.getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
            try (Writer writer = Files.newBufferedWriter(ARQUIVO, StandardCharsets.UTF_8)) {
                props.store(writer, "Progresso das fases");
            }
        } catch (IOException ex) {
            System.err.println("Nao foi possivel salvar progresso das fases: " + ex.getMessage());
        }
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
}
