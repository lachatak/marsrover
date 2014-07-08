import sbt._
import Keys._

object Common {

  val settings: Seq[Setting[_]] = Seq(
    organization := "org.kaloz.excercise",
    version := "1.0.0",
    scalacOptions := Seq(
      "-unchecked",
      "-deprecation",
      "-encoding", "utf8"
    )
  )

}
