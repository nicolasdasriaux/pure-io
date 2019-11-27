package pureio.kata.guessnumber;

import pureio.presentation.console.purefree.Program;

public class Random {
    private static final java.util.Random random = new java.util.Random();

    public static Program<Integer> randomBetween(final int min, final int max) {
        return Program.of(() -> random.nextInt(max - min + 1) + min);
    }
}
