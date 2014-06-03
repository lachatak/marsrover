package org.kaloz.excercise.marsrover

import akka.actor._

class MarsRoverController(roverConfigurations: RoverConfiguration, marsRover: ActorRef, display: ActorRef) extends Actor with ActorLogging {

  import MarsRover._
  import MarsRoverController._
  import Display._

  var roverActions = roverConfigurations.actions

  context.watch(marsRover)

  def receive = {
    case StartRover =>
      log.info("Start command has arrived. Let's GO!")
      log.info(s"Deploying ${marsRover.path.name} to ${roverConfigurations.roverPosition}")
      marsRover ! DeployRover(roverConfigurations.roverPosition)
    case RoverDeployed =>
      log.info(s"${marsRover.path.name} has been deployed, waiting for start command!")
      sendNextAction
    case Position(roverPosition) =>
      log.info(s"$roverPosition is acknowledged from ${marsRover.path.name}")
      display ! RegisterPosition(marsRover, roverPosition)
      sendNextAction
    case Terminated(marsRover) =>
      log.info(s"Disaster with ${marsRover.path.name}")
      context.parent ! Disaster(marsRover)
      self ! PoisonPill
  }

  private def sendNextAction {
    if (!roverActions.isEmpty) {
      log.info(s"Sending action ${roverActions.head} to ${marsRover.path.name}")
      marsRover ! RoverAction(roverActions.head)
      roverActions = roverActions.tail
    } else {
      log.info("No more actions. Controller is stopping...")
      marsRover ! PoisonPill
      self ! PoisonPill
    }
  }
}

object MarsRoverController {

  def props(roverConfigurations: RoverConfiguration, marsRover: ActorRef, display: ActorRef): Props = Props(classOf[MarsRoverController], roverConfigurations, marsRover, display)

  case object StartRover

  case object RoverDeployed

  case class RoverAction(action: Action.Value)

  case class Disaster(marsRover: ActorRef)

}
