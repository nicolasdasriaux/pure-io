package pureio.kata.guessnumber

import zio._
import zio.clock.Clock
import zio.console.Console
import zio.random.Random

import scala.util._

object GuessNumberApp extends App {
  override def run(args: List[String]): ZIO[Random with Console, Nothing, ExitCode] = game.as(ExitCode.success)

  def randomNumber: ZIO[Random, Nothing, Int] =
    random.nextIntBetween(1, 20)

  def getGuess: ZIO[Console, Nothing, Int] =
    getIntBetween(1, 20).retry(Schedule.forever).provideSomeLayer[Console](Clock.live).orDie

  def game: ZIO[Console with Random, Nothing, Unit] = for {
    _ <- console.putStrLn("Guess a number between 1 and 20.")
    number <- randomNumber
    _ <- guessLoop(number, 1)
  } yield ()

  def guessLoop(number: Int, attempt: Int): ZIO[Console, Nothing, Unit] = for {
    _ <- console.putStr(s"Attempt $attempt > ")
    guess <- getGuess

    won <-
      if (guess > number)
        console.putStrLn("It's too large.").as(false)
      else if (guess < number)
        console.putStrLn("It's too small.").as(false)
      else
        console.putStrLn(s"You won after $attempt attempt(s).").as(true)

    _ <- if (won) ZIO.unit else guessLoop(number, attempt + 1)
  } yield ()

  def parseInt(s: String): Either[NumberFormatException, Int] =
    Try(s.toInt).toEither
      .left.map {
        case ex: NumberFormatException => ex
        case ex => throw ex
      }

  val getInt: ZIO[Console, NumberFormatException, Int] =
    console.getStrLn.orDie
      .flatMap(s => ZIO.fromEither(parseInt(s)))

  def getIntBetween(min: Int, max: Int): ZIO[Console, NumberFormatException, Int] =
    getInt.filterOrElse(n => min <= n && n <= max)(n => ZIO.fail(new NumberFormatException(s"Number not between $min and $max ($n)")))
}
