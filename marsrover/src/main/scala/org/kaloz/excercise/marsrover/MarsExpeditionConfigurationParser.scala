package org.kaloz.excercise.marsrover

import org.kaloz.excercise.marsrover.api.{PlateauConfiguration, Action, Facing, RoverPosition}

import scala.util.Random
import scala.util.parsing.combinator._

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

case class RoverConfiguration(roverPosition: RoverPosition, actions: List[Action.Value])

case class MarsExpeditionConfiguration(definePlateau: PlateauConfiguration, roverConfigurations: List[RoverConfiguration])
