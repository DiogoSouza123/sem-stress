package com.semstress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BoardTest {

    @Test
    public void deveTrocarPecasComSwap() {
        Board board = BoardTestUtils.boardFromPattern(new TestRandom(0, 1, 2, 3, 4));
        Position first = new Position(0, 0);
        Position second = new Position(0, 1);

        int originalFirst = board.get(0, 0);
        int originalSecond = board.get(0, 1);

        board.swap(first, second);

        assertEquals(originalSecond, board.get(0, 0));
        assertEquals(originalFirst, board.get(0, 1));
    }

    @Test
    public void devePreencherValoresDentroDoLimite() {
        Board board = new Board(new TestRandom(0, 1, 2, 3, 4));
        board.fillRandom();

        for (int row = 0; row < board.getLinhas(); row++) {
            for (int col = 0; col < board.getColunas(); col++) {
                int value = board.get(row, col);
                assertTrue("Valor fora da faixa: " + value, value >= 0 && value < board.getTiposPeca());
            }
        }
    }

    @Test
    public void devePersistirSetGet() {
        Board board = BoardTestUtils.boardFromPattern(new TestRandom(0, 1, 2));

        board.set(3, 4, 2);

        assertEquals(2, board.get(3, 4));
    }
}
