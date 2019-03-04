package pureio.console.impure;

import static pureio.console.impure.Console.getStrLn;
import static pureio.console.impure.Console.putStrLn;

public class BrokenInlineVariableConsoleApp {
    public static void main(String[] args) {
        putStrLn("What's player 1 name?");
        putStrLn("What's player 2 name?");
        final String player2 = getStrLn();
        putStrLn(String.format("Players are %s and %s.", getStrLn(), player2));
    }
}
