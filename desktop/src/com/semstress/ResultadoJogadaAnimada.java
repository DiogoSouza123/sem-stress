package com.semstress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResultadoJogadaAnimada {
    private final boolean valid;
    private final int points;
    private final List<RodadaAnimacao> rodadas;

    public ResultadoJogadaAnimada(boolean valid, int points, List<RodadaAnimacao> rodadas) {
        this.valid = valid;
        this.points = points;
        this.rodadas = Collections.unmodifiableList(new ArrayList<>(rodadas));
    }

    public static ResultadoJogadaAnimada invalido() {
        return new ResultadoJogadaAnimada(false, 0, Collections.<RodadaAnimacao>emptyList());
    }

    public boolean isValid() {
        return valid;
    }

    public int getPoints() {
        return points;
    }

    public List<RodadaAnimacao> getRodadas() {
        return rodadas;
    }
}
