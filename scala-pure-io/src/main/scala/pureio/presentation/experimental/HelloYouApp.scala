package pureio.presentation.experimental

import java.io.IOException
import zio.console._
import zio.{App, ExitCode, ZIO}

class HelloYouApp extends App {
  def run(args: List[String]): ZIO[Console, Nothing, ExitCode] = {
    helloWorld.exitCode
  }

  def helloWorld: ZIO[Console, IOException, Unit] = {
    for  {
      _ <- putStrLn("What's you name?")
      name <- getStrLn
      _ <- putStrLn(s"Hello $name!")
    } yield ()
  }
}
