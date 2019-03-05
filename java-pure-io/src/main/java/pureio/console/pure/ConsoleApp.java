package pureio.console.pure;

import io.vavr.control.Option;
import io.vavr.control.Try;

import static pureio.console.pure.ConsoleProgram.*;

@SuppressWarnings({"CodeBlock2Expr", "WeakerAccess", "Convert2MethodRef"})
public class ConsoleApp {
    // ------ Hello App -----------------------------------------------------------------------------------------------

    public static final ConsoleProgram<Unit> helloApp =
            putStrLn("What's you name?").thenChain(__ -> {
                return getStrLn().thenChain(name -> {
                    return putStrLn("Hello " + name + "!");
                });
            });

    // ------ Get Int -------------------------------------------------------------------------------------------------

    public static Option<Integer> parseInt(final String s) {
        return Try.of(() -> Integer.valueOf(s)).toOption();
    }

    public static ConsoleProgram<Integer> getInt() {
        return getStrLn()
                .thenTransform(s -> parseInt(s))
                .thenChain(maybeInt -> {
                    return maybeInt.isDefined() ? yield(maybeInt.get()) : /* RECURSE */ getInt();
                });
    }

    public static ConsoleProgram<Integer> getIntBetween(final int min, final int max) {
        final String message = String.format("Enter a number between %d and %d", min, max);
        return putStrLn(message).thenChain(__ -> {
            return getInt().thenChain(i -> {
                return min <= i && i <= max ? yield(i) : /* RECURSE */ getIntBetween(min, max);
            });
        });
    }

    // ------ Countdown App -------------------------------------------------------------------------------------------

    public static final ConsoleProgram<Unit> countdownApp =
            getIntBetween(10, 100000).thenChain(n -> {
                return countdown(n);
            });

    public static ConsoleProgram<Unit> countdown(final int n) {
        if (n == 0) {
            return putStrLn("BOOM!!!");
        } else {
            return putStrLn(Integer.toString(n)).thenChain(__ -> {
                return /* RECURSE */ countdown(n - 1);
            });
        }
    }

    // ------ Main App ------------------------------------------------------------------------------------------------

    public static final ConsoleProgram<Unit> displayMenu =
            putStrLn("Menu")
                    .thenChain(__ -> putStrLn("1) Hello"))
                    .thenChain(__ -> putStrLn("2) Countdown"))
                    .thenChain(__ -> putStrLn("3) Exit"));

    public static final ConsoleProgram<Integer> getChoice =
            getIntBetween(1, 3);

    public static ConsoleProgram<Boolean> launchMenuItem(final int choice) {
        switch (choice) {
            case 1: return helloApp.thenTransform(__ -> false);
            case 2: return countdownApp.thenTransform(__ -> false);
            case 3: return yield(true); // Should exit
            default: throw new IllegalArgumentException("Unexpected item number");
        }
    }

    public static ConsoleProgram<Unit> mainApp() {
        return displayMenu.thenChain(__ -> {
            return getChoice.thenChain(choice -> {
                return launchMenuItem(choice).thenChain(exit -> {
                    if (exit) {
                        return yield(Unit.of());
                    } else {
                        return /* RECURSE */ mainApp();
                    }
                });
            });
        });
    }

    public static void main(String[] args) {
        final ConsoleProgram<Unit> program = mainApp();
        unsafeRun(program);
    }
}