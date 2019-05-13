package pureio.console.purefree;

import org.immutables.value.Value;

import java.util.function.Function;
import java.util.function.Supplier;

public interface Program<A> {
    @Value.Immutable
    abstract class Of<A> implements Program<A> {
        @Value.Parameter
        abstract Supplier<A> unsafeRun();

        static <A> Of<A> of(final Supplier<A> unsafeRun) {
            return ImmutableOf.of(unsafeRun);
        }
    }

    @Value.Immutable
    abstract class Yield<A> implements Program<A> {
        @Value.Parameter
        abstract A value();

        static <A> Yield<A> of(final A value) {
            return ImmutableYield.of(value);
        }
    }

    @Value.Immutable
    abstract class ThenChain<A, B> implements Program<B> {
        @Value.Parameter
        abstract Program<A> pa();

        @Value.Parameter
        abstract Function<A, Program<B>> f();

        static <A, B> ThenChain<A, B> of(final Program<A> pa, final Function<A, Program<B>> f) {
            return ImmutableThenChain.of(pa, f);
        }
    }

    static <A> Program<A> of(final Supplier<A> unsafeRun) {
        return Of.of(unsafeRun);
    }

    static <A> Program<A> yield(final A a) {
        return Yield.of(a);
    }

    default <B> Program<B> thenChain(final Function<A, Program<B>> f) {
        return ThenChain.of(this, f);
    }

    default <B> Program<B> thenTransform(final Function<A, B> f) {
        return ThenChain.of(this, a -> Yield.of(f.apply(a)));
    }

    static <A> A unsafeRun(final Program<A> program) {
        Program<A> current = program;
        do { // Run all steps (mostly) stack-free even for recursion (trampoline)
            if (current instanceof Of) {
                final Of<A> of = (Of<A>) current;
                return of.unsafeRun().get();                              // RETURN result
            } else if (current instanceof Yield) {
                final Yield<A> yield = (Yield<A>) current;
                return yield.value();                                     // RETURN result
            } else if (current instanceof ThenChain) {
                final ThenChain<Object, A> thenChain = (ThenChain<Object, A>) current;
                final Object a = /* RECURSE */ unsafeRun(thenChain.pa()); // EXECUTE current step
                current = thenChain.f().apply(a);                         // GET remaining steps (continuation)
            } else {
                throw new IllegalArgumentException("Unexpected Program");
            }
        } while (true);
    }
}
