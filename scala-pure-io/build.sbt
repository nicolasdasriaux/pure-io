lazy val root = (project in file("."))
  .settings(
    organization := "pureio",
    name := "scala-pure-io",
    version := "0.1",

    scalaVersion := "2.12.8",
    scalacOptions ++= Seq("-Ypartial-unification", "-deprecation"),

    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "1.0.0-RC17",
      "dev.zio" %% "zio-test" % "1.0.0-RC17" % Test
    )
  )
