package zioapp

import cats.effect.Resource
import doobie.hikari._
import scalaz.zio._
import scalaz.zio.interop.catz._
import doobie.util.transactor.Transactor

import scala.concurrent.ExecutionContext

object CustomerService {

  def transactor(databaseConfig: DatabaseConfig, connectEC: ExecutionContext, transactEC: ExecutionContext): Resource[Task, HikariTransactor[Task]]  = {
    def xa: Resource[Task, HikariTransactor[Task]] = HikariTransactor.newHikariTransactor[Task](
      databaseConfig.driver,
      databaseConfig.url,
      databaseConfig.user,
      databaseConfig.password,
      connectEC,
      transactEC
    )

    ???
  }
}
