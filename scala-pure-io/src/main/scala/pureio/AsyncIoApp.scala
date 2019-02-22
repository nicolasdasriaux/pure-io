package pureio

import java.io.IOException
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.{Executors, ScheduledExecutorService, ScheduledFuture, TimeUnit}

import scalaz.zio.Exit.Cause.Interrupt
import scalaz.zio._
import scalaz.zio.console.putStrLn
import scalaz.zio.duration._

case class StopError(message: String) extends Exception

object AsyncIoApp extends App {
  def run(args: List[String]): IO[Nothing, AsyncIoApp.ExitStatus] = {
    async.attempt.map(_.fold(_ => 1, _ => 0)).map(ExitStatus.ExitNow(_))
  }

  val async: IO[IOException, Unit] = {
    val id = 1
    val ids = 1 to 10
    val getNames = IO.foreachPar(ids) { id => NameService.getName(id) }

    for {
      nameFiber <- NameService.getName(id).fork
      _ <- nameFiber.interrupt.delay(3.second).fork
      name <- nameFiber.await.flatMap(exitResult => IO.done(exitResult.fold({ case Interrupt => Exit.succeed(None); case cause => Exit.halt(cause) }, name => Exit.succeed(Some(name)))))
      _ <- putStrLn(s"Name for $id is $name")

      _ <- File.printAllLines(Paths.get("/Users/axa/Development/presentations/pure-io/build.sbt")).race(IO.sleep(5.seconds))

      namesFiber <- getNames.fork
      _ <- namesFiber.interrupt.delay(2.seconds).fork
      names <- namesFiber.await.flatMap(exitResult => IO.done(exitResult.fold({ case Interrupt => Exit.succeed(List.empty); case cause => Exit.halt(cause) }, names => Exit.succeed(names))))
      _ <- putStrLn(s"names=$names")
    } yield ()
  }
}

object NameService {
  private lazy val executorService: ScheduledExecutorService = Executors.newScheduledThreadPool(5)

  def getName(id: Int): IO[Nothing, String] = {
    val everySecond = Schedule.fixed(1.second).jittered
    val ticks = Schedule.forever.map(tick => s"Tick #$tick for $id")
    val log = Schedule.logInput[String](putStrLn)
    val ticker = IO.unit.repeat(ticks >>> log <* everySecond)

    val task: IO[Nothing, String] =
      putStrLn(s"Started $id") *>
        IO.sleep(4.seconds) *>
        putStrLn(s"Succeeded $id") *>
        IO.succeedLazy(s"Name $id")

    task.sandbox
    task.onTermination(_ => putStrLn(s"Terminated $id")).race(ticker)
  }

  def getName2(id: Int): IO[Nothing, String] = IO.asyncInterrupt[Nothing, String] { (callback: IO[Nothing, String] => Unit) =>
    println(s"Running $id")

    val notifyCompletion: Runnable = { () =>
      println(s"Completing $id")
      callback(IO.succeedLazy(s"Name $id"))
    }

    val eventualResult: ScheduledFuture[_] = executorService.schedule(notifyCompletion, 5, TimeUnit.SECONDS)

    val canceler: Canceler = IO.sync {
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
      IO.syncCatch(Files.newBufferedReader(path)) {
        case e: IOException => e
      }

    def close(bufferedReader: BufferedReader): IO[IOException, Unit] =
      IO.syncCatch(bufferedReader.close()) {
        case e: IOException => e
      }

    def readLine(bufferedReader: BufferedReader): IO[IOException, Option[String]] =
      IO.syncCatch(Option(bufferedReader.readLine())) {
        case e: IOException => e
      }
  }

  def printAllLines(path: Path): IO[IOException, List[String]] = {
    val collectLines = Schedule.collect[Option[String]].map(_.flatten)
    val everySecondJittered = Schedule.fixed(500.millis).jittered
    val untilEof = Schedule.doUntil[Option[String]](_.isEmpty)

    BufferedReader.open(path)
      .bracket(bufferedReader => BufferedReader.close(bufferedReader).catchAll(_ => IO.unit)) { reader =>
        {
          for {
            maybeLine <- BufferedReader.readLine(reader)
            _ <- maybeLine.fold(IO.unit)(putStrLn)
          } yield maybeLine
        }.repeat(collectLines <* everySecondJittered <* untilEof)
      }
  }
}

