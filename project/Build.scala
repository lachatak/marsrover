import play.PlayScala
import sbt._
import Keys._


object Build extends Build with BuildExtra {

  import BuildSettings._

  override lazy val settings = super.settings :+ {
    shellPrompt := { s => "[" + scala.Console.BLUE + Project.extract(s).currentProject.id + scala.Console.RESET + "] $ "}
  }

  lazy val marsroverroot = Project("marsroverroot", file("."))
    .aggregate(marsroverapi, marsrover, marsroverplay)
    .settings(basicSettings: _*)

  lazy val marsroverapi = Project("marsroverapi", file("marsroverapi"))
    .settings(basicSettings: _*)
    .settings(libraryDependencies ++= Dependencies.marsroverapi)

  lazy val marsrover = Project("marsrover", file("marsrover"))
    .aggregate(marsroverapi)
    .dependsOn(marsroverapi)
    .settings(basicSettings: _*)
    .settings(libraryDependencies ++= Dependencies.marsrover)

  lazy val marsroverplay = Project("marsroverplay", file("marsroverplay"))
    .aggregate(marsroverapi)
    .dependsOn(marsroverapi)
    .settings(basicSettings: _*)
    .settings(libraryDependencies ++= Dependencies.marsroverplay).enablePlugins(PlayScala)

}