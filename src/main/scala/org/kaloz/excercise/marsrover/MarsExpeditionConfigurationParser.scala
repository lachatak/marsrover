package org.kaloz.excercise.marsrover

import scala.util.parsing.combinator._
import scala.util.Random

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

  def random = "X" ^^ {
    case a => Action.apply(new Random().nextInt(Action.values.size))
  }

  def roverConfiguration = roverStartPosition ~ eol ~ rep(action | random) <~ opt(eol) ^^ {
    case roverStartPosition ~ e ~ actions => RoverConfiguration(roverStartPosition, actions)
  }

  def marsExpeditionConfiguration = plateauConfiguration ~ eol ~ rep(roverConfiguration) ^^ {
    case plateauSize ~ e ~ definitions => MarsExpeditionConfiguration(plateauSize, definitions)
  }
}

object Facing extends Enumeration with Serializable {
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

case class PlateauConfiguration(x: Integer, y: Integer)

case class RoverPosition(x: Integer, y: Integer, facing: Facing.Value) {
  def doAction(action: Action.Value) = action match {
    case Action.L => copy(x, y, facing.turnLeft)
    case Action.R => copy(x, y, facing.turnRight)
    case Action.M => copy(x + facing.moveX, y + facing.moveY, facing)
  }
}

case class RoverConfiguration(roverPosition: RoverPosition, actions: List[Action.Value])

case class MarsExpeditionConfiguration(definePlateau: PlateauConfiguration, roverConfigurations: List[RoverConfiguration])
