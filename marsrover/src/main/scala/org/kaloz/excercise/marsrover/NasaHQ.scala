package org.kaloz.excercise.marsrover

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{Subscribe, SubscribeAck}

class NasaHQ(configuration: MarsExpeditionConfiguration)(implicit actorFactory: (ActorRefFactory, Props, String) => ActorRef) extends Actor with ActorLogging {

  import org.kaloz.excercise.marsrover.Display._
  import org.kaloz.excercise.marsrover.MarsRoverController._
  import org.kaloz.excercise.marsrover.NasaHQ._

  val display = actorFactory(context, Display.props(configuration.definePlateau), "Display")
  val mediator = DistributedPubSubExtension(context.system).mediator

  var controllers = List.empty[ActorRef]
  var disaster = false

  context.become(startUp())
  mediator ! Subscribe("registration", self)

  def startUp(marsRovers: List[ActorRef] = List.empty): Receive = {
    case SubscribeAck(Subscribe("registration", None, self)) =>
      log.info("Subscribed to registration topic!")
      log.info("Waiting rovers to register!")
    case RegisterRover(marsRover) =>
      handleRoverRegistration(marsRover :: marsRovers)
  }

  def handleRoverRegistration(marsRovers: List[ActorRef]) {
    marsRovers.head ! RoverRegistered

    if (marsRovers.size == configuration.roverConfigurations.size) {
      log.info("Expedition is ready to be kicked!")
      startRovers(marsRovers.zip(configuration.roverConfigurations))
    } else {
      log.info(s"Expedition still needs ${configuration.roverConfigurations.size - marsRovers.size} mars rovers!")
      context.become(startUp(marsRovers))
    }
  }

  def startRovers(components: List[Pair[ActorRef, RoverConfiguration]]) {
    context.become(receive)

    components.zipWithIndex.foreach {
      case Pair(Pair(rover, configuration), index) =>
        val marsRoverController = actorFactory(context, MarsRoverController.props(configuration, rover, display), s"marsRoverController-$index")
        context.watch(marsRoverController)
        controllers = marsRoverController :: controllers
        marsRoverController ! StartRover
    }
  }

  def receive = {
    case Disaster(marsRover) =>
      log.info(s"Disaster happaned with ${marsRover.path.name}! May be broken down or got lost!")
      disaster = true
    case Terminated(marsRoverController) =>
      controllers = controllers.filter(c => c != marsRoverController)
      if (controllers.isEmpty) {
        display ! ShowPositions
        log.info(s"Nasa expedition has been finished ${if (disaster) "with disaster" else "successfully"}!")
      }
    case PositionsDisplayed =>
      context.parent ! PoisonPill
  }
}

object NasaHQ {

  def props(configuration: MarsExpeditionConfiguration, actorFactory: (ActorRefFactory, Props, String) => ActorRef): Props = Props(classOf[NasaHQ], configuration, actorFactory)

  case class StartExpedition(roverConfigurations: List[RoverConfiguration])

  case class RegisterRover(marsRover: ActorRef)

  case object RoverRegistered

}