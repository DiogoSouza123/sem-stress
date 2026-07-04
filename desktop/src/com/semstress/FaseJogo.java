package com.semstress;

public class FaseJogo {
    private final int id;
    private final String nome;
    private final String descricao;
    private final ConfiguracaoJogo configuracao;

    public FaseJogo(int id, String nome, String descricao, ConfiguracaoJogo configuracao) {
        this.id = id;
        this.nome = nome == null || nome.trim().isEmpty() ? "Fase " + id : nome.trim();
        this.descricao = descricao == null ? "" : descricao.trim();
        this.configuracao = configuracao;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public ConfiguracaoJogo getConfiguracao() {
        return configuracao;
    }
}
