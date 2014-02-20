package org.kaloz.excercise.marsrover

import akka.actor._
import scala.concurrent.duration._

class MarsRover(roverPosition: RoverPosition) extends Actor with Configuration with ActorLogging {

  import Plateau._
  import MarsRover._
  import MarsRoverController._
  import context.dispatcher

  val plateau: ActorSelection = context.actorSelection("../../../plateau")

  var roverState = RoverState.UNDER_DEPLOYMENT
  var actualRoverPosition = roverPosition
  var lastAction: Action.Value = _
  var marsRoverController: ActorRef = _

  def receive = {
    case DeployRover =>
      log.info(s"Mars rover is approaching to $actualRoverPosition")
      plateau ! Position(actualRoverPosition)
      marsRoverController = sender
    case RoverAction(action) =>
      log.info(s"Mars rover is moving to ${actualRoverPosition.doAction(action)} with action $action")
      lastAction = action
      roverState = RoverState.MOVING
      context.system.scheduler.scheduleOnce(lastAction match {
        case Action.L | Action.R => turningSpeed
        case Action.M => movementSpeed
      }, self, EndOfMovement)
    case EndOfMovement =>
      actualRoverPosition = actualRoverPosition.doAction(lastAction)
      log.info(s"Mars rover has arrived to $actualRoverPosition with action $lastAction")
      plateau ! Position(actualRoverPosition)
    case Collusion =>
      log.info(s"Mars rover has broken down")
      self ! PoisonPill
    case GotLost =>
      log.info(s"Mars rover got lost!")
      self ! PoisonPill
    case Ack =>
      if (roverState.equals(RoverState.UNDER_DEPLOYMENT)) {
        roverState = RoverState.READY
        marsRoverController ! RoverDeployed
      } else {
        roverState = RoverState.READY
        log.info(s"Mars rover is waiting for the next action")
        marsRoverController ! Position(actualRoverPosition)
      }
  }

  object RoverState extends Enumeration {
    type RoverState = Value
    val UNDER_DEPLOYMENT, READY, MOVING = Value
  }

  case object EndOfMovement

}

object MarsRover {

  def props(roverPosition: RoverPosition): Props = Props(classOf[MarsRover], roverPosition)

  case class Position(position: RoverPosition)

}

trait Configuration {
  def movementSpeed = 100 millis

  def turningSpeed = 100 millis
}

