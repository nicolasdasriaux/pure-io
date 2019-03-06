autoscale: true
footer: Immutable Pure I/O
slidenumbers: true

# Immutable
# [fit] **Pure I/O**
## [fit] in Java... and in Scala with ZIO

---

# Previously on Practical Immutability...

* Immutable Classes
* Immutable Collections and Options
* Immutable variables
* Expressions
* Algebraic Data Types (ADT)
* Pattern Matching

---

# Functional Programming

* FP is programming with **functions** that are:
  - **Deterministic**: same arguments implies same result
  - **Total**: result always available for arguments
  - **Pure**: no side-effects, only effect is computing result
  
* A consequence of FP is **referential transparency**.

---

# Referential Transparency

* FP programs are **referentially transparent**.
  - **Typical refactorings cannot break a working program** :smile:.
* Applies to the following refactorings:
  - Extract variable
  - Inline variable
  - Extract method
  - Inline method

---

# Refactorings Break Impure :imp:  Programs

---

# A Working Program

```java
public class ConsoleApp {
    public static void main(String[] args) {
        putStrLn("What's player 1 name?");
        final String player1 = getStrLn();
        putStrLn("What's player 2 name?");
        final String player2 = getStrLn();
        putStrLn(String.format("Players are %s and %s.", player1, player2));
    }
}
```

```
What's player 1 name?
> Paul
What's player 2 name?
> Mary
Players are Paul and Mary.
```

---

# Broken Extract Variable Refactoring

```java
public class BrokenExtractVariableConsoleApp {
    public static void main(String[] args) {
        final String s = getStrLn();
        putStrLn("What's player 1 name?");
        final String player1 = s;
        putStrLn("What's player 2 name?");
        final String player2 = s;
        putStrLn(String.format("Players are %s and %s.", player1, player2));
    }
}
```

```
> Paul
What's player 1 name?
What's player 2 name?
Players are Paul and Paul.
```

---

# Broken Inline Variable Refactoring

```java
public class BrokenInlineVariableConsoleApp {
    public static void main(String[] args) {
        putStrLn("What's player 1 name?");
        putStrLn("What's player 2 name?");
        final String player2 = getStrLn();
        putStrLn(String.format("Players are %s and %s.", getStrLn(), player2));
    }
}
```

```
What's player 1 name?
What's player 2 name?
> Paul
> Mary
Players are Mary and Paul.
```

---

# Building a Pure Program<br>From The Ground Up
## in Java with _Immutables_ and _Vavr_

---

# Console Program

```java
interface ConsoleProgram<A> { /* ... */ }
```

* Describes a **program** performing I/Os on **console**
* When run, will eventually yield a **result** of type `A`
* `ConsoleProgram<A>` can be a program that
  - **reads a line** from console (`GetStrLn`) and then do the rest, 
  - **prints a line** to console (`PutStrLn`) and the do the rest,
  - just **yields a result** (`Yield`).

---

# Read Line from Console (`GetStrLn`)

```java
interface ConsoleProgram<A> {
    @Value.Immutable
    abstract class GetStrLn<A> implements ConsoleProgram<A> {
        @Value.Parameter
        public abstract Function<String, ConsoleProgram<A>> next();

        public static <A> GetStrLn<A> of(
                final Function<String, ConsoleProgram<A>> next) {
            return ImmutableGetStrLn.of(next);
        }
    } // ...
}
```

---

# Print Line to Console (`PutStrLn`)

```java
interface ConsoleProgram<A> { // ...
    @Value.Immutable
    abstract class PutStrLn<A> implements ConsoleProgram<A> {
        @Value.Parameter
        public abstract String line();
        @Value.Parameter
        public abstract Supplier<ConsoleProgram<A>> next();

        public static <A> PutStrLn<A> of(final String line,
                final Supplier<ConsoleProgram<A>> next) {
            return ImmutablePutStrLn.of(line, next);
        }
    } // ...
}
```

---

# Yield a Result (`Yield`)

