package org.kaloz.excercise.marsrover

import akka.actor.{ActorRef, ActorLogging, Actor}

class Plateau(plateuaConfigarutaion: PlateauConfiguration) extends Actor with ActorLogging {

  import Plateau._
  import MarsRover._

  var roverPositions = Map.empty[ActorRef, RoverPosition]

  def receive = {
    case Position(roverPosition) =>
      roverPositions += (sender -> roverPosition)
      if (roverPositions.values.filter(_ == roverPosition).size > 1) {
        roverPositions.foreach {
          case (marsRover: ActorRef, position: RoverPosition) =>
            log.info(s"${marsRover.path.name} has collided at $position")
            marsRover ! Collusion
        }
      } else if (getLost(roverPosition)) {
        log.info(s"${sender.path.name} got lost at $roverPosition")
        sender ! GotLost
      } else {
        log.info(s"${sender.path.name} position at $roverPosition is safe")
        sender ! Ack
      }
  }

  private def getLost(roverPosition: RoverPosition) =
    if (roverPosition.x < 0 || roverPosition.x > plateuaConfigarutaion.x || roverPosition.y < 0 || roverPosition.y > plateuaConfigarutaion.y) true
    else false
}

object Plateau {

  case object Collusion

  case object Ack

  case object GotLost

}
