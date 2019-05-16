
# References

* [test](https://github.com/politrons/reactiveScala/blob/master/scala_features/src/main/scala/app/impl/scalaz/zio/ZIOMonad.scala)
* [ZIO & Cats Effect: A Match Made in Heaven](http://degoes.net/articles/zio-cats-effect)
* [ZIO Cheat Sheet](https://github.com/ghostdogpr/zio-cheatsheet)
* [ZIO todo Backend](https://github.com/mschuwalow/zio-todo-backend)


* [PureConfig](https://github.com/pureconfig/pureconfig)

https://www.youtube.com/watch?v=mGxcaQs3JWI&t=249s


---

# Exceptions are Impure

---

# Mutability is Impure

---

# Traditional IOs are Impure

---

Pure Values — IO.point
Synchronous Effects — IO.sync
Asynchronous Effects — IO.async
Concurrent Effects — io.fork
Resource Effects — io.bracket
The concurrency model is based on fibers, a user-land lightweight thread, which permit cooperative multitasking, fine-grained interruption, and very high performance with large numbers of concurrently executing fibers.

IO values compose with other IO values in a variety of ways to build complex, rich, interactive applications. See the methods on IO for more details about how to compose IO values.

In order to integrate with Scala, IO values must be interpreted into the Scala runtime. This process of interpretation executes the effects described by a given immutable IO value. For more information on interpreting IO values, see the default interpreter in RTS or the safe main function in App.

---

# Hello You

```scala
class HelloYouApp extends App {
  def run(args: List[String]): IO[Nothing, ExitStatus] = {
    helloWorld.attempt.map(_.fold(_ => 1, _ => 0)).map(ExitStatus.ExitNow(_))
  }

  def helloWorld: IO[IOException, Unit] = {
    for  {
      _ <- putStrLn("What's you name?")
      name <- getStrLn
      _ <- putStrLn(s"Hello $name!")
    } yield ()
  }
}
```

---

# `IO` is Pure

* `IO` values are **pure** :innocent:.
* Can be **combined** to form complex programs
* Can be **inlined** or **extracted** without changing the meaning of the code
* A full **program** can be represented as a **single `IO` value**
* Can eventually be run in the `main` method
* Only **impure** :imp: point of the code  

---

# Hello `IO`

```scala
object HelloWorldApp {
  // Wraps synchronous (blocking) side-effecting code in an IO
  val helloWorld: IO[Nothing, Unit] = IO.effectTotal(/* () => */ Console.println("Hello World!"))
  // The IO just holds a lambda but does not run it.

  // Creates a Runtime system as a single instance named RTS
  object RTS extends DefaultRuntime

  def main(args: Array[String]): Unit = {
    // Run the IO with the RTS. Prints "Hello World!".
    val program = helloWorld
    RTS.unsafeRun(program) // Comment this line and nothing will ever print
  }
}
```

--- 

# Wrapping Side-Effecting Code in `IO`

* Wrap a **synchronous** (blocking) side-effecting code
  - When **non exception-throwing**, use `IO.sync`
  - When **exception-throwing**, use `IO.syncCatch`, `syncThrowable`, `syncException`
  - Catches exceptions and wraps them with `IO.fail`
* Wrap an **asynchronous** (non-blocking) side-effecting code
  * When **uninterruptible**, use `IO.async`
  * When **interruptible** , use `IO.asyncInterrupt`
* Can then combine all kinds of `IO`s seamlessly
---

# Asynchronous, Uninterruptible

```scala
object Calculator {
  private lazy val executor = Executors.newScheduledThreadPool(5)

  def add(a: Int, b: Int): IO[Nothing, Int] = {
    IO.async { (callback: IO[Nothing, Int] => Unit) =>
      val completion: Runnable = { () => callback(IO.point(a + b)) }
      executor.schedule(completion, 5, TimeUnit.SECONDS)
    }
  }
}
```

---

# Asynchronous, Interruptible

```scala
object Calculator {
  private lazy val executor = Executors.newScheduledThreadPool(5)

  def add(a: Int, b: Int): IO[Nothing, Int] = {
    IO.asyncInterrupt { (callback: IO[Nothing, Int] => Unit) =>
      val complete: Runnable = { () => callback(IO.point(a + b)) }
      val eventualResult = executor.schedule(complete, 5, TimeUnit.SECONDS)
      val canceler: Canceler = IO.sync(eventualResult.cancel(false))
      Left(canceler)
    }
  }
}
 ```

---

# Console Program

```java
interface Program<A> { /* ... */ }
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

* Cannot use `Void`
* Cannot create instances (`private` constructor :worried:)
* Can just use `null` :imp:

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
