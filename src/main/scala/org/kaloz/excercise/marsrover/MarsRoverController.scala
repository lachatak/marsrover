package org.kaloz.excercise.marsrover

import akka.actor._

class MarsRoverController(roverConfigurations: RoverConfiguration, display: ActorRef) extends Actor with ActorLogging {

  import MarsRover._
  import MarsRoverController._
  import Display._

  val marsRover = context.actorOf(Props(classOf[MarsRover], roverConfigurations.roverPosition, self), name = s"marsRover-${extractCounter}")
  var roverActions = roverConfigurations.actions
  context.watch(marsRover)
  log.info(s"Deploying ${marsRover.path.name} to ${roverConfigurations.roverPosition}")
  marsRover ! DeployRover

  def receive = {
    case RoverDeployed =>
      log.info(s"${marsRover.path.name} has been deployed, sending start action")
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
      log.info(s"No more actions. Controller is stopping")
      self ! PoisonPill
    }
  }

  private def extractCounter = self.path.name.substring(self.path.name.lastIndexOf("-") + 1)
}

object MarsRoverController {

  case object DeployRover

  case object RoverDeployed

  case class RoverAction(action: Action.Value)

  case class Disaster(marsRover: ActorRef)

}
