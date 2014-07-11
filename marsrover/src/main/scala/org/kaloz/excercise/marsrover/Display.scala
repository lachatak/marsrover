package org.kaloz.excercise.marsrover

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.persistence.{EventsourcedProcessor, SnapshotOffer, SnapshotSelectionCriteria}
import org.kaloz.excercise.marsrover.api._

import scala.concurrent.duration._

case object Exception

class Display(plateauConfiguration: PlateauConfiguration) extends EventsourcedProcessor with ActorLogging {

  import org.kaloz.excercise.marsrover.Display._

  case object TakeSnapshot

  case object FailureCheck

  val mediator = DistributedPubSubExtension(context.system).mediator

  var roverPositions = DisplayState()

  var remoteDisplay: Option[ActorRef] = None

  var nasa: ActorRef = _

  def scheduleFailure = false

  def scheduleSnapshot = false

  def chancheToFailure = 0.4

  def initialDelayForSnapshot = 5 second

  def scheduledSnapshots = 5 second

  def initialDelayForFailureCheck = 5 second

  def scheduledFailureCheck = 5 second

  mediator ! Subscribe("remoteDisplay", self)

  override def preStart() {
    deleteSnapshots(SnapshotSelectionCriteria())
    deleteMessages(100000l)

    implicit val executor = context.dispatcher
    if (scheduleSnapshot) context.system.scheduler.schedule(initialDelayForSnapshot, scheduledSnapshots, self, TakeSnapshot)
    if (scheduleFailure) context.system.scheduler.schedule(initialDelayForFailureCheck, scheduledFailureCheck, self, FailureCheck)

    super.preStart()
  }

  def updateState(evt: PositionChangedEvent) = evt match {
    case event@PositionChangedEvent(marsRover, roverPosition) =>
      log.info(s"Register $roverPosition to ${marsRover}")
      roverPositions = roverPositions.update(event)

      remoteDisplay match {
        case Some(display) => display ! roverPositions
        case None =>
      }
  }

  val receiveRecover: Receive = {
    case evt: PositionChangedEvent => log.warning(s"Recover event - $evt"); updateState(evt)
    case SnapshotOffer(_, snapshot: DisplayState) => log.warning(s"Recover snapshot - $snapshot"); roverPositions = snapshot
    case _ =>
  }

  val receiveCommand: Receive = {
    case SubscribeAck(Subscribe("remoteDisplay", None, self)) =>
      log.info("Subscribed to remoteDisplay topic!")
      log.info("Waiting Remote display to register!")
    case RegisterDisplay(display) =>
      remoteDisplay = Some(display)
      display ! DisplayRegistered(plateauConfiguration)
    case RegisterPosition(marsRover, roverPosition) =>
      persist(PositionChangedEvent(marsRover.path.name, roverPosition))(updateState)
    case FinalPositionsDisplayed =>
      nasa ! PositionsDisplayed
    case ShowPositions =>
      roverPositions.state.foreach {
        case (marsRover, position :: history) => log.info(s"Last known position of ${marsRover} is $position. History[$history]")
        case _ =>
      }

      remoteDisplay match {
        case Some(display) =>
          nasa = sender
          log.info(s"sending final !!!!! ${display}")
          display ! DisplayFinalPositions(roverPositions)
        case None =>
          sender ! PositionsDisplayed
      }

    case TakeSnapshot =>
      log.info("Taking snapshot...")
      saveSnapshot(roverPositions)
    case FailureCheck =>
      val r = math.random
      log.warning(s"Chance to blow was $r")
      if (r < chancheToFailure) self ! Exception
    case Exception =>
      throw new Exception("Some really serious problem!");
  }

  override def postRestart(err: Throwable) = {
    log.info("I am back again...")
  }
}

object Display {

  def props(plateauConfiguration: PlateauConfiguration): Props = Props(classOf[Display], plateauConfiguration)

  case object ShowPositions

  case object PositionsDisplayed

  case class RegisterPosition(marsRover: ActorRef, roverPosition: RoverPosition)

}
