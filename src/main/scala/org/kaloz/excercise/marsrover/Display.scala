package org.kaloz.excercise.marsrover

import akka.actor._

class Display extends Actor with ActorLogging {

  import Display._

  var roverPositions = Map.empty[ActorRef, RoverPosition]

  def receive = {
    case RegisterPosition(marsRover, roverPosition) =>
      log.info(s"Register $roverPosition to ${marsRover.path.name}")
      roverPositions += (marsRover -> roverPosition)
    case ShowPositions =>
      roverPositions.foreach {
        case (marsRover, position) => log.info(s"Last known position of ${marsRover.path.name} is $position")
      }
  }
}

object Display {

  def props: Props = Props(classOf[Display])

  case object ShowPositions

  case class RegisterPosition(marsRover: ActorRef, roverPosition: RoverPosition)

}
