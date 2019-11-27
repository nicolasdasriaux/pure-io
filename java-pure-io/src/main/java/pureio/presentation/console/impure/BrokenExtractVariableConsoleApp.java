package pureio.presentation.console.impure;

import static pureio.presentation.console.impure.Console.getStrLn;
import static pureio.presentation.console.impure.Console.putStrLn;

public class BrokenExtractVariableConsoleApp {
    public static void main(final String[] args) {
        final String s = getStrLn();
        putStrLn("What's player 1 name?");
        final String player1 = s;
        putStrLn("What's player 2 name?");
        final String player2 = s;
        putStrLn(String.format("Players are %s and %s.", player1, player2));
    }
}
