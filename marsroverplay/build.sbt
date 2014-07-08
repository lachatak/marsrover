name := "marsroverplay"

Common.settings

libraryDependencies ++= Seq(
  "org.kaloz.excercise" % "marsrover-api" % "1.0.0",
  "com.typesafe.akka" %% "akka-contrib" % "2.3.0",
  "com.typesafe.akka" %% "akka-remote" % "2.3.0",
  "org.webjars" %% "webjars-play" % "2.3.0",
  "org.webjars" % "raphaeljs" % "2.1.2-1",
  "org.webjars" % "bootstrap" % "3.1.1-1"
)

lazy val marsroverplay = (project in file(".")).enablePlugins(PlayScala)
