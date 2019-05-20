import sbt.Keys.version

lazy val root = (project in file("."))
  .settings(
    name := "zio-app",
    version := "0.1",
    scalaVersion := "2.12.8",

    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-zio" % "1.0-RC4",
      "org.scalaz" %% "scalaz-zio-interop-cats" % "1.0-RC4",

      "com.github.pureconfig" %% "pureconfig" % "0.10.2",
      "org.tpolecat" %% "doobie-core" % "0.6.0",
      "org.tpolecat" %% "doobie-hikari" % "0.6.0",
      "org.tpolecat" %% "doobie-postgres" % "0.6.0"
    ),

    run / fork := true
  )
