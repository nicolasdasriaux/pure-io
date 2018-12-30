package pureio

import java.io.IOException

import scalaz.zio.console._
import scalaz.zio.{App, IO}

import scala.util.{Random, Try}

object PureIoApp extends App {
  def run(args: List[String]): IO[Nothing, ExitStatus] = {
    menu.attempt.map(_.fold(_ => 1, _ => 0)).map(ExitStatus.ExitNow(_))
  }

  def parseInt(s: String): Option[Int] = Try(s.toInt).toOption
  def randomInt: IO[Nothing, Int] = IO.sync(Random.nextInt(10) + 1)

  val menu: IO[IOException, Unit] = {
    {
      for {
        _ <- putStrLn("Menu")
        _ <- putStrLn("1) Hello World")
        _ <- putStrLn("2) Guess a Number")
        _ <- putStrLn("3) Countdown")
        _ <- putStrLn("4) Fibonacci")
        _ <- putStrLn("5) Quit")
        number <- getNumber(1, 5)

        exit <- number match {
          case 1 => helloWorld.const(false)
          case 2 => guessNumber.const(false)
          case 3 => countdown(50).const(false)
          case 4 => fibonacciCalculator.const(false)
          case 5 => IO.now(true)
        }
      } yield exit
    }.flatMap { exit =>
      if (exit) IO.unit else menu
    }
  }

  val helloWorld: IO[IOException, Unit] = {
    for {
      _ <- putStrLn("What's your name?")
      name <- getStrLn
      _ <- putStrLn(s"Hello $name!")
    } yield ()
  }

  val guessNumber: IO[IOException, Unit] = {
    for {
      number <- randomInt
      _ <- putStrLn("Guess a number between 1 and 10")
      _ <- guessLoop(number, 1)
    } yield ()
  }

  def guessLoop(number: Int, attempt: Int): IO[IOException, Unit] = {
    getNumber(1, 10).flatMap { guessedNumber =>
      if (guessedNumber == number) putStrLn(s"You guessed well after $attempt attempt(s)")
      else if (guessedNumber > number) putStrLn("It's to big") *> guessLoop(number, attempt + 1)
      else putStrLn("It's to small") *> guessLoop(number, attempt + 1)
    }
  }

  def getNumber(min: Int, max: Int): IO[IOException, Int] = {
    {
      for {
        _ <- putStrLn(s"Please enter a number between $min and $max")
        s <- getStrLn
      } yield parseInt(s)
    }.flatMap {
      case Some(number) if 1 <= number && number <= 10 => IO.now(number)
      case Some(number) => putStrLn(s"Number should be between $min and $max") *> getNumber(min, max)
      case None => putStrLn("Not a number") *> getNumber(min, max)
    }
  }

  def countdown(n: Int): IO[Nothing, Unit] = {
    {
      putStrLn(n.toString)
    }.flatMap { _ =>
      if (n == 0) IO.unit else countdown(n - 1)
    }
  }

  val fibonacciCalculator: IO[IOException, Unit] = {
    for {
      n <- getNumber(1, 10)
      result <- fibonacci(n)
      _ <- putStrLn(s"fibonacci($n) = $result")
    } yield ()
  }

  def fibonacci(n: BigInt): IO[Nothing, BigInt] = {
    if (n == 0) IO.now(1)
    else if (n == 1) IO.now(1)
    else
      for {
        fn1 <- fibonacci(n - 2).fork
        fn2 <- fibonacci(n - 1).fork
        n1 <- fn1.join
        n2 <- fn2.join
      } yield n1 + n2
  }
}
