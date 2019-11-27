package pureio.presentation.console.impure;

import static pureio.presentation.console.impure.Console.getStrLn;
import static pureio.presentation.console.impure.Console.putStrLn;

public class BrokenInlineVariableConsoleApp {
    public static void main(final String[] args) {
        putStrLn("What's player 1 name?");
        putStrLn("What's player 2 name?");
        final String player2 = getStrLn();
        putStrLn(String.format("Players are %s and %s.", getStrLn(), player2));
    }
}
