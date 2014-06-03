name := "marsrover"

organization := "org.kaloz.excercise"

version := "1.0.0"

scalaVersion := "2.10.2"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8")

javacOptions ++= Seq("-Xlint:deprecation", "-encoding", "utf8", "-XX:MaxPermSize=256M")

crossPaths := false

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-actor_2.10" % "2.3.0",
  "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.0",
  "com.typesafe.akka" %% "akka-contrib" % "2.3.0",
  "com.typesafe.akka" %% "akka-remote" % "2.3.0",
  "org.scala-lang" % "jline" % "2.10.2",
  "junit" % "junit" % "4.11" % "test",
  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "org.specs2" % "specs2_2.10" % "2.3.7" % "test",
  "com.typesafe.akka" % "akka-testkit_2.10" % "2.3.0" % "test",
  "org.scalatest" % "scalatest_2.10" % "2.0" % "test"
)
