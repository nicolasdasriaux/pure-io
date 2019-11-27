package pureio.kata.guessnumber

import zio.test.Assertion._
import zio.test._
import zio.test.environment.{TestConsole, TestRandom}

object GuessNumberAppSpec extends DefaultRunnableSpec({
  def hasExceptionMessage(message: String): Assertion[Exception] = hasField("getMessage", (ex: Exception) => ex.getMessage, equalTo(message))

  suite("Guess a number")(
    suite("Get Int")(
      testM("should succeed when proper integer string") {
        for {
          _ <- TestConsole.feedLines("42")
          n <- GuessNumberApp.getInt
        } yield assert(n, equalTo(42))
      },

      testM("should fail when invalid integer string") {
        val io = for {
          _ <- TestConsole.feedLines("abc")
          n <- GuessNumberApp.getInt
        } yield n

        assertM(io.either, isLeft(hasExceptionMessage("""For input string: "abc"""")))
      }
    ),

    suite("Get Int between")(
      testM("succeed when number in range") {
        for {
          _ <- TestConsole.feedLines("15")
          n <- GuessNumberApp.getIntBetween(1, 20)
        } yield assert(n, equalTo(15))
      },

      testM("should fail when number out of range") {
        val io = for {
          _ <- TestConsole.feedLines("42")
          n <- GuessNumberApp.getIntBetween(1, 20)
        } yield n

        assertM(io.either, isLeft(hasExceptionMessage("""Number not between 1 and 20 (42)""")))
      }
    ),

    suite("Game")(
      testM("should handle a full game session") {
        for {
          _ <- TestRandom.feedInts(11)
          _ <- TestConsole.feedLines("1", "20", "10", "15", "12")
          _ <- GuessNumberApp.game
          output <- TestConsole.output
        } yield assert(output, equalTo(
          Seq(
            "Guess a number between 1 and 20.\n",
            "Attempt 1 > ",
            "It's too small.\n",
            "Attempt 2 > ",
            "It's too large.\n",
            "Attempt 3 > ",
            "It's too small.\n",
            "Attempt 4 > ",
            "It's too large.\n",
            "Attempt 5 > ",
            "You won after 5 attempt(s).\n",
          )
        ))
      }
    )
  )
})
