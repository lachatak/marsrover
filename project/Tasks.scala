import sbt._

object Tasks {

  lazy val startRoverTask = addCommandAlias("rover", ";project marsrover; runMain org.kaloz.excercise.marsrover.MarsRover")
  lazy val startHqTask = addCommandAlias("hq", ";project marsrover; runMain org.kaloz.excercise.marsrover.MarsExpedition")
  lazy val startUITask = addCommandAlias("ui", ";project marsroverplay; run")
}