package pureio.console;

import java.util.Scanner;
import java.util.function.Function;
import java.util.function.Supplier;

import static pureio.console.Console.*;

@SuppressWarnings({"CodeBlock2Expr", "WeakerAccess"})
public class ConsoleApp {
    public static Console<Void> program() {
        return putStrLn("Menu").flatMap(_1 -> {
            return putStrLn("1) Hello").flatMap(_2 -> {
                return putStrLn("2) Countdown").flatMap(_3 -> {
                    return putStrLn("3) Exit").flatMap(_4 -> {
                        return getInt(1, 3).flatMap(itemNumber -> {
                            final Console<Boolean> itemAction;

                            switch (itemNumber) {
                                case 1:
                                    itemAction = hello.flatMap(_5 -> result(false));
                                    break;
                                case 2:
                                    itemAction = countdown.flatMap(_5 -> result(false));
                                    break;
                                case 3:
                                    itemAction = result(true);
                                    break;
                                default:
                                    throw new IllegalArgumentException("Unexpected item number");
                            }

                            return itemAction.flatMap(exit -> exit ? Console.result(null) : program());
                        });
                    });
                });
            });
        });
    }

    public static final Console<Void> hello =
            putStrLn("What's you name?").flatMap($_1 -> {
                return getStrLn().flatMap(name -> {
                    return putStrLn("Hello " + name + "!");
                });
            });

    public static final Console<Void> countdown =
            getInt(10, 100000).flatMap(n -> countdown(n));

    public static Console<Void> countdown(final int n) {
        if (n == 0) {
            return putStrLn("Boom");
        } else {
            return putStrLn(Integer.toString(n)).flatMap($_1 -> {
                return countdown(n - 1);
            });
        }
    }

    public static Console<Integer> getInt() {
        return getStrLn().map(Integer::valueOf);
    }

    public static Console<Integer> getInt(final int min, final int max) {
        return putStrLn(String.format("Enter a number between %d and %d", min, max)).flatMap($_1 -> {
            return getInt().flatMap(i -> {
                return min <= i && i <= max ? Console.result(i) : getInt(min, max);
            });
        });
    }

    public static void main(String[] args) {
        Console.runUnsafe(program());
    }
}

interface Console<A> {
    default <B> Console<B> map(final Function<A, B> f) {
        return this.flatMap(a -> Result.of(f.apply(a)));
    }

    default <B> Console<B> flatMap(final Function<A, Console<B>> f) {
        if (this instanceof GetStrLn) {
            final GetStrLn<A> getStrLn = (GetStrLn<A>) this;
            return GetStrLn.of(line -> getStrLn.next().apply(line).flatMap(f));
        } else if (this instanceof PutStrLn) {
            final PutStrLn<A> putStrLn = (PutStrLn<A>) this;
            return PutStrLn.of(putStrLn.line(), () -> putStrLn.next().get().flatMap(f));
        } else if (this instanceof Console.Result) {
            final Result<A> result = (Result<A>) this;
            return f.apply(result.value());
        } else {
            throw new IllegalArgumentException("Unexpected Console operation");
        }
    }

    class GetStrLn<A> implements Console<A> {
        private final Function<String, Console<A>> next;

        private GetStrLn(final Function<String, Console<A>> next) {
            this.next = next;
        }

        public Function<String, Console<A>> next() {
            return next;
        }

        public static <A> GetStrLn<A> of(final Function<String, Console<A>> next) {
            return new GetStrLn<>(next);
        }
    }

    class PutStrLn<A> implements Console<A> {
        private final String line;
        private final Supplier<Console<A>> next;

        private PutStrLn(final String line, final Supplier<Console<A>> next) {
            this.line = line;
            this.next = next;
        }

        public String line() {
            return line;
        }

        public final Supplier<Console<A>> next() {
            return next;
        }

        public static <A> PutStrLn<A> of(final String line, final Supplier<Console<A>> next) {
            return new PutStrLn<>(line, next);
        }
    }

    class Result<A> implements Console<A> {
        private final A value;

        private Result(final A value) {
            this.value = value;
        }

        public A value() {
            return value;
        }

        public static <A> Result<A> of(final A a) {
            return new Result<>(a);
        }
    }

    static <A> A runUnsafe(final Console<A> console) {
        Console<A> current = console;

        do {
            if (current instanceof GetStrLn) {
                final GetStrLn<A> getStrLn = (GetStrLn<A>) current;
                final String line = new Scanner(System.in).nextLine();
                current = getStrLn.next().apply(line);
            } else if (current instanceof PutStrLn) {
                final PutStrLn<A> putStrLn = (PutStrLn<A>) current;
                System.out.println(putStrLn.line());
                current = putStrLn.next().get();
            } else if (current instanceof Result) {
                final Result<A> result = (Result<A>) current;
                return result.value();
            } else {
                throw new IllegalArgumentException("Unexpected Console operation");
            }
        } while (true);
    }

    static Console<String> getStrLn() {
        return GetStrLn.of(line -> Result.of(line));
    }

    static Console<Void> putStrLn(final String line) {
        return new PutStrLn<>(line, () -> Result.of(null));
    }

    static <A> Console<A> result(final A value) {
        return Result.of(value);
    }
}
