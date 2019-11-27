package pureio.presentation

import java.io.IOException

import zio.console._
import zio.{App, ZIO}

class HelloYouApp extends App {
  def run(args: List[String]): ZIO[Console, Nothing, Int] = {
    helloWorld.either.map(_.fold(_ => 1, _ => 0))
  }

  def helloWorld: ZIO[Console, IOException, Unit] = {
    for  {
      _ <- putStrLn("What's you name?")
      name <- getStrLn
      _ <- putStrLn(s"Hello $name!")
    } yield ()
  }
}
