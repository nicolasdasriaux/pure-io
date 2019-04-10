package pureio.console.purefree;

import org.immutables.value.Value;
import pureio.console.pure.Unit;

import java.util.Scanner;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "Convert2MethodRef", "UnnecessaryLocalVariable"})
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
            final Function<String, ConsoleProgram<A>> next = getStrLn.next();

            final Function<String, ConsoleProgram<B>> chainedNext = line -> {
                final ConsoleProgram<A> cpa = next.apply(line);
                final ConsoleProgram<B> cpb = cpa.thenChain(f);
                return cpb;
            };

            return GetStrLn.of(chainedNext);
        } else if (this instanceof PutStrLn) {
            final PutStrLn<A> putStrLn = (PutStrLn<A>) this;
            final String line = putStrLn.line();
            final Supplier<ConsoleProgram<A>> next = putStrLn.next();

            final Supplier<ConsoleProgram<B>> chainedNext = () -> {
                final ConsoleProgram<A> cpa = next.get();
                final ConsoleProgram<B> cpb = cpa.thenChain(f);
                return cpb;
            };

            return PutStrLn.of(line, chainedNext);
        } else if (this instanceof Yield) {
            final Yield<A> yield = (Yield<A>) this;
            final A a = yield.value();
            final ConsoleProgram<B> cpb = f.apply(a);
            return cpb;
        } else {
            throw new IllegalArgumentException("Unexpected Console Program");
        }
    }

    default <B> ConsoleProgram<B> thenTransform(final Function<A, B> f) {
        return this.thenChain(a -> {
            final B b = f.apply(a);
            return Yield.of(b);
        });
    }

    static <A> A unsafeRun(final ConsoleProgram<A> consoleProgram) {
        ConsoleProgram<A> current = consoleProgram;
        do { // Run all steps stack-free even for recursion (trampoline)
            if (current instanceof GetStrLn) { final GetStrLn<A> getStrLn = (GetStrLn<A>) current;
                final String line = new Scanner(System.in).nextLine(); // EXECUTE current step
                current = getStrLn.next().apply(line);                 // GET remaining steps (continuation)
            } else if (current instanceof PutStrLn) { final PutStrLn<A> putStrLn = (PutStrLn<A>) current;
                System.out.println(putStrLn.line());                   // EXECUTE current setp
                current = putStrLn.next().get();                       // GET remaining steps (continuation)
            } else if (current instanceof Yield) { final Yield<A> yield = (Yield<A>) current;
                return yield.value();                                  // RETURN result
            } else {
                throw new IllegalArgumentException("Unexpected Console Program");
            }
        } while (true);
    }
}
