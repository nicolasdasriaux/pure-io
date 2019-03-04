package pureio.console.impure;

import static pureio.console.impure.Console.getStrLn;
import static pureio.console.impure.Console.putStrLn;

public class BrokenExtractVariableConsoleApp {
    public static void main(String[] args) {
        final String s = getStrLn();
        putStrLn("What's player 1 name?");
        final String player1 = s;
        putStrLn("What's player 2 name?");
        final String player2 = s;
        putStrLn(String.format("Players are %s and %s.", player1, player2));
    }
}
