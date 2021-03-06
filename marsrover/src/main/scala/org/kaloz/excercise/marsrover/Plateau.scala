package org.kaloz.excercise.marsrover

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.SubscribeAck
import org.kaloz.excercise.marsrover.api.{PlateauConfiguration, RoverPosition}

class Plateau(plateauConfigarutaion: PlateauConfiguration) extends Actor with ActorLogging {

  import akka.contrib.pattern.DistributedPubSubMediator.Subscribe
  import org.kaloz.excercise.marsrover.MarsRover._
  import org.kaloz.excercise.marsrover.Plateau._

  val mediator = DistributedPubSubExtension(context.system).mediator

  var roverPositions = Map.empty[ActorRef, RoverPosition]

  mediator ! Subscribe("position", self)

  def receive = {
    case SubscribeAck(Subscribe("position", None, self)) => log.info("Subscribed to position topic")
    case Position(roverPosition, publisher) =>
      roverPositions += (publisher -> roverPosition)
      if (roverPositions.values.filter(_ == roverPosition).size > 1) {
        roverPositions.foreach {
          case (marsRover: ActorRef, currentPosition: RoverPosition) if (currentPosition == roverPosition) =>
            log.info(s"${marsRover.path.name} has collided at $currentPosition")
            marsRover ! Collusion
          case _ =>
        }
      } else if (getLost(roverPosition)) {
        log.info(s"${publisher.path.name} got lost at $roverPosition")
        publisher ! GotLost
      } else {
        log.info(s"${publisher.path.name} position at $roverPosition is safe")
        publisher ! Ack
      }
  }

  private def getLost(roverPosition: RoverPosition) =
    if (roverPosition.x < 0 || roverPosition.x > plateauConfigarutaion.x || roverPosition.y < 0 || roverPosition.y > plateauConfigarutaion.y) true
    else false
}

object Plateau {

  def props(plateauConfiguration: PlateauConfiguration): Props = Props(classOf[Plateau], plateauConfiguration)

  case object Collusion

  case object Ack

  case object GotLost

}
