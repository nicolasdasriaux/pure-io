
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
