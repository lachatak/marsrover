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
      case Success(mec, _) => MarsExpedition(mec).startExpedition
      case x => println(x)
    }
  }

  def apply(configuration: MarsExpeditionConfiguration) = new MarsExpedition(configuration)
}

class MarsExpedition(val configuration: MarsExpeditionConfiguration) {

  val system = ActorSystem("MarsExpedition")
  val plateau = system.actorOf(Props(classOf[Plateau], configuration.definePlateau), name = "plateau")
  val NasaHQ = system.actorOf(Props[NasaHQ], name="NasaHQ")

  def startExpedition {
    NasaHQ ! StartExpedition(configuration.roverConfigurations)
  }
}

class NasaHQ extends Actor with ActorLogging {

  var marsRovers = Map.empty[ActorRef, ActorRef]

  def receive = {
    case StartExpedition(roverConfigurations) =>
      log.info(s"$self Nasa expedition has started with rover configuration $roverConfigurations")
      deployMarsRovers(roverConfigurations)
    case RoverDeployed =>
      log.info(s"$sender mars rover has been deployed, start sending actions")
      marsRovers.get(sender).get ! StartRover
  }

  private def deployMarsRovers(roverConfigurations: List[RoverConfiguration]) {
    roverConfigurations.foreach(rc => {
      val marsRover = context.actorOf(Props(classOf[MarsRover], rc.roverPosition))
      log.info(s"$self Nasa deploys $marsRover")
      marsRover ! DeployRover
      marsRovers += (marsRover -> context.actorOf(Props(classOf[MarsRoverController], rc, marsRover)))
    })
  }
}

class MarsRoverController(val roverConfiguration: RoverConfiguration, marsRover: ActorRef) extends Actor with ActorLogging {

  var lastKnownPosition = roverConfiguration.roverPosition
  var actions = roverConfiguration.actions

  def receive = {
    case StartRover =>
      log.info(s"$self controller starts rover $marsRover")
      sendNextAction
    case Position(roverPosition) =>
      lastKnownPosition = roverPosition
      log.info(s"$self controller acknowledged position $roverPosition to $marsRover")
      sendNextAction
  }

  private def sendNextAction {
    if (!actions.isEmpty) {
      log.info(s"$self controller send action ${actions.head} to $marsRover")
      marsRover ! RoverAction(actions.head)
      actions = actions.tail
    }
  }
}

trait Configuration {
  def movementSpeed = 300 millis

  def turningSpeed = 1 second
}

class MarsRover(var roverPosition: RoverPosition) extends Actor with Configuration with ActorLogging {

  import context.dispatcher

  var lastAction: Action.Value = _
  var roverState = RoverState.UNDER_DEPLOYMENT
  var controller: ActorRef = _
  val plateau: ActorSelection = context.actorSelection("../../plateau")

  def receive = {
    case DeployRover =>
      log.info(s"$self mars rover has been deployed at $roverPosition")
      controller = sender
      plateau ! Position(roverPosition)
    case RoverAction(action) =>
      if (!roverState.equals(RoverState.BROKEN)) {
        log.info(s"$self mars rover is moving to ${roverPosition.doAction(action)} with action $action")
        controller = sender
        lastAction = action
        roverState = RoverState.MOVING
        context.system.scheduler.scheduleOnce(lastAction match {
          case Action.L | Action.R => turningSpeed
          case Action.M => movementSpeed
        }, self, EndOfMovement)
      }
    case EndOfMovement =>
      roverPosition = roverPosition.doAction(lastAction)
      log.info(s"$self mars rover has arrived to $roverPosition with action $lastAction")
      plateau ! Position(roverPosition)
    case Collusion =>
      roverState = RoverState.BROKEN
      log.info(s"$self mars rover has broken down")
    case Ack =>
      if (roverState.equals(RoverState.UNDER_DEPLOYMENT)) {
        roverState = RoverState.READY
        controller ! RoverDeployed
      } else {
        roverState = RoverState.READY
        log.info(s"$self mars rover is waiting for the next action")
        controller ! Position(roverPosition)
      }
  }

  object RoverState extends Enumeration {
    type RoverState = Value
    val UNDER_DEPLOYMENT, READY, MOVING, BROKEN = Value
  }

  case object EndOfMovement

}

class Plateau(val plateuaConfigarutaion: PlateauConfiguration) extends Actor with ActorLogging {

  var roverPositions = Map.empty[ActorRef, RoverPosition]

  def receive = {
    case Position(position) =>
      if (roverPositions.values.exists(_ == position)) {
        log.info(s"$sender mars rover has collided at $position")
        sender ! Collusion
      } else {
        roverPositions += (sender -> position)
        log.info(s"$sender mars rover has moved to $position")
        sender ! Ack
      }
  }
}

case class StartExpedition(val roverConfigurations: List[RoverConfiguration])

case object DeployRover

case object RoverDeployed

case object StartRover

case class RoverAction(val action: Action.Value)

case class Position(val position: RoverPosition)

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
      case N => 1
      case S => -1
      case W => 0
      case E => 0
    }

    def moveY = facing match {
      case N => 0
      case S => 0
      case W => -1
      case E => 1
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

object TestLoopParser extends MarsExpeditionConfigurationParser with App {
  val input = """|5 5
                |1 2 N
                |LMLMLMLMM
                |3 3 E
                |MMRMMRMRRM""".stripMargin

  parseAll(marsExpeditionConfiguration, input) match {
    case Success(lup, _) => println(lup)
    case x => println(x)
  }
}
