lazy val root = (project in file("."))
  .settings(
    organization := "pureio",
    name := "scala-pure-io",
    version := "0.1",
    scalaVersion := "2.12.8",

    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-zio" % "1.0-RC3"
    )
  )
