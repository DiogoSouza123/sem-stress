package com.semstress;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ProgressoFases {
    private int maiorFaseDesbloqueada;
    private int faseAtual;
    private final Map<Integer, Integer> melhorPontuacaoPorFase = new HashMap<>();

    public ProgressoFases(int maiorFaseDesbloqueada, int faseAtual) {
        this.maiorFaseDesbloqueada = Math.max(1, maiorFaseDesbloqueada);
        this.faseAtual = Math.max(1, faseAtual);
    }

    public boolean isDesbloqueada(int idFase) {
        return idFase <= maiorFaseDesbloqueada;
    }

    public int getMaiorFaseDesbloqueada() {
        return maiorFaseDesbloqueada;
    }

    public int getFaseAtual() {
        return faseAtual;
    }

    public void setFaseAtual(int faseAtual) {
        this.faseAtual = Math.max(1, faseAtual);
    }

    public int getMelhorPontuacao(int idFase) {
        Integer valor = melhorPontuacaoPorFase.get(idFase);
        return valor == null ? 0 : valor;
    }

    public Map<Integer, Integer> getMelhorPontuacaoPorFase() {
        return Collections.unmodifiableMap(melhorPontuacaoPorFase);
    }

    public void registrarResultado(int idFase, int pontos, boolean venceu, int totalFases) {
        int melhorAtual = getMelhorPontuacao(idFase);
        if (pontos > melhorAtual) {
            melhorPontuacaoPorFase.put(idFase, pontos);
        }

        if (venceu) {
            int proximaFase = Math.min(totalFases, idFase + 1);
            if (proximaFase > maiorFaseDesbloqueada) {
                maiorFaseDesbloqueada = proximaFase;
            }
            faseAtual = Math.max(faseAtual, idFase);
        }
    }

    public int contarFasesCompletas() {
        int total = 0;
        for (Map.Entry<Integer, Integer> entry : melhorPontuacaoPorFase.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                total++;
            }
        }
        return total;
    }

    public int totalPontos() {
        int total = 0;
        for (Integer pontos : melhorPontuacaoPorFase.values()) {
            if (pontos != null && pontos > 0) {
                total += pontos;
            }
        }
        return total;
    }

    public int mediaPontos() {
        int completas = contarFasesCompletas();
        if (completas <= 0) {
            return 0;
        }
        return totalPontos() / completas;
    }
}
