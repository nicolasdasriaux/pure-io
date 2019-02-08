package pureio.robot;

import org.immutables.value.Value;

@Value.Immutable
public abstract class Position {
    @Value.Parameter
    public abstract int x();

    @Value.Parameter
    public abstract int y();

    public Position move(final Direction direction) {
        switch (direction) {
            case Up: return ImmutablePosition.copyOf(this).withY(y() - 1);
            case Down: return ImmutablePosition.copyOf(this).withY(y() + 1);
            case Left: return ImmutablePosition.copyOf(this).withX(x() - 1);
            case Right: return ImmutablePosition.copyOf(this).withX(x() + 1);
            default: throw new IllegalArgumentException(
                    String.format("Unknown Direction (%s)", direction));
        }
    }

    public static Position of(final int x, final int y) {
        return ImmutablePosition.of(x, y);
    }
}
