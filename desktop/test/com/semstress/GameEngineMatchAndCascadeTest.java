package com.semstress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GameEngineMatchAndCascadeTest {

    private final GameEngine engine = new GameEngine();

    @Test
    public void devePontuarMatchHorizontalDe3() {
        Board board = BoardTestUtils.boardFromPattern(new TestRandom(0, 1, 2, 3, 4));
        board.set(0, 0, 2);
        board.set(0, 1, 2);
        board.set(0, 2, 2);

        int points = engine.resolveBoard(board);

        assertEquals(500, points);
        assertFalse(BoardTestUtils.hasAnyMatch(board));
    }

    @Test
    public void devePontuarMatchVerticalDe3() {
        Board board = BoardTestUtils.boardFromPattern(new TestRandom(0, 1, 2, 3, 4));
        board.set(0, 0, 3);
        board.set(1, 0, 3);
        board.set(2, 0, 3);
        board.set(3, 0, 4);

        int points = engine.resolveBoard(board);

        assertEquals(500, points);
        assertFalse(BoardTestUtils.hasAnyMatch(board));
    }

    @Test
    public void devePontuarMatchHorizontalDe4() {
        Board board = BoardTestUtils.boardFromPattern(new TestRandom(0, 1, 2, 3, 4));
        board.set(0, 0, 4);
        board.set(0, 1, 4);
        board.set(0, 2, 4);
        board.set(0, 3, 4);
        board.set(0, 4, 2);

        int points = engine.resolveBoard(board);

        assertEquals(1000, points);
        assertFalse(BoardTestUtils.hasAnyMatch(board));
    }

    @Test
    public void deveResolverCascadeSemCelulasInvalidas() {
        Board board = BoardTestUtils.boardFromPattern(new TestRandom(0, 1, 2, 3, 4));

        // Um bloco 3x3 do mesmo tipo para forcar multiplas remocoes e queda.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                board.set(row, col, 1);
            }
        }

        int points = engine.resolveBoard(board);

        assertTrue(points >= 1500);
        for (int row = 0; row < board.getLinhas(); row++) {
            for (int col = 0; col < board.getColunas(); col++) {
                int value = board.get(row, col);
                assertTrue("Valor invalido apos cascade: " + value, value >= 0 && value < board.getTiposPeca());
            }
        }
    }
}
