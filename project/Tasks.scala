import sbt._

object Aliases {

  lazy val startRoverAlias = addCommandAlias("rover", ";project marsrover; runMain org.kaloz.excercise.marsrover.MarsRover")
  lazy val startHqAlias = addCommandAlias("hq", ";project marsrover; runMain org.kaloz.excercise.marsrover.MarsExpedition")
  lazy val startUiAlias = addCommandAlias("ui", ";project marsroverplay; run")
}
