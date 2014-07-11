import sbt._
import Keys._

object BuildSettings {

  import Tasks._

  lazy val basicSettings = Seq(
    version := "1.0.0",
    organization := "org.kaloz.excercise",
    description := "MarsRover DEMO Application",
    scalaVersion := "2.10.4",
    scalacOptions := Seq(
      "-encoding", "utf8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-target:jvm-1.7",
      "-language:postfixOps",
      "-language:implicitConversions"
    )
  ) ++
    startRoverTask ++
    startHqTask ++
    startUITask

}