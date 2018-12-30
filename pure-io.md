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

# Combining `IO`s

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

# Wrapping Synchronous Side Effect

```scala

```

---

# Transforming `IO` (`map`)

```scala
def randomBetween(min: Int, max: Int): IO[Nothing, Int] =
  IO.sync(Random.nextInt(max - min) + min)
  
val randomLetter: IO[Nothing, Char] =
  randomBetween('A', 'Z').map { i /* Int */ =>
    i.toChar /* Char */
  }
```

---

# Sequencing `IO`s (`flatMap`)

```scala
def putStrLn(line: String): IO[Nothing, Unit] =
  IO.sync(Console.println(line))

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
val welcomeNewPlayer: IO[Nothing, Unit] =
  putStrLn("What's your name?").flatMap { _ /* Unit */ =>
    getStrLn.catchAll(_ => IO.point("")).flatMap { name /* String */ =>
      randomBetween(0, 20).flatMap { x /* Int */ =>
        randomBetween(0, 20).flatMap { y /* Int */ =>
          randomBetween(0, 20).flatMap { z /* Int */ =>
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
val welcomeNewPlayer: IO[Nothing, Unit] =
  for {
    _ <- putStrLn("What's your name?")
    name <- getStrLn.catchAll(_ => IO.point(""))
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
|------------|------------------------- |-------------------------|
| production | `IO[E, R]`               | `R`                     |

* Combines **only `DBIO[E, T]`**, **no mix** with `Option[T]`, `Future[T]`, `Seq[T]`...
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