```java
interface ConsoleProgram<A> { // ...
    @Value.Immutable
    abstract class Yield<A> implements ConsoleProgram<A> {
        @Value.Parameter
        public abstract A value();

        public static <A> Yield<A> of(final A a) {
            return ImmutableYield.of(a);
        }
    } // ...
}
```

---

# Elementary Programs

```java
interface ConsoleProgram<A> { // ...
    static ConsoleProgram<String> getStrLn() {
        return GetStrLn.of(line -> yield(line));
    }

    static ConsoleProgram<Unit> putStrLn(final String line) {
        return PutStrLn.of(line, () -> yield(Unit.of()));
    }

    static <A> ConsoleProgram<A> yield(final A value) {
        return Yield.of(value);
    } // ...
}
```

---

# A Value Containing Void (`Unit`)

```java
@Value.Immutable(singleton = true)
public abstract class Unit {
    public static Unit of() {
        return ImmutableUnit.of();
    }
}
```

---

# Chaining Programs

```java
interface ConsoleProgram<A> { // ...
    default <B> ConsoleProgram<B> thenChain(final Function<A, ConsoleProgram<B>> f) {
        if (this instanceof GetStrLn) {
            final GetStrLn<A> getStrLn = (GetStrLn<A>) this;
            // ...
        } else if (this instanceof PutStrLn) {
            final PutStrLn<A> putStrLn = (PutStrLn<A>) this;
            // ...
        } else if (this instanceof Yield) {
            final Yield<A> yield = (Yield<A>) this;
            // ...
        } else {
            throw new IllegalArgumentException("Unexpected Console Program");
        }
    } // ...
}
```

---

# Chaining After `GetStrLn`

```java
default <B> ConsoleProgram<B> thenChain(final Function<A, ConsoleProgram<B>> f) {
    // ...
        final GetStrLn<A> getStrLn = (GetStrLn<A>) this;
        final Function<String, ConsoleProgram<A>> next = getStrLn.next();

        final Function<String, ConsoleProgram<B>> chainedNext = line -> {
            final ConsoleProgram<A> cpa = next.apply(line);
            final ConsoleProgram<B> cpb = cpa.thenChain(f);
            return cpb;
        };

        return GetStrLn.of(chainedNext);
    // ...
}
```

---

# Chaining After `PutStrLn`

```java
default <B> ConsoleProgram<B> thenChain(final Function<A, ConsoleProgram<B>> f) {
    // ...
        final PutStrLn<A> putStrLn = (PutStrLn<A>) this;
        final String line = putStrLn.line();
        final Supplier<ConsoleProgram<A>> next = putStrLn.next();

        final Supplier<ConsoleProgram<B>> chainedNext = () -> {
            final ConsoleProgram<A> cpa = next.get();
            final ConsoleProgram<B> cpb = cpa.thenChain(f);
            return cpb;
        };

        return PutStrLn.of(line, chainedNext);
    // ...
}
```

---

# Chaining After `Yield`

```java
default <B> ConsoleProgram<B> thenChain(final Function<A, ConsoleProgram<B>> f) {
    // ...
        final Yield<A> yield = (Yield<A>) this;
        final A a = yield.value();
        final ConsoleProgram<B> cpb = f.apply(a);
        return cpb;
    // ...
}
```

---

# Transforming Result of Program

```java
interface ConsoleProgram<A> { // ...
    default <B> ConsoleProgram<B> thenTransform(final Function<A, B> f) {
        return this.thenChain(a -> {
            final B b = f.apply(a);
            return Yield.of(b);
        });
    } // ...
}
```

---

# Instantiating a Program

```java
public class ConsoleApp {
    public static final ConsoleProgram<Unit> helloApp =
            putStrLn("What's you name?").thenChain(__ -> {
                return getStrLn().thenChain(name -> {
                    return putStrLn("Hello " + name + "!");
                });
            });
    
    public static void main(String[] args) {
        final ConsoleProgram<Unit> program = helloApp;
    }
}
```

---

# But Program Does Not Run

```java
public class ConsoleApp {
    // ...
    public static void main(String[] args) {
        final ConsoleProgram<Unit> program = helloApp;
        System.out.println(program);
    }
}
```

