package pureio

import scalaz.zio.console._
import scalaz.zio.{IO, RTS}

import scala.util.Random

case class Point(x: Int, y: Int)

object RTS extends RTS

object TestApp {
  def main(args: Array[String]): Unit = {
    RTS.unsafeRun(printRandomXYPoint)
  }

  def program: IO[Nothing, Unit] = {
    val randomBetween1To25 = randomBetween(1, 25)

    for {
      r1 <- randomBetween1To25
      r2 <- randomBetween1To25
      _ <-  putStr(s"${r1 + r2} = $r1 + $r2")
    } yield ()
  }

  def randomBetween(min: Int, max: Int): IO[Nothing, Int] = IO.sync(Random.nextInt(max - min) + min)

  def randomLetter: IO[Nothing, Char] = randomBetween('A', 'Z').map(_.toChar)
  def printRandomNumber: IO[Nothing, Unit] = randomBetween(1, 6).flatMap(dice => putStrLn(s"Dice shows $dice"))

  def randomPoint: IO[Nothing, Point] =
    randomBetween(0, 20).flatMap { x =>
      randomBetween(0, 20).map { y =>
        Point(x, y)
      }
    }

  def printRandomPoint: IO[Nothing, Unit] =
    randomBetween(0, 20).flatMap { x =>
      randomBetween(0, 20).flatMap { y =>
        val point = Point(x, y)
        putStrLn(s"point=$point")
      }
    }

  def randomPointWithForComprehension: IO[Nothing, Point] = {
    for {
      x /* Int */ <- randomBetween(0, 20) /* IO[Nothing, Int] */
      y /* Int */ <- randomBetween(0, 20) /* IO[Nothing, Int] */
    } yield Point(x, y) /* Point */
  } /* IO[Nothing, Point] */

  def printRandomPointWithForComprehension: IO[Nothing, Unit] = {
    for {
      x     /* Int   */ <- randomBetween(0, 10)      /* IO[Nothing, Int]  */
      y     /* Int   */ <- randomBetween(0, 10)      /* IO[Nothing, Int]  */
      point /* Point */ =  Point(x, y)               /* Point             */
      _     /* Unit  */ <- putStrLn(s"point=$point") /* IO[Nothing, Unit] */
    } yield () /* Unit */
  } /* IO[Nothing, Unit] */

  def printRandomXYPoint: IO[Nothing, Unit] =
    for {
      x     <- randomBetween(0, 10)
      _     <- putStrLn(s"x=$x")
      y     <- randomBetween(0, 10)
      _     <- putStrLn(s"y=$y")
      point =  Point(x, y)
      _     <- putStrLn(s"point=$point")
    } yield ()
}
