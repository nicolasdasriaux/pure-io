package pureio.console;

import io.vavr.control.Option;
import io.vavr.control.Try;

import static pureio.console.ConsoleProgram.*;

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

    public static ConsoleProgram<Option<Integer>> getMaybeInt() {
        return getStrLn().thenTransform(s -> parseInt(s));
    }

    public static ConsoleProgram<Integer> getInt() {
        return getMaybeInt().thenChain(maybeInt -> {
            return maybeInt.isDefined() ? yield(maybeInt.get()) : getInt() /* RECURSE */;
        });
    }

    public static ConsoleProgram<Integer> getIntBetween(final int min, final int max) {
        return putStrLn(String.format("Enter a number between %d and %d", min, max)).thenChain(__ -> {
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

    public static final ConsoleProgram<Unit> printMenu =
            putStrLn("Menu")
                    .thenChain(__ -> putStrLn("1) Hello"))
                    .thenChain(__ -> putStrLn("2) Countdown"))
                    .thenChain(__ -> putStrLn("3) Exit"));

    public static ConsoleProgram<Unit> mainApp() {
        return printMenu.thenChain(__1 -> {
            return getIntBetween(1, 3).thenChain(itemNumber -> {
                final ConsoleProgram<Boolean> itemAction;
                switch (itemNumber) {
                    case 1: itemAction = helloApp.thenChain(__2 -> yield(false)); break;
                    case 2: itemAction = countdownApp.thenChain(__2 -> yield(false)); break;
                    case 3: itemAction = yield(true); break;
                    default: throw new IllegalArgumentException("Unexpected item number");
                }
                return itemAction.thenChain(exit -> exit ? yield(Unit.of()) : /* RECURSE */ mainApp());
            });
        });
    }

    public static void main(String[] args) {
        final ConsoleProgram<Unit> program = mainApp();
        runUnsafe(program);
    }
}
