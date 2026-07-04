package com.semstress;

import java.util.Random;

public class Board {
    private final int[][] cells;
    private final Random random;
    private final int linhas;
    private final int colunas;
    private final int tiposPeca;

    public Board() {
        this(ConfiguracaoJogo.get(), null);
    }

    public Board(Random random) {
        this(ConfiguracaoJogo.get(), random);
    }

    public Board(ConfiguracaoJogo config) {
        this(config, null);
    }

    public Board(ConfiguracaoJogo config, Random random) {
        this.linhas = config.getLinhasTabuleiro();
        this.colunas = config.getColunasTabuleiro();
        this.tiposPeca = config.getTiposPeca();
        this.random = random == null ? config.criarRandom() : random;
        this.cells = new int[linhas][colunas];
    }

    public int getLinhas() {
        return linhas;
    }

    public int getColunas() {
        return colunas;
    }

    public int getTiposPeca() {
        return tiposPeca;
    }

    public boolean dimensoesIguais(int linhas, int colunas) {
        return this.linhas == linhas && this.colunas == colunas;
    }

    public boolean posicaoValida(Position posicao) {
        return posicao.getRow() >= 0 && posicao.getRow() < linhas
                && posicao.getCol() >= 0 && posicao.getCol() < colunas;
    }

    public void fillRandom() {
        for (int row = 0; row < linhas; row++) {
            for (int col = 0; col < colunas; col++) {
                cells[row][col] = nextPiece();
            }
        }
    }

    public int get(int row, int col) {
        return cells[row][col];
    }

    public void set(int row, int col, int value) {
        cells[row][col] = value;
    }

    public void swap(Position first, Position second) {
        int temp = cells[first.getRow()][first.getCol()];
        cells[first.getRow()][first.getCol()] = cells[second.getRow()][second.getCol()];
        cells[second.getRow()][second.getCol()] = temp;
    }

    public int nextPiece() {
        return random.nextInt(tiposPeca);
    }
}
