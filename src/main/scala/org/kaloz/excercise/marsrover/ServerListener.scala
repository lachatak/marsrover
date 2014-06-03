package org.kaloz.excercise.marsrover

import akka.actor._
import akka.cluster.Cluster

object ServerListener {
  def props(address: Address): Props = Props(classOf[ServerListener], address)
}

class ServerListener(address: Address) extends Actor with ActorLogging {

  import akka.cluster.ClusterEvent.UnreachableMember

  val cluster = Cluster(context.system)

  override def preStart(): Unit =
    cluster.subscribe(self, classOf[UnreachableMember])

  override def postStop(): Unit =
    cluster unsubscribe self

  def receive = {
    case UnreachableMember(member) if (member.address == address) =>
      log.info("Server has stopped. Shooting down client actor system!")
      context.parent ! PoisonPill
  }
}
