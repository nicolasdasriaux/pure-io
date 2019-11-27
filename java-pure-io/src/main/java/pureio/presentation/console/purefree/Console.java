package pureio.presentation.console.purefree;

import java.util.Scanner;

public class Console {
    public static Program<String> getStrLn() {
        return Program.of(() -> {
            final String line = new Scanner(System.in).nextLine();
            return line;
        });
    }

    public static Program<Unit> putStrLn(final String line) {
        return Program.of(() -> {
            System.out.println(line);
            return Unit.of();
        });
    }
}
