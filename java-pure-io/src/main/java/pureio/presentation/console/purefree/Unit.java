package pureio.presentation.console.purefree;

import org.immutables.value.Value;

@Value.Immutable(singleton = true)
public abstract class Unit {
    public static Unit of() {
        return ImmutableUnit.of();
    }
}
