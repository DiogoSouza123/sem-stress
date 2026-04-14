package com.semstress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RodadaAnimacao {
    private final int[][] estadoAntesLimpeza;
    private final Set<Position> posicoesMatch;
    private final int[][] estadoAposLimpeza;
    private final List<int[][]> quadrosQueda;
    private final int pontosRodada;

    public RodadaAnimacao(
            int[][] estadoAntesLimpeza,
            Set<Position> posicoesMatch,
            int[][] estadoAposLimpeza,
            List<int[][]> quadrosQueda,
            int pontosRodada
    ) {
        this.estadoAntesLimpeza = copiarMatriz(estadoAntesLimpeza);
        this.posicoesMatch = Collections.unmodifiableSet(new HashSet<>(posicoesMatch));
        this.estadoAposLimpeza = copiarMatriz(estadoAposLimpeza);
        this.quadrosQueda = Collections.unmodifiableList(copiarQuadros(quadrosQueda));
        this.pontosRodada = pontosRodada;
    }

    public int[][] getEstadoAntesLimpeza() {
        return copiarMatriz(estadoAntesLimpeza);
    }

    public Set<Position> getPosicoesMatch() {
        return posicoesMatch;
    }

    public int[][] getEstadoAposLimpeza() {
        return copiarMatriz(estadoAposLimpeza);
    }

    public List<int[][]> getQuadrosQueda() {
        return copiarQuadros(quadrosQueda);
    }

    public int getPontosRodada() {
        return pontosRodada;
    }

    private static List<int[][]> copiarQuadros(List<int[][]> quadros) {
        List<int[][]> copia = new ArrayList<>();
        for (int[][] quadro : quadros) {
            copia.add(copiarMatriz(quadro));
        }
        return copia;
    }

    private static int[][] copiarMatriz(int[][] origem) {
        int[][] copia = new int[origem.length][];
        for (int i = 0; i < origem.length; i++) {
            copia[i] = new int[origem[i].length];
            System.arraycopy(origem[i], 0, copia[i], 0, origem[i].length);
        }
        return copia;
    }
}
