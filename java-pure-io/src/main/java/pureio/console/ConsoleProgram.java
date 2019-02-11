package pureio.console;

import org.immutables.value.Value;

import java.util.Scanner;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "Convert2MethodRef"})
interface ConsoleProgram<A> {
    @Value.Immutable
    abstract class GetStrLn<A> implements ConsoleProgram<A> {
        @Value.Parameter
        public abstract Function<String, ConsoleProgram<A>> next();

        public static <A> GetStrLn<A> of(final Function<String, ConsoleProgram<A>> next) {
            return ImmutableGetStrLn.of(next);
        }
    }

    @Value.Immutable
    abstract class PutStrLn<A> implements ConsoleProgram<A> {
        @Value.Parameter
        public abstract String line();

        @Value.Parameter
        public abstract Supplier<ConsoleProgram<A>> next();

        public static <A> PutStrLn<A> of(final String line, final Supplier<ConsoleProgram<A>> next) {
            return ImmutablePutStrLn.of(line, next);
        }
    }

    @Value.Immutable
    abstract class Yield<A> implements ConsoleProgram<A> {
        @Value.Parameter
        public abstract A value();

        public static <A> Yield<A> of(final A a) {
            return ImmutableYield.of(a);
        }
    }

    static ConsoleProgram<String> getStrLn() {
        return GetStrLn.of(line -> yield(line));
    }

    static ConsoleProgram<Unit> putStrLn(final String line) {
        return PutStrLn.of(line, () -> yield(Unit.of()));
    }

    static <A> ConsoleProgram<A> yield(final A value) {
        return Yield.of(value);
    }

    default <B> ConsoleProgram<B> thenChain(final Function<A, ConsoleProgram<B>> f) {
        if (this instanceof GetStrLn) {
            final GetStrLn<A> getStrLn = (GetStrLn<A>) this;
            return GetStrLn.of(line -> getStrLn.next().apply(line).thenChain(f));
        } else if (this instanceof PutStrLn) {
            final PutStrLn<A> putStrLn = (PutStrLn<A>) this;
            return PutStrLn.of(putStrLn.line(), () -> putStrLn.next().get().thenChain(f));
        } else if (this instanceof Yield) {
            final Yield<A> yield = (Yield<A>) this;
            return f.apply(yield.value());
        } else {
            throw new IllegalArgumentException("Unexpected Console Program");
        }
    }

    default <B> ConsoleProgram<B> thenTransform(final Function<A, B> f) {
        return this.thenChain(a -> Yield.of(f.apply(a)));
    }

    static <A> A runUnsafe(final ConsoleProgram<A> consoleProgram) {
        ConsoleProgram<A> current = consoleProgram;
        do { // Run all steps (trampoline)
            if (current instanceof GetStrLn) { final GetStrLn<A> getStrLn = (GetStrLn<A>) current;
                final String line = new Scanner(System.in).nextLine(); // Run current step
                current = getStrLn.next().apply(line);                 // Run remaining steps (continuation)
            } else if (current instanceof PutStrLn) { final PutStrLn<A> putStrLn = (PutStrLn<A>) current;
                System.out.println(putStrLn.line());                   // Run current setp
                current = putStrLn.next().get();                       // Run remaining steps (continuation)
            } else if (current instanceof Yield) { final Yield<A> yield = (Yield<A>) current;
                return yield.value();                                  // Return result
            } else {
                throw new IllegalArgumentException("Unexpected ConsoleProgram operation");
            }
        } while (true);
    }
}
