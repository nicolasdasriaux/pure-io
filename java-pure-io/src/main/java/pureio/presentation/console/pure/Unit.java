package pureio.presentation.console.pure;

import org.immutables.value.Value;

@Value.Immutable(singleton = true)
public abstract class Unit {
    public static Unit of() {
        return ImmutableUnit.of();
    }
}
