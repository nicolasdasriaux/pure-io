package pureio.presentation

import zio.{App, IO}

object HelloWorldApp extends App {
  // Wraps synchronous (blocking) side-effecting code in an IO
  val helloWorld: IO[Nothing, Unit] =
    IO.effectTotal(/* () => */ Console.println("Hello World!"))
    // The IO just holds a lambda but does not run it for now.

  def run(args: List[String]): IO[Nothing, Int] = {
    helloWorld.as(0)
  }
}
