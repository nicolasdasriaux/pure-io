autoscale: true
footer: Purely Fonctional IO
slidenumbers: true

# [fit] **Purely Fonctional IO**
## in _Scala_ and _ZIO_

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
  val helloWorld: IO[Nothing, Unit] = IO.sync(Console.println("Hello World!"))
  // Nothing is printed after this line has run.
  // Somehow equivalent to IO.sync(() => Console.println("Hello World!"))
  // So the IO holds a lambda (() => Console.println("Hello World!")) but do not run it.

  // Creates a Runtime system as a single instance named RTS
  object RTS extends RTS

  def main(args: Array[String]): Unit = {
    // Run the IO with the RTS. Prints "Hello World!".
    RTS.unsafeRun(helloWorld) // Comment this line and nothing will ever print
  }
}
```

---

# Basic `IO`s

```scala
val success: IO[Nothing, Int] = IO.point(42)
// Will never fail
// Will always succeed with result 42

val failure: IO[String, Nothing] = IO.fail("Failed")
// Will always fail with error "Failed"
// will never succeed

val exceptionFailure: IO[IllegalStateException, Nothing] =
  IO.fail(new IllegalStateException("Failure"))
// Error can be any type
// Error can then be an exception (but just as a value, never thrown!)
```

---

# Wrapping in `IO`

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

# Synchronous, Non Exception-Throwing

```scala
def randomBetween(min: Int, max: Int): IO[Nothing, Int] =
  // Side-effecting code updates the state of a random generator,
  // and returns a random number (Int).
  // It can never fail (Nothing).
  IO.sync(Random.nextInt(max - min) + min)

def putStrLn(line: String): IO[Nothing, Unit] =
  // Side-defecting code prints a line,
  // and returns void (Unit).
  // It can never fail (Nothing).
  IO.sync(scala.Console.println(line))
```

---

# Synchronous, Exception-Throwing

```scala
def getStrLn: IO[IOException, String] = {
  // Side-effecting code reads from keyboard until a line is available,
  // and returns the line (String).
  // It might throw an IOException. IO catches exception,
  // and translates it into a failure containing the error (IOException).
  // IOException is neutralized, it is NOT propagated but just used as a value.
  IO.syncCatch(scala.io.StdIn.readLine()) {
    case e: IOException => e
  }
}
```

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

# Sequencing `IO`s (`flatMap`)

```scala
val printRolledDiceWRONG: IO[Nothing, IO[Nothing, Unit]] = // Oops! Wrong type!
  randomBetween(1, 6).map { dice /* Int */ =>
    putStrLn(s"Dice shows $dice") /* IO[Nothing, Unit] */
  }

val printRolledDice: IO[Nothing, Unit] =
  randomBetween(1, 6).flatMap { dice /* Int */ =>
    putStrLn(s"Dice shows $dice") /* IO[Nothing, Unit] */
  }
```

---

# Too Much Nesting

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

# Flatten Them All!

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

# Anatomy of `for` Comprehension

---

# `for` Comprehension **Types**

```scala
val printRandomPoint: IO[Nothing, Unit] = {
  for {
    x     /* Int   */ <- randomBetween(0, 10)            /* IO[Nothing, Int]  */
    _     /* Unit  */ <- putStrLn(s"x=$x")               /* IO[Nothing, Unit] */
    y     /* Int   */ <- randomBetween(0, 10)            /* IO[Nothing, Int]  */
    _     /* Unit  */ <- putStrLn(s"y=$y")               /* IO[Nothing, Unit] */
    point /* Point */ =  Point(x, y)                     /* Point             */
    _     /* Unit  */ <- putStrLn(s"point.x=${point.x}") /* IO[Nothing, Unit] */
    _     /* Unit  */ <- putStrLn(s"point.y=${point.y}") /* IO[Nothing, Unit] */
  } yield () /* Unit */
} /* IO[Nothing, Unit] */
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

* Combines **only `IO[E, T]`**, **no mix** with `Option[T]`, `Future[T]`, `Seq[T]`...
* But it could be **only** `Option[T]`, or **only** `Future[T]`, or **only** `Seq[T]`...

---

# `for` Comprehension **Scopes**

```scala
val printRandomPoint: IO[Nothing, Unit] = {
  for {
    x <- randomBetween(0, 10)            /*  x                */
    _ <- putStrLn(s"x=$x")               /*  O                */
    y <- randomBetween(0, 10)            /*  |    y           */
    _ <- putStrLn(s"y=$y")               /*  |    O           */
    point = Point(x, y)                  /*  O    O    point  */
    _ <- putStrLn(s"point.x=${point.x}") /*  |    |    O      */
    _ <- putStrLn(s"point.y=${point.y}") /*  |    |    O      */
  } yield ()                             /*  |    |    |      */
}
```

---

# `for` Comprehension **Implicit Nesting**

```scala
val printRandomPoint: IO[Nothing, Unit] = {
  for {
       x <- randomBetween(0, 10)
    /* | */ _ <- putStrLn(s"x=$x")
    /* |    | */ y <- randomBetween(0, 10)
    /* |    |    | */ _ <- putStrLn(s"y=$y")
    /* |    |    |    | */ point = Point(x, y)
    /* |    |    |    |    | */ _ <- putStrLn(s"point.x=${point.x}")
    /* |    |    |    |    |    | */ _ <- putStrLn(s"point.y=${point.y}")
  } /* |    |    |    |    |    |    | */ yield ()
}
```
