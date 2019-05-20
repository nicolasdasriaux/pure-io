package zioapp

import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.auto._
import scalaz.zio.{IO, ZIO}

case class AppConfig(api: ApiConfig, database: DatabaseConfig)
case class ApiConfig(interface: String, port: Int)
case class DatabaseConfig(url: String, driver: String, user: String, password: String)

object ConfigService {
  def load(): IO[ConfigReaderFailures, AppConfig] = ZIO.fromEither(pureconfig.loadConfig[AppConfig]("app"))
}
