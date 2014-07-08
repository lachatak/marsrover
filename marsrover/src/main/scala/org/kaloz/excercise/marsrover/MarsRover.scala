package org.kaloz.excercise.marsrover

import akka.actor._
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import scala.tools.jline.console.ConsoleReader
import akka.contrib.pattern.DistributedPubSubExtension
import akka.cluster.Cluster
import org.kaloz.excercise.marsrover.NasaHQ.{RoverRegistered, RegisterRover}
import org.kaloz.excercise.marsrover.api.{Action, RoverPosition}

object MarsRover extends App {

  def props: Props = Props[MarsRover]

  case class Position(position: RoverPosition, marsRover: ActorRef*)

  case class DeployRover(roverPosition: RoverPosition)

  val identity = new ConsoleReader().readLine("rover id: ")
  val system = ActorSystem("MarsExpedition", ConfigFactory.load.getConfig("marsrover"))

  /**
   * Join to the cluster
   */
  val serverconfig = ConfigFactory.load.getConfig("headquarter")
  val serverHost = serverconfig.getString("akka.remote.netty.tcp.hostname")
  val serverPort = serverconfig.getString("akka.remote.netty.tcp.port")
  val address = Address("akka.tcp", "MarsExpedition", serverHost, serverPort.toInt)
  Cluster(system).join(address)

  system.actorOf(ServerListener.props(address), name = "serverListener")
  system.actorOf(MarsRover.props, name = identity)

}

class MarsRover extends Actor with ActorLogging {

  import akka.contrib.pattern.DistributedPubSubMediator.Publish
  import Plateau._
  import MarsRover._
  import MarsRoverController._
  import context.dispatcher

  def movementSpeed = 2 seconds

  def turningSpeed = 3 seconds

  val mediator = DistributedPubSubExtension(context.system).mediator

  var roverState = RoverState.CREATED
  var actualRoverPosition: RoverPosition = _
  var lastAction: Action.Value = _
  var marsRoverController: ActorRef = _

  context.become(registration)
  scheduler.scheduleOnce(1 seconds, self, Tick)

  def scheduler = context.system.scheduler

  def registration: Receive = {
    case Tick =>
      if (roverState != RoverState.REGISTERED) {
        log.info("Publishing registration!")
        mediator ! Publish("registration", RegisterRover(self))
        scheduler.scheduleOnce(1 seconds, self, Tick)
      }
    case RoverRegistered =>
      log.info("Successful registration!")
      context become receive
      roverState = RoverState.REGISTERED
  }

  def receive = {
    case DeployRover(roverPosition: RoverPosition) =>
      actualRoverPosition = roverPosition
      log.info(s"Mars rover is approaching to $roverPosition")
      publishPosition
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
      publishPosition
    case Collusion =>
      log.info(s"Mars rover has broken down")

      marsRoverController ! Position(actualRoverPosition)
      self ! PoisonPill
    case GotLost =>
      log.info(s"Mars rover got lost!")

      //marsRoverController ! Position(actualRoverPosition)
      self ! PoisonPill
    case Ack =>
      if (roverState.equals(RoverState.REGISTERED)) {
        roverState = RoverState.DEPLOYED
        marsRoverController ! RoverDeployed
      } else {
        roverState = RoverState.READY
        log.info(s"Mars rover is waiting for the next action")
        marsRoverController ! Position(actualRoverPosition)
      }

  }

  private def publishPosition = mediator ! Publish("position", Position(actualRoverPosition, self))

  case object EndOfMovement

  case object Tick

}

object RoverState extends Enumeration {
  type RoverState = Value
  val CREATED, REGISTERED, DEPLOYED, READY, MOVING = Value
}

