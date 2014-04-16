package org.kaloz.excercise.marsrover

import akka.actor._
import akka.persistence.{SnapshotSelectionCriteria, SnapshotOffer, EventsourcedProcessor}
import scala.concurrent.duration._

case class DisplayState(state: Map[String, List[RoverPosition]] = Map.empty) {
  def update(event: PositionChangedEvent) = copy(state + (event.marsRover -> (event.roverPosition :: state.getOrElse(event.marsRover, List.empty))))
}

case class PositionChangedEvent(marsRover: String, roverPosition: RoverPosition)

class Display extends EventsourcedProcessor with ActorLogging {

  import Display._

  def chancheToFailure = 0.4

  case object TakeSnapshot

  case object Exception

  implicit val executor = context.dispatcher
  context.system.scheduler.schedule(2 second, 2 second, self, TakeSnapshot)
  context.system.scheduler.schedule(5 second, 5 second, self, Exception)

  var roverPositions = DisplayState()

  override def preStart() {
    deleteSnapshots(SnapshotSelectionCriteria())
    deleteMessages(100000l)
    super.preStart()
  }

  def updateState(evt: PositionChangedEvent) = evt match {
    case event@PositionChangedEvent(marsRover, roverPosition) =>
      log.info(s"Register $roverPosition to ${marsRover}")
      roverPositions = roverPositions.update(event)
  }

  val receiveRecover: Receive = {
    case evt: PositionChangedEvent => log.warning(s"Recover event - $evt"); updateState(evt)
    case SnapshotOffer(_, snapshot: DisplayState) => log.warning(s"Recover snapshot - $snapshot"); roverPositions = snapshot
  }

  val receiveCommand: Receive = {
    case RegisterPosition(marsRover, roverPosition) =>
      persist(PositionChangedEvent(marsRover.path.name, roverPosition))(updateState)
    case ShowPositions =>
      roverPositions.state.foreach {
        case (marsRover, position :: history) => log.info(s"Last known position of ${marsRover} is $position. History[$history]")
        case _ =>
      }
      sender ! PositionsDisplayed
    case TakeSnapshot =>
      log.info("Taking snapshot...")
      saveSnapshot(roverPositions)
    case Exception =>
      val r = math.random
      log.warning(s"Chance to blow was $r")
      if (r < chancheToFailure) throw new Exception("Some really serious problem!");
  }

  override def postRestart(err: Throwable) = {
    log.info("I am back again...")
  }
}

object Display {

  def props: Props = Props(classOf[Display])

  case object ShowPositions

  case object PositionsDisplayed

  case class RegisterPosition(marsRover: ActorRef, roverPosition: RoverPosition)

}
