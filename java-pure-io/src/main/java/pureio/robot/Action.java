package pureio.robot;

import io.vavr.Tuple;
import io.vavr.Tuple0;
import io.vavr.Tuple1;
import io.vavr.match.annotation.Patterns;
import io.vavr.match.annotation.Unapply;
import org.immutables.value.Value;

@Patterns
public interface Action {
    @Value.Immutable(singleton = true)
    abstract class Sleep implements Action {
        public static Sleep of() { return ImmutableSleep.of(); }
    }
    
    @Value.Immutable
    abstract class Walk implements Action {
        @Value.Parameter public abstract Direction direction();
        public static Walk of(final Direction direction) { return ImmutableWalk.of(direction); }
    }
    
    @Value.Immutable
    abstract class Jump implements Action {
        @Value.Parameter public abstract Position position();
        public static Jump of(final Position position) { return ImmutableJump.of(position); }
    }

    @Unapply
    static Tuple0 Sleep(final Sleep sleep) {
        return Tuple.empty();
    }

    @Unapply
    static Tuple1<Position> Jump(final Jump jump) {
        return Tuple.of(jump.position());
    }

    @Unapply
    static Tuple1<Direction> Walk(final Walk walk) {
        return Tuple.of(walk.direction());
    }
}
