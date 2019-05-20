package zioapp

import scalaz.zio.{App, ZIO}
import scalaz.zio.console._

object ZioApp extends App {
  def run(args: List[String]): ZIO[ZioApp.Environment, Nothing, Int] = {
    val program = for {
      appConfig <- ConfigService.load()
      _ <- putStrLn(appConfig.toString)
    } yield ()

    program.either.fold(_ => 1, _ => 0)
  }
}
