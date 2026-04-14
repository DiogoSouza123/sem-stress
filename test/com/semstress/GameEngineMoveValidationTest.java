package com.semstress;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GameEngineMoveValidationTest {

    private final GameEngine engine = new GameEngine();

    @Test
    public void deveRejeitarTrocaNaoAdjacenteSemAlterarTabuleiro() {
        Board board = BoardTestUtils.boardFromPattern(new TestRandom(0, 1, 2, 3, 4));
        int[][] before = BoardTestUtils.snapshot(board);

        MoveResult result = engine.tryMove(board, new Position(0, 0), new Position(2, 2));

        assertFalse(result.isValid());
        BoardTestUtils.assertBoardEquals(before, board);
    }

    @Test
    public void deveRejeitarTrocaAdjacenteSemMatchSemAlterarTabuleiro() {
        Board board = BoardTestUtils.boardFromPattern(new TestRandom(0, 1, 2, 3, 4));
        int[][] before = BoardTestUtils.snapshot(board);

        MoveResult result = engine.tryMove(board, new Position(0, 0), new Position(0, 1));

        assertFalse(result.isValid());
        BoardTestUtils.assertBoardEquals(before, board);
    }

    @Test
    public void deveAceitarTrocaAdjacenteQueGeraMatch() {
        Board board = BoardTestUtils.boardFromPattern(new TestRandom(0, 1, 2, 3, 4));

        // Configura jogada: trocar (0,1) com (1,1) gera linha 1-1-1 na linha 0.
        board.set(0, 0, 1);
        board.set(0, 1, 2);
        board.set(0, 2, 1);
        board.set(1, 1, 1);

        MoveResult result = engine.tryMove(board, new Position(0, 1), new Position(1, 1));

        assertTrue(result.isValid());
        assertTrue("Pontuacao esperada >= 500", result.getPoints() >= 500);
    }
}
