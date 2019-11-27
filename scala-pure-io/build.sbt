val zioVersion = SettingKey[String]("zioVersion")
val zioMacrosVersion = SettingKey[String]("zioMacrosVersion")

lazy val root = (project in file("."))
  .settings(
    organization := "pureio",
    name := "scala-pure-io",
    version := "0.1",

    scalaVersion := "2.12.8",
    scalacOptions ++= Seq("-Ypartial-unification", "-deprecation"),
    addCompilerPlugin(("org.scalamacros" % "paradise"  % "2.1.1") cross CrossVersion.full),
    zioVersion := "1.0.0-RC17",
    zioMacrosVersion := "0.6.0",

    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion.value,
      "dev.zio" %% "zio-streams" % zioVersion.value,
      "dev.zio" %% "zio-macros-core" % zioMacrosVersion.value,
      "dev.zio" %% "zio-macros-test" % zioMacrosVersion.value,

      "dev.zio" %% "zio-test" % zioVersion.value % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion.value % Test
    ),

    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
