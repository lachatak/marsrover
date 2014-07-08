package controllers.actor

import akka.actor._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import controllers.actor.ClientRequestHandlerActor.RegisterWebSocket
import org.kaloz.excercise.marsrover.api._

object ClientRequestHandlerActor {
  def props = Props(classOf[ClientRequestHandlerActor])

  case class RegisterWebSocket(webSocketChannelActor: ActorRef)

}

class ClientRequestHandlerActor extends Actor with ActorLogging with Stash {

  import context.dispatcher

import scala.concurrent.duration._

  val mediator = DistributedPubSubExtension(context.system).mediator

  context.become(registration)
  scheduler.scheduleOnce(1 seconds, self, Tick)

  def scheduler = context.system.scheduler

  def registration: Receive = {
    case Tick =>
      log.info("Publishing display registration!")
      mediator ! Publish("remoteDisplay", RegisterDisplay(self))
      scheduler.scheduleOnce(1 seconds, self, Tick)
    case DisplayRegistered(plateauConfiguration) =>
      log.info("Successful registration!")
      context become handleRequests(plateauConfiguration)
      unstashAll()
    case RegisterWebSocket(webSocketChannel) => stash()
  }

  def handleRequests(plateauConfiguration: PlateauConfiguration, websocketChannels: List[ActorRef] = List.empty): Receive = {
    case RegisterWebSocket(webSocketChannel) =>
      log.info("new context is {}", (webSocketChannel :: websocketChannels))
      context.become(handleRequests(plateauConfiguration, webSocketChannel :: websocketChannels))
      webSocketChannel ! plateauConfiguration
    case displayState@DisplayState(_) =>
      log.info("position has arrived {}", displayState)
      websocketChannels.foreach(_ ! displayState)
    case position@DisplayFinalPositions(_) =>
      log.info("final position has arrived {}", position)
      websocketChannels.foreach(_ ! position)
      sender ! FinalPositionsDisplayed
  }

  case object Tick

  def receive: Receive = {
    case _ =>
  }
}
