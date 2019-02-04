package pureio

import scalaz.zio.console._
import scalaz.zio.duration._
import scalaz.zio._


object FiberApp extends App {
  override def run(args: List[String]): IO[Nothing, ExitStatus] =
    program.attempt.map(_.fold(_ => 1, _ => 0)).map(ExitStatus.ExitNow)

  def program: IO[Nothing, Unit] = {
    val a =
      putStrLn("A ")
        .repeat(Schedule.recurs(3) && Schedule.spaced(1.second).jittered)
        .void
        .delay(4.seconds)

    val b =
      putStrLn(" B")
        .repeat(Schedule.recurs(30) && Schedule.fibonacci(100.millis))
        .void

    val ticker =
      putStrLn(".")
        .delay(500.milliseconds)
        .forever
        .onTermination(_ => putStrLn("Done"))

    def offer(queue: Queue[Int]) =
      IO.unit
        .repeat(
          (Schedule.spaced(500.millis) *> Schedule.recurs(5))
            .logOutput(i => queue.offer(i).void)
            andThen Schedule.succeedLazy(0).logOutput(i => queue.offer(i).void)
        )

    def take(queue: Queue[Int]) =
      queue.take
        .flatMap(v => putStrLn(s"v=$v").const(v))
        .repeat(Schedule.doUntil(_ == 0))

    for {
      queue <- Queue.bounded[Int](5)
      promise <- Promise.make[Nothing, Int]
      _ <- take(queue).fork
      _ <- IO.succeed(10).delay(5.seconds).to(promise).fork
      _ <- offer(queue).fork
      _ <- putStrLn("Waiting")
      value <- ticker.race(promise.await)
      _ <- putStrLn(s"value=$value")
    } yield ()
  }
}