* Will print something like `PutStrLn{line=What's you name?, next=pureio.console.ConsoleProgram$$Lambda$3/396873410@31221be2}`
* This is just an **immutable object**, it does no side-effect, it's **pure** :innocent:. 
* Need an **interpreter** to run!

---

# Interpreting a Program


```java
interface ConsoleProgram<A> { // ...
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
```

---

# Running a Program


```java
public class ConsoleApp {
    // PURE ...
    public static void main(String[] args) {
        final ConsoleProgram<Unit> program = helloApp; // PURE
        unsafeRun(program); // IMPURE!!! But that's OK!
    }
}
```

* Sure, `unsafeRun` call point (**_end of the world_**) is **impure** :imp:... 
* But the **rest of the code** is fully **pure** :innocent:!

---

# Counting Down

```java
    public static final ConsoleProgram<Unit> countdownApp =
            getIntBetween(10, 100000).thenChain(n -> {
                return countdown(n);
            });

    public static ConsoleProgram<Unit> countdown(final int n) {
        if (n == 0) {
            return putStrLn("BOOM!!!");
        } else {
            return putStrLn(Integer.toString(n)).thenChain(__ -> {
                return /* RECURSE */ countdown(n - 1);
            });
        }
    }
```

---

# Diplaying Menu and Getting Choice

```java
public static final ConsoleProgram<Unit> displayMenu =
        putStrLn("Menu")
                .thenChain(__ -> putStrLn("1) Hello"))
                .thenChain(__ -> putStrLn("2) Countdown"))
                .thenChain(__ -> putStrLn("3) Exit"));

public static final ConsoleProgram<Integer> getChoice =
        getIntBetween(1, 3);
```

---

# Launching Menu Item

```java
public static ConsoleProgram<Boolean> launchMenuItem(final int choice) {
    switch (choice) {
        case 1: return helloApp.thenTransform(__ -> false);
        case 2: return countdownApp.thenTransform(__ -> false);
        case 3: return yield(true); // Should exit
        default: throw new IllegalArgumentException("Unexpected choice");
    }
}
```

---

# Looping over Menu

```java
public static ConsoleProgram<Unit> mainApp() {
    return displayMenu.thenChain(__ -> {
        return getChoice.thenChain(choice -> {
            return launchMenuItem(choice).thenChain(exit -> {
                if (exit) {
                    return yield(Unit.of());
                } else {
                    return /* RECURSE */ mainApp();
                }
            });
        });
    });
}
```

---

# Parsing an Integer with a Total Function

```java
public static Option<Integer> parseInt(final String s) {
    return Try.of(() -> Integer.valueOf(s)).toOption();
}
```

* `parseInt` is defined for any `String`, it's **total**.
* No exception! :wink:

| Expression      |  Result   |
|-----------------|-----------|
| `parseInt("3")` | `Some(3)` |
| `parseInt("a")` | `None`    |

---

# Getting Integer from Console

```java
public static ConsoleProgram<Integer> getInt() {
    return getStrLn()
            .thenTransform(s -> parseInt(s))
            .thenChain(maybeInt -> {
                return maybeInt.isDefined() ? yield(maybeInt.get()) : /* RECURSE */ getInt();
            });
}

public static ConsoleProgram<Integer> getIntBetween(final int min, final int max) {
    final String message = String.format("Enter a number between %d and %d", min, max);
    return putStrLn(message).thenChain(__ -> {
        return getInt().thenChain(i -> {
            return min <= i && i <= max ? yield(i) : /* RECURSE */ getIntBetween(min, max);
        });
    });
}
```

---

# Just a Toy

* What's **good** :smile:
  - Stack safe including with recursion
  - Rather efficient
  - Unlimited refactorings
* What's **not so good** :worried:
  - Not general enough, only console programs
  - Nesting can be annoying (extract variables and methods!)

---

# Business-Ready Pure IO
## in Scala with _ZIO_

---

# `IO[E, A]`

```scala
IO[E, A] // E = Error, A = Result
```

