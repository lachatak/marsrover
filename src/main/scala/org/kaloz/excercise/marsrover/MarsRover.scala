package org.kaloz.excercise.marsrover

import scala.util.parsing.combinator._
import akka.actor.{ActorRef, Actor}
import scala.concurrent.duration._

object MarsExpedition extends MarsExpeditionConfigurationParser {
  def main(args: Array[String]) {
    val lines = scala.io.Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(if (args.length == 1) args(0) else "input.txt")).mkString
    parseAll(marsExpeditionConfiguration, lines) match {
      case Success(mec, _) => MarsExpedition(mec).playScenario
      case x => println(x)
    }
  }

  def apply(configuration: MarsExpeditionConfiguration) = new MarsExpedition(configuration)
}

class MarsExpedition(val configuration: MarsExpeditionConfiguration) {

  def playScenario {
    println(configuration)
  }
}

trait Configuration {
  def movementSpeed = 300 millis

  def turningSpeed = 1 second
}

class MarsRover(var roverPosition: RoverPosition, val controller: ActorRef, val plateau: ActorRef) extends Actor with Configuration {

  import context.dispatcher

  var lastAction: Action.Value = _
  var roverState = RoverState.READY

  def receive = {
    case Command(action) =>
      if (!roverState.equals(RoverState.BROKEN)) {
        lastAction = action
        roverState = RoverState.MOVING
        context.system.scheduler.scheduleOnce(lastAction match {
          case Action.L | Action.R => turningSpeed
          case Action.M => movementSpeed
        }, self, EndOfMovement)
      }
    case EndOfMovement =>
      roverPosition = roverPosition.doAction(lastAction)
      plateau ! Position(roverPosition)
    case Collusion => roverState = RoverState.BROKEN
    case Ack =>
      roverState = RoverState.READY
      controller ! Ready
    case GetPosition =>
      if (!roverState.equals(RoverState.BROKEN)) {
        controller ! Position(roverPosition)
      }
  }

  object RoverState extends Enumeration {
    type RoverState = Value
    val READY, MOVING, BROKEN = Value
  }

  case object EndOfMovement

}

class Plateau(val plateuaConfigarutaion: PlateauConfiguration) extends Actor {

  var roverPositions = List.empty[RoverPosition]

  def receive = {
    case Position(position) =>
      if (roverPositions.exists(_ == position)) {
        sender ! Collusion
      } else {
        roverPositions = position :: roverPositions
        sender ! Ack
      }
  }
}

case class Command(val action: Action.Value)

case object Ready

case object GetPosition

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

case class MarsExpeditionConfiguration(val definePlateau: PlateauConfiguration, val roverDefinition: List[RoverConfiguration])

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
