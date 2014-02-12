package org.kaloz.excercise.marsrover

import scala.util.parsing.combinator._
import akka.actor._
import scala.concurrent.duration._

object MarsExpedition extends MarsExpeditionConfigurationParser {

  //  val defaultInput = """|5 5
  //                |1 2 N
  //                |LMLMLMLMM
  //                |3 3 E
  //                |MMRMMRMRRM""".stripMargin

  def main(args: Array[String]) {
    //    val input = if (args.length == 1) scala.io.Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(args(0))).mkString else defaultInput
    val input = scala.io.Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(if (args.length == 1) args(0) else "input.txt")).mkString
    parseAll(marsExpeditionConfiguration, input) match {
      case Success(mec, _) => new MarsExpedition(mec).startExpedition
      case x => println(x)
    }
  }
}

class MarsExpedition(val configuration: MarsExpeditionConfiguration) {

  val system = ActorSystem("MarsExpedition")
  val plateau = system.actorOf(Props(classOf[Plateau], configuration.definePlateau), name = "plateau")
  val NasaHQ = system.actorOf(Props[NasaHQ], name = "NasaHQ")

  def startExpedition {
    NasaHQ ! StartExpedition(configuration.roverConfigurations)
  }
}

class NasaHQ extends Actor with ActorLogging {

  var marsRoverToController = Map.empty[ActorRef, ActorRef]
  var controllerToMarsRover = Map.empty[ActorRef, ActorRef]

  val display = context.actorOf(Props[Display], name = "Display")
  var disaster = false

  def receive = {
    case StartExpedition(roverConfigurations) =>
      log.info(s"Nasa expedition has started with rover configuration $roverConfigurations")
      deployMarsRovers(roverConfigurations)
    case RoverDeployed =>
      log.info(s"${sender.path.name} has been deployed, sending start action via its Controller")
      marsRoverToController.get(sender).get ! StartRover
    case Disaster =>
      log.info(s"Disaster happaned with ${controllerToMarsRover.get(sender).get.path.name}!")
      disaster = true
    case Terminated(marsRoverController) =>
      controllerToMarsRover = controllerToMarsRover - marsRoverController
      if (controllerToMarsRover.isEmpty) {
        display ! ShowPositions
        context.parent ! PoisonPill
        log.info(s"Nasa expedition has been finished ${ if(disaster) "with disaster" else "successfully"}!")
      }
  }

  private def deployMarsRovers(roverConfigurations: List[RoverConfiguration]) {
    var count = 0
    roverConfigurations.foreach(rc => {
      count = count + 1
      val marsRover = context.actorOf(Props(classOf[MarsRover], rc.roverPosition), name = s"marsRover-$count")
      log.info(s"Nasa deploys ${marsRover.path.name} to ${rc.roverPosition}")
      val marsRoverController = context.actorOf(Props(classOf[MarsRoverController], rc.actions, marsRover, display), name = s"marsRoverController-$count")
      context.watch(marsRoverController)
      log.info(s"Nasa start ${marsRoverController.path.name} for ${marsRover.path.name}")
      marsRoverToController += (marsRover -> marsRoverController)
      marsRover ! DeployRover
    })
    controllerToMarsRover = marsRoverToController map {
      _.swap
    }
  }
}

case class StartExpedition(val roverConfigurations: List[RoverConfiguration])

case object DeployRover

case object RoverDeployed

case object StartRover

class Display extends Actor with ActorLogging {

  var roverPositions = Map.empty[ActorRef, RoverPosition]

  def receive = {
    case RegisterPosition(marsRover, roverPosition) =>
      log.info(s"Register $roverPosition to ${marsRover.path.name}")
      roverPositions += (marsRover -> roverPosition)
    case ShowPositions =>
      roverPositions.foreach {
        case (marsRover, position) => log.info(s"Final position of ${marsRover.path.name} is $position")
      }
  }
}

case class RegisterPosition(marsRover: ActorRef, roverPosition: RoverPosition)

case object ShowPositions

class MarsRoverController(var roverActions: List[Action.Value], val marsRover: ActorRef, val display: ActorRef) extends Actor with ActorLogging {

  context.watch(marsRover)

  def receive = {
    case StartRover =>
      log.info(s"Controller starts ${marsRover.path.name}")
      sendNextAction
    case Position(roverPosition) =>
      log.info(s"Controller acknowledged $roverPosition from ${marsRover.path.name}")
      display ! RegisterPosition(marsRover, roverPosition)
      sendNextAction
    case Terminated(marsRover) =>
      log.info(s"TERMINATED ${marsRover.path.name}")
      context.parent ! Disaster
      self ! PoisonPill
  }

  private def sendNextAction {
    if (!roverActions.isEmpty) {
      log.info(s"Controller send action ${roverActions.head} to ${marsRover.path.name}")
      marsRover ! RoverAction(roverActions.head)
      roverActions = roverActions.tail
    } else {
      log.info(s"No more actions. Controller is stopping")
      self ! PoisonPill
    }
  }
}