* An immutable object that **describes** an **action performing side-effects**
* An `IO` does nothing, it's just a value holding a program
* It must be interpreted by a **runtime system** or **RTS**
* Only when **run** by the RTS, it will either
    - fail with an **error** of type **`E`**,
    - or eventually produce a **result** of type **`A`**.

---

# Hello World!

```scala
object HelloWorldApp extends App {
  // Wraps synchronous (blocking) side-effecting code in an IO
  val helloWorld: IO[Nothing, Unit] =
    IO.effectTotal(/* () => */ Console.println("Hello World!"))
    // The IO just holds a lambda but does not run it for now.

  def run(args: List[String]): IO[Nothing, Int] = {
    helloWorld.either.fold(_ => 1, _ => 0)
  }
}
```

---

# Wrapping in `IO`

---

# Pure in `IO`

```scala
val success: IO[Nothing, Int] = IO.succeed(42)
val successLazy: IO[Nothing, Int] = IO.succeedLazy(/* () => */ 40 + 2)
// Will never fail (Nothing)
// Will always succeed with result 42 (Int)

val failure: IO[String, Nothing] = IO.fail("Failure")
// Will always fail with error "Failed" (String)
// will never succeed (Nothing)

val exceptionFailure: IO[IllegalStateException, Nothing] =
  IO.fail(new IllegalStateException("Failure"))
// Error can be an exception (but just as a value, never thrown!)
 ```

---

# Impure, Synchronous in `IO`

```scala
def randomBetween(min: Int, max: Int): IO[Nothing, Int] = {
  // Side-effecting code updates the state of a random generator,
  // and returns a random number (Int).
  // It can never fail (Nothing).
  IO.effectTotal(Random.nextInt(max - min) + min)
}

def putStrLn(line: String): IO[Nothing, Unit] = {
  // Side-defecting code prints a line,
  // and returns void (Unit).
  // It can never fail (Nothing).
  IO.effectTotal(scala.Console.println(line))
}
```

---

# [fit] Impure, Synchronous, Exception-Throwing in `IO`

```scala
def getStrLn: IO[IOException, String] = {
  // Side-effecting code reads from keyboard until a line is available,
  // and returns the line (String).
  // It might throw an IOException. IO catches exception,
  // and translates it into a failure containing the error (IOException).
  // IOException is neutralized, it is NOT propagated but just used as a value.
  IO.effect(scala.io.StdIn.readLine()).refineOrDie {
    case e: IOException => e
  }
}
```

---

# Combining `IO`s

---

# Transforming `IO` (`map`)

```scala
val randomLetter: IO[Nothing, Char] =
  randomBetween('A', 'Z').map { i /* Int */ =>
    i.toChar /* Char */
  }
```

---

# Chaining `IO`s (broken `map`)

```scala
val printRolledDiceWRONG: IO[Nothing, IO[Nothing, Unit]] =
  randomBetween(1, 6).map { dice /* Int */ =>
    putStrLn(s"Dice shows $dice") /* IO[Nothing, Unit] */
  }
```

* Wrong **nested** type `IO[Nothing, IO[Nothing, Unit]]`
* Needs to be made **flat** somehow as `IO[Nothing, Unit]`

---

# Chaining `IO`s (`flatMap`)

```scala
val printRolledDice: IO[Nothing, Unit] =
  randomBetween(1, 6).flatMap { dice /* Int */ =>
    putStrLn(s"Dice shows $dice") /* IO[Nothing, Unit] */
  }
```

---

# Chaining and Transforming `IO`s

```scala
randomBetween(0, 20).flatMap { x =>
  randomBetween(0, 20).map { y =>
    Point(x, y)
  }
}
```

---

# Pyramid of `map`s and `flatMap`s :smiling_imp:

```scala
val welcomeNewPlayer: IO[IOException, Unit] =
  putStrLn("What's your name?").flatMap { _ =>
    getStrLn.flatMap { name =>
      randomBetween(0, 20).flatMap { x =>
        randomBetween(0, 20).flatMap { y =>
          randomBetween(0, 20).flatMap { z =>
            putStrLn(s"Welcome $name, you start at coordinates($x, $y, $z).")
          }
        }
      }
    }
  }
```

