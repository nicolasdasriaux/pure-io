package pureio {
  import scalaz.zio.console._
  import scalaz.zio.{IO, RTS}

  import scala.util.Random

  case class Point(x: Int, y: Int)

  object RTS extends RTS

  package mapflatmap {
    package map {
      object Main {
        def randomBetween(min: Int, max: Int): IO[Nothing, Int] = IO.sync(Random.nextInt(max - min) + min)
        val randomLetter: IO[Nothing, Char] = randomBetween('A', 'Z').map(_.toChar)

        def main(args: Array[String]): Unit = {
          println(RTS.unsafeRun(randomLetter))
        }
      }
    }

    package flatmap {
      object Main {
        def randomBetween(min: Int, max: Int): IO[Nothing, Int] = IO.sync(Random.nextInt(max - min) + min)

        val printRolledDice_WRONG: IO[Nothing, IO[Nothing, Unit]] = // Oops! Wrong type!
          randomBetween(1, 6).map(dice => putStrLn(s"Dice shows $dice"))

        val printRolledDice: IO[Nothing, Unit] =
          randomBetween(1, 6).flatMap(dice => putStrLn(s"Dice shows $dice"))

        def main(args: Array[String]): Unit = {
          RTS.unsafeRun(printRolledDice)
        }
      }
    }

    object rest {
      def randomBetween(min: Int, max: Int): IO[Nothing, Int] = IO.sync(Random.nextInt(max - min) + min)

      val randomPoint: IO[Nothing, Point] =
        randomBetween(0, 20).flatMap { x =>
          randomBetween(0, 20).map { y =>
            Point(x, y)
          }
        }

      val printRandomPoint: IO[Nothing, Unit] =
        randomBetween(0, 20).flatMap { x =>
          randomBetween(0, 20).flatMap { y =>
            val point = Point(x, y)
            putStrLn(s"point=$point")
          }
        }

      val randomPointWithForComprehension: IO[Nothing, Point] = {
        for {
          x /* Int */ <- randomBetween(0, 20) /* IO[Nothing, Int] */
          y /* Int */ <- randomBetween(0, 20) /* IO[Nothing, Int] */
        } yield Point(x, y) /* Point */
      } /* IO[Nothing, Point] */

    }
  }

  package purity {
    package io {
      object Main {
        val printHello: IO[Nothing, Unit] = IO.sync(println("Hello!"))
        // Equivalent to IO.sync(() => println("Hello!"))
        // Do not compile

        def main(args: Array[String]): Unit = {
          println("Start")
          RTS.unsafeRun(printHello)
          println("End")
        }
      }
    }

    package stateful {
      object Main {
        val randomBetween1And10000: IO[Nothing, Int] = IO.sync(Random.nextInt(10000) + 1)
        // Equivalent IO.sync(() => Random.nextInt(1000) + 1)
        // Do not compile

        def main(args: Array[String]): Unit = {
          println("Start")
          println(RTS.unsafeRun(randomBetween1And10000))
          println(RTS.unsafeRun(randomBetween1And10000))
          println("End")
        }
      }
    }
  }

  package referentialtransparency {
    package inlined {
      object Main {
        def randomBetween(min: Int, max: Int): IO[Nothing, Int] = IO.sync(Random.nextInt(max - min) + min)

        def program: IO[Nothing, Unit] = {
          for {
            r1 <- randomBetween(1, 25)
            r2 <- randomBetween(1, 25)
            _ <- putStr(s"$r1 + $r2 = ${r1 + r2}")
          } yield ()
        }

        def main(args: Array[String]): Unit = {
          RTS.unsafeRun(program)
        }
      }
    }

    package extracted {
      object Main {
        def randomBetween(min: Int, max: Int): IO[Nothing, Int] = IO.sync(Random.nextInt(max - min) + min)

        def program: IO[Nothing, Unit] = {
          val randomBetween1To25 = randomBetween(1, 25)

          for {
            r1 <- randomBetween1To25
            r2 <- randomBetween1To25
            _ <- putStr(s"$r1 + $r2 = ${r1 + r2}")
          } yield ()
        }

        def main(args: Array[String]): Unit = {
          RTS.unsafeRun(program)
        }
      }
    }
  }

  package forcomprehension {
    package types {
      object Main {
        def randomBetween(min: Int, max: Int): IO[Nothing, Int] = IO.sync(Random.nextInt(max - min) + min)

        def printRandomPoint: IO[Nothing, Unit] = {
          for {
            x     /* Int   */ <- randomBetween(0, 10)            /* IO[Nothing, Int]  */
            _     /* Unit  */ <- putStrLn(s"x=$x")               /* IO[Nothing, Unit] */
            y     /* Int   */ <- randomBetween(0, 10)            /* IO[Nothing, Int]  */
            _     /* Unit  */ <- putStrLn(s"y=$y")               /* IO[Nothing, Unit] */
            point /* Point */ =  Point(x, y)                     /* Point             */
            _     /* Unit  */ <- putStrLn(s"point.x=${point.x}") /* IO[Nothing, Unit] */
            _     /* Unit  */ <- putStrLn(s"point.y=${point.y}") /* IO[Nothing, Unit] */
          } yield () /* Unit */
        } /* IO[Nothing, Unit] */

        def main(args: Array[String]): Unit = {
          RTS.unsafeRun(printRandomPoint)
        }
      }
    }

    package scopes {
      object Main {
        def randomBetween(min: Int, max: Int): IO[Nothing, Int] = IO.sync(Random.nextInt(max - min) + min)

        def printRandomPoint: IO[Nothing, Unit] = {
          for {                                  /*  x    y    point  */
            x <- randomBetween(0, 10)            /*  -    -    -      */
            _ <- putStrLn(s"x=$x")               /*  o    -    -      */
            y <- randomBetween(0, 10)            /*  o    -    -      */
            _ <- putStrLn(s"y=$y")               /*  o    o    -      */
            point = Point(x, y)                  /*  o    o    -      */
            _ <- putStrLn(s"point.x=${point.x}") /*  o    o    o      */
            _ <- putStrLn(s"point.y=${point.y}") /*  o    o    o      */
          } yield ()                             /*  o    o    o      */
        }

        def main(args: Array[String]): Unit = {
          RTS.unsafeRun(printRandomPoint)
        }
      }
    }

    package nesting {
      object Main {
        def randomBetween(min: Int, max: Int): IO[Nothing, Int] = IO.sync(Random.nextInt(max - min) + min)

        def printRandomPoint: IO[Nothing, Unit] = {
          for {
               x <- randomBetween(0, 10)
            /* | */ _ <- putStrLn(s"x=$x")
            /* |    | */ y <- randomBetween(0, 10)
            /* |    |    | */ _ <- putStrLn(s"y=$y")
            /* |    |    |    | */ point = Point(x, y)
            /* |    |    |    |    | */ _ <- putStrLn(s"point.x=${point.x}")
            /* |    |    |    |    |    | */ _ <- putStrLn(s"point.y=${point.y}")
          } /* |    |    |    |    |    |    | */ yield ()
        }

        def main(args: Array[String]): Unit = {
          RTS.unsafeRun(printRandomPoint)
        }
      }
    }
  }
}
