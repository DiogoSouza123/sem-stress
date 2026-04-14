package com.semstress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GameEngineSemMovimentosTest {

    @Test
    public void deveDetectarQuandoHaMovimentoDisponivel() {
        ConfiguracaoJogo config = criarConfiguracaoPadraoTeste();
        Board board = new Board(config, new TestRandom(0, 1, 2, 3, 4));
        GameEngine engine = new GameEngine(config);

        int[][] matriz = {
                {0, 1, 0, 2},
                {3, 0, 4, 1},
                {2, 4, 1, 3},
                {1, 2, 3, 4}
        };
        preencher(board, matriz);

        assertTrue(engine.temMovimentoDisponivel(board));
    }

    @Test
    public void deveDetectarQuandoNaoHaMovimentoDisponivel() {
        ConfiguracaoJogo config = criarConfiguracaoPadraoTeste();
        Board board = new Board(config, new TestRandom(0, 1, 2, 3, 4));
        GameEngine engine = new GameEngine(config);

        int[][] matriz = {
                {0, 1, 2, 3},
                {1, 2, 3, 4},
                {2, 3, 4, 0},
                {3, 4, 0, 1}
        };
        preencher(board, matriz);

        assertFalse(engine.temMovimentoDisponivel(board));
    }

    @Test
    public void deveReembaralharComAnimacaoParaTabuleiroJogavel() {
        ConfiguracaoJogo config = criarConfiguracaoPadraoTeste();
        Board board = new Board(config, new TestRandom(4, 2, 0, 3, 1, 4, 0, 2, 1, 3, 4, 1, 0, 2, 3));
        GameEngine engine = new GameEngine(config);

        int[][] matrizSemMovimento = {
                {0, 1, 2, 3},
                {1, 2, 3, 4},
                {2, 3, 4, 0},
                {3, 4, 0, 1}
        };
        preencher(board, matrizSemMovimento);

        ResultadoJogadaAnimada resultado = engine.resetarTabuleiroSemMovimentosAnimado(board);

        assertTrue(resultado.isValid());
        assertEquals(0, resultado.getPoints());
        assertEquals(1, resultado.getRodadas().size());
        assertEquals(16, resultado.getRodadas().get(0).getPosicoesMatch().size());
        assertFalse(resultado.getRodadas().get(0).getQuadrosQueda().isEmpty());
        assertFalse(BoardTestUtils.hasAnyMatch(board));
        assertTrue(engine.temMovimentoDisponivel(board));
    }

    private ConfiguracaoJogo criarConfiguracaoPadraoTeste() {
        return new ConfiguracaoJogo(
                4,
                4,
                5,
                3,
                100,
                200,
                300,
                1,
                true,
                20,
                2000,
                true,
                false,
                true,
                false,
                false,
                "/com/semstress/audio/musica-exemplo.wav",
                0,
                null
        );
    }

    private void preencher(Board board, int[][] matriz) {
        for (int row = 0; row < board.getLinhas(); row++) {
            for (int col = 0; col < board.getColunas(); col++) {
                board.set(row, col, matriz[row][col]);
            }
        }
    }
}
