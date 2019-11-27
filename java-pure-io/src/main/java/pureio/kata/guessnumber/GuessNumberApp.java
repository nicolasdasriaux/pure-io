package pureio.kata.guessnumber;

import io.vavr.control.Option;
import io.vavr.control.Try;
import pureio.presentation.console.purefree.Program;
import pureio.presentation.console.purefree.Unit;

import static pureio.kata.guessnumber.Random.randomBetween;
import static pureio.presentation.console.purefree.Console.getStrLn;
import static pureio.presentation.console.purefree.Console.putStrLn;

@SuppressWarnings({"CodeBlock2Expr", "Convert2MethodRef", "WeakerAccess"})
public class GuessNumberApp {
    public static void main(final String[] args) {
        Program.unsafeRun(mainApp);
    }

    public static final Program<Unit> mainApp =
            putStrLn("Guess a number between 1 and 20.").thenChain(__ -> {
                return randomBetween(1, 20).thenChain(number -> {
                    return guessLoop(number, 1);
                });
            });

    public static Program<Unit> guessLoop(final int number, final int attempt) {
        return putStrLn(String.format("Attempt %d>", attempt)).thenChain(__ -> {
            return getIntBetween(1, 20).thenChain(guess -> {
                return checkAttempt(number, attempt, guess).thenChain(won -> {
                    return won ? Program.yield(Unit.of()) : guessLoop(number, attempt + 1);
                });
            });
        });
    }

    public static Program<Boolean> checkAttempt(final int number, final int attempt, final int guess) {
        if (guess < number) {
            return putStrLn("It's too small.").thenTransform(__ -> false);
        } else if (guess > number) {
            return putStrLn("It's too large.").thenTransform(__ -> false);
        } else {
            return putStrLn(String.format("You won after %d attempt(s).", attempt)).thenTransform(__ -> true);
        }
    }

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
        return getInt().thenChain(i -> {
            return min <= i && i <= max ? Program.yield(i) : /* RECURSE */ getIntBetween(min, max);
        });
    }
}
