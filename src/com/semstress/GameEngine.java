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
        if (!board.posicaoValida(first) || !board.posicaoValida(second)) {
            return new MoveResult(false, 0);
        }

        if (config.isSomenteTrocaAdjacente() && !isAdjacent(first, second)) {
            return new MoveResult(false, 0);
        }

        board.swap(first, second);

        Set<Position> initialMatches = findMatchedCells(board);
        if (initialMatches.isEmpty()) {
            board.swap(first, second);
            return new MoveResult(false, 0);
        }

        int points = resolveBoard(board);
        return new MoveResult(true, points);
    }

    public int resolveBoard(Board board) {
        int totalPoints = 0;
        int nivelCascata = 0;

        while (true) {
            List<Integer> runLengths = findRunLengths(board);
            if (runLengths.isEmpty()) {
                return totalPoints;
            }

            nivelCascata++;
            Set<Position> matched = findMatchedCells(board);
            if (nivelCascata == 1 || config.isPontuarCascata()) {
                totalPoints += calculatePoints(runLengths, nivelCascata);
            }
            clearMatched(board, matched);
            collapseAndRefill(board);
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
        for (int col = 0; col < board.getColunas(); col++) {
            List<Integer> pieces = new ArrayList<>();
            for (int row = board.getLinhas() - 1; row >= 0; row--) {
                int value = board.get(row, col);
                if (value != EMPTY) {
                    pieces.add(value);
                }
            }

            int row = board.getLinhas() - 1;
            for (Integer piece : pieces) {
                board.set(row, col, piece);
                row--;
            }

            while (row >= 0) {
                board.set(row, col, board.nextPiece());
                row--;
            }
        }
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
}
