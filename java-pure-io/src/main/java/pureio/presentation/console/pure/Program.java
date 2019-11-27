package pureio.presentation.console.pure;

import org.immutables.value.Value;

import java.util.function.Function;
import java.util.function.Supplier;

@Value.Immutable
public abstract class Program<A> {
    @Value.Parameter
    public abstract Supplier<A> unsafeAction();

    public static <A> Program<A> of(final Supplier<A> unsafeAction) {
        return ImmutableProgram.of(unsafeAction);
    }

    public static <A> Program<A> yield(final A a) {
        return Program.of(() -> a);
    }

    public <B> Program<B> thenChain(final Function<A, Program<B>> f) {
        final Program<A> pa = this;
        final Program<B> pb = Program.of(() -> {
            final A a = pa.unsafeAction().get();
            final Program<B> _pb = f.apply(a);
            final B b = _pb.unsafeAction().get();
            return b;
        });
        return pb;
    }

    public <B> Program<B> thenTransform(final Function<A, B> f) {
        final Program<A> pa = this;
        final Program<B> pb = pa.thenChain(a -> {
            final B b = f.apply(a);
            final Program<B> _pb = Program.yield(b);
            return _pb;
        });
        return pb;
    }

    public static <A> A unsafeRun(final Program<A> program) {
        return program.unsafeAction().get();
    }
}
