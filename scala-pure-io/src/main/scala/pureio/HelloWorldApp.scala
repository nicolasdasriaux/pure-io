package pureio

import scalaz.zio.{DefaultRuntime, IO}

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
