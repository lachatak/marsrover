package org.kaloz.excercise.marsrover

import akka.actor._
import akka.persistence.{SnapshotOffer, EventsourcedProcessor}
import scala.concurrent.duration._

case class DisplayState(state: Map[ActorRef, RoverPosition] = Map.empty) {
  def update(event: PositionChangedEvent) = copy(state + (event.marsRover -> event.roverPosition))
}

case class PositionChangedEvent(marsRover: ActorRef, roverPosition: RoverPosition)

class Display extends EventsourcedProcessor with ActorLogging {

  import Display._

  case object TakeSnapshot

  context.system.scheduler.schedule(2 second, 2 second, self, TakeSnapshot)(context.dispatcher)

  var roverPositions = DisplayState()

  def updateState(evt: PositionChangedEvent) = evt match {
    case event@PositionChangedEvent(marsRover, roverPosition) =>
      log.info(s"Register $roverPosition to ${marsRover}")
      roverPositions = roverPositions.update(event)
  }

  val receiveRecover: Receive = {
    case evt: PositionChangedEvent => updateState(evt)
    case SnapshotOffer(_, snapshot: DisplayState) => roverPositions = snapshot
  }

  def receiveCommand: Receive = {
    case RegisterPosition(marsRover, roverPosition) =>
      persist(PositionChangedEvent(marsRover, roverPosition))(updateState)
    case ShowPositions =>
      roverPositions.state.foreach {
        case (marsRover, position) => log.info(s"Last known position of ${marsRover.path.name} is $position")
      }
    case TakeSnapshot =>
      log.info("Taking snapshot...")
      saveSnapshot(roverPositions)
  }
}

object Display {

  def props: Props = Props(classOf[Display])

  case object ShowPositions

  case class RegisterPosition(marsRover: ActorRef, roverPosition: RoverPosition)

}
