package pureio.kata.guessnumber

import java.io.IOException

import zio._
import zio.console.Console
import zio.random.Random

import scala.util._

object GuessNumberApp extends App {
  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = game.fold(_ => 1, _ => 0)

  def parseInt(s: String): Either[NumberFormatException, Int] = Try(s.toInt).toEither.left.map({ case ex: NumberFormatException => ex})

  val getInt: ZIO[Console, NumberFormatException, Int] =
    console.getStrLn.orDie
      .flatMap(s => ZIO.fromEither(parseInt(s)))

  def getIntBetween(min: Int, max: Int): ZIO[Console, NumberFormatException, Int] =
    getInt.filterOrElse(n => min <= n && n <= max)(n => ZIO.fail(new NumberFormatException(s"Number not between $min and $max ($n)")))

  def game: ZIO[Console with Random, IOException, Unit] = for {
    _ <- console.putStrLn("Guess a number between 1 and 20.")
    number <- generateRandomNumber
    _ <- guessLoop(number, 1)
  } yield ()

  def guessLoop(number: Int, attempt: Int): ZIO[Console, IOException, Unit] = for {
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

  def generateRandomNumber: ZIO[Random, Nothing, Int] = random.nextInt(20).map(_ + 1)

  def getGuess: ZIO[Console, Nothing, Int] =
    getIntBetween(1, 20).retry(Schedule.forever).orDie
}
