package pureio.presentation.console.impure;

import static pureio.presentation.console.impure.Console.*;

public class ConsoleApp {
    public static void main(final String[] args) {
        putStrLn("What's player 1 name?");
        final String player1 = getStrLn();
        putStrLn("What's player 2 name?");
        final String player2 = getStrLn();
        putStrLn(String.format("Players are %s and %s.", player1, player2));
    }
}
