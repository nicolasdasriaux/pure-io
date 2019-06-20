package pureio {
  import pureio.sync.Main._
  import scalaz.zio._
  import scalaz.zio.duration._

  object RTS extends DefaultRuntime
  case class Point(x: Int, y: Int)

  package basic {
    import java.io.FileNotFoundException

    object Main {
      val success: IO[Nothing, Int] = IO.succeed(42)
      val successLazy: IO[Nothing, Int] = IO.succeedLazy(/* () => */ 40 + 2)

      // Will never fail (Nothing)
      // Will always succeed with result 42 (Int)

      val failure: IO[String, Nothing] = IO.fail("Failed")
      // Will always fail with error "Failed" (String)
      // will never succeed (Nothing)

      val exceptionFailure: IO[FileNotFoundException, Nothing] =
        IO.fail(new FileNotFoundException("Expected"))
      // Error can be an exception (but just as a value, never thrown!)

      val death: IO[Nothing, Nothing] =
        IO.die(new IndexOutOfBoundsException("Unexpected"))
      // Will never fail (Nothing)
      // Will never succeed (Nothing)
      // Will always die with IndexOutOfBoundsException error (not reflected in type)
      // Error can only be an exception (but just as a value, never thrown!)

      val deathMessage: IO[Nothing, Nothing] =
        IO.dieMessage("Unexpected")
      // When just in need of a RuntimeException with a message
   }
  }

  package sync {
    import java.io.IOException

    import scala.io.StdIn
    import scala.util.Random

    object Main {

      def randomBetween(min: Int, max: Int): IO[Nothing, Int] = {
        // Side-effecting code updates the state of a random generator,
        // and returns a random number (Int).
        // It can never fail (Nothing).
        IO.effectTotal(/* () => */ Random.nextInt(max - min) + min)
      }

      def putStrLn(line: String): IO[Nothing, Unit] = {
        // Side-effecting code prints a line,
        // and returns void (Unit).
        // It can never fail (Nothing).
        IO.effectTotal(/* () => */ Console.println(line))
      }

      def getStrLn: IO[IOException, String] = {
        // Side-effecting code reads from keyboard until a line is available,
        // and returns the line (String).

        // In case an IOException is thrown, catch it
        // and fail with the exception (not thrown)
        // or die in case of any other exception.
        IO.effect(/* () => */ StdIn.readLine()) /* IO[Throwable, String] */
          .refineOrDie {
            case e: IOException => e
          }
      }
    }
  }

  package async {
    import java.util.concurrent.{Executors, TimeUnit}

    package non_interruptible {
      object Main {
        private lazy val executor = Executors.newScheduledThreadPool(5)

        def addAsync(a: Int, b: Int, onSuccess: Int => Unit, onFailure: String => Unit): Unit = {
          val completion: Runnable = if (a > 0 && b > 0) { () => onSuccess(a + b) } else { () => onFailure("Negative a or b")}
          executor.schedule(completion, 5, TimeUnit.SECONDS)
        }

        def add(a: Int, b: Int): IO[String, Int] = {
          IO.effectAsync { (notify: IO[String, Int] => Unit) =>
            addAsync(a, b,
              result => notify(IO.succeed(result)),
              error => notify(IO.fail(error))
            )
          }
        }

        def main(args: Array[String]): Unit = {
          println(RTS.unsafeRun(add(5, 7)))
        }
      }
    }

    package interruptible {
      object Main {
        private lazy val executor = Executors.newScheduledThreadPool(5)

        def addAsync(a: Int, b: Int, onSuccess: Int => Unit, onFailure: String => Unit): () => Unit = {
          val completion: Runnable = if (a > 0 && b > 0) { () => onSuccess(a + b) } else { () => onFailure("Negative a or b")}
          val eventualResult = executor.schedule(completion, 5, TimeUnit.SECONDS)

          () => eventualResult.cancel(false)
        }

        def add(a: Int, b: Int): IO[String, Int] = {
          IO.effectAsyncInterrupt { (notify: IO[String, Int] => Unit) =>
            val canceler = addAsync(a, b,
              result => notify(IO.succeed(result)),
              error => notify(IO.fail(error))
            )

            Left(IO.effectTotal(/* () => */ canceler()))
          }
        }

        def main(args: Array[String]): Unit = {
          val program = for {
            resultFiber <- add(5, 7).fork
            _ <- resultFiber.interrupt.delay(7.second).fork
            result <- resultFiber.join
          } yield result

          println(RTS.unsafeRun(program))
        }
      }
    }

    package future {
      import scala.concurrent._

      object Main {
        def addAsync(a: Int, b: Int)(implicit ec: ExecutionContext): Future[Int] = {
          Future.successful(a + b)
        }

        def add(a: Int, b: Int): IO[Throwable, Int] = {
          IO.fromFuture { implicit ec =>
            addAsync(a, b) /* (ec) */
          }
        }

        def main(args: Array[String]): Unit = {
          println(RTS.unsafeRun(add(5, 7)))
        }
      }
    }
  }

  package map_flatmap {
    package map {
      object Main {
        val randomLetter: IO[Nothing, Char] =
          randomBetween('A', 'Z') /* IO[Nothing, Int] */
            .map { i /* Int */ =>
              i.toChar /* Char */
            }

        def main(args: Array[String]): Unit = {
          println(RTS.unsafeRun(randomLetter))
        }
      }
    }

    package flatmap {
      object Main {
        val printRolledDiceWRONG: IO[Nothing, IO[Nothing, Unit]] =
          randomBetween(1, 6) /* IO[Nothing, Int] */
            .map { dice /* Int */ =>
              putStrLn(s"Dice shows $dice") /* IO[Nothing, Unit] */
            }

        val printRolledDice: IO[Nothing, Unit] =
          randomBetween(1, 6) /* IO[Nothing, Int] */
            .flatMap { dice /* Int */ =>
              putStrLn(s"Dice shows $dice") /* IO[Nothing, Unit] */
            }

        def main(args: Array[String]): Unit = {
          RTS.unsafeRun(printRolledDice)
        }
      }
    }

    package both {
      package map_flatmap {
        object Main {
          val randomPoint: IO[Nothing, Point] =
            randomBetween(0, 20).flatMap { x =>
              randomBetween(0, 20).map { y =>
                Point(x, y)
              }
            }

          def main(args: Array[String]): Unit = {
            RTS.unsafeRun(randomPoint)
          }
        }
      }

      package for_comprehension {
        object Main {
          val randomPoint: IO[Nothing, Point] =
            for {
              x <- randomBetween(0, 20)
              y <- randomBetween(0, 20)
            } yield Point(x, y)

          def main(args: Array[String]): Unit = {
            RTS.unsafeRun(randomPoint)
          }
        }
      }
    }

    package intermediary_variable {
      package map_flatmap {
        object Main {
          val printRandomPoint: IO[Nothing, Unit] =
            randomBetween(0, 20).flatMap { x =>
              randomBetween(0, 20).flatMap { y =>
                val point = Point(x, y)
                putStrLn(s"point=$point")

              }
            }

          def main(args: Array[String]): Unit = {
            RTS.unsafeRun(printRandomPoint)
          }
        }
      }

      package for_comprehension {
        object Main {
          val printRandomPoint: IO[Nothing, Unit] =
            for {
              x <- randomBetween(0, 20)
              y <- randomBetween(0, 20)
              point = Point(x, y) // Not running an IO, '=' instead of '<-'
              _ <- putStrLn(s"point=$point")
            } yield ()

          def main(args: Array[String]): Unit = {
            RTS.unsafeRun(printRandomPoint)
          }
        }
      }
    }

    package too_many_maps_and_flatmaps {
      package map_flatmap {
        object Main {
          import java.io.IOException

          val welcomeNewPlayer: IO[IOException, Unit] =
            putStrLn("What's your name?").flatMap { _ =>
              getStrLn.flatMap { name =>
                randomBetween(0, 20).flatMap { x =>
                  randomBetween(0, 20).flatMap { y =>
                    randomBetween(0, 20).flatMap { z =>
                      putStrLn(s"Welcome $name, you start at coordinates($x, $y, $z).")
                    }
                  }
                }
              }
            }

          def main(args: Array[String]): Unit = {
            RTS.unsafeRun(welcomeNewPlayer)
          }
        }
      }

      package for_comprehension {
        import java.io.IOException

        object Main {
          val welcomeNewPlayer: IO[IOException, Unit] =
            for {
              _ <- putStrLn("What's your name?")
              name <- getStrLn
              x <- randomBetween(0, 20)
              y <- randomBetween(0, 20)
              z <- randomBetween(0, 20)
              _ <- putStrLn(s"Welcome $name, you start at coordinates($x, $y, $z).")
            } yield ()

          def main(args: Array[String]): Unit = {
            RTS.unsafeRun(welcomeNewPlayer)
          }
        }
      }
    }
  }

  package for_comprehension_anatomy {
    package types {
      object Main {
        import scala.util.Random

        case class Point(x: Int, y: Int)

        def randomBetween(min: Int, max: Int): IO[Nothing, Int] = IO.effectTotal(Random.nextInt(max - min) + min)

        val printRandomPoint: IO[Nothing, Point] = {
          for {
            x     /* Int   */ <- randomBetween(0, 10)            /* IO[Nothing, Int]  */
            _     /* Unit  */ <- putStrLn(s"x=$x")               /* IO[Nothing, Unit] */
            y     /* Int   */ <- randomBetween(0, 10)            /* IO[Nothing, Int]  */
            _     /* Unit  */ <- putStrLn(s"y=$y")               /* IO[Nothing, Unit] */
            point /* Point */ =  Point(x, y)                     /* Point             */
            _     /* Unit  */ <- putStrLn(s"point.x=${point.x}") /* IO[Nothing, Unit] */
            _     /* Unit  */ <- putStrLn(s"point.y=${point.y}") /* IO[Nothing, Unit] */
          } yield point /* Point */
        } /* IO[Nothing, Point] */

        def main(args: Array[String]): Unit = {
          RTS.unsafeRun(printRandomPoint)
        }
      }
    }

    package scopes {
      import scala.util.Random

      object Main {
        def randomBetween(min: Int, max: Int): IO[Nothing, Int] = IO.effectTotal(Random.nextInt(max - min) + min)

        val printRandomPoint: IO[Nothing, Point] = {
          for {
            x <- randomBetween(0, 10)            /*  x                */
            _ <- putStrLn(s"x=$x")               /*  O                */
            y <- randomBetween(0, 10)            /*  |    y           */
            _ <- putStrLn(s"y=$y")               /*  |    O           */
            point = Point(x, y)                  /*  O    O    point  */
            _ <- putStrLn(s"point.x=${point.x}") /*  |    |    O      */
            _ <- putStrLn(s"point.y=${point.y}") /*  |    |    O      */
          } yield point                          /*  |    |    O      */
        }

        def main(args: Array[String]): Unit = {
          RTS.unsafeRun(printRandomPoint)
        }
      }
    }

    package implicit_nesting {
      object Main {
        val printRandomPoint: IO[Nothing, Point] = {
          for {
            x <- randomBetween(0, 10)
            /* | */ _ <- putStrLn(s"x=$x")
            /* |    | */ y <- randomBetween(0, 10)
            /* |    |    | */ _ <- putStrLn(s"y=$y")
            /* |    |    |    | */ point = Point(x, y)
            /* |    |    |    |    | */ _ <- putStrLn(s"point.x=${point.x}")
            /* |    |    |    |    |    | */ _ <- putStrLn(s"point.y=${point.y}")
          } /* |    |    |    |    |    |    | */ yield point
        }

        def main(args: Array[String]): Unit = {
          RTS.unsafeRun(printRandomPoint)
        }
      }
    }
  }

  package condition {
    object Main {
      def describeNumber(n: Int): IO[Nothing, Unit] = {
        for {
          _ <- if (n % 2 == 0) putStrLn("Even") else putStrLn("Odd")
          _ <- if (n == 42) putStrLn("The Answer") else IO.unit
        } yield ()
      }

      val program: IO[Nothing, Unit] = describeNumber(42)

      def main(args: Array[String]): Unit = {
        RTS.unsafeRun(program)
      }
    }
  }

  package loop {
    package recursion {
      object Main {
        def findName(id: Int): IO[Nothing, String] =
          IO.succeedLazy(s"Name $id")

        def findNames(ids: List[Int]): IO[Nothing, List[String]] = {
          ids match {
            case Nil => IO.succeed(Nil)

            case id :: restIds =>
              for {
                name      /* String       */ <- findName(id)       /* IO[Nothing, String]       */
                restNames /* List[String] */ <- findNames(restIds) /* IO[Nothing, List[String]] */
              } yield name :: restNames /* List[String] */
          }
        }

        val program: IO[Nothing, Unit] = for {
          names <- findNames(List(1, 3, 5))
          _ <- putStrLn(names.toString)
        } yield ()

        def main(args: Array[String]): Unit = {
          RTS.unsafeRun(program)
        }
      }
    }

    package foreach {
      object Main {
        def findName(id: Int): IO[Nothing, String] =
          IO.succeedLazy(s"Name $id")

        def findNames(ids: List[Int]): IO[Nothing, List[String]] =
          IO.foreach(ids) { id => findName(id) }

        val program: IO[Nothing, Unit] = for {
          names <- findNames(List(1, 3, 5))
          _ <- putStrLn(names.toString)
        } yield ()

        def main(args: Array[String]): Unit = {
          RTS.unsafeRun(program)
        }
      }
    }
  }

  package retry {
    import scala.util.Random

    object Main {
      def findName(id: Int): IO[Int, String] =  {
        for {
          n <- IO.effectTotal(Random.nextInt())
          name <- if (n > 0) IO.succeed(s"Name $id") else IO.fail(-1)
        } yield name
      }

      val retrySchedule = Schedule.recurs(5) && Schedule.exponential(1.second)

      val program =
        for {
          name <- findName(1).retry(retrySchedule)
          _ <- putStrLn(s"name=$name")
        } yield ()

      def main(args: Array[String]): Unit = {
        RTS.unsafeRun(program)
      }
    }
  }

  package resource {
    object Main {
      class Resource {
        def close: IO[Nothing, Unit] = IO.unit
        def read: IO[Int, String] = IO.succeed("read")
      }

      object Resource {
        def open(name: String): IO[Int, Resource] = IO.effectTotal(new Resource)
      }

      val program: IO[Int, Unit] =
        IO.bracket(Resource.open("hello"))(_.close) { resource =>
          for {
            line <- resource.read
            _ <- putStrLn(line)
          } yield ()
        }

      def main(args: Array[String]): Unit = {
        RTS.unsafeRun(program)
      }
    }
  }

  package fork {
    import scalaz.zio.clock.Clock

    object Main {
      val analyze: IO[Nothing, String] = IO.succeed("Analysis").delay(1.second).provide(Clock.Live)
      val validate: IO[Nothing, Boolean] = IO.succeed(false)

      val program: IO[Nothing, String] =
        for {
          analyzeFiber <- analyze.fork
          validateFiber <- validate.fork
          validated <- validateFiber.join
          _ <- if (validated) IO.unit else analyzeFiber.interrupt
          analysis <- analyzeFiber.join
          _ <- putStrLn(analysis)
        } yield analysis

      def main(args: Array[String]): Unit = {
        RTS.unsafeRun(program)
      }
    }
  }

  package environment {
    object Main {
      import java.util.concurrent.TimeUnit

      import scalaz.zio.{clock, console, random, system}
      import scalaz.zio.clock.Clock
      import scalaz.zio.console.Console
      import scalaz.zio.random.Random
      import scalaz.zio.system.System

      val program: ZIO[System with Clock with Random with Console, Throwable, Unit] = for {
        randomNumber <- random.nextInt(10)
        maybeJavaVersion <- system.property("java.version")
        millisSinceEpoch <- clock.currentTime(TimeUnit.MILLISECONDS)

        _ <- ZIO.foreach(maybeJavaVersion) { javaVersion =>
          console.putStrLn(s"Java Version is $javaVersion")
        }

        _ <- console.putStrLn(s"Milliseconds since epoch is $millisSinceEpoch")
        _ <- console.putStrLn(s"Random number is $randomNumber")
      } yield ()

      def main(args: Array[String]): Unit = {
        RTS.unsafeRun(program)
      }
    }
  }

  package other {
    package console {
      import scala.annotation.tailrec

      sealed trait Console[A] {
        def map[B](f: A => B): Console[B] = this.flatMap(f.andThen(Return(_)))

        def flatMap[B](f: A => Console[B]): Console[B] = this match {
          case GetStrLn(next) => GetStrLn(line => next(line).flatMap(f))
          case PutStrLn(line, next) => PutStrLn(line, () => next().flatMap(f))
          case Return(value) => f(value)
        }
      }

      final case class GetStrLn[A](next: String => Console[A]) extends Console[A]
      final case class PutStrLn[A](line: String, next: () => Console[A]) extends Console[A]
      final case class Return[A](value: A) extends Console[A]

      object Console {
        @tailrec
        def runUnsafe[A](console: Console[A]): A = console match {
          case PutStrLn(line, next) =>
            println(line)
            runUnsafe(next())

          case GetStrLn(next) =>
            val line = scala.io.StdIn.readLine()
            runUnsafe(next(line))

          case Return(value) => value
        }

        def getStrLn: Console[String] = GetStrLn(Return(_))
        def putStrLn(line: String): Console[Unit] = PutStrLn(line, () => Return(()))
        def point[A](value: A) = Return(value)
      }

      object Main {
        import Console._

        def main(args: Array[String]): Unit = {
          val value = countdown(10000)
          Console.runUnsafe(value)
        }

        def hello: Console[Unit] = for {
          _ <- putStrLn("What's your name?")
          name <- getStrLn
          _ <- putStrLn(s"Hello $name!")
        } yield ()

        def countdown(n: Int): Console[Unit] = {
          if (n == 0)
            putStrLn("Boom")
          else
            for {
              _ <- putStrLn(n.toString)
              _ <- countdown(n - 1)
            } yield ()
        }
      }
    }
  }

  package experiment {
    import java.io.FileNotFoundException
    import java.nio.file.{Files, Paths}
    import scala.util.Random

    object Main {
      import scala.collection.JavaConverters._

      // UIO[-R, +E, +A]

      val successIO: UIO[Int] = ZIO.succeed(42)
      val successLazyIO: UIO[Int] = ZIO.succeedLazy(40 + 2)

      val failureIO: IO[FileNotFoundException, Nothing] = ZIO.fail(new FileNotFoundException("expectable"))
      val deathIO: UIO[Nothing] = ZIO.die(new IndexOutOfBoundsException("unexpectable"))

      val randomIntIO: UIO[Int] = ZIO.effectTotal(/* () => */ Random.nextInt(42))
      val readFileIO: Task[Seq[String]] = ZIO.effect(/* () => */ Files.readAllLines(Paths.get("build.sbt")).asScala.toList)

      val readFile_IO: IO[FileNotFoundException, List[String]] = ZIO
        .effect(/* () => */ Files.readAllLines(Paths.get("build___.sbt")).asScala.toList)
        .refineOrDie {
          case ex: FileNotFoundException => ex
        }

      {
        // trait Cause[+E]
        val failureCause: Exit.Cause[FileNotFoundException] = Exit.Cause.fail(new FileNotFoundException("expectable"))
        val deathCause: Exit.Cause[Nothing] = Exit.Cause.die(new IndexOutOfBoundsException("unexpectable"))
        val interruptionCause: Exit.Cause[Nothing] = Exit.Cause.interrupt

        val failureExit: Exit[FileNotFoundException, Nothing] = Exit.halt(failureCause)
      }

      {
        // trait Exit[+E, +A]
        val successExit: Exit[Nothing, Int] = Exit.succeed(42)

        val failureExit: Exit[FileNotFoundException, Nothing] = Exit.fail(new FileNotFoundException("expectable"))
        val deathExit: Exit[Nothing, Nothing] = Exit.die(new IndexOutOfBoundsException("unexpectable"))
        val interruptionExit: Exit[Nothing, Nothing] = Exit.interrupt
      }
    }
  }
}
