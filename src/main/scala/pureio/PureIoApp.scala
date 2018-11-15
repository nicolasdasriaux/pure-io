package pureio

import java.io.IOException

import scalaz.zio.console._
import scalaz.zio.{App, IO}

object PureIoApp extends App {
  def run(args: List[String]): IO[Nothing, ExitStatus] = {
    helloWorld.attempt.map(_.fold(_ => 1, _ => 0)).map(ExitStatus.ExitNow(_))
  }

  def helloWorld: IO[IOException, Unit] = {
    for {
      _ <- putStrLn("What's your name?")
      name <- getStrLn
      _  <- putStrLn(s"Hello $name!")
    } yield ()
  }
}
