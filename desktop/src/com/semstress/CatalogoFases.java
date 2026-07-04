package com.semstress;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public final class CatalogoFases {
    private static final String RECURSO_PADRAO = "/com/semstress/fases.properties";
    private static final Path ARQUIVO_EXTERNO = Paths.get("config", "fases.properties");

    private CatalogoFases() {
    }

    public static List<FaseJogo> carregar() {
        ConfiguracaoJogo base = ConfiguracaoJogo.get();
        Properties props = carregarPropertiesFases();
        List<Integer> ids = resolverIds(props);
        if (ids.isEmpty()) {
            return Collections.singletonList(new FaseJogo(
                    1,
                    "Fase 1 - Inicio",
                    "Fase padrao",
                    base
            ));
        }

        List<FaseJogo> fases = new ArrayList<>();
        for (Integer id : ids) {
            Properties sobrescritas = extrairPropriedadesDaFase(props, id);
            ConfiguracaoJogo configuracaoFase = ConfiguracaoJogo.fromProperties(sobrescritas, base);
            String nome = props.getProperty("fase." + id + ".nome", "Fase " + id);
            String descricao = props.getProperty("fase." + id + ".descricao", "");
            fases.add(new FaseJogo(id, nome, descricao, configuracaoFase));
        }
        return fases;
    }

    private static Properties carregarPropertiesFases() {
        Properties props = new Properties();

        if (Files.exists(ARQUIVO_EXTERNO)) {
            try (Reader reader = Files.newBufferedReader(ARQUIVO_EXTERNO, StandardCharsets.UTF_8)) {
                props.load(reader);
                return props;
            } catch (IOException ex) {
                System.err.println("Nao foi possivel ler config/fases.properties. Usando recurso interno.");
            }
        }

        try (InputStream input = CatalogoFases.class.getResourceAsStream(RECURSO_PADRAO)) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException ex) {
            System.err.println("Nao foi possivel carregar fases.properties interno.");
        }

        return props;
    }

    private static List<Integer> resolverIds(Properties props) {
        String ordem = props.getProperty("fases.ordem", "").trim();
        if (!ordem.isEmpty()) {
            List<Integer> ids = new ArrayList<>();
            for (String token : ordem.split(",")) {
                String valor = token.trim();
                if (valor.isEmpty()) {
                    continue;
                }
                try {
                    int id = Integer.parseInt(valor);
                    if (id > 0 && !ids.contains(id)) {
                        ids.add(id);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            if (!ids.isEmpty()) {
                return ids;
            }
        }

        Set<Integer> ids = new TreeSet<>();
        for (String key : props.stringPropertyNames()) {
            if (!key.startsWith("fase.")) {
                continue;
            }
            String restante = key.substring("fase.".length());
            int idxPonto = restante.indexOf('.');
            if (idxPonto <= 0) {
                continue;
            }
            String idTexto = restante.substring(0, idxPonto);
            try {
                int id = Integer.parseInt(idTexto);
                if (id > 0) {
                    ids.add(id);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return new ArrayList<>(ids);
    }

    private static Properties extrairPropriedadesDaFase(Properties props, int idFase) {
        Properties fase = new Properties();
        String prefixo = "fase." + idFase + ".";
        for (String key : props.stringPropertyNames()) {
            if (!key.startsWith(prefixo)) {
                continue;
            }
            String chaveSemPrefixo = key.substring(prefixo.length());
            if ("nome".equals(chaveSemPrefixo) || "descricao".equals(chaveSemPrefixo)) {
                continue;
            }
            fase.setProperty(chaveSemPrefixo, props.getProperty(key));
        }
        return fase;
    }
}
