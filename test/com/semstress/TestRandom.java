package com.semstress;

import java.util.Random;

public class TestRandom extends Random {
    private final int[] sequence;
    private int index;

    public TestRandom(int... sequence) {
        this.sequence = sequence == null ? new int[0] : sequence.clone();
        this.index = 0;
    }

    @Override
    public int nextInt(int bound) {
        if (sequence.length == 0) {
            return 0;
        }
        int value = sequence[index % sequence.length];
        index++;
        int normalized = value % bound;
        return normalized < 0 ? normalized + bound : normalized;
    }
}
