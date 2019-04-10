package pureio.console.pure;

import io.vavr.control.Option;
import io.vavr.control.Try;

import static pureio.console.pure.Console.getStrLn;
import static pureio.console.pure.Console.putStrLn;

@SuppressWarnings({"CodeBlock2Expr", "WeakerAccess", "Convert2MethodRef", "Duplicates"})
public class ConsoleApp {
    // ------ Hello App -----------------------------------------------------------------------------------------------

    public static final Program<Unit> helloApp =
            putStrLn("What's you name?").thenChain(__ -> {
                return getStrLn().thenChain(name -> {
                    return putStrLn("Hello " + name + "!");
                });
            });

    // ------ Get Int -------------------------------------------------------------------------------------------------

    public static Option<Integer> parseInt(final String s) {
        return Try.of(() -> Integer.valueOf(s)).toOption();
    }

    public static Program<Integer> getInt() {
        return getStrLn()
                .thenTransform(s -> parseInt(s))
                .thenChain(maybeInt -> {
                    return maybeInt.isDefined() ? Program.yield(maybeInt.get()) : /* RECURSE */ getInt();
                });
    }

    public static Program<Integer> getIntBetween(final int min, final int max) {
        final String message = String.format("Enter a number between %d and %d", min, max);
        return putStrLn(message).thenChain(__ -> {
            return getInt().thenChain(i -> {
                return min <= i && i <= max ? Program.yield(i) : /* RECURSE */ getIntBetween(min, max);
            });
        });
    }

    // ------ Countdown App -------------------------------------------------------------------------------------------

    public static final Program<Unit> countdownApp =
            getIntBetween(10, 100000).thenChain(n -> {
                return countdown(n);
            });

    public static Program<Unit> countdown(final int n) {
        if (n == 0) {
            return putStrLn("BOOM!!!");
        } else {
            return putStrLn(Integer.toString(n)).thenChain(__ -> {
                return /* RECURSE */ countdown(n - 1);
            });
        }
    }

    // ------ Main App ------------------------------------------------------------------------------------------------

    public static final Program<Unit> displayMenu =
            putStrLn("Menu")
                    .thenChain(__ -> putStrLn("1) Hello"))
                    .thenChain(__ -> putStrLn("2) Countdown"))
                    .thenChain(__ -> putStrLn("3) Exit"));

    public static final Program<Integer> getChoice =
            getIntBetween(1, 3);

    public static Program<Boolean> launchMenuItem(final int choice) {
        switch (choice) {
            case 1: return helloApp.thenTransform(__ -> false);
            case 2: return countdownApp.thenTransform(__ -> false);
            case 3: return Program.yield(true); // Should exit
            default: throw new IllegalArgumentException("Unexpected choice");
        }
    }

    public static Program<Unit> mainApp() {
        return displayMenu.thenChain(__ -> {
            return getChoice.thenChain(choice -> {
                return launchMenuItem(choice).thenChain(exit -> {
                    if (exit) {
                        return Program.yield(Unit.of());
                    } else {
                        return /* RECURSE */ mainApp();
                    }
                });
            });
        });
    }

    public static void main(String[] args) {
        final Program<Unit> program = mainApp();
        Program.unsafeRun(program);
    }
}
