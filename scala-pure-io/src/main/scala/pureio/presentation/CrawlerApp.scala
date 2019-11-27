package pureio.presentation

import zio.{App, IO, Queue}

object CrawlerApp extends App {
  def putStrLn(line: String): IO[Nothing, Unit] = IO.effectTotal(Console.println(line))
  def sleep(millis: Int): IO[Nothing, Unit] = IO.effectTotal(Thread.sleep(millis))
  case class PageId(id: Int)

  def scanLinks(pageId: PageId): IO[Nothing, List[PageId]] = {
    val links = if (pageId.id < 10)
      List(PageId(pageId.id * 3), PageId(pageId.id * 3 + 1), PageId(pageId.id * 3 + 2))
    else
      List.empty[PageId]


    sleep((pageId.id % 10 + 1) * 200) *> IO.succeed(links)
  }

  def scanLinkWorker(workerQueue: Queue[PageId]): IO[Nothing, Unit] = {
    {
      for {
        pageId <- workerQueue.take
        _ <- putStrLn(s"Scanning page $pageId")
        links <- scanLinks(pageId)
        _ <- putStrLn(s"Found page $pageId links $links")
        _ <- workerQueue.offerAll(links)
      } yield ()
    }.forever
  }

  val program: IO[Nothing, Unit] = for {
    workerQueue <- Queue.unbounded[PageId]
    _ <- workerQueue.offer(PageId(1))
    _ <- IO.forkAll((1 to 10).map(_ => scanLinkWorker(workerQueue).fork))
    _ <- workerQueue.awaitShutdown
  } yield ()

  override def run(args: List[String]): IO[Nothing, Int] = {
    program.as(1)
  }
}
