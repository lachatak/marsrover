package org.kaloz.excercise.marsrover.api

import akka.actor.ActorRef

object Action extends Enumeration {
  type Action = Value
  val L, R, M = Value
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

case class RoverPosition(x: Integer, y: Integer, facing: Facing.Value) {
  def doAction(action: Action.Value) = action match {
    case Action.L => copy(x, y, facing.turnLeft)
    case Action.R => copy(x, y, facing.turnRight)
    case Action.M => copy(x + facing.moveX, y + facing.moveY, facing)
  }
}

case class DisplayState(state: Map[String, List[RoverPosition]] = Map.empty) {
  def update(event: PositionChangedEvent) = copy(state + (event.marsRover -> (event.roverPosition :: state.getOrElse(event.marsRover, List.empty))))
}

case class DisplayFinalPositions(displayState: DisplayState)

case object FinalPositionsDisplayed

case class PositionChangedEvent(marsRover: String, roverPosition: RoverPosition)

case class PlateauConfiguration(x: Integer, y: Integer)

case class RegisterDisplay(display: ActorRef)

case class DisplayRegistered(plateauConfiguration: PlateauConfiguration)