package pureio

import scalaz.zio.{IO, RTS}

object MinimalisticApp {
  object MinimalisticApp {
    // Wraps synchronous (blocking) side-effecting code in an IO
    val helloWorld: IO[Nothing, Unit] = IO.sync(Console.println("Hello World!"))
    // Nothing is printed after this line has run.
    // Somehow equivalent to IO.sync(() => Console.println("Hello World!"))
    // So the IO holds a lambda (() => Console.println("Hello World!")) but do not run it.

    // Creates a Runtime system as a single instance named RTS
    object RTS extends RTS

    def main(args: Array[String]): Unit = {
      // Run the IO with the RTS
      RTS.unsafeRun(helloWorld) // Comment this line and nothing will ever print
    }
  }
}
