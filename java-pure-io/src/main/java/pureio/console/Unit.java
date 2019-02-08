package pureio.console;

import io.vavr.match.annotation.Unapply;
import org.immutables.value.Value;

@Value.Immutable(singleton = true)
public abstract class Unit {
    public static Unit of() {
        return ImmutableUnit.of();
    }
}
