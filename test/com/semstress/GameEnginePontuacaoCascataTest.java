package com.semstress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GameEnginePontuacaoCascataTest {

    @Test
    public void devePontuarSomentePrimeiraRodadaQuandoCascataDesabilitada() {
        ConfiguracaoJogo config = criarConfiguracao(false);
        Board board = criarTabuleiroComCascata(config);
        GameEngine engine = new GameEngine(config);

        int pontos = engine.resolveBoard(board);

        assertEquals(100, pontos);
    }

    @Test
    public void devePontuarMaisQuandoCascataHabilitada() {
        ConfiguracaoJogo configSemCascata = criarConfiguracao(false);
        ConfiguracaoJogo configComCascata = criarConfiguracao(true);

        Board boardSemCascata = criarTabuleiroComCascata(configSemCascata);
        Board boardComCascata = criarTabuleiroComCascata(configComCascata);

        GameEngine engineSemCascata = new GameEngine(configSemCascata);
        GameEngine engineComCascata = new GameEngine(configComCascata);

        int pontosSemCascata = engineSemCascata.resolveBoard(boardSemCascata);
        int pontosComCascata = engineComCascata.resolveBoard(boardComCascata);

        assertEquals(100, pontosSemCascata);
        assertTrue("Com pontuacao de cascata habilitada, esperado valor maior.", pontosComCascata > pontosSemCascata);
    }

    private Board criarTabuleiroComCascata(ConfiguracaoJogo config) {
        // Sequencia de refill:
        // - Primeiras 3 pecas geram 3 matches verticais apos o primeiro clear.
        // - Demais valores evitam um loop de matches infinitos.
        TestRandom random = new TestRandom(2, 1, 0, 0, 1, 2, 1, 2, 0, 2, 0, 1, 1, 0, 2);
        Board board = new Board(config, random);

        // 4x3 com match inicial somente na ultima linha (1,1,1).
        board.set(0, 0, 2);
        board.set(0, 1, 1);
        board.set(0, 2, 0);

        board.set(1, 0, 2);
        board.set(1, 1, 1);
        board.set(1, 2, 0);

        board.set(2, 0, 0);
        board.set(2, 1, 2);
        board.set(2, 2, 1);

        board.set(3, 0, 1);
        board.set(3, 1, 1);
        board.set(3, 2, 1);

        return board;
    }

    private ConfiguracaoJogo criarConfiguracao(boolean pontuarCascata) {
        return new ConfiguracaoJogo(
                4,  // linhas
                3,  // colunas
                3,  // tipos de peca
                3,  // tamanho minimo match
                100, // match 3
                200, // match 4
                300, // match 5+
                2,   // multiplicador cascata
                pontuarCascata,
                30,
                1000,
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
}
