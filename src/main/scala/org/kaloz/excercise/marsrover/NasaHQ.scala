package org.kaloz.excercise.marsrover

import akka.actor._

class NasaHQ(implicit actorFactory: (ActorRefFactory, Props, String) => ActorRef) extends Actor with ActorLogging {

  import NasaHQ._
  import MarsRoverController._
  import Display._

  val display = actorFactory(context, Display.props, "Display")

  var controllers = List.empty[ActorRef]
  var disaster = false

  context.become(idle)

  def idle: Receive = {
    case StartExpedition(roverConfigurations) =>
      log.info(s"Nasa expedition has started with rover configuration $roverConfigurations")
      deployMarsRovers(roverConfigurations)
      context.become(receive)
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

  private def deployMarsRovers(roverConfigurations: List[RoverConfiguration]) {
    var count = 0
    roverConfigurations.foreach(rc => {
      count = count + 1
      val marsRoverController = actorFactory(context, MarsRoverController.props(rc, display), s"marsRoverController-$count")
      context.watch(marsRoverController)
      controllers = marsRoverController :: controllers
    })
  }
}

object NasaHQ {

  def props(actorFactory: (ActorRefFactory, Props, String) => ActorRef): Props = Props(classOf[NasaHQ], actorFactory)

  case class StartExpedition(roverConfigurations: List[RoverConfiguration])

}