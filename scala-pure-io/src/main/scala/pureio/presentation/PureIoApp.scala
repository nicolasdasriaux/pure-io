package pureio.presentation

import java.io.IOException

import zio.console.{Console, getStrLn, putStrLn}
import zio.{App, IO, ZIO}

import scala.util.{Random, Try}

object PureIoApp extends App {
  def run(args: List[String]): ZIO[Console, Nothing, Int] = {
    menu.either.map(_.fold(_ => 1, _ => 0))
  }

  val displayMenu: ZIO[Console, Nothing, Unit] =
    putStrLn("Menu") *>
      putStrLn("1) Hello World") *>
      putStrLn("2) Guess a Number") *>
      putStrLn("3) Countdown") *>
      putStrLn("4) Quit")

  val getChoice: ZIO[Console, IOException, Int] =
    getNumber(1, 5)

  def launchMenuItem(choice: Int): ZIO[Console, IOException, Boolean] = choice match {
    case 1 => helloApp.as(false)
    case 2 => guessNumberApp.as(false)
    case 3 => countDownApp.as(false)
    case 4 => IO.succeed(true)
  }

  val menu: ZIO[Console, IOException, Unit] = {
    {
      for {
        _ <- displayMenu
        choice <- getChoice
        exit <- launchMenuItem(choice)
      } yield exit
    }.flatMap { exit =>
      if (exit) IO.unit else menu
    }
  }

  val helloApp: ZIO[Console, IOException, Unit] = {
    for {
      _ <- putStrLn("What's your name?")
      name <- getStrLn
      _ <- putStrLn(s"Hello $name!")
    } yield ()
  }

  val guessNumberApp: ZIO[Console, IOException, Unit] = {
    for {
      number <- randomInt
      _ <- putStrLn("Guess a number between 1 and 10")
      _ <- guessLoop(number, 1)
    } yield ()
  }

  def guessLoop(number: Int, attempt: Int): ZIO[Console, IOException, Unit] = {
    getNumber(1, 10).flatMap { guessedNumber =>
      if (guessedNumber == number)
        putStrLn(s"You guessed well after $attempt attempt(s)")
      else if (guessedNumber > number)
        putStrLn("It's to big") *> guessLoop(number, attempt + 1)
      else
        putStrLn("It's to small") *> guessLoop(number, attempt + 1)
    }
  }

  val countDownApp: ZIO[Console, IOException, Unit] =
    for {
      n <- getNumber(1, 10000)
      _ <- countdown(n)
    } yield ()

  def countdown(n: Int): ZIO[Console, Nothing, Unit] = {
    if (n == 0)
      putStrLn("BOOM!!!")
    else
      putStrLn(n.toString) *> countdown(n - 1)
  }

  def parseInt(s: String): Option[Int] = Try(s.toInt).toOption

  def getNumber(min: Int, max: Int): ZIO[Console, IOException, Int] = {
    {
      for {
        _ <- putStrLn(s"Please enter a number between $min and $max")
        s <- getStrLn
      } yield parseInt(s)
    }.flatMap {
      case Some(number) if min <= number && number <= max => IO.succeed(number)
      case Some(_) => putStrLn(s"Number should be between $min and $max") *> getNumber(min, max)
      case None => putStrLn("Not a number") *> getNumber(min, max)
    }
  }

  def randomInt: IO[Nothing, Int] = IO.effectTotal(Random.nextInt(10) + 1)
}
