
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
