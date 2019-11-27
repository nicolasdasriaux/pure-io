package pureio.presentation.console.impure;

import java.util.Scanner;

public class Console {
    public static String getStrLn() {
        return new Scanner(System.in).nextLine();
    }

    public static void putStrLn(final String line) {
        System.out.println(line);
    }
}