case class RoverAction(val action: Action.Value)
case object Disaster

trait Configuration {
  def movementSpeed = 100 millis

  def turningSpeed = 100 millis
}

class MarsRover(var roverPosition: RoverPosition) extends Actor with Configuration with ActorLogging {

  import context.dispatcher

  var lastAction: Action.Value = _
  var roverState = RoverState.UNDER_DEPLOYMENT
  var controller: ActorRef = _
  val plateau: ActorSelection = context.actorSelection("../../plateau")

  def receive = {
    case DeployRover =>
      log.info(s"Mars rover is approaching to $roverPosition")
      controller = sender
      plateau ! Position(roverPosition)
    case RoverAction(action) =>
      log.info(s"Mars rover is moving to ${roverPosition.doAction(action)} with action $action")
      controller = sender
      lastAction = action
      roverState = RoverState.MOVING
      context.system.scheduler.scheduleOnce(lastAction match {
        case Action.L | Action.R => turningSpeed
        case Action.M => movementSpeed
      }, self, EndOfMovement)
    case EndOfMovement =>
      roverPosition = roverPosition.doAction(lastAction)
      log.info(s"Mars rover has arrived to $roverPosition with action $lastAction")
      plateau ! Position(roverPosition)
    case Collusion =>
      log.info(s"Mars rover has broken down")
      self ! PoisonPill
    case Ack =>
      if (roverState.equals(RoverState.UNDER_DEPLOYMENT)) {
        roverState = RoverState.READY
        controller ! RoverDeployed
      } else {
        roverState = RoverState.READY
        log.info(s"Mars rover is waiting for the next action")
        controller ! Position(roverPosition)
      }
  }

  object RoverState extends Enumeration {
    type RoverState = Value
    val UNDER_DEPLOYMENT, READY, MOVING = Value
  }

  case object EndOfMovement

}

case class Position(val position: RoverPosition)

class Plateau(val plateuaConfigarutaion: PlateauConfiguration) extends Actor with ActorLogging {

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
      } else {
        log.info(s"${sender.path.name} position at $roverPosition is safe")
        sender ! Ack
      }
  }
}

case object Collusion

case object Ack

class MarsExpeditionConfigurationParser extends JavaTokenParsers {
  override val whiteSpace = """[ \t]+""".r

  val eol = """[\r?\n]+""".r

  def integer = decimalNumber ^^ {
    case x => x.toInt
  }

  def plateauConfiguration = integer ~ integer ^^ {
    case x ~ y => PlateauConfiguration(x, y)
  }

  def facing = ("N" | "S" | "W" | "E") ^^ {
    case f => Facing.withName(f)
  }

  def roverStartPosition = integer ~ integer ~ facing ^^ {
    case x ~ y ~ facing => RoverPosition(x, y, facing)
  }

  def action = ("L" | "R" | "M") ^^ {
    case a => Action.withName(a)
  }

  def roverConfiguration = roverStartPosition ~ eol ~ rep(action) <~ opt(eol) ^^ {
    case roverStartPosition ~ e ~ actions => RoverConfiguration(roverStartPosition, actions)
  }

  def marsExpeditionConfiguration = plateauConfiguration ~ eol ~ rep(roverConfiguration) ^^ {
    case plateauSize ~ e ~ definitions => MarsExpeditionConfiguration(plateauSize, definitions)
  }
}

object Facing extends Enumeration {
  type Facing = Value
  val N, S, W, E = Value

  class FacingValue(facing: Value) {
    def turnLeft = facing match {
      case N => W
      case S => E
      case W => S
      case E => N
    }

    def turnRight = facing match {
      case N => E
      case S => W
      case W => N
      case E => S
    }

    def moveX = facing match {
      case N => 0
      case S => 0
      case W => -1
      case E => 1
    }

    def moveY = facing match {
      case N => 1
      case S => -1
      case W => 0
      case E => 0
    }
  }

  implicit def value2FacingValue(facing: Value) = new FacingValue(facing)
}

object Action extends Enumeration {
  type Action = Value
  val L, R, M = Value
}

case class PlateauConfiguration(val x: Integer, val y: Integer)

case class RoverPosition(val x: Integer, val y: Integer, val facing: Facing.Value) {
  def doAction(action: Action.Value) = action match {
    case Action.L => new RoverPosition(x, y, facing.turnLeft)
    case Action.R => new RoverPosition(x, y, facing.turnRight)
    case Action.M => new RoverPosition(x + facing.moveX, y + facing.moveY, facing)
  }

  override def equals(other: Any) = other match {
    case that: RoverPosition => this.x == that.x && this.y == that.y && this.facing == that.facing
    case _ => false
  }
}

case class RoverConfiguration(val roverPosition: RoverPosition, val actions: List[Action.Value])

case class MarsExpeditionConfiguration(val definePlateau: PlateauConfiguration, val roverConfigurations: List[RoverConfiguration])