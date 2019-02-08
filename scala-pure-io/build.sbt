lazy val root = (project in file("."))
  .settings(
    name := "pureio",
    version := "0.1",
    scalaVersion := "2.12.7",
    libraryDependencies := Seq(
      "org.scalaz" %% "scalaz-zio" % "0.6.0"
    )
  )
