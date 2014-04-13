package org.kaloz.excercise.marsrover

import akka.actor._
import akka.persistence.{SnapshotOffer, EventsourcedProcessor}
import scala.concurrent.duration._

class Display extends EventsourcedProcessor with ActorLogging {

  import Display._

  case class PositionChangedEvent(marsRover: String, roverPosition: RoverPosition)

  case class DisplayState(roverPositions: Map[String, RoverPosition])

  case object TakeSnapshot

  context.system.scheduler.schedule(2 second, 2 second, self, TakeSnapshot)(context.dispatcher)

  var roverPositions = Map.empty[String, RoverPosition]

  def updateState(evt: PositionChangedEvent) = evt match {
    case PositionChangedEvent(marsRover, roverPosition) =>
      log.info(s"Register $roverPosition to ${marsRover}")
      roverPositions += (marsRover -> roverPosition)
  }

  val receiveRecover: Receive = {
    case evt: PositionChangedEvent => updateState(evt)
    case SnapshotOffer(_, DisplayState(positions)) => roverPositions = positions
  }

  def receiveCommand: Receive = {
    case RegisterPosition(marsRover, roverPosition) =>
      persist(PositionChangedEvent(marsRover.path.name, roverPosition))(updateState)
    case ShowPositions =>
      roverPositions.foreach {
        case (marsRover, position) => log.info(s"Last known position of ${marsRover} is $position")
      }
    case TakeSnapshot =>
      println("Taking snapshot...")
      saveSnapshot(DisplayState(roverPositions))
  }
}

object Display {

  def props: Props = Props(classOf[Display])

  case object ShowPositions

  case class RegisterPosition(marsRover: ActorRef, roverPosition: RoverPosition)

}
