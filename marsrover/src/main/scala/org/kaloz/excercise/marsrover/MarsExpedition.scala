package org.kaloz.excercise.marsrover

import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.cluster.Cluster

object MarsExpedition extends MarsExpeditionConfigurationParser {

  def main(args: Array[String]) {

    val input = scala.io.Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(if (args.length == 1) args(0) else "input.txt")).mkString
    parseAll(marsExpeditionConfiguration, input) match {
      case Success(mec, _) => new MarsExpedition(mec)
      case x => println(x)
    }
  }
}

class MarsExpedition(configuration: MarsExpeditionConfiguration) {

  val system = ActorSystem("MarsExpedition", ConfigFactory.load.getConfig("headquarter"))
  val joinAddress = Cluster(system).selfAddress
  Cluster(system).join(joinAddress)

  val plateau = system.actorOf(Plateau.props(configuration.definePlateau), name = "plateau")
  val nasaHQ = system.actorOf(NasaHQ.props(configuration, (actorFactory: ActorRefFactory, props: Props, name: String) => actorFactory.actorOf(props, name)), name = "NasaHQ")

}
