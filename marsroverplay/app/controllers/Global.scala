package controllers

import akka.actor.{ActorRef, ActorSystem, Address}
import akka.cluster.Cluster
import com.typesafe.config.ConfigFactory
import controllers.actor.ClientRequestHandlerActor
import play.api.{Application, GlobalSettings}

object Global extends GlobalSettings {

  var clientRequestHandler: ActorRef = _

  override def onStart(application: Application) {

    val system = ActorSystem("MarsExpedition", ConfigFactory.load.getConfig("display"))

    val serverconfig = ConfigFactory.load.getConfig("headquarter")
    val serverHost = serverconfig.getString("akka.remote.netty.tcp.hostname")
    val serverPort = serverconfig.getString("akka.remote.netty.tcp.port")
    val address = Address("akka.tcp", "MarsExpedition", serverHost, serverPort.toInt)

    Cluster(system).join(address)

    clientRequestHandler = system.actorOf(ClientRequestHandlerActor.props, name="ClientRequestHandlerActor")
  }
}

