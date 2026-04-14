package com.semstress;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameEngine {
    private static final int EMPTY = -1;
    private final ConfiguracaoJogo config;

    public GameEngine() {
        this(ConfiguracaoJogo.get());
    }

    public GameEngine(ConfiguracaoJogo config) {
        this.config = config;
    }

    public MoveResult tryMove(Board board, Position first, Position second) {
        ResultadoJogadaAnimada resultadoAnimado = tryMoveAnimado(board, first, second);
        return new MoveResult(resultadoAnimado.isValid(), resultadoAnimado.getPoints());
    }

    public ResultadoJogadaAnimada tryMoveAnimado(Board board, Position first, Position second) {
        if (!board.posicaoValida(first) || !board.posicaoValida(second)) {
            return ResultadoJogadaAnimada.invalido();
        }

        if (config.isSomenteTrocaAdjacente() && !isAdjacent(first, second)) {
            return ResultadoJogadaAnimada.invalido();
        }

        board.swap(first, second);

        Set<Position> initialMatches = findMatchedCells(board);
        if (initialMatches.isEmpty()) {
            board.swap(first, second);
            return ResultadoJogadaAnimada.invalido();
        }

        ResultadoResolucao resultado = resolverBoardComTimeline(board);
        return new ResultadoJogadaAnimada(true, resultado.pontosTotais, resultado.rodadas);
    }

    public int resolveBoard(Board board) {
        return resolverBoardComTimeline(board).pontosTotais;
    }

    private ResultadoResolucao resolverBoardComTimeline(Board board) {
        int totalPoints = 0;
        int nivelCascata = 0;
        List<RodadaAnimacao> rodadas = new ArrayList<>();

        while (true) {
            List<Integer> runLengths = findRunLengths(board);
            if (runLengths.isEmpty()) {
                return new ResultadoResolucao(totalPoints, rodadas);
            }

            nivelCascata++;
            Set<Position> matched = findMatchedCells(board);
            int pontosRodada = 0;
            if (nivelCascata == 1 || config.isPontuarCascata()) {
                pontosRodada = calculatePoints(runLengths, nivelCascata);
                totalPoints += pontosRodada;
            }

            int[][] estadoAntesLimpeza = snapshot(board);
            clearMatched(board, matched);
            int[][] estadoAposLimpeza = snapshot(board);
            List<int[][]> quadrosQueda = collapseAndRefillComQuadros(board);
            rodadas.add(new RodadaAnimacao(
                    estadoAntesLimpeza,
                    matched,
                    estadoAposLimpeza,
                    quadrosQueda,
                    pontosRodada
            ));
        }
    }

    private int calculatePoints(List<Integer> runLengths, int nivelCascata) {
        int points = 0;
        for (Integer runLength : runLengths) {
            if (runLength >= 5) {
                points += config.getPontuacaoMatch5OuMais();
            } else if (runLength >= 4) {
                points += config.getPontuacaoMatch4();
            } else {
                points += config.getPontuacaoMatch3();
            }
        }

        int multiplicadorBase = Math.max(1, config.getMultiplicadorCascata());
        int multiplicadorRodada = 1 + ((nivelCascata - 1) * (multiplicadorBase - 1));
        return points * multiplicadorRodada;
    }

    private boolean isAdjacent(Position first, Position second) {
        int rowDiff = Math.abs(first.getRow() - second.getRow());
        int colDiff = Math.abs(first.getCol() - second.getCol());
        return (rowDiff + colDiff) == 1;
    }

    private void clearMatched(Board board, Set<Position> matched) {
        for (Position position : matched) {
            board.set(position.getRow(), position.getCol(), EMPTY);
        }
    }

    private void collapseAndRefill(Board board) {
        collapseAndRefillComQuadros(board);
    }

    private List<int[][]> collapseAndRefillComQuadros(Board board) {
        List<int[][]> quadros = new ArrayList<>();

        while (aplicarQuedaUmPasso(board)) {
            quadros.add(snapshot(board));
        }

        preencherVaziosComNovasPecas(board);
        quadros.add(snapshot(board));

        return quadros;
    }

    private boolean aplicarQuedaUmPasso(Board board) {
        boolean houveMovimento = false;
        for (int row = board.getLinhas() - 2; row >= 0; row--) {
            for (int col = 0; col < board.getColunas(); col++) {
                int atual = board.get(row, col);
                if (atual == EMPTY) {
                    continue;
                }
                if (board.get(row + 1, col) == EMPTY) {
                    board.set(row + 1, col, atual);
                    board.set(row, col, EMPTY);
                    houveMovimento = true;
                }
            }
        }
        return houveMovimento;
    }

    private void preencherVaziosComNovasPecas(Board board) {
        for (int col = 0; col < board.getColunas(); col++) {
            for (int row = board.getLinhas() - 1; row >= 0; row--) {
                if (board.get(row, col) == EMPTY) {
                    board.set(row, col, board.nextPiece());
                }
            }
        }
    }

    private int[][] snapshot(Board board) {
        int[][] estado = new int[board.getLinhas()][board.getColunas()];
        for (int row = 0; row < board.getLinhas(); row++) {
            for (int col = 0; col < board.getColunas(); col++) {
                estado[row][col] = board.get(row, col);
            }
        }
        return estado;
    }

    private Set<Position> findMatchedCells(Board board) {
        Set<Position> matched = new HashSet<>();

        // Horizontal runs
        for (int row = 0; row < board.getLinhas(); row++) {
            int col = 0;
            while (col < board.getColunas()) {
                int start = col;
                int value = board.get(row, col);
                while (col + 1 < board.getColunas() && board.get(row, col + 1) == value) {
                    col++;
                }
                int length = col - start + 1;
                if (length >= config.getTamanhoMinimoMatch()) {
                    for (int c = start; c <= col; c++) {
                        matched.add(new Position(row, c));
                    }
                }
                col++;
            }
        }

        // Vertical runs
        for (int col = 0; col < board.getColunas(); col++) {
            int row = 0;
            while (row < board.getLinhas()) {
                int start = row;
                int value = board.get(row, col);
                while (row + 1 < board.getLinhas() && board.get(row + 1, col) == value) {
                    row++;
                }
                int length = row - start + 1;
                if (length >= config.getTamanhoMinimoMatch()) {
                    for (int r = start; r <= row; r++) {
                        matched.add(new Position(r, col));
                    }
                }
                row++;
            }
        }

        return matched;
    }

    private List<Integer> findRunLengths(Board board) {
        List<Integer> lengths = new ArrayList<>();

        // Horizontal runs
        for (int row = 0; row < board.getLinhas(); row++) {
            int col = 0;
            while (col < board.getColunas()) {
                int start = col;
                int value = board.get(row, col);
                while (col + 1 < board.getColunas() && board.get(row, col + 1) == value) {
                    col++;
                }
                int length = col - start + 1;
                if (length >= config.getTamanhoMinimoMatch()) {
                    lengths.add(length);
                }
                col++;
            }
        }

        // Vertical runs
        for (int col = 0; col < board.getColunas(); col++) {
            int row = 0;
            while (row < board.getLinhas()) {
                int start = row;
                int value = board.get(row, col);
                while (row + 1 < board.getLinhas() && board.get(row + 1, col) == value) {
                    row++;
                }
                int length = row - start + 1;
                if (length >= config.getTamanhoMinimoMatch()) {
                    lengths.add(length);
                }
                row++;
            }
        }

        return lengths;
    }

    private static class ResultadoResolucao {
        private final int pontosTotais;
        private final List<RodadaAnimacao> rodadas;

        private ResultadoResolucao(int pontosTotais, List<RodadaAnimacao> rodadas) {
            this.pontosTotais = pontosTotais;
            this.rodadas = rodadas;
        }
    }
}
