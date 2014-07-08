import sbt.Keys._
import sbt._

object Common {

  val settings: Seq[Setting[_]] = Seq(
    organization := "org.kaloz.excercise",
    version := "1.0.0",
    scalaVersion := "2.10.4",
    scalacOptions := Seq(
      "-unchecked",
      "-deprecation",
      "-encoding", "utf8"
    )
  )

}
