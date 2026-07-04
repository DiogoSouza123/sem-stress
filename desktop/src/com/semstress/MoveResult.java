package com.semstress;

public class MoveResult {
    private final boolean valid;
    private final int points;

    public MoveResult(boolean valid, int points) {
        this.valid = valid;
        this.points = points;
    }

    public boolean isValid() {
        return valid;
    }

    public int getPoints() {
        return points;
    }
}
