package org.kaloz.excercise.marsrover

import akka.actor._

class NasaHQ extends Actor with ActorLogging {

  this:DisplayProvider =>

  import NasaHQ._
  import MarsRoverController._
  import Display._

//  val display = context.actorOf(Props[Display], name = "Display")

  var controllers = List.empty[ActorRef]
  var disaster = false

  def receive = {
    case StartExpedition(roverConfigurations) =>
      log.info(s"Nasa expedition has started with rover configuration $roverConfigurations")
      deployMarsRovers(roverConfigurations)
    case Disaster(marsRover) =>
      log.info(s"Disaster happaned with ${marsRover.path.name}! May be broken down or got lost!")
      disaster = true
    case Terminated(marsRoverController) =>
      controllers = controllers.filter(c => c != marsRoverController)
      if (controllers.isEmpty) {
        display ! ShowPositions
        context.parent ! PoisonPill
        log.info(s"Nasa expedition has been finished ${if (disaster) "with disaster" else "successfully"}!")
      }
  }

  private def deployMarsRovers(roverConfigurations: List[RoverConfiguration]) {
    var count = 0
    roverConfigurations.foreach(rc => {
      count = count + 1
      val marsRoverController = context.actorOf(Props(classOf[MarsRoverController], rc, display), name = s"marsRoverController-$count")
      context.watch(marsRoverController)
      controllers = marsRoverController :: controllers
    })
  }
}

object NasaHQ {

  case class StartExpedition(roverConfigurations: List[RoverConfiguration])

}

trait DisplayProvider{

  def display:ActorRef
}

trait ProductionDisplayProvider extends DisplayProvider{
  this:Actor =>

  val display = context.actorOf(Props[Display], name = "Display")
}