---

# Flatten Them All :innocent:

```scala
val welcomeNewPlayer: IO[IOException, Unit] =
  for {
    _ <- putStrLn("What's your name?")
    name <- getStrLn
    x <- randomBetween(0, 20)
    y <- randomBetween(0, 20)
    z <- randomBetween(0, 20)
    _ <- putStrLn(s"Welcome $name, you start at coordinates($x, $y, $z).")
  } yield ()
```

---

# Intermediary Variable

```scala
val printRandomPoint: IO[Nothing, Unit] =
  for {
    x <- randomBetween(0, 20)
    y <- randomBetween(0, 20)
    point = Point(x, y) // Not running an IO, '=' instead of '<-'
    _ <- putStrLn(s"point=$point")
  } yield ()
```

---

# Anatomy of `for` Comprehension

---

# [fit] **`for` comprehension is not a `for` loop**.
## It can be a `for` loop...
# [fit] But it can handle **many other things**
## like `IO` and ... `Seq`, `Option`, `Future`...

---

# `for` Comprehension **Types**

```scala
val printRandomPoint: IO[Nothing, Point] = {
  for {
    x     /* Int   */ <- randomBetween(0, 10)            /* IO[Nothing, Int]  */
    _     /* Unit  */ <- putStrLn(s"x=$x")               /* IO[Nothing, Unit] */
    y     /* Int   */ <- randomBetween(0, 10)            /* IO[Nothing, Int]  */
    _     /* Unit  */ <- putStrLn(s"y=$y")               /* IO[Nothing, Unit] */
    point /* Point */ =  Point(x, y)                     /* Point             */
    _     /* Unit  */ <- putStrLn(s"point.x=${point.x}") /* IO[Nothing, Unit] */
    _     /* Unit  */ <- putStrLn(s"point.y=${point.y}") /* IO[Nothing, Unit] */
  } yield point /* Point */
} /* IO[Nothing, Point] */
```

---

# `for` Comprehension **Type Rules**

|            | `val` type | operator | expression type |
|------------|------------|----------|-----------------|
| generator  | `A`        | `<-`     | `IO[E, A]`      |
| assignment | `B`        | `=`      | `B`             |

|            | `for` comprehension type | `yield` expression type |
|------------|--------------------------|-------------------------|
| production | `IO[E, R]`               | `R`                     |

* Combines **only `IO[E, T]`**, **no mix** with `Seq[T]`, `Option[T]`, `Future[T]`...
* But it could be **only** `Seq[T]`, **only** `Option[T]`, **only** `Future[T]`...

---

# `for` Comprehension **Scopes**

```scala
val printRandomPoint: IO[Nothing, Point] = {
  for {
    x <- randomBetween(0, 10)            /*  x                */
    _ <- putStrLn(s"x=$x")               /*  O                */
    y <- randomBetween(0, 10)            /*  |    y           */
    _ <- putStrLn(s"y=$y")               /*  |    O           */
    point = Point(x, y)                  /*  O    O    point  */
    _ <- putStrLn(s"point.x=${point.x}") /*  |    |    O      */
    _ <- putStrLn(s"point.y=${point.y}") /*  |    |    O      */
  } yield point                          /*  |    |    O      */
}
```

---

# `for` Comprehension **Implicit Nesting**

```scala
val printRandomPoint: IO[Nothing, Point] = {
  for {
       x <- randomBetween(0, 10)
    /* | */ _ <- putStrLn(s"x=$x")
    /* |    | */ y <- randomBetween(0, 10)
    /* |    |    | */ _ <- putStrLn(s"y=$y")
    /* |    |    |    | */ point = Point(x, y)
    /* |    |    |    |    | */ _ <- putStrLn(s"point.x=${point.x}")
    /* |    |    |    |    |    | */ _ <- putStrLn(s"point.y=${point.y}")
  } /* |    |    |    |    |    |    | */ yield point
}
```
