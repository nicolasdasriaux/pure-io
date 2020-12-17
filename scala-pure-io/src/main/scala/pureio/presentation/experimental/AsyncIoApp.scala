package pureio.presentation.experimental

import java.io.IOException
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.{Executors, ScheduledExecutorService, ScheduledFuture, TimeUnit}

import zio.Cause.Interrupt
import zio._
import zio.clock.Clock
import zio.console._
import zio.duration._
import zio.random.Random

case class StopError(message: String) extends Exception

object AsyncIoApp extends App {
  def run(args: List[String]): ZIO[Console with Clock with Random, Nothing, ExitCode] = {
    async.exitCode
  }

  val async: ZIO[Console with Clock with Random, IOException, Unit] = {
    val id = 1
    val ids = 1 to 10
    val getNames = ZIO.foreachPar(ids) { id => NameService.getName(id) }

    for {
      nameFiber <- NameService.getName(id).fork
      _ <- nameFiber.interrupt.delay(3.second).fork
      name <- nameFiber.await.flatMap(exitResult => ZIO.done(exitResult.fold({ case Interrupt(_) => Exit.succeed(None); case cause => Exit.halt(cause) }, name => Exit.succeed(Some(name)))))
      _ <- putStrLn(s"Name for $id is $name")

      _ <- File.printAllLines(Paths.get("/Users/nicolasdasriaux/Development/presentations/pure-io/scala-pure-io/build.sbt")).race(ZIO.sleep(5.seconds))

      namesFiber <- getNames.fork
      _ <- namesFiber.interrupt.delay(2.seconds).fork
      names <- namesFiber.await.flatMap(exitResult => IO.done(exitResult.fold({ case Interrupt(_) => Exit.succeed(List.empty); case cause => Exit.halt(cause) }, names => Exit.succeed(names))))
      _ <- putStrLn(s"names=$names")
    } yield ()
  }
}

object NameService {
  private lazy val executorService: ScheduledExecutorService = Executors.newScheduledThreadPool(5)

  def getName(id: Int): ZIO[Console with Clock with Random, Nothing, String] = {
    val everySecond = Schedule.spaced(1.second)
    val ticks = Schedule.forever.map(tick => s"Tick #$tick for $id")
    val log = Schedule.identity[String].tapInput[Console, String](line => putStrLn(line))
    val ticker = IO.unit.repeat(ticks >>> log <* everySecond)

    val task: ZIO[Console with Clock, Nothing, String] =
      putStrLn(s"Started $id") *>
        ZIO.sleep(4.seconds) *>
        putStrLn(s"Succeeded $id") *>
        IO.effectTotal(s"Name $id")

    task
      // .onTermination(_ => putStrLn(s"Terminated $id"))
      .race(ticker)
  }

  def getName2(id: Int): IO[Nothing, String] = IO.effectAsyncInterrupt { (callback: IO[Nothing, String] => Unit) =>
    println(s"Running $id")

    val notifyCompletion: Runnable = { () =>
      println(s"Completing $id")
      callback(IO.effectTotal(s"Name $id"))
    }

    val eventualResult: ScheduledFuture[_] = executorService.schedule(notifyCompletion, 5, TimeUnit.SECONDS)

    val canceler: Canceler[Any] = IO.effectTotal {
      println(s"Cancelling $id")
      eventualResult.cancel(false)
    }

    Left(canceler)
  }
}

object File {
  type BufferedReader = java.io.BufferedReader

  object BufferedReader {
    def open(path: Path): IO[IOException, BufferedReader] =
      IO.effect(Files.newBufferedReader(path)).refineOrDie {
        case e: IOException => e
      }

    def close(bufferedReader: BufferedReader): IO[IOException, Unit] =
      IO.effect(bufferedReader.close()).refineOrDie {
        case e: IOException => e
      }

    def readLine(bufferedReader: BufferedReader): IO[IOException, Option[String]] =
      IO.effect(Option(bufferedReader.readLine())).refineOrDie {
        case e: IOException => e
      }
  }

  def printAllLines(path: Path): ZIO[Console with Clock with Random, IOException, List[String]] = {
    val collectLines = Schedule.collectAll[Option[String]].map(_.flatMap(_.toSeq).toList)
    val everySecondJittered = Schedule.spaced(500.millis)
    val untilEof = Schedule.identity[Option[String]].untilOutput(_.isEmpty)

    val value: Schedule[Clock, Option[String], List[String]] = collectLines <* everySecondJittered <* untilEof

    BufferedReader.open(path)
      .bracket(bufferedReader => BufferedReader.close(bufferedReader).catchAll(_ => IO.unit)) { reader =>
        {
          for {
            maybeLine <- BufferedReader.readLine(reader)
            _ <- maybeLine.fold(ZIO.unit: ZIO[Console, Nothing, Unit])(line => putStrLn(line))
          } yield maybeLine
        }.repeat(value)
      }
  }
}

